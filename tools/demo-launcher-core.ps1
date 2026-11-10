$ErrorActionPreference = 'Stop'

function Get-DemoModules {
    @(
        @{Name='user-service';Directory='collab-office-service/user-service';Port=8101},
        @{Name='document-service';Directory='collab-office-service/document-service';Port=8102},
        @{Name='collaboration-service';Directory='collab-office-service/collaboration-service';Port=8103},
        @{Name='message-service';Directory='collab-office-service/message-service';Port=8104},
        @{Name='meeting-service';Directory='collab-office-service/meeting-service';Port=8105},
        @{Name='task-service';Directory='collab-office-service/task-service';Port=8106},
        @{Name='file-service';Directory='collab-office-service/file-service';Port=8107},
        @{Name='notification-service';Directory='collab-office-service/notification-service';Port=8109},
        @{Name='search-service';Directory='collab-office-service/search-service';Port=8110},
        @{Name='collab-office-ws';Directory='collab-office-ws';Port=9001},
        @{Name='collab-office-gateway';Directory='collab-office-gateway';Port=8080}
    )
}

function Find-DemoCommand([string]$Name, [string[]]$Candidates) {
    $command = Get-Command $Name -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($command) { return $command.Source }
    foreach ($candidate in $Candidates) {
        $file = Get-Item $candidate -ErrorAction SilentlyContinue | Where-Object { -not $_.PSIsContainer } | Select-Object -First 1
        if ($file) { return $file.FullName }
    }
    return $null
}

function Get-DemoVersion([string]$Exe, [string]$Arguments) {
    $info = New-Object Diagnostics.ProcessStartInfo
    $info.FileName=$Exe; $info.Arguments=$Arguments; $info.UseShellExecute=$false
    $info.CreateNoWindow=$true; $info.RedirectStandardOutput=$true; $info.RedirectStandardError=$true
    $process = New-Object Diagnostics.Process
    $process.StartInfo=$info
    try {
        [void]$process.Start()
        $stdout=$process.StandardOutput.ReadToEndAsync(); $stderr=$process.StandardError.ReadToEndAsync()
        if (-not $process.WaitForExit(8000)) { $process.Kill(); throw '版本检查超时' }
        if ($process.ExitCode -ne 0) { throw '无法执行版本检查' }
        return ($stdout.Result + $stderr.Result).Trim()
    } finally { $process.Dispose() }
}

function Read-DemoConfig([string]$File) {
    $values=@{}
    if (Test-Path -LiteralPath $File) {
        foreach ($line in Get-Content -LiteralPath $File -Encoding UTF8) {
            if ($line -match '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$') { $values[$matches[1]]=$matches[2].Trim().Trim('"').Trim("'") }
        }
    }
    return $values
}

function Get-DemoMissingConfig($Values) {
    $required=@('JWT_SECRET','INTERNAL_CALL_TOKEN','DB_PASSWORD','RABBITMQ_USERNAME','RABBITMQ_PASSWORD','MINIO_ACCESS_KEY','MINIO_SECRET_KEY','MINIO_BUCKET')
    @($required | Where-Object { -not $Values.ContainsKey($_) -or [string]::IsNullOrWhiteSpace($Values[$_]) })
}

function Test-DemoManagedListener([int]$OwnerId, [string]$Name, $Inventory, $Managed) {
    $current=$OwnerId
    for ($depth=0; $depth -lt 8 -and $current -gt 0; $depth++) {
        if ($Managed.ContainsKey($current) -and $Managed[$current] -eq $Name) { return $true }
        if (-not $Inventory.ContainsKey($current)) { break }
        $current=[int]$Inventory[$current].ParentProcessId
    }
    return $false
}

