$ErrorActionPreference='Stop'
. "$PSScriptRoot/seed-local-demo.ps1"
$fixture=Get-Content "$PSScriptRoot/native-runtime/prd-batch2-content.json" -Raw | ConvertFrom-Json
$demoHeaders=@{Authorization=$headers.Authorization;'X-Space-Id'="$($fixture.spaceId)"}
foreach($path in @('/document/list?spaceId=13&pageSize=1','/meeting/list','/task/list')) {Invoke-LocalApi $path 'Get' $null $headers | Out-Null}
Assert-LocalDemoDatabase
$before=@{projects=Invoke-LocalApi "/task/project/list?spaceId=$($fixture.spaceId)" 'Get' $null $demoHeaders;tasks=Invoke-LocalApi '/task/list' 'Get' $null $demoHeaders}
$before | ConvertTo-Json -Depth 20 | Set-Content "$PSScriptRoot/native-runtime/before-prd-tasks-$(Get-Date -Format 'yyyyMMdd-HHmmss').json" -Encoding UTF8
function Expect-Rejection([scriptblock]$Action,[string]$Pattern) {
    try {& $Action | Out-Null} catch {if($_.Exception.Message -match $Pattern){return};throw}
    throw "Expected rejection did not occur: $Pattern"
}
$project=Invoke-LocalApi "/task/project/create?name=PRD-local-project&spaceId=$($fixture.spaceId)" 'Post' $null $demoHeaders
$projectBody=@{name='PRD 验收 · 产品交付';description='本机任务与项目功能验收';status=1;memberIds=@('1');milestones=@(@{title='需求评审';dueDate=(Get-Date).ToString('yyyy-MM-dd');done=$true},@{title='版本验收';dueDate=(Get-Date).AddDays(7).ToString('yyyy-MM-dd');done=$false})}
Invoke-LocalApi "/task/project/$project" 'Put' $projectBody $demoHeaders | Out-Null
$column=Invoke-LocalApi "/kanban/$project/columns" 'Post' @{name='待验收';wipLimit=1;sortOrder=0;color='#3370ff';isDoneColumn=0} $demoHeaders
$task=Invoke-LocalApi '/task/create' 'Post' @{title='PRD 验收 · 交付回归';projectId=$project;priority=2;assigneeId='1';tags='验收,前端';dueDate=(Get-Date).AddDays(2).ToString('yyyy-MM-dd');repeatRule='weekly';reminderEnabled=1;reminderDays=2;reminderFrequency='daily'} $demoHeaders
$other=Invoke-LocalApi '/task/create' 'Post' @{title='PRD 验收 · 发布清单';projectId=$project;priority=3} $demoHeaders
$subtask=Invoke-LocalApi '/task/create' 'Post' @{title='PRD 验收 · 检查窄屏';projectId=$project;parentId=$task} $demoHeaders
$children=@(Invoke-LocalApi "/kanban/task/$task/subtasks" 'Get' $null $demoHeaders)
if(-not($children|Where-Object taskId -eq $subtask)){throw 'Subtask missing'}
Invoke-LocalApi "/kanban/columns/$($column.id)/move-task?taskId=$task" 'Post' $null $demoHeaders | Out-Null
$moved=Invoke-LocalApi "/task/$task" 'Get' $null $demoHeaders
if($moved.columnId -ne $column.id -or $moved.status -ne 1){throw 'Kanban move did not persist'}
Invoke-LocalApi "/kanban/columns/$($column.id)/move-task?taskId=$task" 'Post' $null $demoHeaders | Out-Null
Expect-Rejection {Invoke-LocalApi "/task/$other" 'Put' @{title='Must roll back';status=1;columnId=$column.id} $demoHeaders} 'WIP'
$rolledBack=Invoke-LocalApi "/task/$other" 'Get' $null $demoHeaders
if($rolledBack.title -ne 'PRD 验收 · 发布清单' -or $rolledBack.status -ne 0){throw 'Failed WIP move was not atomic'}
Expect-Rejection {Invoke-LocalApi "/kanban/columns/$($column.id)" 'Delete' $null $demoHeaders} '移出'
Invoke-LocalApi "/kanban/task/$other/dependencies?dependsOnTaskId=$task" 'Post' $null $demoHeaders | Out-Null
Expect-Rejection {Invoke-LocalApi "/task/$other/status?status=2" 'Put' $null $demoHeaders} '前置'
Expect-Rejection {Invoke-LocalApi "/kanban/task/$task/dependencies?dependsOnTaskId=$other" 'Post' $null $demoHeaders} 'CIRCULAR'
$upload=Invoke-RestMethod 'http://127.0.0.1:3000/api/v1/file/upload' -Method Post -Headers $demoHeaders -Form @{file=Get-Item "$PSScriptRoot/native-runtime/prd-batch2-export.pdf"}
if($upload.code -ne 200){throw 'Attachment upload failed'}
Invoke-LocalApi "/task/$task" 'Put' @{attachments=@(@{fileId=$upload.data.fileId})} $demoHeaders | Out-Null
$withFile=Invoke-LocalApi "/task/$task" 'Get' $null $demoHeaders
if($withFile.attachments[0].fileId -ne $upload.data.fileId){throw 'Attachment association failed'}
Invoke-LocalApi "/task/$task/status?status=2" 'Put' $null $demoHeaders | Out-Null
$completed=Invoke-LocalApi "/task/$task" 'Get' $null $demoHeaders
if(-not $completed.repeatNextId){throw 'Recurring task not created'}
$next=Invoke-LocalApi "/task/$($completed.repeatNextId)" 'Get' $null $demoHeaders
if($next.dueDate -ne (Get-Date).AddDays(9).ToString('yyyy-MM-dd') -or $next.status -ne 0){throw 'Next occurrence date or status incorrect'}
Invoke-LocalApi "/task/$task/status?status=2" 'Put' $null $demoHeaders | Out-Null
$again=Invoke-LocalApi "/task/$task" 'Get' $null $demoHeaders
if($again.repeatNextId -ne $completed.repeatNextId){throw 'Repeated completion generated another occurrence'}
Invoke-LocalApi "/task/$other/status?status=2" 'Put' $null $demoHeaders | Out-Null
Invoke-LocalApi "/task/$subtask/status?status=2" 'Put' $null $demoHeaders | Out-Null
$statistics=Invoke-LocalApi "/task/statistics?projectId=$project" 'Get' $null $demoHeaders
if($statistics.total -ne 4 -or $statistics.completed -ne 3 -or $statistics.completionRate -ne 75 -or $statistics.trend.Count -ne 14){throw 'Statistics inconsistent with tasks'}
$projectBody.status=2
Invoke-LocalApi "/task/project/$project" 'Put' $projectBody $demoHeaders | Out-Null
Expect-Rejection {Invoke-LocalApi "/task/$task" 'Put' @{title='Archived write forbidden'} $demoHeaders} '归档'
$projectBody.status=1
Invoke-LocalApi "/task/project/$project" 'Put' $projectBody $demoHeaders | Out-Null
$projectDetail=Invoke-LocalApi "/task/project/$project" 'Get' $null $demoHeaders
if($projectDetail.milestones.Count -ne 2 -or $projectDetail.memberIds.Count -ne 1){throw 'Project details missing'}
@{projectId=$project;taskId=$task;subtaskId=$subtask;otherId=$other;nextTaskId=$completed.repeatNextId;columnId=$column.id;fileId=$upload.data.fileId;wipAtomic=$true;repeatIdempotent=$true;archiveAndRestore=$true;statistics=$statistics} | ConvertTo-Json -Depth 20 | Set-Content "$PSScriptRoot/native-runtime/prd-batch3-tasks.json" -Encoding UTF8
Write-Host 'Task recurrence, attachments, subtasks, WIP atomicity, dependencies, milestones, archive/restore and statistics verified.'
