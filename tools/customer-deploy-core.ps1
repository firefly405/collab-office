$ErrorActionPreference = 'Stop'

function Get-CustomerDockerPath {
    $command = Get-Command docker.exe -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($command) { return $command.Source }
    $candidates = @(
        "$env:ProgramFiles/Docker/Docker/resources/bin/docker.exe",
        "$env:ProgramFiles/Docker/Docker/resources/cli-plugins/docker.exe",
        "$env:LOCALAPPDATA/Docker/cli-plugins/docker.exe"
    )
    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath $candidate -PathType Leaf) { return (Get-Item -LiteralPath $candidate).FullName }
    }
    return $null
}

function Get-CustomerDesktopPath {
    foreach ($candidate in @(
        "$env:ProgramFiles/Docker/Docker/Docker Desktop.exe",
        "$env:ProgramFiles/Docker/Docker Desktop.exe",
        "$env:LOCALAPPDATA/Programs/Docker/Docker/Docker Desktop.exe"
    )) {
        if (Test-Path -LiteralPath $candidate -PathType Leaf) { return (Get-Item -LiteralPath $candidate).FullName }
    }
    return $null
}

function Get-CustomerWingetPath {
    $command = Get-Command winget.exe -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($command) { return $command.Source }
    return $null
}

function Get-CustomerWslPath {
    $command = Get-Command wsl.exe -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($command) { return $command.Source }
    return Join-Path $env:WINDIR 'System32/wsl.exe'
}

function Test-CustomerWslReady([string]$Wsl) {
    if (-not (Test-Path -LiteralPath $Wsl -PathType Leaf)) { return $false }
    $status = @(& $Wsl --status 2>&1)
    if ($LASTEXITCODE -ne 0) { return $false }
    $defaultVersion = $status | Where-Object { $_ -match '(Default Version|默认版本)' } | Select-Object -First 1
    return (-not $defaultVersion) -or ($defaultVersion -match ':\s*2\s*$')
}

function Invoke-CustomerProcess {
    param([string]$FilePath, [string[]]$Arguments, [string]$WorkingDirectory, [int]$TimeoutSeconds = 3600)
    $info = New-Object Diagnostics.ProcessStartInfo
    $info.FileName = $FilePath
    $info.Arguments = (($Arguments | ForEach-Object { if ($_ -match '[\s"]') { '"' + $_.Replace('"','\"') + '"' } else { $_ } }) -join ' ')
    $info.WorkingDirectory = $WorkingDirectory
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    $process = New-Object Diagnostics.Process
    $process.StartInfo = $info
    try {
        [void]$process.Start()
        $stdoutTask = $process.StandardOutput.ReadLineAsync()
        $stderrTask = $process.StandardError.ReadLineAsync()
        $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
        while (-not $process.HasExited) {
            if ([DateTime]::UtcNow -ge $deadline) {
                try { $process.Kill() } catch { }
                throw "命令超过 $TimeoutSeconds 秒仍未完成：$FilePath"
            }
            while ($stdoutTask -and $stdoutTask.IsCompleted) {
                $line = $stdoutTask.Result
                if ($null -eq $line) { $stdoutTask = $null } else { Write-Output $line; $stdoutTask = $process.StandardOutput.ReadLineAsync() }
            }
            while ($stderrTask -and $stderrTask.IsCompleted) {
                $line = $stderrTask.Result
                if ($null -eq $line) { $stderrTask = $null } else { Write-Output $line; $stderrTask = $process.StandardError.ReadLineAsync() }
            }
            Start-Sleep -Milliseconds 20
        }
        while ($stdoutTask -and $stdoutTask.IsCompleted) { $line = $stdoutTask.Result; if ($null -eq $line) { break }; Write-Output $line; $stdoutTask = $process.StandardOutput.ReadLineAsync() }
        while ($stderrTask -and $stderrTask.IsCompleted) { $line = $stderrTask.Result; if ($null -eq $line) { break }; Write-Output $line; $stderrTask = $process.StandardError.ReadLineAsync() }
        if ($process.ExitCode -ne 0) { throw "命令执行失败（退出码 $($process.ExitCode)）：$FilePath $($Arguments -join ' ')" }
    } finally { $process.Dispose() }
}

