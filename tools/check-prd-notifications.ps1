$ErrorActionPreference='Stop'
. "$PSScriptRoot/seed-local-demo.ps1"
foreach($path in @('/document/list?spaceId=15&pageSize=1','/meeting/list','/task/list')){Invoke-LocalApi $path 'Get' $null $headers|Out-Null}
Assert-LocalDemoDatabase
$fixture=Get-Content "$PSScriptRoot/native-runtime/acceptance-fixtures.json" -Raw|ConvertFrom-Json
$qa=Get-Content "$PSScriptRoot/native-runtime/acceptance-users.json" -Raw|ConvertFrom-Json
$h=@{Authorization=$headers.Authorization;'X-Space-Id'="$($fixture.spaceId)"}
$login=Invoke-LocalApi '/auth/login' 'Post' @{account=$qa.member.username;password=$qa.member.password} @{}
$memberHeaders=@{Authorization="Bearer $($login.accessToken)";'X-Space-Id'="$($fixture.spaceId)"}
$doc=Invoke-LocalApi "/document/$($fixture.docId)" 'Get' $null $h
if($doc.title -ne 'Acceptance shared document'){throw 'Expected isolated acceptance document'}
$before=@{comments=@(Invoke-LocalApi "/document/$($fixture.docId)/comments" 'Get' $null $h);notifications=@(Invoke-LocalApi '/notification/list' 'Get' $null $h)}
$before|ConvertTo-Json -Depth 30|Set-Content "$PSScriptRoot/native-runtime/before-prd-notifications-$(Get-Date -Format yyyyMMdd-HHmmss).json" -Encoding UTF8
$marker='Notification dedup '+[guid]::NewGuid().ToString('N')
$comment=$null
$matching=@()
try {
    $comment=Invoke-LocalApi "/document/$($fixture.docId)/comments" 'Post' @{docId=$fixture.docId;content=$marker} $memberHeaders
    for($i=0;$i -lt 20;$i++){
        Start-Sleep -Milliseconds 250
        $matching=@(Invoke-LocalApi '/notification/list' 'Get' $null $h|Where-Object { $_.content -like "*$marker*" })
        if($matching.Count -gt 1){throw 'Duplicate comment notification received'}
    }
    if($matching.Count -ne 1){throw 'Expected exactly one real MQ comment notification'}
    @{verifiedAt=(Get-Date).ToString('o');spaceId=$fixture.spaceId;docId=$fixture.docId;commentId=$comment.commentId;notificationCount=$matching.Count;observationSamples=20}|ConvertTo-Json|Set-Content "$PSScriptRoot/native-runtime/prd-batch11-notification-dedup.json" -Encoding UTF8
    Write-Host 'PASS real comment through both bound queues produced exactly one notification.'
} finally {
    if($comment){Invoke-LocalApi "/document/$($fixture.docId)/comments/$($comment.commentId)" 'Delete' $null $memberHeaders|Out-Null}
    foreach($notification in $matching){Invoke-LocalApi "/notification/$($notification.id)" 'Delete' $null $h|Out-Null}
}
