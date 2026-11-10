$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/seed-local-demo.ps1"
Assert-LocalDemoDatabase
$runtime = Join-Path $PSScriptRoot 'native-runtime'
$health = Invoke-RestMethod 'http://127.0.0.1:8080/api/health'
$web = Invoke-WebRequest 'http://127.0.0.1:3000/'
if($health.status -ne 'UP' -or $web.StatusCode -ne 200){throw 'Gateway or frontend is unavailable'}
$logs = @(Get-ChildItem $runtime -Filter '*-app.log' | ForEach-Object { @{file=$_.FullName;lines=@(Get-Content $_.FullName).Count} })
$statsBefore = Invoke-LocalApi '/admin/dashboard/stats' 'Get' $null $headers
@{stats=$statsBefore;spaces=(Invoke-LocalApi '/space/my' 'Get' $null $headers)} | ConvertTo-Json -Depth 20 | Set-Content "$runtime/before-demo-simplification-$(Get-Date -Format yyyyMMdd-HHmmss).json" -Encoding utf8
$team = Invoke-LocalApi '/space/create' 'Post' @{spaceName='Demo 管理审计复验 '+(Get-Date -Format HHmmss)} $headers
$teamHeaders = @{Authorization=$headers.Authorization;'X-Space-Id'="$($team.spaceId)"}
try {
    Invoke-LocalApi "/space/$($team.spaceId)/organization/departments" 'Post' @{name='复验部门'} $teamHeaders | Out-Null
    $after = Invoke-LocalApi '/admin/dashboard/stats' 'Get' $null $headers
    if($after.todayOperations -ne $statsBefore.todayOperations+1 -or $after.totalLogs -ne $statsBefore.totalLogs+1){throw 'Real management audit was not counted exactly once'}
    if($after.trend[-1].count -ne $after.todayOperations){throw 'Today statistic and trend disagree'}
    Write-Host 'PASS real management change increments total, today and trend consistently'
} finally {
    Invoke-LocalApi "/space/$($team.spaceId)/organization/dissolve" 'Post' @{confirm=$team.spaceName} $teamHeaders | Out-Null
}
$probes = foreach($port in @(8101,8102,8103,8104,8105,8106,8107,8109,8110,9001)) {
    $response = Invoke-WebRequest "http://127.0.0.1:$port/api/health" -SkipHttpErrorCheck
    if($response.StatusCode -ne 404){throw "Missing resource on $port returned $($response.StatusCode) instead of 404"}
    @{port=$port;httpStatus=$response.StatusCode}
}
$errors = foreach($item in $logs) {
    $added = @(Get-Content $item.file | Select-Object -Skip $item.lines)
    $count = @($added | Where-Object {$_ -match '\sERROR\s'}).Count
    if($count){throw "New application ERROR in $($item.file)"}
    @{file=(Split-Path $item.file -Leaf);newErrors=$count}
}
@{checkedAt=(Get-Date).ToString('o');gateway=$health.status;webHttpStatus=$web.StatusCode;testSpaceId=$team.spaceId;testSpaceDeactivated=$true;before=$statsBefore;afterChange=$after;missingResourceProbes=$probes;logs=$errors} | ConvertTo-Json -Depth 20 | Set-Content "$runtime/batch17-simplification.json" -Encoding utf8
Write-Host 'PASS all 10 missing-resource probes return 404 without application ERROR logs'