function Get-DemoEnvironment([string]$Root) {
    $Root=[IO.Path]::GetFullPath($Root)
    $rows=New-Object 'Collections.Generic.List[object]'
    function Add-Check($Name, [bool]$Passed, $Detail, $Help='', $Url='', [bool]$Required=$true) {
        $state=if($Passed){'通过'}elseif($Required){'缺失 / 不符合'}else{'提醒'}
        $rows.Add([pscustomobject]@{Name=$Name;Passed=$Passed;Required=$Required;State=$state;Detail=$Detail;Help=$Help;Url=$Url})
    }
    Add-Check 'Windows 64位' ([Environment]::OSVersion.Platform -eq 'Win32NT' -and [Environment]::Is64BitOperatingSystem) ([Environment]::OSVersion.VersionString) '此启动器适用于 Windows 10/11 64位。'
    Add-Check '项目路径' ($Root -notmatch '\s') $Root '现有原生组件启动参数要求项目放在不含空格的目录；迁移时请保留 tools/native-runtime。'
    $shell=Find-DemoCommand 'pwsh.exe' @("$env:ProgramFiles/PowerShell/7/pwsh.exe","$env:LOCALAPPDATA/Programs/PowerShell/7/pwsh.exe","$env:USERPROFILE/.cache/codex-runtimes/codex-primary-runtime/dependencies/native/powershell/pwsh.exe")
    $java=Find-DemoCommand 'java.exe' @("$env:JAVA_HOME/bin/java.exe","$env:ProgramFiles/Microsoft/jdk-17*/bin/java.exe")
    $node=Find-DemoCommand 'node.exe' @("$env:ProgramFiles/nodejs/node.exe")
    $maven=Find-DemoCommand 'mvn.cmd' @('C:/DevTools/apache-maven-*/bin/mvn.cmd')
    foreach ($tool in @(
        @{Name='PowerShell 7';Exe=$shell;Args='-NoProfile -Command "$PSVersionTable.PSVersion.ToString()"';Pattern='^7\.';Help='安装 PowerShell 7 的 Windows x64 MSI，然后重新检测。';Url='https://github.com/PowerShell/PowerShell/releases'},
        @{Name='Java 17';Exe=$java;Args='-version';Pattern='version "17\.';Help='安装 Microsoft OpenJDK 17 x64，配置 JAVA_HOME 和 PATH。';Url='https://learn.microsoft.com/java/openjdk/download'},
        @{Name='Node.js 20+';Exe=$node;Args='--version';Pattern='^v(2[0-9]|[3-9][0-9])\.';Help='安装 Node.js 20 或 22 x64，安装后重新检测。';Url='https://nodejs.org/en/download'}
    )) {
        $version='未找到可执行程序'; $ok=$false
        if ($tool.Exe) { try { $version=Get-DemoVersion $tool.Exe $tool.Args; $ok=$version -match $tool.Pattern } catch { $version=$_.Exception.Message } }
        Add-Check $tool.Name $ok (($version -split "`r?`n")[0]) $tool.Help $tool.Url
    }
    Add-Check 'Maven（仅重新构建需要）' ([bool]$maven) $(if($maven){$maven}else{'未找到；已有 JAR 时不影响启动'}) '源码重建使用 Maven 3.8+；安装后将 bin 加入 PATH。' 'https://maven.apache.org/download.cgi' $false
    $components=@(
        @{Name='MySQL 8.0.43';Files=@('C:/DevTools/mysql-8.0.43-winx64/bin/mysqld.exe','C:/DevTools/mysql-8.0.43-winx64/bin/mysql.exe','C:/DevTools/mysql-8.0.43-winx64/bin/mysqldump.exe');Help='下载 Windows x64 ZIP，解压到 C:/DevTools/mysql-8.0.43-winx64。';Url='https://downloads.mysql.com/archives/community/'},
        @{Name='Redis 5';Files=@("$Root/tools/Redis/redis-server.exe");Help='下载兼容 Windows 的 Redis 5 ZIP，解压到 tools/Redis。';Url='https://github.com/tporadowski/redis/releases'},
        @{Name='Nacos 2.4.3';Files=@("$Root/tools/nacos-server-2.4.3/nacos/target/nacos-server.jar","$Root/tools/nacos-server-2.4.3/nacos/conf/application.properties");Help='下载2.4.3发行包，保留 nacos 子目录，解压到 tools/nacos-server-2.4.3。';Url='https://github.com/alibaba/nacos/releases/tag/2.4.3'},
        @{Name='Erlang';Files=@("$Root/tools/erlang/bin/erl.exe");Help='安装/解压兼容 RabbitMQ 3.13 的 Erlang 26 到 tools/erlang。';Url='https://www.erlang.org/downloads'},
        @{Name='RabbitMQ 3.13.7';Files=@("$Root/tools/rabbitmq_server-3.13.7/sbin/rabbitmq-server.bat");Help='下载3.13.7 Windows通用包，解压到 tools/rabbitmq_server-3.13.7。';Url='https://github.com/rabbitmq/rabbitmq-server/releases/tag/v3.13.7'},
        @{Name='Elasticsearch 7.17.27';Files=@("$Root/tools/elasticsearch-7.17.27/bin/elasticsearch.bat");Help='下载7.17.27 Windows x64 ZIP，解压到 tools/elasticsearch-7.17.27。';Url='https://www.elastic.co/downloads/past-releases/elasticsearch-7-17-27'},
        @{Name='MinIO / mc';Files=@("$Root/tools/minio.exe","$Root/tools/mc.exe");Help='官方Windows二进制下载已下架。请向项目维护者获取配套离线工具包，恢复 tools/minio.exe 与 tools/mc.exe；或按官方源码说明编译Windows版本。mc源码：https://github.com/minio/mc。不要用空文件代替。';Url='https://github.com/minio/minio'},
        @{Name='Mailpit';Files=@("$Root/tools/mailpit/mailpit.exe");Help='下载 Windows amd64 ZIP，解压到 tools/mailpit。';Url='https://github.com/axllent/mailpit/releases'}
    )
    foreach ($component in $components) {
        $missing=@($component.Files | Where-Object { -not (Test-Path -LiteralPath $_ -PathType Leaf) })
        Add-Check $component.Name ($missing.Count -eq 0) $(if($missing.Count){'缺少：'+($missing -join '；')}else{'所需组件文件存在'}) $component.Help $component.Url
    }
    $office="$Root/tools/libreoffice-extract/program/soffice.exe"
    Add-Check 'LibreOffice（PDF导出需要）' (Test-Path -LiteralPath $office -PathType Leaf) $(if(Test-Path -LiteralPath $office){'本机Office转换工具存在'}else{'缺少转换工具，启动不受影响，但PDF导出不可用'}) '下载 Windows x64 版；恢复配套离线解压目录 tools/libreoffice-extract，保证 program/soffice.exe 存在。普通安装路径不会被现有启动脚本自动使用。' 'https://www.libreoffice.org/download/download-libreoffice/' $false
    $config=Read-DemoConfig "$Root/.env"
    $missingConfig=Get-DemoMissingConfig $config
    $jwtOk=$config.ContainsKey('JWT_SECRET') -and [Text.Encoding]::UTF8.GetByteCount($config.JWT_SECRET) -ge 32
    Add-Check '本机配置（不展示密钥）' ($missingConfig.Count -eq 0 -and $jwtOk) $(if($missingConfig.Count){'缺少配置项：'+($missingConfig -join '、')}elseif(-not $jwtOk){'JWT_SECRET 必须至少32字节'}else{'必填配置已填写；密钥不在界面展示'}) '参考 .env.example 和 LOCAL-RUN.md 配置 .env；不要覆盖现有数据库密码。'
    $marker="$Root/tools/native-runtime/schema-version.txt"
    $schema=if(Test-Path -LiteralPath $marker){(Get-Content -LiteralPath $marker -Raw).Trim()}else{''}
    $databaseOk=$schema -eq '23' -and (Test-Path -LiteralPath "$Root/tools/native-runtime/mysql-data/mysql" -PathType Container)
    Add-Check '本地数据库与版本23' $databaseOk $(if($databaseOk){'项目独立数据目录和版本标记存在'}else{'缺少本地数据库，或版本未到23；不会自动新建/清空数据库'}) '按 LOCAL-RUN.md 恢复本机备份或审核初始化/升级；只允许127.0.0.1:13306，不能只修改版本标记。'
    $missingJars=@(Get-DemoModules | Where-Object { -not (Test-Path -LiteralPath "$Root/$($_.Directory)/target/$($_.Name)-1.0.0-SNAPSHOT.jar" -PathType Leaf) } | ForEach-Object {$_.Name})
    Add-Check '11个后端构建产物' ($missingJars.Count -eq 0) $(if($missingJars.Count){'缺少：'+($missingJars -join '、')}else{'11个服务JAR存在'}) '在 collab-office 目录，先停应用，再运行 mvn --batch-mode package。启动器不会自动安装依赖或覆盖运行中的JAR。'
    $vite="$Root/collab-office-web/node_modules/vite/bin/vite.js"
    Add-Check '前端依赖' (Test-Path -LiteralPath $vite -PathType Leaf) $(if(Test-Path -LiteralPath $vite){'Vite本机依赖存在'}else{'缺少 node_modules/vite'}) '在 collab-office/collab-office-web 运行 npm install；再运行 npm run build 验证。' 'https://docs.npmjs.com/cli/commands/npm-install'
    Add-Check '启动脚本' ((Test-Path -LiteralPath "$Root/start-local.ps1") -and (Test-Path -LiteralPath "$Root/tools/update-local-schema.ps1")) '复用项目原有本机启动与迁移守卫' '请恢复完整源码中的 start-local.ps1 和 tools/update-local-schema.ps1。'
    try {
        $memory=Get-CimInstance Win32_OperatingSystem -ErrorAction Stop
        Add-Check '可用内存' ($memory.FreePhysicalMemory -ge 2MB) ('当前约 '+[Math]::Round($memory.FreePhysicalMemory/1MB,1)+' GB') '建议16GB内存；可用不足2GB时先关闭占内存程序。' '' $false
        $inventory=@{}; foreach($process in Get-CimInstance Win32_Process -ErrorAction Stop){$inventory[[int]$process.ProcessId]=$process}
        $managed=@{}
        $stateFile="$Root/tools/native-runtime/processes.json"
        if(Test-Path -LiteralPath $stateFile){
            $records=Get-Content -LiteralPath $stateFile -Raw | ConvertFrom-Json
            foreach($entry in $records){
                $live=Get-Process -Id $entry.pid -ErrorAction SilentlyContinue
                if($live -and $live.StartTime.ToUniversalTime().Ticks -eq $entry.startedTicks){$managed[[int]$entry.pid]=$entry.name}
            }
        }
        $ports=@(@{Name='mysql';Port=13306},@{Name='redis';Port=6379},@{Name='nacos';Port=8848},@{Name='nacos';Port=9848},@{Name='rabbitmq';Port=5672},@{Name='minio';Port=9000},@{Name='minio';Port=19001},@{Name='elasticsearch';Port=9200},@{Name='mailpit';Port=1025},@{Name='mailpit';Port=18025}) + @(Get-DemoModules) + @(@{Name='collab-office-ws';Port=9090},@{Name='web';Port=3000})
        $listeners=@(Get-NetTCPConnection -State Listen -ErrorAction Stop)
        foreach($port in $ports){
            $occupied=@($listeners | Where-Object LocalPort -eq $port.Port)
            $foreign=@($occupied | Where-Object { -not (Test-DemoManagedListener ([int]$_.OwningProcess) $port.Name $inventory $managed) })
            if($port.Name -eq 'mysql' -and $occupied.Count){
                $expected=('--datadir='+"$Root/tools/native-runtime/mysql-data").Replace('\','/')
                $foreign+=@($occupied | Where-Object { $_.LocalAddress -ne '127.0.0.1' -or -not $inventory[[int]$_.OwningProcess].CommandLine.Replace('\','/').Contains($expected) })
            }
            $detail=if($foreign.Count){'被其他/未登记进程占用，PID：'+(($foreign.OwningProcess | Select-Object -Unique) -join ',')}elseif($occupied.Count){'本项目已运行，可复用'}else{'空闲，可启动'}
            Add-Check ("端口 "+$port.Port+' / '+$port.Name) ($foreign.Count -eq 0) $detail '请核实占用程序后手工处理再检测；启动器不会杀掉未知进程或连接远端服务。'
        }
    } catch { Add-Check '本机进程与端口检查' $false '无法读取进程/端口，不能安全判断是否可启动' '使用当前Windows账号重新打开；检查系统CIM与网络查询权限。' }
    $blocked=@($rows | Where-Object {$_.Required -and -not $_.Passed})
    [pscustomobject]@{Allowed=($blocked.Count -eq 0);BlockedCount=$blocked.Count;CheckedAt=(Get-Date).ToString('yyyy-MM-dd HH:mm:ss');Root=$Root;Shell=$shell;Java=$java;Node=$node;Maven=$maven;Rows=@($rows.ToArray())}
}

function Get-DemoRuntime([string]$Root) {
    $ports=@((Get-DemoModules).Port)+@(9090,3000,13306,6379,8848,9848,5672,9000,9200,1025)
    $missing=New-Object 'Collections.Generic.List[int]'
    foreach($port in $ports){
        $socket=New-Object Net.Sockets.TcpClient
        try { $result=$socket.BeginConnect('127.0.0.1',$port,$null,$null); if(-not $result.AsyncWaitHandle.WaitOne(200)){throw 'not listening'}; $socket.EndConnect($result) }
        catch { $missing.Add($port) } finally { $socket.Dispose() }
    }
    $healthy=$false
    if(-not $missing.Count){
        try {
            $health=Invoke-RestMethod 'http://127.0.0.1:8080/api/health' -TimeoutSec 3
            $web=Invoke-WebRequest 'http://127.0.0.1:3000/' -UseBasicParsing -TimeoutSec 3
            $healthy=$health.status -eq 'UP' -and $web.StatusCode -eq 200
        } catch {}
    }
    [pscustomobject]@{Ready=$healthy;MissingPorts=@($missing.ToArray())}
}

function Start-DemoApplication([string]$Root, [switch]$NoBrowser) {
    $check=Get-DemoEnvironment $Root
    if(-not $check.Allowed){throw ('环境检查未通过：'+(($check.Rows | Where-Object {$_.Required -and -not $_.Passed} | ForEach-Object {$_.Name}) -join '、'))}
    $running=Get-DemoRuntime $Root
    $reused=$running.Ready
    if(-not $running.Ready){
        $paths=@($check.Java,$check.Node,$check.Maven,$check.Shell) | Where-Object {$_} | ForEach-Object {Split-Path $_ -Parent} | Select-Object -Unique
        $env:PATH=($paths -join ';')+';'+$env:PATH
        & $check.Shell -NoProfile -File "$Root/start-local.ps1" -NoBrowser | Out-Null
        if($LASTEXITCODE -ne 0){throw '启动脚本执行失败，请查看启动日志。'}
        $deadline=(Get-Date).AddMinutes(10)
        do {
            Start-Sleep -Seconds 2
            $running=Get-DemoRuntime $Root
            if($running.Ready){break}
            $workerFile="$Root/tools/native-runtime/startup-process.json"
            if(Test-Path -LiteralPath $workerFile){
                $entry=Get-Content -LiteralPath $workerFile -Raw | ConvertFrom-Json
                $worker=Get-Process -Id $entry.pid -ErrorAction SilentlyContinue
                if(-not $worker -or $worker.StartTime.ToUniversalTime().Ticks -ne $entry.startedTicks){throw '启动进程已退出，但系统尚未就绪；请查看日志后重新检测。'}
            }
        } while((Get-Date) -lt $deadline)
        if(-not $running.Ready){throw '启动超过10分钟仍未就绪；后台保留，请检查启动日志。'}
    }
    if(-not $NoBrowser){Start-Process 'http://127.0.0.1:3000/'}
    [pscustomobject]@{Ready=$true;Url='http://127.0.0.1:3000/';ReusedExisting=$reused;Message='前端、后端及网关已就绪'}
}
