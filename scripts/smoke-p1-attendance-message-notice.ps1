param(
    [string]$BaseUrl = "http://127.0.0.1:8000",
    [string]$AttendanceBaseUrl = "http://127.0.0.1:9105",
    [string]$Username = "employee",
    [string]$Password = "123456",
    [string]$SettlementDate = "2026-01-16"
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

function Wait-ForAttendanceNotice {
    param(
        [hashtable]$Headers,
        [string]$Date,
        [object]$RecordId
    )

    for ($i = 1; $i -le 12; $i++) {
        $messages = Invoke-OfficeApi `
            -Method Get `
            -Uri "$BaseUrl/api/messages?current=1&size=20&businessType=ATTENDANCE" `
            -Headers $Headers

        $matches = @($messages.data.records | Where-Object {
                $_.businessId -eq $RecordId -and
                $_.title -eq "Attendance abnormal" -and
                $_.content -like "*$Date*"
            })
        if ($matches.Count -ge 1) {
            return $matches
        }
        Start-Sleep -Seconds 2
    }
    throw "Attendance abnormal notice not found for date=$Date recordId=$RecordId"
}

$token = Login
$headers = @{ Authorization = "Bearer $token" }
Write-Host "login OK: $Username"

$firstSettlement = Invoke-OfficeApi `
    -Method Post `
    -Uri "$AttendanceBaseUrl/internal/attendance/jobs/daily-settlement?date=$SettlementDate"

if ($firstSettlement.data.totalUsers -lt 1) {
    throw "Daily settlement did not process any active users"
}
Write-Host "daily settlement OK: date=$SettlementDate users=$($firstSettlement.data.totalUsers) inserted=$($firstSettlement.data.recordsInserted) updated=$($firstSettlement.data.recordsUpdated)"

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
Write-Host "record OK: id=$($record.id) checkIn=$($record.checkInStatus) checkOut=$($record.checkOutStatus)"

$firstMatches = @(Wait-ForAttendanceNotice -Headers $headers -Date $SettlementDate -RecordId $record.id)
Write-Host "attendance notice OK: count=$($firstMatches.Count)"

$secondSettlement = Invoke-OfficeApi `
    -Method Post `
    -Uri "$AttendanceBaseUrl/internal/attendance/jobs/daily-settlement?date=$SettlementDate"
Write-Host "repeat settlement OK: inserted=$($secondSettlement.data.recordsInserted) updated=$($secondSettlement.data.recordsUpdated)"

$secondMatches = @(Wait-ForAttendanceNotice -Headers $headers -Date $SettlementDate -RecordId $record.id)
if ($secondMatches.Count -ne 1) {
    throw "Expected idempotent single attendance notice, got $($secondMatches.Count)"
}

Write-Host "idempotent notice OK: count=$($secondMatches.Count)"
Write-Host "P1 attendance abnormal notice smoke passed."
