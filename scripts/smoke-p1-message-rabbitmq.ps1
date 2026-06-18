param(
    [string]$BaseUrl = "http://127.0.0.1:8000",
    [string]$RabbitContainer = "smart-office-rabbitmq",
    [string]$NoticeQueue = "smart-office.message.notice.queue",
    [string]$Password = "123456"
)

$ErrorActionPreference = "Stop"

& "$PSScriptRoot\smoke-p0-approval-message.ps1" -BaseUrl $BaseUrl -Password $Password

$queueLines = docker exec $RabbitContainer rabbitmqctl list_queues name durable consumers messages
$queueLine = @($queueLines | Where-Object { $_ -match "^$([regex]::Escape($NoticeQueue))\s+" }) | Select-Object -First 1

if (-not $queueLine) {
    throw "RabbitMQ notice queue not found: $NoticeQueue"
}

$columns = $queueLine -split "\s+"
if ($columns.Length -lt 4) {
    throw "RabbitMQ queue output is invalid: $queueLine"
}

$durable = $columns[1]
$consumers = [int]$columns[2]
$messages = [int]$columns[3]

if ($durable -ne "true") {
    throw "RabbitMQ notice queue is not durable: $NoticeQueue"
}

if ($consumers -lt 1) {
    throw "RabbitMQ notice queue has no active consumers: $NoticeQueue"
}

if ($messages -ne 0) {
    throw "RabbitMQ notice queue still has pending messages: queue=$NoticeQueue messages=$messages"
}

Write-Host "RabbitMQ notice queue OK: name=$NoticeQueue messages=$messages consumers=$consumers"
Write-Host "P1 message/RabbitMQ smoke passed."
