param([switch]$Cleanup)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/seed-local-demo.ps1"
Invoke-LocalApi '/meeting/list' 'Get' $null $headers | Out-Null
Assert-LocalDemoDatabase
$proof = "$PSScriptRoot/native-runtime/prd-batch1-meeting.json"
$all = @(Invoke-LocalApi '/meeting/list' 'Get' $null $headers)
if (-not $Cleanup) {
    $all | ConvertTo-Json -Depth 10 | Set-Content "$PSScriptRoot/native-runtime/before-prd-meetings-$(Get-Date -Format 'yyyyMMdd-HHmmss').json" -Encoding UTF8
    $title = 'PRD 验收 · 取消会议 ' + (Get-Date -Format 'HHmmss')
    $demoHeaders = @{Authorization=$headers.Authorization;'X-Space-Id'='13'}
    $start = (Get-Date).AddDays(7)
    $id = Invoke-LocalApi '/meeting/create' 'Post' @{title=$title;startTime=$start.ToString('yyyy-MM-ddTHH:mm:ss');endTime=$start.AddHours(1).ToString('yyyy-MM-ddTHH:mm:ss');participantIds=@()} $demoHeaders
    $created = Invoke-LocalApi "/meeting/$id" 'Get' $null $demoHeaders
    if ($created.status -ne 1) { throw 'Scheduled state must be 1.' }
    Invoke-LocalApi "/meeting/$id" 'Delete' $null $demoHeaders | Out-Null
    Invoke-LocalApi "/meeting/$id" 'Delete' $null $demoHeaders | Out-Null
    $cancelled = Invoke-LocalApi "/meeting/$id" 'Get' $null $demoHeaders
    if ($cancelled.status -ne 4) { throw 'Cancelled state must be 4.' }
    @{meetingId="$id";title=$title;hostId="$($user.userId)";createdState=$created.status;cancelledState=$cancelled.status} | ConvertTo-Json | Set-Content $proof -Encoding UTF8
    Write-Host 'Scheduled=1, cancelled=4, repeated cancellation succeeded. Browser proof fixture retained until Cleanup.'
} else {
    $fixture=Get-Content $proof -Raw | ConvertFrom-Json
    if ("$($fixture.meetingId)" -notmatch '^\d+$' -or "$($fixture.hostId)" -ne "$($user.userId)" -or $fixture.title -notlike 'PRD 验收 · 取消会议 *') { throw 'Unexpected fixture; cleanup refused.' }
    $all | ConvertTo-Json -Depth 10 | Set-Content "$PSScriptRoot/native-runtime/before-prd-meeting-cleanup-$(Get-Date -Format 'yyyyMMdd-HHmmss').json" -Encoding UTF8
    $oldPassword=$env:MYSQL_PWD
    try {
        $line=Get-Content "$root/.env" | Where-Object {$_ -match '^DB_PASSWORD='} | Select-Object -First 1
        $env:MYSQL_PWD=$line.Substring('DB_PASSWORD='.Length).Trim().Trim('"').Trim("'")
        $sql="UPDATE meeting_room SET deleted=1 WHERE room_id='$($fixture.meetingId)' AND host_id=$($fixture.hostId) AND status=4 AND title LIKE 'PRD 验收 · 取消会议 %';"
        & 'C:/DevTools/mysql-8.0.43-winx64/bin/mysql.exe' --host=127.0.0.1 --port=13306 --user=root --default-character-set=utf8mb4 collab_office "--execute=$sql"
        if($LASTEXITCODE -ne 0){throw 'Fixture cleanup failed.'}
    } finally {$env:MYSQL_PWD=$oldPassword}
    $remaining=@(Invoke-LocalApi '/meeting/list' 'Get' $null $headers)
    if($remaining | Where-Object meetingId -eq $fixture.meetingId){throw 'Fixture remained visible.'}
    Write-Host 'Only the newly created PRD fixture was soft-deleted; pre-existing meetings retained.'
}
