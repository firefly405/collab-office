$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot

# Deliberately preserves named volumes and all application data.
docker compose down --remove-orphans
