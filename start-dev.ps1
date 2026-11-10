# Collab Office - Dev Startup Script
# Run: right-click -> "Run with PowerShell", or: powershell -ExecutionPolicy Bypass -File start-dev.ps1

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root
$tools = Join-Path $root "tools"

Write-Host ""
Write-Host "  ============================================"  -ForegroundColor Cyan
Write-Host "    Collab Office - Starting dev services..."    -ForegroundColor Cyan
Write-Host "  ============================================"  -ForegroundColor Cyan
Write-Host ""

# ---- Java detection ----
$javaHome = $env:JAVA_HOME
if (-not $javaHome) {
    $candidates = @(
        "$env:USERPROFILE\.jdks\ms-17.0.19",
        "$env:USERPROFILE\.jdks\ms-21.0.11"
    )
    foreach ($c in $candidates) {
        if (Test-Path "$c\bin\java.exe") { $javaHome = $c; break }
    }
}

# ---- Redis ----
Write-Host "[1/3] Redis" -ForegroundColor Yellow

$existingRedis = Get-Process -Name "redis-server" -ErrorAction SilentlyContinue
if ($existingRedis) {
    Write-Host "       Redis already running - skip" -ForegroundColor Green
} else {
    $redisDir = Get-ChildItem -Path $tools -Directory -Filter "Redis*" | Select-Object -First 1
    if ($redisDir) {
        $redisExe = Join-Path $redisDir.FullName "redis-server.exe"
        $redisConf = Join-Path $redisDir.FullName "redis.windows.conf"
        if (Test-Path $redisConf) {
            Start-Process -FilePath $redisExe -ArgumentList $redisConf -WindowStyle Minimized
        } else {
            Start-Process -FilePath $redisExe -WindowStyle Minimized
        }
        Write-Host "       Started (127.0.0.1:6379)" -ForegroundColor Green
    } else {
        Write-Host "       Not found in tools\ - skip" -ForegroundColor DarkYellow
    }
}

# ---- Nacos ----
Write-Host "[2/3] Nacos" -ForegroundColor Yellow

$nacosPortCheck = netstat -ano 2>$null | Select-String "127.0.0.1:8848"
if ($nacosPortCheck) {
    Write-Host "       Nacos already running on port 8848 - skip" -ForegroundColor Green
} elseif (-not $javaHome) {
    Write-Host "       JAVA_HOME not found - skip" -ForegroundColor DarkYellow
} else {
    $nacosDirs = Get-ChildItem -Path $tools -Directory -Filter "nacos*"
    $nacosDir = $null
    foreach ($d in $nacosDirs) {
        $startup = Join-Path $d.FullName "bin\startup.cmd"
        if (Test-Path $startup) { $nacosDir = $d.FullName; break }
        $nested = Join-Path $d.FullName "nacos\bin\startup.cmd"
        if (Test-Path $nested) { $nacosDir = Join-Path $d.FullName "nacos"; break }
    }

    if ($nacosDir) {
        Write-Host "       Starting Nacos (this takes ~20s)..." -ForegroundColor Green
        Write-Host "       JAVA_HOME = $javaHome"

        $env:JAVA_HOME = $javaHome
        $startupCmd = Join-Path $nacosDir "bin\startup.cmd"
        Start-Process cmd.exe -ArgumentList "/c", "cd /d `"$nacosDir\bin`" && startup.cmd -m standalone" -WindowStyle Minimized

        # Wait for Nacos port 8848 (max 2 minutes)
        $waited = 0
        while ($waited -lt 40) {
            Start-Sleep -Seconds 3
            $check = netstat -ano 2>$null | Select-String "127.0.0.1:8848"
            if ($check) {
                Write-Host "       Nacos ready (127.0.0.1:8848)" -ForegroundColor Green
                break
            }
            $waited++
            Write-Host "       Waiting... ($waited/40)"
        }
        if ($waited -ge 40) {
            Write-Host "       Nacos did not start in 2min - check the Nacos window" -ForegroundColor DarkYellow
        }
    } else {
        Write-Host "       Not found in tools\ - skip" -ForegroundColor DarkYellow
    }
}

# ---- Frontend ----
Write-Host "[3/3] Frontend" -ForegroundColor Yellow

$webDir = Join-Path $root "collab-office-web"
if (-not (Test-Path "$webDir\package.json")) {
    Write-Host "       collab-office-web not found - skip" -ForegroundColor DarkYellow
} else {
    $frontendCheck = netstat -ano 2>$null | Select-String "127.0.0.1:3000"
    if ($frontendCheck) {
        Write-Host "       Frontend already running on port 3000 - skip" -ForegroundColor Green
    } else {
        if (-not (Test-Path "$webDir\node_modules")) {
            Write-Host "       npm install..."
            Set-Location $webDir
            npm install
            Set-Location $root
        }
        Start-Process cmd.exe -ArgumentList "/c", "cd /d `"$webDir`" && npm run dev"
        Write-Host "       Started (http://localhost:3000)" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "  ============================================"  -ForegroundColor Cyan
Write-Host "    All done"                                    -ForegroundColor Cyan
Write-Host "  ============================================"  -ForegroundColor Cyan
Write-Host ""
Write-Host "  Redis : localhost:6379"
Write-Host "  Nacos : localhost:8848  (console http://localhost:8848/nacos)"
Write-Host "  Web   : http://localhost:3000"
Write-Host "  API   : http://localhost:8080"
Write-Host ""
Read-Host "Press Enter to exit"
