@echo off
setlocal
chcp 65001 >nul

cd /d "%~dp0"
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0docker-offline-export.ps1" %*
set "EXIT_CODE=%ERRORLEVEL%"

echo.
if not "%EXIT_CODE%"=="0" (
    echo 离线安装包生成失败，请查看上方错误信息。
) else (
    echo 离线安装包已经生成完成。
)
pause
exit /b %EXIT_CODE%
