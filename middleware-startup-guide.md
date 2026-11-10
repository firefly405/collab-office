# Collab Office — 中间件启动手册（纯命令行版）

> 本文档记录所有中间件在 Windows 环境下的纯命令行启动方式，不依赖 Docker。
> MySQL 使用本机已安装的服务，其余 5 个中间件使用 `tools/` 目录下的本地版本或需单独下载。

---

## 1. MySQL 8.0（本机服务）

本机已安装 MySQL 8.0，作为 Windows 服务运行。

### 启动 / 停止

```bat
:: 启动
net start MySQL80

:: 停止
net stop MySQL80

:: 查看状态
sc query MySQL80
```

### 连接

```bat
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p
```

### 初始化项目数据库

```sql
CREATE DATABASE IF NOT EXISTS collab_office
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

然后按顺序执行 `src/main/resources/sql/` 下的 36 个 SQL 脚本：

```bat
set MYSQL="C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
for %f in (src\main\resources\sql\V*.sql) do %MYSQL% -u root -p collab_office < "%f"
```

### 验证

```bat
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p -e "SHOW DATABASES;"
```

---

## 2. Redis 5.0.14

使用项目 `tools/Redis/` 下的本地版本。

### 启动

```bat
cd tools\Redis
redis-server.exe redis.windows.conf
```

### 连接

```bat
tools\Redis\redis-cli.exe -h localhost -p 6379
```

### 常用命令

```bat
:: 测试连接
redis-cli.exe ping

:: 清空数据
redis-cli.exe flushall

:: 查看信息
redis-cli.exe info
```

### 验证

期望返回 `PONG`：

```bat
tools\Redis\redis-cli.exe ping
```

---

## 3. Nacos 2.4.3（单机模式）

使用项目 `tools/nacos-server-2.4.3/nacos/` 下的本地版本。

### 启动

```bat
cd tools\nacos-server-2.4.3\nacos\bin
startup.cmd -m standalone
```

### 停止

```bat
cd tools\nacos-server-2.4.3\nacos\bin
shutdown.cmd
```

### 查看启动日志

```bat
type tools\nacos-server-2.4.3\nacos\logs\start.out
```

### 验证

期望返回 JSON 且 `status` 为 `UP`：

```bat
curl http://localhost:8848/nacos/v1/console/health/readiness
```

### 管理界面

- 地址：http://localhost:8848/nacos
- 账号：`nacos` / `nacos`（默认，本项目关闭了认证）

---

## 4. RabbitMQ 3.13

需要单独下载安装。

### 安装

1. 先安装 Erlang：https://www.erlang.org/downloads
2. 下载 RabbitMQ：https://github.com/rabbitmq/rabbitmq-server/releases/download/v3.13.7/rabbitmq-server-3.13.7.exe
3. 以管理员身份运行安装程序

```bat
:: 启用管理界面插件（安装后执行一次）
"C:\Program Files\RabbitMQ Server\rabbitmq_server-3.13.7\sbin\rabbitmq-plugins.bat" enable rabbitmq_management
```

### 启动 / 停止

```bat
:: 启动（作为 Windows 服务）
net start RabbitMQ

:: 停止
net stop RabbitMQ

:: 查看状态
"C:\Program Files\RabbitMQ Server\rabbitmq_server-3.13.7\sbin\rabbitmqctl.bat" status
```

### 验证

```bat
curl http://localhost:15672/api/overview -u guest:guest
```

### 管理界面

- 地址：http://localhost:15672
- 账号：`guest` / `guest`

---

## 5. MinIO

需要单独下载。

### 下载

```bat
:: 下载 minio.exe
curl -L -o minio.exe https://dl.min.io/server/minio/release/windows-amd64/minio.exe

:: 创建数据目录
mkdir C:\minio-data
```

### 启动

```bat
set MINIO_ROOT_USER=minioadmin
set MINIO_ROOT_PASSWORD=minioadmin
minio.exe server C:\minio-data --address ":9000" --console-address ":9001"
```

### 验证

```bat
curl http://localhost:9000/minio/health/live
```

### 管理界面

- 地址：http://localhost:9001
- 账号：`minioadmin` / `minioadmin`

---

## 6. Elasticsearch 7.17.27

需要单独下载。

### 下载

```bat
:: 下载
curl -L -o elasticsearch.zip https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-7.17.27-windows-x86_64.zip

