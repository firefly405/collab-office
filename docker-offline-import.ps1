param(
    [switch]$ValidateOnly
)

$ErrorActionPreference = "Stop"
$packageRoot = $PSScriptRoot
Set-Location $packageRoot

function Test-DockerReady {
    & docker info *> $null
    return $LASTEXITCODE -eq 0
}

function New-HexSecret {
    param([int]$ByteCount = 32)

    $bytes = New-Object byte[] $ByteCount
    $generator = New-Object System.Security.Cryptography.RNGCryptoServiceProvider
    try {
        $generator.GetBytes($bytes)
    } finally {
        $generator.Dispose()
    }
    return -join ($bytes | ForEach-Object { $_.ToString("x2") })
}

function Set-EnvironmentValue {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Content,
        [Parameter(Mandatory = $true)]
        [string]$Name,
        [Parameter(Mandatory = $true)]
        [string]$Value
    )

    return [regex]::Replace(
        $Content,
        "(?m)^$([regex]::Escape($Name))=.*$",
        "$Name=$Value"
    )
}

try {
    Write-Host "[1/7] Checking Docker Desktop..." -ForegroundColor Cyan
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw "The docker command was not found. Install Docker Desktop and use Linux Containers."
    }

    if (-not (Test-DockerReady)) {
        $dockerDesktop = Join-Path $env:ProgramFiles "Docker\Docker\Docker Desktop.exe"
        if (-not (Test-Path -LiteralPath $dockerDesktop)) {
            throw "Docker Desktop is not running and was not found in its default installation directory."
        }

        Write-Host "Starting Docker Desktop..." -ForegroundColor Yellow
        Start-Process -FilePath $dockerDesktop
        $deadline = (Get-Date).AddMinutes(5)
        do {
            Start-Sleep -Seconds 3
            if (Test-DockerReady) {
                break
            }
        } while ((Get-Date) -lt $deadline)

        if (-not (Test-DockerReady)) {
            throw "Timed out waiting for Docker Desktop. Start it manually and run this installer again."
        }
    }

    & docker compose version *> $null
    if ($LASTEXITCODE -ne 0) {
        throw "Docker Compose V2 is not available."
    }

    $archivePath = Join-Path $packageRoot "collab-office-images.tar"
    $hashPath = Join-Path $packageRoot "collab-office-images.sha256"
    $composePath = Join-Path $packageRoot "docker-compose.yml"
    $exampleEnvironmentPath = Join-Path $packageRoot ".env.example"
    foreach ($requiredPath in @($archivePath, $hashPath, $composePath, $exampleEnvironmentPath)) {
        if (-not (Test-Path -LiteralPath $requiredPath)) {
            throw "The offline package is incomplete. Missing: $requiredPath"
        }
    }

    Write-Host "[2/7] Verifying the image archive checksum..." -ForegroundColor Cyan
    $expectedHash = (Get-Content -LiteralPath $hashPath -Raw).Trim().ToLowerInvariant()
    $actualHash = (Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($expectedHash -ne $actualHash) {
        throw "The image archive SHA256 checksum failed. The file may be incomplete."
    }

    $sourcePlatformPath = Join-Path $packageRoot "platform.txt"
    if (Test-Path -LiteralPath $sourcePlatformPath) {
        $sourcePlatform = (Get-Content -LiteralPath $sourcePlatformPath -Raw).Trim()
        $targetPlatform = (& docker info --format '{{.OSType}}/{{.Architecture}}').Trim()
        if ($sourcePlatform -ne $targetPlatform) {
            Write-Warning "The package platform is $sourcePlatform but target Docker is $targetPlatform. Emulation may be required."
        }
    }

    if ($ValidateOnly) {
        & docker compose --env-file $exampleEnvironmentPath config --quiet
        if ($LASTEXITCODE -ne 0) {
            throw "Offline package Compose validation failed."
        }
        Write-Host "Offline package validation completed successfully." -ForegroundColor Green
        exit 0
    }

    Write-Host "[3/7] Loading all images into Docker Desktop. This can take a while..." -ForegroundColor Cyan
    & docker image load --input $archivePath
    if ($LASTEXITCODE -ne 0) {
        throw "Docker image loading failed."
    }

    Write-Host "[4/7] Preparing runtime configuration..." -ForegroundColor Cyan
    $environmentPath = Join-Path $packageRoot ".env"
    if (-not (Test-Path -LiteralPath $environmentPath)) {
        & docker volume inspect "collab-office_mysql-data" *> $null
        if ($LASTEXITCODE -eq 0) {
            throw "Existing Collab Office data was found but this package has no .env. Restore the original .env before reinstalling."
        }

        $environmentText = Get-Content -LiteralPath $exampleEnvironmentPath -Raw
        $environmentText = Set-EnvironmentValue -Content $environmentText -Name "JWT_SECRET" -Value (New-HexSecret 48)
        $environmentText = Set-EnvironmentValue -Content $environmentText -Name "INTERNAL_CALL_TOKEN" -Value (New-HexSecret 32)
        $environmentText = Set-EnvironmentValue -Content $environmentText -Name "DB_PASSWORD" -Value (New-HexSecret 24)
        $environmentText = Set-EnvironmentValue -Content $environmentText -Name "RABBITMQ_PASSWORD" -Value (New-HexSecret 24)
        $environmentText = Set-EnvironmentValue -Content $environmentText -Name "MINIO_ACCESS_KEY" -Value "collabadmin"
        $environmentText = Set-EnvironmentValue -Content $environmentText -Name "MINIO_SECRET_KEY" -Value (New-HexSecret 24)
        $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
        [System.IO.File]::WriteAllText($environmentPath, $environmentText, $utf8NoBom)
        Write-Host "Random secrets were generated in .env. Keep this file safe."
    } else {
        Write-Host "An existing .env was found and will be preserved."
    }

    Write-Host "[5/7] Validating the offline Compose configuration..." -ForegroundColor Cyan
    & docker compose config --quiet
    if ($LASTEXITCODE -ne 0) {
        throw "Compose configuration validation failed."
    }

    Write-Host "[6/7] Starting the complete application..." -ForegroundColor Cyan
    & docker compose up --detach --no-build --pull never --remove-orphans
    if ($LASTEXITCODE -ne 0) {
        throw "Container startup failed."
    }

    $webPort = 3030
    $webPortLine = Get-Content -LiteralPath $environmentPath | Where-Object { $_ -match '^WEB_PORT=' } | Select-Object -First 1
    if ($webPortLine) {
        $configuredPort = ($webPortLine -split '=', 2)[1].Trim()
        if ($configuredPort -match '^\d+$') {
            $webPort = [int]$configuredPort
        }
    }

    Write-Host "[7/7] Waiting for Web health. First-time initialization may take several minutes..." -ForegroundColor Cyan
    $healthUrl = "http://localhost:$webPort/health"
    $healthDeadline = (Get-Date).AddMinutes(10)
    $healthy = $false
    do {
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri $healthUrl -TimeoutSec 5
            if ($response.StatusCode -eq 200) {
                $healthy = $true
                break
            }
        } catch {
            Start-Sleep -Seconds 5
        }
    } while ((Get-Date) -lt $healthDeadline)

    & docker compose ps -a
    if (-not $healthy) {
        throw "Timed out waiting for Web health. Run docker compose logs --tail=200 for details."
    }

    $applicationUrl = "http://localhost:$webPort"
    Write-Host ""
    Write-Host "Collab Office is ready: $applicationUrl" -ForegroundColor Green
    Start-Process $applicationUrl
    exit 0
} catch {
    Write-Host ""
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host ""
    Write-Host "For diagnostics, run: docker compose logs --tail=200" -ForegroundColor Yellow
    exit 1
}
