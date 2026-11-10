$ErrorActionPreference='Stop'
. "$PSScriptRoot/seed-local-demo.ps1"
Assert-LocalDemoDatabase
function Read-DemoSnapshot {
    $snapshot=@{}
    foreach($team in @(Invoke-LocalApi '/space/my' 'Get' $null $headers)|Where-Object spaceName -in @('产品协作演示','空状态验收')) {
        $h=@{Authorization=$headers.Authorization;'X-Space-Id'="$($team.spaceId)"}
        $snapshot["space-$($team.spaceId)"]=@{
            documents=@((Invoke-LocalApi "/document/list?spaceId=$($team.spaceId)&pageSize=100" 'Get' $null $h).records|ForEach-Object docId|Sort-Object)
            tasks=@(Invoke-LocalApi '/task/list' 'Get' $null $h|ForEach-Object taskId|Sort-Object)
            files=@(Invoke-LocalApi "/file/list?spaceId=$($team.spaceId)" 'Get' $null $h|ForEach-Object fileId|Sort-Object)
        }
    }
    $snapshot.meetings=@(Invoke-LocalApi '/meeting/list' 'Get' $null $headers|ForEach-Object meetingId|Sort-Object)
    return $snapshot
}
$before=Read-DemoSnapshot
& "$PSScriptRoot/seed-local-demo.ps1" -PopulateWorkspace
$first=Read-DemoSnapshot
& "$PSScriptRoot/seed-local-demo.ps1" -PopulateWorkspace
$second=Read-DemoSnapshot
foreach($key in $first.Keys) {
    if($key -eq 'meetings') {
        if(@(Compare-Object @($first.meetings) @($second.meetings)).Count){throw 'Repeated seed duplicated meetings'}
    } else {
        foreach($kind in @('documents','tasks','files')) {
            if(@(Compare-Object @($first[$key][$kind]) @($second[$key][$kind])).Count){throw "Repeated seed changed $key/$kind"}
        }
    }
}
@{before=$before;first=$first;second=$second;idempotent=$true;verifiedAt=(Get-Date).ToString('o')}|ConvertTo-Json -Depth 30|Set-Content "$PSScriptRoot/native-runtime/prd-batch11-seed.json" -Encoding UTF8
Write-Host 'Seed idempotency verified: repeated run kept document/task/file/meeting IDs unchanged.'