:: 解压
powershell -command "Expand-Archive -Path elasticsearch.zip -DestinationPath C:\elasticsearch"
```

### 启动

```bat
C:\elasticsearch\elasticsearch-7.17.27\bin\elasticsearch.bat -E discovery.type=single-node
```

### 停止

按 `Ctrl+C` 或查找 Java 进程关闭：

```bat
tasklist | findstr java
taskkill /PID <进程ID> /F
```

### 验证

期望返回 JSON 且 `cluster_name` 为 `elasticsearch`：

```bat
curl http://localhost:9200
curl http://localhost:9200/_cluster/health
```

---

## 端口汇总

| 中间件 | 端口 | 用途 |
|--------|------|------|
| MySQL | 3306 | 业务数据库 |
| Redis | 6379 | 缓存 / 会话 / Token 黑名单 |
| Nacos | 8848 / 9848 | 服务注册与配置中心 |
| RabbitMQ | 5672 / 15672 | 消息队列 / 管理界面 |
| MinIO | 9000 / 9001 | 对象存储 / 管理界面 |
| Elasticsearch | 9200 | 全文检索 |

---

## 一键启动脚本

将以下内容保存为 `start-all-middleware.bat` 放在 `collab-office/` 目录下：

```bat
@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
title Collab Office — 中间件启动

echo.
echo   ============================================
echo     Collab Office — 启动全部中间件
echo   ============================================
echo.

:: ---- MySQL ----
echo [1/5] MySQL
sc query MySQL80 | findstr "RUNNING" >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo       Already running - skip
) else (
    net start MySQL80
    echo       Started
)

:: ---- Redis ----
echo [2/5] Redis
tasklist | findstr "redis-server" >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo       Already running - skip
) else (
    set REDIS_DIR=%~dp0tools\Redis
    if exist "!REDIS_DIR!\redis-server.exe" (
        if exist "!REDIS_DIR!\redis.windows.conf" (
            start "Redis" /min "!REDIS_DIR!\redis-server.exe" "!REDIS_DIR!\redis.windows.conf"
        ) else (
            start "Redis" /min "!REDIS_DIR!\redis-server.exe"
        )
        echo       Started (localhost:6379)
    ) else (
        echo       Not found in tools\Redis - skip
    )
)

:: ---- Nacos ----
echo [3/5] Nacos
netstat -ano | findstr ":8848" >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo       Already running - skip
) else (
    set NACOS_BIN=%~dp0tools\nacos-server-2.4.3\nacos\bin
    if exist "!NACOS_BIN!\startup.cmd" (
        cd /d "!NACOS_BIN!"
        start "Nacos" /min cmd /c "startup.cmd -m standalone"
        cd /d %~dp0
        echo       Starting (this takes ~20s)...
    ) else (
        echo       Not found in tools\nacos-server-2.4.3 - skip
    )
)

:: ---- RabbitMQ ----
echo [4/5] RabbitMQ
sc query RabbitMQ | findstr "RUNNING" >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo       Already running - skip
) else (
    net start RabbitMQ
    if !ERRORLEVEL! EQU 0 (
        echo       Started
    ) else (
        echo       Not installed - skip
    )
)

:: ---- MinIO ----
echo [5/5] MinIO
netstat -ano | findstr ":9000" >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo       Already running - skip
) else (
    where minio.exe >nul 2>nul
    if %ERRORLEVEL% EQU 0 (
        if not exist "C:\minio-data" mkdir "C:\minio-data"
        set MINIO_ROOT_USER=minioadmin
        set MINIO_ROOT_PASSWORD=minioadmin
        start "MinIO" /min cmd /c "minio.exe server C:\minio-data --address :9000 --console-address :9001"
        echo       Started (localhost:9000 / 9001)
    ) else (
        echo       minio.exe not found - skip
    )
)

:: ---- 等待 Elasticsearch 不在此脚本中（需单独下载，手动启动） ----

echo.
echo   ============================================
echo     启动完成
echo   ============================================
echo.
echo   MySQL        : localhost:3306
echo   Redis        : localhost:6379
echo   Nacos        : localhost:8848
echo   RabbitMQ     : localhost:5672  (管理界面 :15672)
echo   MinIO        : localhost:9000  (管理界面 :9001)
echo   Elasticsearch: localhost:9200  (需手动启动)
echo.
pause
```

---

## 一键停止脚本

将以下内容保存为 `stop-all-middleware.bat`：

```bat
@echo off
chcp 65001 >nul
title Collab Office — 停止中间件

echo.
echo   ============================================
echo     Collab Office — 停止全部中间件
echo   ============================================
echo.

echo [1/5] Redis
taskkill /IM redis-server.exe /F >nul 2>nul
echo       Done

echo [2/5] Nacos
cd /d "%~dp0tools\nacos-server-2.4.3\nacos\bin"
call shutdown.cmd >nul 2>nul
cd /d "%~dp0"
echo       Done

echo [3/5] RabbitMQ
net stop RabbitMQ >nul 2>nul
echo       Done

echo [4/5] MinIO
taskkill /IM minio.exe /F >nul 2>nul
echo       Done

echo [5/5] Elasticsearch (Java)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":9200" ^| findstr "LISTENING"') do (
    taskkill /PID %%a /F >nul 2>nul
)
echo       Done

echo.
echo   注意: MySQL 未停止 (系统服务，请手动 net stop MySQL80)
echo.
pause
```
