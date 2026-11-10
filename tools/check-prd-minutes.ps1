$ErrorActionPreference='Stop'
. "$PSScriptRoot/seed-local-demo.ps1"
foreach($p in @('/document/list?spaceId=13&pageSize=1','/meeting/list','/task/list')){Invoke-LocalApi $p 'Get' $null $headers|Out-Null}
Assert-LocalDemoDatabase
$spaces=Invoke-LocalApi '/space/my' 'Get' $null $headers
$sid=($spaces|Where-Object spaceName -eq 'PRD 功能验收'|Select-Object -First 1).spaceId
if(-not $sid){throw 'Missing isolated PRD fixture space'}
$h=@{Authorization=$headers.Authorization;'X-Space-Id'="$sid"}
$before=Invoke-LocalApi '/meeting/list' 'Get' $null $h
@{meetings=$before;tasks=(Invoke-LocalApi '/task/list' 'Get' $null $h)}|ConvertTo-Json -Depth 30|Set-Content "$PSScriptRoot/native-runtime/before-prd-minutes-$(Get-Date -Format yyyyMMdd-HHmmss).json" -Encoding UTF8
$start=(Get-Date).AddDays(2).Date.AddHours(14)
$id=Invoke-LocalApi '/meeting/create' 'Post' @{title='PRD 验收 · 纪要转任务';startTime=$start.ToString('yyyy-MM-ddTHH:mm:ss');endTime=$start.AddMinutes(45).ToString('yyyy-MM-ddTHH:mm:ss');agenda='确认验收范围与负责人';participantIds=@()} $h
$old=Invoke-LocalApi "/meeting/$id/minutes" 'Get' $null $h
$todos=@(@{id='review';title='核对验收清单';assigneeId='1';dueDate=$start.AddDays(1).ToString('yyyy-MM-dd')},@{id='demo';title='准备演示材料';assigneeId='';dueDate=''})
$body=@{minuteContent="会议结论：通过主干验收。`n- [ ] 核对验收清单`n待办：准备演示材料";todoListJson=$todos;revision=$old.revision}
Invoke-LocalApi "/meeting/$id/minutes" 'Put' $body $h|Out-Null
$one=Invoke-LocalApi '/task/from-meeting' 'Post' @{meetingId=$id;itemIds=@('review')} $h
$two=Invoke-LocalApi '/task/from-meeting' 'Post' @{meetingId=$id;itemIds=@('review')} $h
if($one.review -ne $two.review){throw 'Duplicate task created'}
$task=Invoke-LocalApi "/task/$($one.review)" 'Get' $null $h
if($task.title -ne '核对验收清单' -or "$($task.assigneeId)" -ne '1'){throw 'Task fields did not persist'}
$minutes=Invoke-LocalApi "/meeting/$id/minutes" 'Get' $null $h
if($minutes.todoListJson[0].taskId -ne $one.review){throw 'Minutes task link missing'}
$response=Invoke-RestMethod -Uri "$base/meeting/$id/minutes" -Method Put -Headers $h -ContentType application/json -Body ([Text.Encoding]::UTF8.GetBytes(($body|ConvertTo-Json -Depth 20)))
if($response.code -ne 409){throw 'Stale minutes overwrote task links'}
@{meetingId=$id;spaceId=$sid;taskId=$one.review;idempotent=$true;staleSaveCode=$response.code;minutes=$minutes;task=$task}|ConvertTo-Json -Depth 30|Set-Content "$PSScriptRoot/native-runtime/prd-batch10-minutes.json" -Encoding UTF8
Write-Host "Minutes verification passed: meeting=$id task=$($one.review), duplicate stable, stale save=409"
