$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
. (Join-Path $PSScriptRoot 'customer-deploy-core.ps1')
$state=Get-CustomerDeploymentState $root
if($state.Rows.Count -ne 8){throw 'Expected eight customer deployment checks.'}
if($state.Allowed){throw 'Docker-less test machine must not be marked deployable.'}
if($state.Rows[0].Name -ne 'Docker Desktop'){throw 'Docker check must be first.'}
if($state.Rows[0].Passed){throw 'Missing Docker Desktop was not detected.'}
$fixture=Join-Path $PSScriptRoot ('native-runtime/customer-deploy-fixture-'+[Guid]::NewGuid().ToString('N'))
[void][IO.Directory]::CreateDirectory($fixture)
try {
    $fixtureState=Get-CustomerDeploymentState $fixture
    if($fixtureState.Allowed){throw 'Incomplete project fixture must be blocked.'}
    if($fixtureState.Rows[5].Passed){throw 'Missing compose files were not detected.'}
} finally { Remove-Item -LiteralPath $fixture -Recurse -Force -ErrorAction SilentlyContinue }
Write-Host 'Customer deployment checks passed: Docker prerequisite and incomplete-package guard.'
