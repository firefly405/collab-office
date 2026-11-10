$ErrorActionPreference='Stop'
. "$PSScriptRoot/seed-local-demo.ps1"
foreach($p in @('/document/list?spaceId=15&pageSize=1','/meeting/list','/task/list')){Invoke-LocalApi $p 'Get' $null $headers|Out-Null}
Assert-LocalDemoDatabase
$h=@{Authorization=$headers.Authorization;'X-Space-Id'='15'}
$id='2110698851412348928'
$before=Invoke-LocalApi "/meeting/$id" 'Get' $null $h
$categories=Invoke-LocalApi '/knowledge/categories' 'Get' $null $h
@{meeting=$before;categories=$categories}|ConvertTo-Json -Depth 30|Set-Content "$PSScriptRoot/native-runtime/before-prd-final-$(Get-Date -Format yyyyMMdd-HHmmss).json" -Encoding UTF8
$body=@{title=$before.title;agenda=$before.agenda;startTime=([datetime]$before.startTime).ToString('yyyy-MM-ddTHH:mm:ss');endTime=([datetime]$before.endTime).ToString('yyyy-MM-ddTHH:mm:ss');participantIds=@()}
$shift=@{}+$body
$shift.startTime=([datetime]$before.startTime).AddMinutes(15).ToString('yyyy-MM-ddTHH:mm:ss')
$shift.endTime=([datetime]$before.endTime).AddMinutes(15).ToString('yyyy-MM-ddTHH:mm:ss')
try{
    Invoke-LocalApi "/meeting/$id/schedule" 'Put' $shift $h|Out-Null
    $changed=Invoke-LocalApi "/meeting/$id" 'Get' $null $h
    if([datetime]$changed.startTime -ne [datetime]$shift.startTime){throw 'Reschedule not persisted'}
}finally{Invoke-LocalApi "/meeting/$id/schedule" 'Put' $body $h|Out-Null}
$name='PRD 分类验收 '+(Get-Date -Format HHmmss)
Invoke-LocalApi '/knowledge/categories' 'Post' @{name=$name} $h|Out-Null
$category=Invoke-LocalApi '/knowledge/categories' 'Get' $null $h|Where-Object name -eq $name|Select-Object -First 1
Invoke-LocalApi "/knowledge/categories/$($category.id)" 'Put' @{name="$name 已改名"} $h|Out-Null
$updated=Invoke-LocalApi '/knowledge/categories' 'Get' $null $h|Where-Object id -eq $category.id
if($updated.name -ne "$name 已改名"){throw 'Category name not saved'}
Invoke-LocalApi "/knowledge/categories/$($category.id)" 'Delete' $null $h|Out-Null
$contributions=Invoke-LocalApi '/knowledge/contributions' 'Get' $null $h
$board='2110703063420506112'
$ops=Invoke-LocalApi "/whiteboard/$board/operations" 'Get' $null $h
$operation=$ops|Where-Object type -eq 'operation'|Select-Object -First 1
$op=$operation.content|ConvertFrom-Json
$retry=Invoke-LocalApi "/whiteboard/$board/operations" 'Post' $op $h
if($retry.Count -ne $ops.Count){throw 'Whiteboard retry duplicated operation'}
$op.x=[double]$op.x+1
$conflict=Invoke-RestMethod "$base/whiteboard/$board/operations" -Method Post -Headers $h -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes(($op|ConvertTo-Json -Depth 30)))
if($conflict.code -ne 409){throw 'Whiteboard conflicting ID accepted'}
@{meetingId=$id;rescheduledStart=$changed.startTime;restoredStart=$body.startTime;categoryRenamed=$true;categoryDeleted=$true;contributions=$contributions;whiteboardRetryCount=$retry.Count;whiteboardConflictCode=$conflict.code}|ConvertTo-Json -Depth 30|Set-Content "$PSScriptRoot/native-runtime/prd-batch11-final.json" -Encoding UTF8
Write-Host 'Final API checks passed: schedule restored, category lifecycle, contributions, whiteboard idempotency and conflict.'
