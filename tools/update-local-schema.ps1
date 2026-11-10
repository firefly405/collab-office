$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$root = $projectRoot
$runtimeDir = Join-Path $PSScriptRoot 'native-runtime'
$versionPath = Join-Path $runtimeDir 'schema-version.txt'
$version = [int](Get-Content $versionPath -Raw).Trim()
if ($version -eq 23) { return }
if ($version -notin @(12,13,14,15,16,17,18,19,20,21,22)) { throw 'Expected local schema version 12 through 22. See LOCAL-RUN.md before upgrading.' }
$seedAst = [System.Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot 'seed-local-demo.ps1'), [ref]$null, [ref]$null)
$guard = $seedAst.Find({param($node) $node -is [System.Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq 'Assert-LocalDemoDatabase'}, $true)
Invoke-Expression $guard.Extent.Text
Assert-LocalDemoDatabase
$mysqlDir = 'C:/DevTools/mysql-8.0.43-winx64/bin'
$previousPassword = $env:MYSQL_PWD
try {
    $env:MYSQL_PWD = $env:DB_PASSWORD
    if (-not $env:MYSQL_PWD) { throw 'DB_PASSWORD must be loaded from the local .env.' }
    $backupPath = Join-Path $runtimeDir ('before-v23-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.sql')
    & "$mysqlDir/mysqldump.exe" --host=127.0.0.1 --port=13306 --user=root --single-transaction --routines --triggers --set-gtid-purged=OFF "--result-file=$backupPath" collab_office
    if ($LASTEXITCODE -ne 0) { throw 'Database backup failed. Upgrade stopped.' }
    $migrations=@{13='V13__local_collaboration.sql';14='V14__share_password_hash.sql';15='V15__web_content_management.sql';16='V16__content_organization.sql';17='V17__task_project_depth.sql';18='V18__document_discussion_history.sql';19='V19__sheet_view_preferences.sql';20='V20__sheet_schema_alignment.sql';21='V21__workspace_preferences.sql';22='V22__organization_permissions.sql'}
    $migrations[23]='V23__platform_roles_reports.sql'
    foreach($nextVersion in (($version+1)..23)) {
        $migration = (Join-Path $projectRoot ('collab-office-common/src/main/resources/db/migration/'+$migrations[$nextVersion])).Replace('\','/')
        & "$mysqlDir/mysql.exe" --host=127.0.0.1 --port=13306 --user=root --default-character-set=utf8mb4 collab_office "--execute=source $migration"
        if ($LASTEXITCODE -ne 0) { throw 'Database upgrade failed. Backup has been retained.' }
        Set-Content -LiteralPath $versionPath -Value $nextVersion -Encoding ASCII
    }
    Write-Host 'Local schema upgraded to 23; database backup retained in native-runtime.'
} finally { $env:MYSQL_PWD = $previousPassword }
