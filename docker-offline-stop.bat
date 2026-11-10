@echo off
setlocal
chcp 65001 >nul

cd /d "%~dp0"
docker compose down --remove-orphans
if errorlevel 1 (
    echo 停止失败，请确认 Docker Desktop 正在运行。
) else (
    echo Collab Office 已停止，MySQL、Redis、MinIO 等数据卷均已保留。
)
pause
