param(
    [string]$BaseUrl = "http://127.0.0.1:8000",
    [string]$Password = "123456"
)

$ErrorActionPreference = "Stop"

function Invoke-OfficeApi {
    param(
        [ValidateSet("Get", "Post")]
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

function Invoke-ExpectedBusinessFailure {
    param(
        [ValidateSet("Post")]
        [string]$Method,
        [string]$Uri,
        [hashtable]$Headers,
        [object]$Body
    )

    try {
        $response = Invoke-OfficeApi -Method $Method -Uri $Uri -Headers $Headers -Body $Body
        throw "Expected business failure, got success: $($response | ConvertTo-Json -Depth 10)"
    } catch {
        if ($_.Exception.Message -like "Request failed:*") {
            return
        }
        throw
    }
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
    return $response.data
}

function AuthHeaders {
    param([string]$Token)
    return @{ Authorization = "Bearer $Token" }
}

$employee = Login "employee"
$manager = Login "manager"
$finance = Login "finance"
Write-Host "login OK: employee, manager, finance"

$title = "Gateway smoke expense $(Get-Date -Format 'yyyyMMddHHmmss')"
$created = Invoke-OfficeApi `
    -Method Post `
    -Uri "$BaseUrl/api/approvals" `
    -Headers (AuthHeaders $employee.token) `
    -Body @{
        approvalType = "EXPENSE"
        title = $title
        content = "runtime smoke high expense"
        amount = 1280.50
    }

$formId = [long]$created.data.id
if ($created.data.status -ne "DRAFT") {
    throw "Expected created approval status DRAFT, got $($created.data.status)"
}
Write-Host "created high expense approval: id=$formId"

$submitted = Invoke-OfficeApi `
    -Method Post `
    -Uri "$BaseUrl/api/approvals/$formId/submit" `
    -Headers (AuthHeaders $employee.token)

if ($submitted.data.status -ne "PENDING") {
    throw "Expected submitted approval status PENDING, got $($submitted.data.status)"
}
Write-Host "submitted approval: id=$formId managerApprover=$($submitted.data.currentApproverId)"

$managerApproved = Invoke-OfficeApi `
    -Method Post `
    -Uri "$BaseUrl/api/approvals/$formId/approve" `
    -Headers (AuthHeaders $manager.token) `
    -Body @{ comment = "manager approved high expense" }

if ($managerApproved.data.status -ne "PROCESSING") {
    throw "Expected manager approval to move to PROCESSING, got $($managerApproved.data.status)"
}
if ($managerApproved.data.currentApproverId -ne $finance.user.id) {
    throw "Expected finance currentApproverId $($finance.user.id), got $($managerApproved.data.currentApproverId)"
}
Write-Host "manager approval OK: nextApprover=$($managerApproved.data.currentApproverId)"

Invoke-ExpectedBusinessFailure `
    -Method Post `
    -Uri "$BaseUrl/api/approvals/$formId/approve" `
    -Headers (AuthHeaders $manager.token) `
    -Body @{ comment = "duplicate manager approval" }
Write-Host "duplicate manager approval rejected OK"

$financeTodos = Invoke-OfficeApi `
    -Method Get `
    -Uri "$BaseUrl/api/messages/todos?current=1&size=10&businessType=APPROVAL&status=PENDING" `
    -Headers (AuthHeaders $finance.token)

$financeTodo = @($financeTodos.data.records | Where-Object { $_.businessId -eq $formId }) | Select-Object -First 1
if (-not $financeTodo) {
    throw "Finance pending approval todo not found for form $formId"
}
Write-Host "finance todo OK: id=$($financeTodo.id)"

$financeApproved = Invoke-OfficeApi `
    -Method Post `
    -Uri "$BaseUrl/api/approvals/$formId/approve" `
    -Headers (AuthHeaders $finance.token) `
    -Body @{ comment = "finance approved high expense" }

if ($financeApproved.data.status -ne "APPROVED") {
    throw "Expected final approval status APPROVED, got $($financeApproved.data.status)"
}
Write-Host "finance approval OK: status=$($financeApproved.data.status)"

$detail = Invoke-OfficeApi `
    -Method Get `
    -Uri "$BaseUrl/api/approvals/$formId" `
    -Headers (AuthHeaders $employee.token)

if ($detail.data.status -ne "APPROVED") {
    throw "Expected detail status APPROVED, got $($detail.data.status)"
}
$recordCount = @($detail.data.records).Count
if ($recordCount -lt 4) {
    throw "Expected at least 4 approval records, got $recordCount"
}
Write-Host "detail OK: status=$($detail.data.status) records=$recordCount"
Write-Host "P1 approval rules/concurrency smoke passed."
