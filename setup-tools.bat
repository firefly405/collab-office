@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
title Collab Office — 一键安装中间件

echo.
echo   ============================================
echo     Collab Office — 中间件环境安装
echo   ============================================
echo.
echo   将在项目 tools/ 目录下安装:
echo     · Redis for Windows (5.0.14.1)
echo     · Nacos Server        (2.3.2)
echo.

set TOOLS_DIR=%~dp0tools
if not exist "%TOOLS_DIR%" mkdir "%TOOLS_DIR%"

:: ── Redis ──────────────────────────────────────
set REDIS_DIR=%TOOLS_DIR%\Redis-x64-5.0.14.1
if exist "%REDIS_DIR%\redis-server.exe" (
    echo [√] Redis 已存在，跳过下载
    goto :nacos
)

echo [↓] 正在下载 Redis for Windows ...
powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://github.com/tporadowski/redis/releases/download/v5.0.14.1/Redis-x64-5.0.14.1.zip' -OutFile '%TOOLS_DIR%\redis.zip'}" 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [×] Redis 下载失败，请手动下载并解压到:
    echo     %REDIS_DIR%
    echo     下载地址: https://github.com/tporadowski/redis/releases
    del "%TOOLS_DIR%\redis.zip" 2>nul
    goto :nacos
)

echo [√] 正在解压 Redis ...
powershell -Command "Expand-Archive -Path '%TOOLS_DIR%\redis.zip' -DestinationPath '%TOOLS_DIR%' -Force" 2>nul
del "%TOOLS_DIR%\redis.zip" 2>nul

:: Fix: the zip may extract to Redis-x64-5.0.14.1 or just dump files
if not exist "%REDIS_DIR%\redis-server.exe" (
    for /d %%d in ("%TOOLS_DIR%\Redis*") do (
        if exist "%%d\redis-server.exe" (
            echo [√] Redis 解压完成: %%d
        )
    )
)

:nacos
:: ── Nacos ──────────────────────────────────────
set NACOS_DIR=%TOOLS_DIR%\nacos
if exist "%NACOS_DIR%\bin\startup.cmd" (
    echo [√] Nacos 已存在，跳过下载
    goto :done
)

echo [↓] 正在下载 Nacos Server (约 120MB，请耐心等待) ...
powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://github.com/alibaba/nacos/releases/download/2.3.2/nacos-server-2.3.2.zip' -OutFile '%TOOLS_DIR%\nacos.zip'}" 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [×] Nacos 下载失败，请手动下载并解压到:
    echo     %NACOS_DIR%
    echo     下载地址: https://github.com/alibaba/nacos/releases
    del "%TOOLS_DIR%\nacos.zip" 2>nul
    goto :done
)

echo [√] 正在解压 Nacos ...
powershell -Command "Expand-Archive -Path '%TOOLS_DIR%\nacos.zip' -DestinationPath '%TOOLS_DIR%' -Force" 2>nul
del "%TOOLS_DIR%\nacos.zip" 2>nul

:: The zip extracts to nacos/ so rename if needed
if exist "%TOOLS_DIR%\nacos-server-2.3.2" (
    if not exist "%NACOS_DIR%" (
        move "%TOOLS_DIR%\nacos-server-2.3.2" "%NACOS_DIR%" >nul 2>nul
    )
)
if exist "%TOOLS_DIR%\nacos\bin\startup.cmd" (
    echo [√] Nacos 解压完成
)

:done
echo.
echo   ============================================
echo     安装完成。现在可以运行 start-dev.bat
echo     一键启动所有开发服务
echo   ============================================
echo.
pause
