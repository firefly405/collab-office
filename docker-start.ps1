$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot

docker info *> $null
if ($LASTEXITCODE -ne 0) {
    throw "Docker Desktop is not available. Start Docker Desktop and retry."
}

Write-Host "Building and starting the complete Collab Office stack..." -ForegroundColor Cyan
docker compose up --build --detach --remove-orphans
if ($LASTEXITCODE -ne 0) {
    throw "docker compose up failed. Run 'docker compose logs --tail=200' for details."
}

docker compose ps
Write-Host ""
Write-Host "Collab Office : http://localhost:3030" -ForegroundColor Green
Write-Host "Gateway API   : http://localhost:18090/api/health"
Write-Host "MinIO Console : http://localhost:19001"
Write-Host "Mailpit       : http://localhost:18025"
Write-Host "Nacos Console : http://localhost:18848/nacos"
