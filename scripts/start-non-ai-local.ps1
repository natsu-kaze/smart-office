param(
    [string]$JavaPath = "C:\Users\14224\.jdks\ms-17.0.18\bin\java.exe",
    [string]$NodeNpmPath = "C:\Program Files\nodejs\npm.cmd",
    [int]$FrontendPort = 5173
)

$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$logDir = Join-Path $root "logs"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

function Test-PortListening {
    param([int]$Port)
    $connection = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -First 1
    return $null -ne $connection
}

function Wait-HttpHealth {
    param(
        [string]$Name,
        [int]$Port,
        [int]$TimeoutSeconds = 90
    )

    $url = "http://127.0.0.1:$Port/actuator/health"
    for ($i = 0; $i -lt $TimeoutSeconds; $i++) {
        try {
            $response = Invoke-RestMethod -Uri $url -TimeoutSec 2
            if ($response.status -eq "UP") {
                Write-Host "$Name health OK: $url"
                return
            }
        } catch {
            Start-Sleep -Seconds 1
        }
    }
    throw "$Name did not become healthy: $url"
}

function Start-CmdFile {
    param(
        [string]$Name,
        [string[]]$Lines
    )

    $cmdPath = Join-Path $logDir "$Name.run.cmd"
    Set-Content -Path $cmdPath -Value $Lines -Encoding ASCII

    $cmdExe = Join-Path $env:SystemRoot "System32\cmd.exe"
    $psi = [System.Diagnostics.ProcessStartInfo]::new()
    $psi.FileName = $cmdExe
    $psi.Arguments = "/d /c `"$cmdPath`""
    $psi.WorkingDirectory = $root
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    $process = [System.Diagnostics.Process]::Start($psi)
    Write-Host "$Name started: pid=$($process.Id)"
}

function Start-ServiceJar {
    param(
        [string]$Name,
        [int]$Port
    )

    if (Test-PortListening -Port $Port) {
        Write-Host "$Name already listening on $Port, skip"
        return
    }

    $jar = Join-Path $root "smart-office-services\$Name\target\$Name-0.0.1-SNAPSHOT.jar"
    if (-not (Test-Path $jar)) {
        throw "$Name jar not found: $jar. Run mvn package first."
    }

    $out = Join-Path $logDir "$Name.out.log"
    $err = Join-Path $logDir "$Name.err.log"
    Start-CmdFile -Name $Name -Lines @(
        "@echo off",
        "cd /d `"$root`"",
        "set NACOS_SERVER_ADDR=127.0.0.1:8951",
        "set MYSQL_PORT=3306",
        "set MYSQL_PASSWORD=123456",
        "set REDIS_PORT=6379",
        "set RABBITMQ_PORT=5673",
        "set ELASTICSEARCH_URIS=http://127.0.0.1:9200",
        "set SEARCH_ES_ENABLED=true",
        "`"$JavaPath`" -jar `"$jar`" 1> `"$out`" 2> `"$err`""
    )
}

function Start-Frontend {
    if (Test-PortListening -Port $FrontendPort) {
        Write-Host "smart-office-web already listening on $FrontendPort, skip"
        return
    }
    if (-not (Test-Path $NodeNpmPath)) {
        throw "npm not found: $NodeNpmPath"
    }

    $webDir = Join-Path $root "smart-office-web"
    $out = Join-Path $logDir "smart-office-web.out.log"
    $err = Join-Path $logDir "smart-office-web.err.log"
    Start-CmdFile -Name "smart-office-web" -Lines @(
        "@echo off",
        "cd /d `"$webDir`"",
        "set VITE_API_PROXY_TARGET=http://127.0.0.1:8000",
        "`"$NodeNpmPath`" run dev:microservice -- --port $FrontendPort 1> `"$out`" 2> `"$err`""
    )
}

$services = @(
    @{ Name = "smart-office-system-service"; Port = 9102 },
    @{ Name = "smart-office-org-service"; Port = 9103 },
    @{ Name = "smart-office-message-service"; Port = 9106 },
    @{ Name = "smart-office-auth-service"; Port = 9101 },
    @{ Name = "smart-office-approval-service"; Port = 9104 },
    @{ Name = "smart-office-attendance-service"; Port = 9105 },
    @{ Name = "smart-office-file-service"; Port = 9107 },
    @{ Name = "smart-office-search-service"; Port = 9108 },
    @{ Name = "smart-office-gateway"; Port = 8000 }
)

foreach ($service in $services) {
    Start-ServiceJar -Name $service.Name -Port $service.Port
    Start-Sleep -Seconds 2
}

foreach ($service in $services) {
    Wait-HttpHealth -Name $service.Name -Port $service.Port
}

Start-Frontend

for ($i = 0; $i -lt 60; $i++) {
    try {
        Invoke-WebRequest -UseBasicParsing "http://127.0.0.1:$FrontendPort" -TimeoutSec 2 | Out-Null
        Write-Host "smart-office-web OK: http://127.0.0.1:$FrontendPort"
        break
    } catch {
        Start-Sleep -Seconds 1
    }
}

Write-Host "Non-AI Smart Office stack is ready."
Write-Host "Frontend: http://127.0.0.1:$FrontendPort"
Write-Host "Gateway:  http://127.0.0.1:8000"
