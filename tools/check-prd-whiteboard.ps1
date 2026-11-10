$ErrorActionPreference='Stop'
. "$PSScriptRoot/seed-local-demo.ps1"
foreach($p in @('/document/list?spaceId=13&pageSize=1','/meeting/list','/task/list')){Invoke-LocalApi $p 'Get' $null $headers|Out-Null}
Assert-LocalDemoDatabase
$spaces=Invoke-LocalApi '/space/my' 'Get' $null $headers
$sid=($spaces|Where-Object spaceName -eq 'PRD 功能验收'|Select-Object -First 1).spaceId
if(-not $sid){throw 'Missing isolated PRD fixture space'}
$h=@{Authorization=$headers.Authorization;'X-Space-Id'="$sid"}
@{documents=(Invoke-LocalApi "/document/list?spaceId=$sid&pageSize=100" 'Get' $null $h)}|ConvertTo-Json -Depth 30|Set-Content "$PSScriptRoot/native-runtime/before-prd-whiteboard-$(Get-Date -Format yyyyMMdd-HHmmss).json" -Encoding UTF8
$id=Invoke-LocalApi '/document/create' 'Post' @{title='PRD 验收 · 协作白板';type=4;spaceId=$sid} $h
$ops=@(
 @{id='title';tool='text';color='#1d2129';width=6;x=100;y=90;w=100;h=90;text='团队交付流程'},
 @{id='scope';tool='rect';color='#3370ff';width=4;x=100;y=140;w=450;h=330},
 @{id='label';tool='text';color='#1d2129';width=4;x=170;y=240;w=170;h=240;text='需求与设计'},
 @{id='link';tool='arrow';color='#00a389';width=5;x=480;y=235;w=680;h=235},
 @{id='review';tool='triangle';color='#d99400';width=4;x=720;y=140;w=1000;h=330},
 @{id='label2';tool='text';color='#1d2129';width=4;x=790;y=290;w=790;h=290;text='验收'}
)
foreach($op in $ops){$result=Invoke-LocalApi "/whiteboard/$id/operations" 'Post' $op $h}
$again=Invoke-LocalApi "/whiteboard/$id/operations" 'Post' $ops[0] $h
if($again.Count -ne 6){throw 'Append idempotency failed'}
$element=($again|Where-Object {($_.content|ConvertFrom-Json).id -eq 'review'}).id
$undone=Invoke-LocalApi "/whiteboard/$id/operations/$element/active" 'Put' @{active=$false} $h
if((($undone|Where-Object id -eq $element).metadata|ConvertFrom-Json).active -ne $false){throw 'Undo not persisted'}
Invoke-LocalApi "/whiteboard/$id/operations/$element/active" 'Put' @{active=$true} $h|Out-Null
$bad=Invoke-RestMethod -Uri "$base/whiteboard/$id/operations" -Method Post -Headers $h -ContentType application/json -Body '{"id":"bad","tool":"pen","color":"#000000","width":3,"points":[[99999,1]]}'
if($bad.code -ne 400){throw 'Invalid points accepted'}
@{boardId=$id;spaceId=$sid;operationCount=$again.Count;idempotent=$true;invalidCode=$bad.code;operations=$again}|ConvertTo-Json -Depth 30|Set-Content "$PSScriptRoot/native-runtime/prd-batch11-whiteboard.json" -Encoding UTF8
Write-Host "Whiteboard verification passed: board=$id, append idempotent, undo restored, invalid=400"
