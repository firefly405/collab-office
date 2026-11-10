$ErrorActionPreference = 'Stop'
$workerFile = Join-Path $PSScriptRoot 'tools/native-runtime/startup-process.json'
if (Test-Path $workerFile) {
    $entry = Get-Content $workerFile -Raw | ConvertFrom-Json
    $worker = Get-Process -Id $entry.pid -ErrorAction SilentlyContinue
    if ($worker -and $worker.StartTime.ToUniversalTime().Ticks -eq $entry.startedTicks) {
        Stop-Process -Id $worker.Id -Force
        Write-Host 'Stopped pending startup'
    }
}
$stateFile = Join-Path $PSScriptRoot 'tools/native-runtime/processes.json'
if (-not (Test-Path $stateFile)) { Write-Host 'No managed local processes.'; exit }
$processes = @(Get-Content $stateFile -Raw | ConvertFrom-Json)
[array]::Reverse($processes)
foreach ($entry in $processes) {
    $process = Get-Process -Id $entry.pid -ErrorAction SilentlyContinue
    if ($process -and $process.StartTime.ToUniversalTime().Ticks -eq $entry.startedTicks) {
        if ($entry.name -eq 'mysql') {
            $passwordLine = Get-Content "$PSScriptRoot/.env" | Where-Object { $_ -match '^DB_PASSWORD=' } | Select-Object -First 1
            $env:MYSQL_PWD = $passwordLine.Substring('DB_PASSWORD='.Length).Trim().Trim('"').Trim("'")
            & 'C:/DevTools/mysql-8.0.43-winx64/bin/mysqladmin.exe' -h 127.0.0.1 -P 13306 -u root shutdown
            Remove-Item Env:MYSQL_PWD
            if ($LASTEXITCODE -ne 0) { throw 'MySQL shutdown failed; preserving process for inspection.' }
            Write-Host 'Stopped mysql'
            continue
        }
        if ($entry.name -eq 'redis') {
            & "$PSScriptRoot/tools/Redis/redis-cli.exe" shutdown
            if ($LASTEXITCODE -ne 0) { throw 'Redis shutdown failed; preserving process for inspection.' }
            Write-Host 'Stopped redis'
            continue
        }
        # Also stop children of the RabbitMQ/Elasticsearch command wrappers.
        & taskkill.exe /PID $process.Id /T /F | Out-Null
        Write-Host "Stopped $($entry.name)"
    }
}
Write-Host 'Local services stopped. All project data has been retained.'
