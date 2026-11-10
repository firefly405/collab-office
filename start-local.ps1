param([switch]$Build, [switch]$NoBrowser, [switch]$BackgroundWorker, [switch]$MiddlewareOnly)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$runtime = Join-Path $root 'tools/native-runtime'
$stateFile = Join-Path $runtime 'processes.json'
if (-not (Test-Path "$runtime/schema-version.txt")) {
    throw 'Native database is not initialized. See LOCAL-RUN.md.'
}
if (-not $BackgroundWorker) {
    $workerFile = Join-Path $runtime 'startup-process.json'
    if (Test-Path $workerFile) {
        $entry = Get-Content $workerFile -Raw | ConvertFrom-Json
        $worker = Get-Process -Id $entry.pid -ErrorAction SilentlyContinue
        if ($worker -and $worker.StartTime.ToUniversalTime().Ticks -eq $entry.startedTicks) {
            Write-Host "Startup is already in progress. See $runtime/startup.log"
            return
        }
    }
    # WMI owns the worker so services survive the invoking terminal/job closing.
    # PowerShell 7 is preferred, but Windows PowerShell 5.1 is enough to own the worker.
    $shell = (Get-Command pwsh.exe -ErrorAction SilentlyContinue | Select-Object -First 1).Source
    if (-not $shell) { $shell = Join-Path $env:SystemRoot 'System32/WindowsPowerShell/v1.0/powershell.exe' }
    if (-not (Test-Path $shell)) { throw 'Neither pwsh.exe nor powershell.exe was found.' }
    $command = "`"$shell`" -NoProfile -WindowStyle Hidden -File `"$PSCommandPath`" -BackgroundWorker"
    if ($Build) { $command += ' -Build' }
    if ($NoBrowser) { $command += ' -NoBrowser' }
    if ($MiddlewareOnly) { $command += ' -MiddlewareOnly' }
    $result = Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{ CommandLine = $command; CurrentDirectory = $root }
    if ($result.ReturnValue -ne 0) { throw "Background startup failed (Windows error $($result.ReturnValue))." }
    $worker = Get-Process -Id $result.ProcessId -ErrorAction Stop
    @{ pid = $worker.Id; startedTicks = $worker.StartTime.ToUniversalTime().Ticks } | ConvertTo-Json | Set-Content $workerFile -Encoding UTF8
    Write-Host "Starting $(if ($MiddlewareOnly) { 'middleware' } else { 'the full application' }) in the background. See $runtime/startup.log"
    return
}
Start-Transcript -Path "$runtime/startup.log" -Force | Out-Null
trap {
    Write-Host "Startup failed: $_"
    Stop-Transcript | Out-Null
    exit 1
}
Get-Content "$root/.env" | ForEach-Object {
    if ($_ -match '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2].Trim().Trim('"').Trim("'"), 'Process')
    }
}
$java = (Get-Command java.exe -ErrorAction Stop).Source
$node = (Get-Command node.exe -ErrorAction Stop).Source
$mysql = 'C:/DevTools/mysql-8.0.43-winx64'
$nacos = "$root/tools/nacos-server-2.4.3/nacos"
$erlang = "$root/tools/erlang"
$rabbit = "$root/tools/rabbitmq_server-3.13.7"
$elastic = "$root/tools/elasticsearch-7.17.27"
foreach ($binary in @("$mysql/bin/mysqld.exe", "$erlang/bin/erl.exe", "$rabbit/sbin/rabbitmq-server.bat", "$elastic/bin/elasticsearch.bat", "$root/tools/minio.exe", "$root/tools/mailpit/mailpit.exe")) {
    if (-not (Test-Path $binary)) { throw "Required native component missing: $binary" }
}

$script:processes = @()
if (Test-Path $stateFile) { $script:processes = @(Get-Content $stateFile -Raw | ConvertFrom-Json) }
function Test-Port([int]$Port) {
    $socket = New-Object Net.Sockets.TcpClient
    try { $socket.Connect('127.0.0.1', $Port); return $true }
    catch { return $false }
    finally { $socket.Dispose() }
}
function Wait-Port([int]$Port, [string]$Name) {
    $deadline = (Get-Date).AddSeconds(90)
    while (-not (Test-Port $Port)) {
        if ((Get-Date) -gt $deadline) { throw "$Name not ready on port $Port. Check $runtime/$Name.*.log" }
        Start-Sleep -Seconds 1
    }
}
function Start-LocalProcess([string]$Name, [string]$Exe, [string[]]$Arguments, [string]$Directory, [int]$Port, [bool]$PersistConsoleLogs = $true) {
    if (Test-Port $Port) { Write-Host "[running] $Name ($Port)"; return }
    $startArgs = @{
        FilePath = $Exe
        ArgumentList = $Arguments
        WorkingDirectory = $Directory
        WindowStyle = 'Hidden'
        PassThru = $true
    }
    if ($PersistConsoleLogs) {
        $startArgs.RedirectStandardOutput = "$runtime/$Name.out.log"
        $startArgs.RedirectStandardError = "$runtime/$Name.err.log"
    }
    $process = Start-Process @startArgs
    $script:processes = @($script:processes | Where-Object name -ne $Name) + @([pscustomobject]@{ name = $Name; pid = $process.Id; startedTicks = $process.StartTime.ToUniversalTime().Ticks })
    $script:processes | ConvertTo-Json | Set-Content $stateFile -Encoding UTF8
    Write-Host "[starting] $Name ($Port)"
}

