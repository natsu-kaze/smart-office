param(
    [string]$BaseUrl = "http://127.0.0.1:8000",
    [string]$Password = "123456"
)

$ErrorActionPreference = "Stop"

function Invoke-OfficeApi {
    param(
        [ValidateSet("Get", "Post", "Patch", "Put", "Delete")]
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
    param([string]$Username)

    $response = Invoke-OfficeApi `
        -Method Post `
        -Uri "$BaseUrl/api/auth/login" `
        -Body @{ username = $Username; password = $Password }

    if (-not $response.data.token) {
        throw "Login response has no token for user $Username"
    }
    return $response.data.token
}

function AuthHeaders {
    param([string]$Token)
    return @{ Authorization = "Bearer $Token" }
}

$employeeToken = Login "employee"
$managerToken = Login "manager"
Write-Host "login OK: employee, manager"

$title = "Gateway smoke leave $(Get-Date -Format 'yyyyMMddHHmmss')"
$created = Invoke-OfficeApi `
    -Method Post `
    -Uri "$BaseUrl/api/approvals" `
    -Headers (AuthHeaders $employeeToken) `
    -Body @{
        approvalType = "LEAVE"
        title = $title
        content = "runtime smoke approval"
    }

$formId = [long]$created.data.id
if ($created.data.status -ne "DRAFT") {
    throw "Expected created approval status DRAFT, got $($created.data.status)"
}
Write-Host "created approval: id=$formId status=$($created.data.status)"

$submitted = Invoke-OfficeApi `
    -Method Post `
    -Uri "$BaseUrl/api/approvals/$formId/submit" `
    -Headers (AuthHeaders $employeeToken)

if ($submitted.data.status -ne "PENDING") {
    throw "Expected submitted approval status PENDING, got $($submitted.data.status)"
}
Write-Host "submitted approval: id=$formId approver=$($submitted.data.currentApproverId)"

$managerTodos = Invoke-OfficeApi `
    -Method Get `
    -Uri "$BaseUrl/api/messages/todos?current=1&size=10&businessType=APPROVAL&status=PENDING" `
    -Headers (AuthHeaders $managerToken)

$pendingTodo = @($managerTodos.data.records | Where-Object { $_.businessId -eq $formId }) | Select-Object -First 1
if (-not $pendingTodo) {
    throw "Manager pending approval todo not found for form $formId"
}
Write-Host "pending todo: id=$($pendingTodo.id) status=$($pendingTodo.status)"

$approved = Invoke-OfficeApi `
    -Method Post `
    -Uri "$BaseUrl/api/approvals/$formId/approve" `
    -Headers (AuthHeaders $managerToken) `
    -Body @{ comment = "runtime smoke approved" }

if ($approved.data.status -ne "APPROVED") {
    throw "Expected approved status APPROVED, got $($approved.data.status)"
}
Write-Host "approved approval: id=$formId status=$($approved.data.status)"

$managerDoneTodos = Invoke-OfficeApi `
    -Method Get `
    -Uri "$BaseUrl/api/messages/todos?current=1&size=10&businessType=APPROVAL&status=DONE" `
    -Headers (AuthHeaders $managerToken)

$doneTodo = @($managerDoneTodos.data.records | Where-Object { $_.businessId -eq $formId }) | Select-Object -First 1
if (-not $doneTodo) {
    throw "Manager completed approval todo not found for form $formId"
}
Write-Host "completed todo: id=$($doneTodo.id) status=$($doneTodo.status)"

$employeeMessages = Invoke-OfficeApi `
    -Method Get `
    -Uri "$BaseUrl/api/messages?current=1&size=10&businessType=APPROVAL" `
    -Headers (AuthHeaders $employeeToken)

$notice = @($employeeMessages.data.records | Where-Object {
        $_.businessId -eq $formId -and $_.title -eq "Approval passed"
    }) | Select-Object -First 1

if (-not $notice) {
    throw "Employee approval passed notice not found for form $formId"
}
Write-Host "employee notice: id=$($notice.id) title=$($notice.title) read=$($notice.readStatus)"

$detail = Invoke-OfficeApi `
    -Method Get `
    -Uri "$BaseUrl/api/approvals/$formId" `
    -Headers (AuthHeaders $employeeToken)

if ($detail.data.status -ne "APPROVED") {
    throw "Expected detail status APPROVED, got $($detail.data.status)"
}
Write-Host "detail OK: status=$($detail.data.status) records=$(@($detail.data.records).Count)"
Write-Host "P0 approval/message gateway smoke passed."
