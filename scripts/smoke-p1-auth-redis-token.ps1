param(
    [string]$BaseUrl = "http://127.0.0.1:8000",
    [string]$Username = "employee",
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

function Invoke-ExpectedUnauthorized {
    param(
        [string]$Uri,
        [hashtable]$Headers
    )

    try {
        Invoke-WebRequest -Method Get -Uri $Uri -Headers $Headers -TimeoutSec 30 -UseBasicParsing | Out-Null
    } catch {
        $statusCode = [int]$_.Exception.Response.StatusCode
        if ($statusCode -eq 401) {
            return
        }
        throw "Expected 401, got HTTP $statusCode"
    }
    throw "Expected 401, but request succeeded"
}

$token = Login
$headers = @{ Authorization = "Bearer $token" }
Write-Host "login OK: $Username"

$me = Invoke-OfficeApi `
    -Method Get `
    -Uri "$BaseUrl/api/auth/me" `
    -Headers $headers

if ($me.data.username -ne $Username) {
    throw "Current user mismatch: expected $Username, got $($me.data.username)"
}
Write-Host "me OK: userId=$($me.data.id) username=$($me.data.username)"

Invoke-OfficeApi `
    -Method Post `
    -Uri "$BaseUrl/api/auth/logout" `
    -Headers $headers | Out-Null

Write-Host "logout OK"

Invoke-ExpectedUnauthorized -Uri "$BaseUrl/api/auth/me" -Headers $headers
Write-Host "old token rejected OK"
Write-Host "P1 auth/Redis token smoke passed."
