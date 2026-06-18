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

function Invoke-MultipartUpload {
    param(
        [string]$Uri,
        [string]$Token,
        [string]$FilePath
    )

    Add-Type -AssemblyName System.Net.Http
    $client = [System.Net.Http.HttpClient]::new()
    $content = $null
    $fileStream = $null
    try {
        $client.DefaultRequestHeaders.Authorization =
            [System.Net.Http.Headers.AuthenticationHeaderValue]::new("Bearer", $Token)

        $content = [System.Net.Http.MultipartFormDataContent]::new()
        $fileStream = [System.IO.File]::OpenRead($FilePath)
        $fileContent = [System.Net.Http.StreamContent]::new($fileStream)
        $fileContent.Headers.ContentType =
            [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("text/plain")
        $content.Add($fileContent, "file", [System.IO.Path]::GetFileName($FilePath))
        $content.Add([System.Net.Http.StringContent]::new("FILE"), "businessType")

        $response = $client.PostAsync($Uri, $content).GetAwaiter().GetResult()
        $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        if (-not $response.IsSuccessStatusCode) {
            throw "Upload HTTP $([int]$response.StatusCode): $body"
        }
        $json = $body | ConvertFrom-Json
        if ($json.code -ne 0) {
            throw "Upload failed: $body"
        }
        return $json
    } finally {
        if ($fileStream) { $fileStream.Dispose() }
        if ($content) { $content.Dispose() }
        $client.Dispose()
    }
}

function Invoke-AuthorizedDownload {
    param(
        [string]$Uri,
        [string]$Token,
        [string]$OutputPath
    )

    Add-Type -AssemblyName System.Net.Http
    $client = [System.Net.Http.HttpClient]::new()
    try {
        $client.DefaultRequestHeaders.Authorization =
            [System.Net.Http.Headers.AuthenticationHeaderValue]::new("Bearer", $Token)
        $response = $client.GetAsync($Uri).GetAwaiter().GetResult()
        if (-not $response.IsSuccessStatusCode) {
            $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
            throw "Download HTTP $([int]$response.StatusCode): $body"
        }
        $bytes = $response.Content.ReadAsByteArrayAsync().GetAwaiter().GetResult()
        [System.IO.File]::WriteAllBytes($OutputPath, $bytes)
    } finally {
        $client.Dispose()
    }
}

function Invoke-PlainGetText {
    param([string]$Uri)

    Add-Type -AssemblyName System.Net.Http
    $client = [System.Net.Http.HttpClient]::new()
    try {
        $response = $client.GetAsync($Uri).GetAwaiter().GetResult()
        $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        if (-not $response.IsSuccessStatusCode) {
            throw "Preview HTTP $([int]$response.StatusCode): $body"
        }
        return $body
    } finally {
        $client.Dispose()
    }
}

$token = Login
Write-Host "login OK: $Username"

$expected = "minio smoke $(Get-Date -Format 'yyyyMMddHHmmssfff')"
$sourcePath = Join-Path $env:TEMP "smart-office-minio-smoke.txt"
$downloadPath = Join-Path $env:TEMP "smart-office-minio-smoke-download.txt"
[System.IO.File]::WriteAllText($sourcePath, $expected, [System.Text.Encoding]::UTF8)

$uploaded = Invoke-MultipartUpload -Uri "$BaseUrl/api/files/upload" -Token $token -FilePath $sourcePath
$fileId = [long]$uploaded.data.id
Write-Host "uploaded file: id=$fileId bucket=$($uploaded.data.bucket) key=$($uploaded.data.objectKey)"

$headers = @{ Authorization = "Bearer $token" }
$list = Invoke-OfficeApi -Method Get -Uri "$BaseUrl/api/files?current=1&size=10" -Headers $headers
$listed = @($list.data.records | Where-Object { $_.id -eq $fileId }) | Select-Object -First 1
if (-not $listed) {
    throw "Uploaded file not found in my files list: $fileId"
}
Write-Host "list OK: id=$($listed.id) size=$($listed.size)"

$preview = Invoke-OfficeApi -Method Get -Uri "$BaseUrl/api/files/$fileId/preview" -Headers $headers
if (-not $preview.data.StartsWith("http")) {
    throw "Preview URL is invalid: $($preview.data)"
}
$previewText = Invoke-PlainGetText -Uri $preview.data
if ($previewText -ne $expected) {
    throw "Preview content mismatch"
}
Write-Host "preview OK"

Invoke-AuthorizedDownload -Uri "$BaseUrl/api/files/$fileId/download" -Token $token -OutputPath $downloadPath
$downloadText = [System.IO.File]::ReadAllText($downloadPath, [System.Text.Encoding]::UTF8)
if ($downloadText -ne $expected) {
    throw "Downloaded content mismatch"
}
Write-Host "download OK"
Write-Host "P1 file/minio gateway smoke passed."
