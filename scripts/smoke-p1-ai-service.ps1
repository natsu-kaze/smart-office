param(
    [string]$BaseUrl = "http://127.0.0.1:8000",
    [string]$AiBaseUrl = "http://127.0.0.1:9109",
    [string]$SearchBaseUrl = "http://127.0.0.1:9108",
    [string]$Username = "employee",
    [string]$Password = "123456"
)

$ErrorActionPreference = "Stop"

function Invoke-OfficeApi {
    param(
        [ValidateSet("Get", "Post", "Put", "Delete")]
        [string]$Method,
        [string]$Uri,
        [hashtable]$Headers,
        [object]$Body
    )

    $params = @{
        Method = $Method
        Uri = $Uri
        TimeoutSec = 30
    }
    if ($Headers) {
        $params.Headers = $Headers
    }
    if ($null -ne $Body) {
        $params.ContentType = "application/json"
        $params.Body = ($Body | ConvertTo-Json -Depth 10)
    }

    $response = Invoke-RestMethod @params
    if ($response.code -ne 0) {
        throw "Request failed: $Uri`n$($response | ConvertTo-Json -Depth 10)"
    }
    return $response
}

function Login {
    $response = Invoke-OfficeApi `
        -Method Post `
        -Uri "$BaseUrl/api/auth/login" `
        -Body @{ username = $Username; password = $Password }

    if (-not $response.data.token) {
        throw "Login response has no token"
    }
    if (-not $response.data.user.id) {
        throw "Login response has no user id"
    }
    return @{
        token = $response.data.token
        userId = $response.data.user.id
    }
}

function Assert-ServiceHealth {
    param(
        [string]$Name,
        [string]$Url
    )

    try {
        $response = Invoke-RestMethod -Uri "$Url/actuator/health" -TimeoutSec 10
        if ($response.status -ne "UP") {
            throw "$Name health status is $($response.status)"
        }
        Write-Host "$Name health OK"
    } catch {
        throw "$Name is not reachable at $Url`: $($_.Exception.Message)"
    }
}

Assert-ServiceHealth -Name "ai-service" -Url $AiBaseUrl

$login = Login
$headers = @{ Authorization = "Bearer $($login.token)" }
$searchHeaders = @{ "X-User-Id" = "$($login.userId)" }
Write-Host "login OK: $Username"

$stamp = Get-Date -Format 'yyyyMMddHHmmssfff'
$keyword = "ai-smoke-$stamp"
$policyTitle = "AI Smoke Policy $stamp"
$policyId = $null
$leaveText = -join ([char[]](
    0x6211, 0x660e, 0x5929, 0x4e0b, 0x5348, 0x8bf7, 0x5047,
    0x534a, 0x5929, 0xff0c, 0x53bb, 0x533b, 0x9662, 0x590d, 0x67e5
))

try {
    $created = Invoke-OfficeApi `
        -Method Post `
        -Uri "$SearchBaseUrl/api/policies" `
        -Headers $searchHeaders `
        -Body @{
            title = $policyTitle
            summary = "AI smoke summary $keyword"
            content = "This policy document verifies AI policy QA through search-service with keyword $keyword."
            documentVersion = "v$stamp"
            status = "PUBLISHED"
        }

    $policyId = [long]$created.data.id
    Write-Host "created policy: id=$policyId keyword=$keyword"

    Invoke-OfficeApi `
        -Method Post `
        -Uri "$SearchBaseUrl/internal/search/policies/reindex" | Out-Null
    Write-Host "policy index rebuilt"

    $chat = Invoke-OfficeApi `
        -Method Post `
        -Uri "$BaseUrl/api/ai/chat" `
        -Headers $headers `
        -Body @{ message = "Help me understand reimbursement policy $keyword" }

    if (-not $chat.data.conversationId -or $chat.data.degraded -ne $true) {
        throw "Unexpected chat response: $($chat | ConvertTo-Json -Depth 10)"
    }
    Write-Host "chat OK: conversationId=$($chat.data.conversationId)"

    $policyFound = $false
    for ($i = 1; $i -le 10; $i++) {
        $qa = Invoke-OfficeApi `
            -Method Post `
            -Uri "$BaseUrl/api/ai/policy-qa" `
            -Headers $headers `
            -Body @{ question = $keyword }

        $references = @($qa.data.references)
        if ($references | Where-Object { $_ -like "$policyTitle*" }) {
            $policyFound = $true
            Write-Host "policy QA OK: references=$($references -join '; ')"
            break
        }
        Start-Sleep -Seconds 1
    }
    if (-not $policyFound) {
        throw "Policy QA did not return the created policy reference"
    }

    $summary = Invoke-OfficeApi `
        -Method Post `
        -Uri "$BaseUrl/api/ai/approval-summary" `
        -Headers $headers `
        -Body @{ content = "Employee requests reimbursement for a business trip. Amount is 1280 CNY with invoices attached." }

    if (-not $summary.data -or $summary.data.Length -lt 10) {
        throw "Unexpected summary response: $($summary | ConvertTo-Json -Depth 10)"
    }
    Write-Host "approval summary OK"

    $draft = Invoke-OfficeApi `
        -Method Post `
        -Uri "$BaseUrl/api/ai/approval-draft" `
        -Headers $headers `
        -Body @{ text = $leaveText }

    if ($draft.data.approvalType -ne "LEAVE") {
        throw "Expected LEAVE approval type, got $($draft.data.approvalType)"
    }
    Write-Host "approval draft OK: type=$($draft.data.approvalType)"

    $risk = Invoke-OfficeApi `
        -Method Post `
        -Uri "$BaseUrl/api/ai/approval-risk" `
        -Headers $headers `
        -Body @{ approvalType = "EXPENSE"; amount = 1200; content = "taxi" }

    if ($risk.data.level -ne "MEDIUM" -or @($risk.data.warnings).Count -lt 1 -or $risk.data.degraded -ne $true) {
        throw "Unexpected risk response: $($risk | ConvertTo-Json -Depth 10)"
    }
    Write-Host "approval risk OK: level=$($risk.data.level)"
} finally {
    if ($policyId) {
        Invoke-OfficeApi `
            -Method Delete `
            -Uri "$SearchBaseUrl/api/policies/$policyId" | Out-Null
        Write-Host "cleanup OK: id=$policyId"
    }
}

Write-Host "P1 ai-service smoke passed."