if ($Build) {
    Push-Location $root
    try {
        & mvn.cmd --batch-mode '-Dmaven.test.skip=true' package
        if ($LASTEXITCODE -ne 0) { throw 'Backend build failed.' }
    } finally { Pop-Location }
}

$env:SERVER_ADDRESS = '127.0.0.1'
$env:APP_INTERNAL_TOKEN = $env:INTERNAL_CALL_TOKEN
$env:SPRING_DATASOURCE_URL = 'jdbc:mysql://127.0.0.1:13306/collab_office?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false'
$env:SPRING_DATASOURCE_USERNAME = 'root'
$env:SPRING_DATASOURCE_PASSWORD = $env:DB_PASSWORD
$env:SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE = '4'
$env:SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE = '1'
$env:SPRING_DATA_REDIS_HOST = '127.0.0.1'
$env:SPRING_DATA_REDIS_PASSWORD = ''
$env:SPRING_CLOUD_NACOS_DISCOVERY_IP = '127.0.0.1'
$env:NACOS_ADDR = '127.0.0.1:8848'
$env:MINIO_ENDPOINT = 'http://127.0.0.1:9000'
$env:MINIO_PUBLIC_ENDPOINT = 'http://127.0.0.1:9000'
$env:SPRING_ELASTICSEARCH_URIS = 'http://127.0.0.1:9200'
$env:SPRING_RABBITMQ_HOST = '127.0.0.1'
$env:SPRING_RABBITMQ_USERNAME = $env:RABBITMQ_USERNAME
$env:SPRING_RABBITMQ_PASSWORD = $env:RABBITMQ_PASSWORD
$env:SMTP_HOST = '127.0.0.1'
$env:SMTP_PORT = '1025'
$env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH = 'false'
$env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE = 'false'
$env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_REQUIRED = 'false'
$env:LIBREOFFICE_CMD = "$root/tools/libreoffice-extract/program/soffice.exe"
$env:LIBREOFFICE_WORK_DIR = "$runtime/office-preview"
$jvm = @('-Xms32m', '-Xmx160m', '-XX:ActiveProcessorCount=2', '-XX:+UseSerialGC')

