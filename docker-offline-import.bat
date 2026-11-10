@echo off
setlocal
chcp 65001 >nul

cd /d "%~dp0"
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0docker-offline-import.ps1"
set "EXIT_CODE=%ERRORLEVEL%"

echo.
if not "%EXIT_CODE%"=="0" (
    echo 安装或启动失败，请查看上方错误信息。
) else (
    echo Collab Office 已经启动完成。
)
pause
exit /b %EXIT_CODE%
