param(
    [string]$BaseUrl = "http://127.0.0.1:8000",
    [string]$SearchBaseUrl = "http://127.0.0.1:9108",
    [string]$ElasticsearchUrl = "http://127.0.0.1:9200",
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
    return $response.data.token
}

function Check-Elasticsearch {
    try {
        $response = Invoke-RestMethod -Uri "$ElasticsearchUrl/_cluster/health" -TimeoutSec 10
        if ($response.status -ne "green" -and $response.status -ne "yellow") {
            throw "Unexpected cluster status $($response.status)"
        }
        return $response.status
    } catch {
        throw "Elasticsearch is not reachable at $ElasticsearchUrl`: $($_.Exception.Message)"
    }
}

$clusterStatus = Check-Elasticsearch
Write-Host "Elasticsearch reachable: status=$clusterStatus"

$token = Login
$headers = @{ Authorization = "Bearer $token" }
Write-Host "login OK: $Username"

$stamp = Get-Date -Format 'yyyyMMddHHmmssfff'
$keyword = "es-smoke-$stamp"
$created = Invoke-OfficeApi `
    -Method Post `
    -Uri "$BaseUrl/api/policies" `
    -Headers $headers `
    -Body @{
        title = "ES Smoke Policy $stamp"
        summary = "Search smoke summary $keyword"
        content = "This policy document verifies Elasticsearch full text search with keyword $keyword."
        documentVersion = "v$stamp"
        status = "PUBLISHED"
    }

$documentId = [long]$created.data.id
Write-Host "created policy: id=$documentId keyword=$keyword"

$reindex = Invoke-OfficeApi `
    -Method Post `
    -Uri "$SearchBaseUrl/internal/search/policies/reindex"

if ($reindex.data -lt 1) {
    throw "Reindex did not index any policy document"
}
Write-Host "reindex OK: indexed=$($reindex.data)"

$encodedKeyword = [uri]::EscapeDataString($keyword)
$found = $false
for ($i = 1; $i -le 10; $i++) {
    $page = Invoke-OfficeApi `
        -Method Get `
        -Uri "$BaseUrl/api/policies?current=1&size=10&keyword=$encodedKeyword" `
        -Headers $headers

    $hit = @($page.data.records | Where-Object { $_.id -eq $documentId }) | Select-Object -First 1
    if ($hit) {
        $found = $true
        Write-Host "search OK: id=$($hit.id) title=$($hit.title)"
        break
    }
    Start-Sleep -Seconds 1
}

if (-not $found) {
    throw "Created policy document was not found by Elasticsearch keyword search"
}

Invoke-OfficeApi `
    -Method Delete `
    -Uri "$BaseUrl/api/policies/$documentId" `
    -Headers $headers | Out-Null

Write-Host "cleanup OK: id=$documentId"
Write-Host "P1 search/Elasticsearch smoke passed."