Start-LocalProcess 'mysql' "$mysql/bin/mysqld.exe" @('--no-defaults', "--basedir=$mysql", "--datadir=$runtime/mysql-data", '--port=13306', '--bind-address=127.0.0.1', '--mysqlx=OFF', '--max-connections=200', "--log-error=$runtime/mysql.log") $runtime 13306
Start-LocalProcess 'redis' "$root/tools/Redis/redis-server.exe" @('--bind', '127.0.0.1', '--port', '6379', '--dir', $runtime, '--appendonly', 'yes') $runtime 6379
Start-LocalProcess 'nacos' $java @('-Xms64m', '-Xmx256m', '-XX:ActiveProcessorCount=2', '-Dnacos.standalone=true', '-DembeddedStorage=true', "-Dnacos.home=$nacos", '-jar', "$nacos/target/nacos-server.jar", '--server.address=127.0.0.1', "--spring.config.additional-location=file:$nacos/conf/", '--spring.sql.init.platform=derby', '--nacos.inetutils.ip-address=127.0.0.1', '--nacos.core.auth.enabled=false') $nacos 8848
$env:ERLANG_HOME = $erlang
$env:ERL_EPMD_ADDRESS = '127.0.0.1'
$env:RABBITMQ_BASE = "$runtime/rabbitmq-data"
$env:RABBITMQ_CONFIG_FILE = "$runtime/rabbitmq"
$env:RABBITMQ_NODENAME = 'rabbit@localhost'
$env:RABBITMQ_SERVER_ADDITIONAL_ERL_ARGS = '+S 2:2 +A 4 -kernel inet_dist_use_interface {127,0,0,1}'
# Credentials are generated from the existing ignored .env and never printed.
@("listeners.tcp.1 = 127.0.0.1:5672", "default_user = $env:RABBITMQ_USERNAME", "default_pass = $env:RABBITMQ_PASSWORD", 'management.tcp.ip = 127.0.0.1') | Set-Content "$runtime/rabbitmq.conf" -Encoding ASCII
Start-LocalProcess 'rabbitmq' $env:ComSpec @('/c', "$rabbit/sbin/rabbitmq-server.bat") "$rabbit/sbin" 5672
$env:MINIO_ROOT_USER = $env:MINIO_ACCESS_KEY
$env:MINIO_ROOT_PASSWORD = $env:MINIO_SECRET_KEY
Start-LocalProcess 'minio' "$root/tools/minio.exe" @('server', "$runtime/minio-data", '--address', '127.0.0.1:9000', '--console-address', '127.0.0.1:19001') $runtime 9000
$env:ES_JAVA_HOME = Split-Path (Split-Path $java -Parent) -Parent
$env:ES_JAVA_OPTS = '-Xms256m -Xmx256m -XX:ActiveProcessorCount=2'
Start-LocalProcess 'elasticsearch' $env:ComSpec @('/c', "$elastic/bin/elasticsearch.bat", '-Enetwork.host=127.0.0.1', '-Ediscovery.type=single-node', '-Expack.security.enabled=false', '-Eingest.geoip.downloader.enabled=false', "-Epath.data=$runtime/es-data", "-Epath.logs=$runtime/es-logs") "$elastic/bin" 9200
Start-LocalProcess 'mailpit' "$root/tools/mailpit/mailpit.exe" @('--listen', '127.0.0.1:18025', '--smtp', '127.0.0.1:1025', '--database', "$runtime/mailpit.db") $runtime 1025
foreach ($dependency in @(@(13306, 'mysql'), @(6379, 'redis'), @(8848, 'nacos'), @(9848, 'nacos'), @(5672, 'rabbitmq'), @(9000, 'minio'), @(9200, 'elasticsearch'), @(1025, 'mailpit'))) { Wait-Port $dependency[0] $dependency[1] }
& "$root/tools/update-local-schema.ps1"
$env:MC_CONFIG_DIR = "$runtime/mc"
& "$root/tools/mc.exe" alias set local $env:MINIO_ENDPOINT $env:MINIO_ACCESS_KEY $env:MINIO_SECRET_KEY | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'MinIO authentication failed.' }
& "$root/tools/mc.exe" mb --ignore-existing "local/$env:MINIO_BUCKET" | Out-Null
& "$root/tools/mc.exe" anonymous set none "local/$env:MINIO_BUCKET" | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'MinIO bucket initialization failed.' }
$existingIndices = @((Invoke-RestMethod 'http://127.0.0.1:9200/_cat/indices?format=json').index)
foreach ($index in @('document_index', 'task_index')) {
    if ($existingIndices -notcontains $index) { Invoke-RestMethod "http://127.0.0.1:9200/$index" -Method Put | Out-Null }
}

if ($MiddlewareOnly) {
    Write-Host 'Middleware is ready. Now start the services from the IDEA run configurations.'
    Stop-Transcript | Out-Null
    return
}

. "$root/tools/demo-launcher-core.ps1"
$modules = Get-DemoModules
# Bounded rolling logs per service. Spring Boot console output is not persisted because
# Start-Process redirect files cannot be rotated while the child owns them on Windows.
# Property names use the Spring Boot 3.x logging.logback.rollingpolicy.* namespace;
# the older logging.file.max-size family is deprecated and may stop working.
foreach ($module in $modules) {
    $directory = "$root/$($module.Directory)"
    $name = $module.Name
    $jar = Get-ChildItem "$directory/target/*.jar" -ErrorAction Stop | Select-Object -First 1
    if (-not $jar) { throw "Build missing for $name. Run start-local.ps1 -Build." }
    $logArgs = @(
        "-Dlogging.file.name=$runtime/$name-app.log",
        '-Dlogging.logback.rollingpolicy.max-file-size=20MB',
        '-Dlogging.logback.rollingpolicy.max-history=3',
        '-Dlogging.logback.rollingpolicy.total-size-cap=60MB'
    )
    Start-LocalProcess $name $java ($jvm + $logArgs + @('-jar', $jar.FullName)) $directory $module.Port $false
    Wait-Port $module.Port $name
}
Wait-Port 9090 'collab-office-ws'
Start-LocalProcess 'web' $node @('node_modules/vite/bin/vite.js', '--host', '127.0.0.1', '--port', '3000', '--strictPort') "$root/collab-office-web" 3000
Wait-Port 3000 'web'
$health = Invoke-RestMethod 'http://127.0.0.1:8080/api/health'
Write-Host 'All local components are listening. Web: http://127.0.0.1:3000/'
Write-Host "Logs: $runtime"
if (-not $NoBrowser) { Start-Process 'http://127.0.0.1:3000/' }
Stop-Transcript | Out-Null
