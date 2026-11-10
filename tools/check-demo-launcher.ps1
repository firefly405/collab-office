$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
. "$PSScriptRoot/demo-launcher-core.ps1"
$evidence=New-Object 'Collections.Generic.List[object]'
function Assert-Launcher([bool]$Condition,[string]$Name){
    if(-not $Condition){throw "FAIL $Name"}
    $evidence.Add(@{name=$Name;passed=$true}); Write-Host "PASS $Name"
}
$inventory=@{100=@{ParentProcessId=0};200=@{ParentProcessId=100};300=@{ParentProcessId=300}}
$managed=@{100='rabbitmq'}
Assert-Launcher (Test-DemoManagedListener 200 'rabbitmq' $inventory $managed) 'Registered component child is reusable'
Assert-Launcher (-not (Test-DemoManagedListener 200 'mysql' $inventory $managed)) 'Another component cannot impersonate database'
Assert-Launcher (-not (Test-DemoManagedListener 999 'web' $inventory $managed)) 'Unknown listener is blocked'
Assert-Launcher (-not (Test-DemoManagedListener 300 'web' $inventory $managed)) 'Broken process ancestry cannot loop forever'
$fixture=Join-Path $PSScriptRoot ('native-runtime/launcher-fixture-'+[Guid]::NewGuid().ToString('N'))
[void][IO.Directory]::CreateDirectory($fixture)
$configFile=Join-Path $fixture '.env'
$empty=Read-DemoConfig $configFile
Assert-Launcher (@(Get-DemoMissingConfig $empty).Count -eq 8) 'Missing configuration is listed explicitly'
[IO.File]::WriteAllLines($configFile,@('# test fixture only','JWT_SECRET="launcher-fixture-secret-with-32-bytes"','INTERNAL_CALL_TOKEN=test-internal','DB_PASSWORD=test-password','RABBITMQ_USERNAME=test','RABBITMQ_PASSWORD=test','MINIO_ACCESS_KEY=test','MINIO_SECRET_KEY=test-secret','MINIO_BUCKET=doc-files'),[Text.Encoding]::UTF8)
$values=Read-DemoConfig $configFile
Assert-Launcher (@(Get-DemoMissingConfig $values).Count -eq 0 -and $values.JWT_SECRET -eq 'launcher-fixture-secret-with-32-bytes') 'Configuration parser accepts quoted values'
$missing=Get-DemoEnvironment $fixture
Assert-Launcher (-not $missing.Allowed) 'Incomplete copied project cannot start'
$jarRow=$missing.Rows | Where-Object Name -eq '11个后端构建产物'
Assert-Launcher (-not $jarRow.Passed -and $jarRow.Help.Contains('mvn --batch-mode package')) 'Missing backend builds have a repair command'
$webRow=$missing.Rows | Where-Object Name -eq '前端依赖'
Assert-Launcher (-not $webRow.Passed -and $webRow.Help.Contains('npm install')) 'Missing frontend dependencies have install instructions'
Assert-Launcher (@($missing.Rows | Where-Object {$_.Name -eq 'Redis 5' -and $_.Url -like 'https://*'}).Count -eq 1) 'Component download links are present'
Assert-Launcher (-not (($missing.Rows | ConvertTo-Json -Depth 4).Contains('test-password'))) 'Environment report does not leak configuration secrets'
$refused=$false
try{Start-DemoApplication $fixture -NoBrowser | Out-Null}catch{$refused=$_.Exception.Message.Contains('环境检查未通过')}
Assert-Launcher $refused 'Start rechecks and refuses incomplete environment'
$live=Get-DemoEnvironment $root
Assert-Launcher $live.Allowed 'Current Windows machine passes required checks'
$runtime=Get-DemoRuntime $root
Assert-Launcher $runtime.Ready 'Running frontend and all backend ports pass readiness'
$priorPath=$env:PATH
try{
    $env:PATH=''
    Assert-Launcher ([bool](Find-DemoCommand 'node.exe' @("$env:ProgramFiles/nodejs/node.exe"))) 'Explorer launch can find installed Node without terminal PATH'
    Assert-Launcher ([bool](Find-DemoCommand 'pwsh.exe' @("$env:ProgramFiles/PowerShell/7/pwsh.exe","$env:USERPROFILE/.cache/codex-runtimes/codex-primary-runtime/dependencies/native/powershell/pwsh.exe"))) 'Explorer launch can find local PowerShell 7 fallback'
}finally{$env:PATH=$priorPath}
$evidence.ToArray() | ConvertTo-Json -Depth 4 | Set-Content "$PSScriptRoot/native-runtime/launcher-self-check.json" -Encoding UTF8
Write-Host "Launcher checks passed: $($evidence.Count). Test fixtures retained in ignored runtime directory."
