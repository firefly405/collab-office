[CmdletBinding()]
param(
    [string]$OutputRoot,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$projectRoot = $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($OutputRoot)) {
    $OutputRoot = Join-Path $projectRoot "offline-packages"
}
Set-Location $projectRoot

function Invoke-DockerCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments,
        [Parameter(Mandatory = $true)]
        [string]$FailureMessage
    )

    & docker @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw $FailureMessage
    }
}

Write-Host "[1/6] Checking Docker Desktop..." -ForegroundColor Cyan
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "The docker command was not found. Install Docker Desktop first."
}

& docker info *> $null
if ($LASTEXITCODE -ne 0) {
    throw "Docker Desktop is not running. Start it and retry."
}

Invoke-DockerCommand -Arguments @("compose", "version") -FailureMessage "Docker Compose V2 is not available."
Invoke-DockerCommand -Arguments @("compose", "config", "--quiet") -FailureMessage "docker-compose.yml validation failed."

if (-not $SkipBuild) {
    Write-Host "[2/6] Building application images..." -ForegroundColor Cyan
    Invoke-DockerCommand -Arguments @("compose", "build") -FailureMessage "Application image build failed."
} else {
    Write-Host "[2/6] Build skipped; exporting the existing local images." -ForegroundColor Yellow
}

Write-Host "[3/6] Checking every image required by the offline package..." -ForegroundColor Cyan
$imageNames = @(& docker compose config --images | Sort-Object -Unique)
if ($LASTEXITCODE -ne 0 -or $imageNames.Count -eq 0) {
    throw "Unable to read the image list from the Compose configuration."
}

foreach ($imageName in $imageNames) {
    & docker image inspect $imageName *> $null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Image $imageName is missing locally; pulling it now..." -ForegroundColor Yellow
        Invoke-DockerCommand -Arguments @("image", "pull", $imageName) -FailureMessage "Unable to pull image $imageName."
    }
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$packageDirectory = Join-Path $OutputRoot "collab-office-offline-$timestamp"
if (Test-Path -LiteralPath $packageDirectory) {
    throw "Output directory already exists: $packageDirectory"
}

Write-Host "[4/6] Preparing the minimal runtime files..." -ForegroundColor Cyan
New-Item -ItemType Directory -Path $packageDirectory -Force | Out-Null
Copy-Item -LiteralPath (Join-Path $projectRoot "docker-compose.yml") -Destination $packageDirectory
Copy-Item -LiteralPath (Join-Path $projectRoot ".env.example") -Destination $packageDirectory
Copy-Item -LiteralPath (Join-Path $projectRoot "docker-offline-import.bat") -Destination $packageDirectory
Copy-Item -LiteralPath (Join-Path $projectRoot "docker-offline-import.ps1") -Destination $packageDirectory
Copy-Item -LiteralPath (Join-Path $projectRoot "docker-offline-stop.bat") -Destination $packageDirectory
Copy-Item -LiteralPath (Join-Path $projectRoot "OFFLINE-README.txt") -Destination $packageDirectory

$migrationSource = Join-Path $projectRoot "collab-office-common\src\main\resources\db\migration"
$migrationTarget = Join-Path $packageDirectory "collab-office-common\src\main\resources\db\migration"
New-Item -ItemType Directory -Path $migrationTarget -Force | Out-Null
Copy-Item -Path (Join-Path $migrationSource "*.sql") -Destination $migrationTarget

# Compose contains build metadata even though offline installation always uses
# --no-build. Keep the Web build context present so Compose validation remains
# compatible across Docker Desktop releases.
New-Item -ItemType Directory -Path (Join-Path $packageDirectory "collab-office-web") -Force | Out-Null

Write-Host "[5/6] Saving $($imageNames.Count) images into one archive. This can take a while..." -ForegroundColor Cyan
$imageArchive = Join-Path $packageDirectory "collab-office-images.tar"
& docker image save --output $imageArchive @imageNames
if ($LASTEXITCODE -ne 0) {
    throw "Docker image export failed. The incomplete directory remains at: $packageDirectory"
}

Write-Host "[6/6] Creating the integrity checksum..." -ForegroundColor Cyan
$archiveHash = (Get-FileHash -LiteralPath $imageArchive -Algorithm SHA256).Hash.ToLowerInvariant()
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText(
    (Join-Path $packageDirectory "collab-office-images.sha256"),
    "$archiveHash`r`n",
    $utf8NoBom
)
[System.IO.File]::WriteAllLines(
    (Join-Path $packageDirectory "images.txt"),
    [string[]]$imageNames,
    $utf8NoBom
)

$platform = (& docker info --format '{{.OSType}}/{{.Architecture}}').Trim()
[System.IO.File]::WriteAllText(
    (Join-Path $packageDirectory "platform.txt"),
    "$platform`r`n",
    $utf8NoBom
)

$archiveSize = [Math]::Round((Get-Item -LiteralPath $imageArchive).Length / 1GB, 2)
Write-Host ""
Write-Host "Offline installation package created:" -ForegroundColor Green
Write-Host $packageDirectory -ForegroundColor Green
Write-Host "Image archive: $archiveSize GB, $($imageNames.Count) images, platform $platform"
Write-Host "Copy the entire directory to the target computer and double-click docker-offline-import.bat."
