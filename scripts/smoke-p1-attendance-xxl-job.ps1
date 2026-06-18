param(
    [string]$BaseUrl = "http://127.0.0.1:8000",
    [string]$AttendanceBaseUrl = "http://127.0.0.1:9105",
    [string]$XxlJobAdminUrl = "http://127.0.0.1:8088/xxl-job-admin",
    [string]$Username = "employee",
    [string]$Password = "123456",
    [string]$SettlementDate = "2026-01-15"
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

function Login {
    $response = Invoke-OfficeApi `
        -Method Post `
        -Uri "$BaseUrl/api/auth/login" `
        -Body @{ username = $Username; password = $Password }

    if (-not $response.data.token) {
        throw "Login response has no token"
    }
    return $response.data.token
}

function Check-XxlJobAdmin {
    try {
        $response = Invoke-WebRequest -Uri $XxlJobAdminUrl -TimeoutSec 10 -UseBasicParsing
        if ($response.StatusCode -lt 200 -or $response.StatusCode -ge 400) {
            throw "Unexpected HTTP status $($response.StatusCode)"
        }
    } catch {
        throw "XXL-JOB Admin is not reachable at $XxlJobAdminUrl`: $($_.Exception.Message)"
    }
}

Check-XxlJobAdmin
Write-Host "XXL-JOB Admin reachable: $XxlJobAdminUrl"

$token = Login
$headers = @{ Authorization = "Bearer $token" }
Write-Host "login OK: $Username"

$settlement = Invoke-OfficeApi `
    -Method Post `
    -Uri "$AttendanceBaseUrl/internal/attendance/jobs/daily-settlement?date=$SettlementDate"

if ($settlement.data.totalUsers -lt 1) {
    throw "Daily settlement did not process any active users"
}
Write-Host "daily settlement OK: date=$SettlementDate users=$($settlement.data.totalUsers) inserted=$($settlement.data.recordsInserted) updated=$($settlement.data.recordsUpdated)"

$month = $SettlementDate.Substring(0, 7)
$summaryJob = Invoke-OfficeApi `
    -Method Post `
    -Uri "$AttendanceBaseUrl/internal/attendance/jobs/monthly-summary?month=$month"

if ($summaryJob.data.summariesUpdated -lt 1) {
    throw "Monthly summary did not update any user summaries"
}
Write-Host "monthly summary job OK: month=$month summaries=$($summaryJob.data.summariesUpdated)"

$records = Invoke-OfficeApi `
    -Method Get `
    -Uri "$BaseUrl/api/attendance/my-records?current=1&size=10&startDate=$SettlementDate&endDate=$SettlementDate" `
    -Headers $headers

$record = @($records.data.records | Where-Object { $_.attendanceDate -eq $SettlementDate }) | Select-Object -First 1
if (-not $record) {
    throw "Employee attendance record not found for $SettlementDate"
}
if ($record.checkInStatus -ne "MISSING" -or $record.checkOutStatus -ne "MISSING") {
    throw "Expected missing punch statuses, got checkIn=$($record.checkInStatus), checkOut=$($record.checkOutStatus)"
}
Write-Host "record OK: date=$SettlementDate checkIn=$($record.checkInStatus) checkOut=$($record.checkOutStatus)"

$summary = Invoke-OfficeApi `
    -Method Get `
    -Uri "$BaseUrl/api/attendance/summary/monthly?month=$month" `
    -Headers $headers

if ($summary.data.missingCount -lt 2) {
    throw "Expected monthly missingCount >= 2, got $($summary.data.missingCount)"
}
Write-Host "summary OK: month=$month missingCount=$($summary.data.missingCount)"
Write-Host "P1 attendance/XXL-JOB smoke passed."