function Test-CustomerDockerReady([string]$Docker) {
    if (-not $Docker) { return $false }
    & $Docker info *> $null
    return $LASTEXITCODE -eq 0
}

function Get-CustomerWebUrl([string]$Root) {
    $port = 3030
    $environmentPath = Join-Path $Root '.env'
    if (Test-Path -LiteralPath $environmentPath) {
        $line = Get-Content -LiteralPath $environmentPath | Where-Object { $_ -match '^WEB_PORT=' } | Select-Object -First 1
        if ($line -and (($line -split '=',2)[1].Trim() -as [int])) { $port = [int](($line -split '=',2)[1].Trim()) }
    }
    return "http://localhost:$port"
}

function Get-CustomerGatewayHealthUrl([string]$Root) {
    $port = 18090
    $environmentPath = Join-Path $Root '.env'
    if (Test-Path -LiteralPath $environmentPath) {
        $line = Get-Content -LiteralPath $environmentPath | Where-Object { $_ -match '^GATEWAY_PORT=' } | Select-Object -First 1
        if ($line -and (($line -split '=',2)[1].Trim() -as [int])) { $port = [int](($line -split '=',2)[1].Trim()) }
    }
    return "http://localhost:$port/api/health"
}

function Get-CustomerDeploymentState([string]$Root) {
    $Root = [IO.Path]::GetFullPath($Root)
    $docker = Get-CustomerDockerPath
    $desktop = Get-CustomerDesktopPath
    $winget = Get-CustomerWingetPath
    $wsl = Get-CustomerWslPath
    $wslReady = Test-CustomerWslReady $wsl
    $hardwareVirtualization = [bool]((Get-CimInstance Win32_Processor -ErrorAction SilentlyContinue | Where-Object VirtualizationFirmwareEnabled).Count -or (Get-CimInstance Win32_ComputerSystem -ErrorAction SilentlyContinue).HypervisorPresent)
    $memoryGb = [math]::Floor((Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory / 1GB)
    $compose = $false
    if ($docker) { & $docker compose version *> $null; $compose = $LASTEXITCODE -eq 0 }
    $offline = @(Get-ChildItem -LiteralPath (Join-Path $Root 'offline-packages') -Filter 'collab-office-images.tar' -Recurse -File -ErrorAction SilentlyContinue | Where-Object {
        (Test-Path (Join-Path $_.DirectoryName 'docker-offline-import.ps1')) -and (Test-Path (Join-Path $_.DirectoryName 'docker-compose.yml'))
    } | Select-Object -First 1)
    $deploymentRoot = if($offline.Count){$offline[0].DirectoryName}else{$Root}
    $rows = @(
        [pscustomobject]@{Name='Docker Desktop';Passed=[bool]$desktop;State=if($desktop){'已安装'}else{'缺失'};Detail=if($desktop){$desktop}else{'客户需要先安装 Docker Desktop，并选择 Linux Containers。'};Help='官方下载：https://www.docker.com/products/docker-desktop/'},
        [pscustomobject]@{Name='Docker 引擎';Passed=([bool]($docker -and $desktop));State=if(Test-CustomerDockerReady $docker){'运行中'}elseif($docker -and $desktop){'未启动（部署时自动启动）'}else{'不可用'};Detail=if(Test-CustomerDockerReady $docker){'Docker 可以接受部署命令'}else{'部署时会自动打开 Docker Desktop，最多等待5分钟。'};Help='请等待 Docker Desktop 左下角状态变为 Running。'},
        [pscustomobject]@{Name='WSL 2';Passed=$wslReady;State=if($wslReady){'可用'}else{'缺失或需要重启'};Detail=if($wslReady){'Windows Linux 容器运行环境可用'}else{'部署时会请求管理员权限安装 WSL；系统可能要求重启后再次双击。'};Help='启用 WSL 2 需要 Windows 管理员确认和硬件虚拟化支持。'},
        [pscustomobject]@{Name='硬件虚拟化与内存';Passed=($hardwareVirtualization -and $memoryGb -ge 8);State=if(-not $hardwareVirtualization){'未启用'}elseif($memoryGb -lt 8){'内存不足'}else{'通过'};Detail="虚拟化：$hardwareVirtualization；内存：${memoryGb}GB（建议至少 8GB）。";Help='在 BIOS/UEFI 开启 Intel VT-x 或 AMD SVM；启用后重启 Windows。'},
        [pscustomobject]@{Name='Docker Compose V2';Passed=$compose;State=if($compose){'可用'}else{'缺失'};Detail=if($compose){'Compose V2 已安装'}else{'Docker Desktop 未提供 docker compose 命令。'};Help='更新或重新安装最新版 Docker Desktop。'},
        [pscustomobject]@{Name='项目部署文件';Passed=((Test-Path (Join-Path $Root 'docker-compose.yml')) -and (Test-Path (Join-Path $Root '.env.example')));State='检查中';Detail='docker-compose.yml 与 .env.example';Help='请从完整交付包重新解压，不要只复制一个启动文件。'},
        [pscustomobject]@{Name='部署方式';Passed=($compose -and ((Test-Path (Join-Path $Root 'docker-compose.yml')) -and (Test-Path (Join-Path $Root '.env.example'))));State=if($offline.Count){'离线镜像可用'}else{'联网构建'};Detail=if($offline.Count){'检测到离线镜像包，将不要求下载镜像。'}else{'首次部署需要下载镜像并构建应用，耗时可能较长。'};Help=if($offline.Count){$offline[0].FullName}else{'请保持网络畅通；Docker 会从镜像仓库下载所需组件。'}},
        [pscustomobject]@{Name='系统内存';Passed=([Environment]::Is64BitOperatingSystem);State=if([Environment]::Is64BitOperatingSystem){'通过'}else{'不符合'};Detail='建议 Docker Desktop 至少分配 8 GB 内存。';Help='在 Docker Desktop Settings > Resources 中调整内存。'}
    )
    $blocked = @($rows | Where-Object { -not $_.Passed })
    $projectFiles = (Test-Path (Join-Path $Root 'docker-compose.yml')) -and (Test-Path (Join-Path $Root '.env.example'))
    [pscustomobject]@{Allowed=($blocked.Count -eq 0);CanAttempt=($projectFiles -and ($docker -or $winget) -and $hardwareVirtualization -and $memoryGb -ge 8);BlockedCount=$blocked.Count;Docker=$docker;Desktop=$desktop;Winget=$winget;Wsl=$wsl;WslReady=$wslReady;OfflineArchive=if($offline.Count){$offline[0].FullName}else{$null};DeploymentRoot=$deploymentRoot;Rows=$rows;WebUrl=(Get-CustomerWebUrl $deploymentRoot);Root=$Root}
}

function Install-CustomerDocker([string]$Winget) {
    if (-not $Winget) { throw '系统没有 winget，无法自动安装 Docker Desktop。请点击“安装 Docker Desktop”打开官方安装页。' }
    Write-Output '正在通过 Windows 应用安装器安装 Docker Desktop；如出现 UAC 请允许。'
    foreach ($line in (Invoke-CustomerProcess $Winget @('install','--id','Docker.DockerDesktop','--exact','--silent','--accept-source-agreements','--accept-package-agreements','--no-upgrade') (Get-Location).Path 1800)) { Write-Output $line }
    $desktop = Get-CustomerDesktopPath
    if (-not $desktop) { throw 'Docker Desktop 安装命令已结束，但未找到安装程序；请重启电脑后重新部署。' }
    Start-Process -FilePath $desktop | Out-Null
}

function Start-CustomerDeployment([string]$Root) {
    $state = Get-CustomerDeploymentState $Root
    if (-not $state.Allowed -and -not $state.CanAttempt) { throw ('部署条件未满足：' + (($state.Rows | Where-Object { -not $_.Passed } | ForEach-Object Name) -join '、')) }
    if (-not $state.WslReady) {
        Write-Output '正在请求管理员权限安装 WSL 2。若 Windows 要求重启，请重启后再次双击客户一键部署.cmd。'
        $wslInstall = Start-Process -FilePath $state.Wsl -ArgumentList @('--install','--no-distribution') -Verb RunAs -Wait -PassThru -WindowStyle Hidden
        if ($wslInstall.ExitCode -in @(3010,1641)) { throw 'WSL 已安装，但 Windows 需要重启。重启后再次双击客户一键部署.cmd。' }
        if ($wslInstall.ExitCode -ne 0) { throw "WSL 安装失败，退出码 $($wslInstall.ExitCode)。请确认 BIOS 虚拟化已开启，并按 Windows 提示重启。" }
        $wslDefault = Start-Process -FilePath $state.Wsl -ArgumentList @('--set-default-version','2') -Verb RunAs -Wait -PassThru -WindowStyle Hidden
        if ($wslDefault.ExitCode -ne 0) { throw "设置 WSL 2 默认版本失败，退出码 $($wslDefault.ExitCode)。请按 Windows 提示重启后重新部署。" }
        if (-not (Test-CustomerWslReady $state.Wsl)) { throw 'WSL 已安装或正在启用，但尚未就绪。请重启电脑后再次双击客户一键部署.cmd。' }
        $state = Get-CustomerDeploymentState $Root
    }
    if (-not $state.Docker -or -not $state.Desktop) {
        Install-CustomerDocker $state.Winget
        $state = Get-CustomerDeploymentState $Root
        if (-not $state.Docker -or -not $state.Desktop) { throw 'Docker Desktop 安装后仍未被系统识别，请重启电脑后再次双击部署程序。' }
    }
    if (-not $state.Allowed) { throw ('Docker 已准备，但部署条件仍未满足：' + (($state.Rows | Where-Object { -not $_.Passed } | ForEach-Object Name) -join '、')) }
    $docker = $state.Docker
    if (-not (Test-CustomerDockerReady $docker)) {
        if (-not $state.Desktop) { throw '未找到 Docker Desktop。请先安装后重新检测。' }
        Write-Output '正在启动 Docker Desktop，请等待引擎就绪…'
        Start-Process -FilePath $state.Desktop | Out-Null
        $deadline = (Get-Date).AddMinutes(5)
        do { Start-Sleep -Seconds 3 } while (-not (Test-CustomerDockerReady $docker) -and (Get-Date) -lt $deadline)
        if (-not (Test-CustomerDockerReady $docker)) { throw 'Docker Desktop 5分钟内没有就绪。请检查 WSL 2、硬件虚拟化和 Docker Desktop 状态后重试。' }
    }
    $mode = if ($state.OfflineArchive) { '离线镜像部署' } else { '在线构建部署' }
    Write-Output "开始$mode；首次部署可能需要数分钟，请不要关闭窗口。"
    if ($state.OfflineArchive) {
        & (Join-Path $state.DeploymentRoot 'docker-offline-import.ps1')
        if ($LASTEXITCODE -ne 0) { throw '离线镜像部署失败，请查看 Docker 日志。' }
    } else {
        foreach ($line in (Invoke-CustomerProcess $docker @('compose','up','--build','--detach','--remove-orphans') $Root 3600)) { Write-Output $line }
    }
    $deadline = (Get-Date).AddMinutes(10)
    $ready = $false
    do {
        Start-Sleep -Seconds 5
        try {
            $webResponse = Invoke-WebRequest -UseBasicParsing -Uri $state.WebUrl -TimeoutSec 5
            $apiResponse = Invoke-WebRequest -UseBasicParsing -Uri (Get-CustomerGatewayHealthUrl $state.DeploymentRoot) -TimeoutSec 5
            $ready = $webResponse.StatusCode -eq 200 -and $apiResponse.StatusCode -eq 200
        } catch { $ready = $false }
    } while (-not $ready -and (Get-Date) -lt $deadline)
    if (-not $ready) { throw '容器已启动，但网页10分钟内未就绪。请查看部署日志。' }
    Start-Process $state.WebUrl
    [pscustomobject]@{Ready=$true;Url=$state.WebUrl;Mode=$mode;Message='项目已部署并打开网页'}
}

function Stop-CustomerDeployment([string]$Root) {
    $docker = Get-CustomerDockerPath
    if (-not $docker) { throw '未找到 Docker 命令。' }
    $state = Get-CustomerDeploymentState $Root
    foreach ($line in (Invoke-CustomerProcess $docker @('compose','down','--remove-orphans') $state.DeploymentRoot)) { Write-Output $line }
    [pscustomobject]@{Stopped=$true;Message='项目已停止，数据卷已保留'}
}
