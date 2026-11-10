$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
Get-Content "$root/.env" | ForEach-Object {if($_ -match '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$'){[Environment]::SetEnvironmentVariable($matches[1],$matches[2].Trim().Trim('"').Trim("'"),'Process')}}
$seedAst=[System.Management.Automation.Language.Parser]::ParseFile("$PSScriptRoot/seed-local-demo.ps1",[ref]$null,[ref]$null)
$guard=$seedAst.Find({param($node) $node -is [System.Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq 'Assert-LocalDemoDatabase'},$true)
Invoke-Expression $guard.Extent.Text
Assert-LocalDemoDatabase
& "$PSScriptRoot/update-local-schema.ps1"
$previousPassword=$env:MYSQL_PWD
try {
  $env:MYSQL_PWD=$env:DB_PASSWORD
  $backup="$PSScriptRoot/native-runtime/before-role-admin-$(Get-Date -Format yyyyMMdd-HHmmss).sql"
  & C:/DevTools/mysql-8.0.43-winx64/bin/mysqldump.exe --host=127.0.0.1 --port=13306 --user=root --single-transaction --set-gtid-purged=OFF "--result-file=$backup" collab_office
  if($LASTEXITCODE -ne 0){throw 'Backup failed'}
  & C:/DevTools/mysql-8.0.43-winx64/bin/mysql.exe --host=127.0.0.1 --port=13306 --user=root collab_office --execute="INSERT IGNORE INTO sys_user_role(user_id,role_id) SELECT u.id,r.id FROM sys_user u JOIN sys_role r ON r.role_code='admin' WHERE u.id=1 AND u.username='admin' AND u.deleted=0 AND u.status=1;"
  if($LASTEXITCODE -ne 0){throw 'Local demo administrator assignment failed'}
  Write-Host 'V23 ready; existing local admin identity assigned. Backup retained.'
}finally{$env:MYSQL_PWD=$previousPassword}
