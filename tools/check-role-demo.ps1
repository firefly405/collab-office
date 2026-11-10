$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$seedAst=[System.Management.Automation.Language.Parser]::ParseFile("$PSScriptRoot/seed-local-demo.ps1",[ref]$null,[ref]$null)
$guard=$seedAst.Find({param($node) $node -is [System.Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq 'Assert-LocalDemoDatabase'},$true)
Invoke-Expression $guard.Extent.Text
Assert-LocalDemoDatabase
. "$PSScriptRoot/seed-local-demo.ps1"
$fixture=Get-Content "$PSScriptRoot/native-runtime/role-demo.json" -Raw|ConvertFrom-Json
$h=@{Authorization=$headers.Authorization;'X-Space-Id'=$fixture.spaceId}
$ph=@{};$uh=@{}
foreach($d in $fixture.accounts){
  $login=Invoke-LocalApi '/auth/login' 'Post' @{account=$d.username;password=$d.password}
  $sessionHeaders=@{Authorization="Bearer $($login.accessToken)";'X-Space-Id'=$fixture.spaceId}
  if($d.role -eq 'PROJECT_ADMIN'){$ph=$sessionHeaders}else{$uh=$sessionHeaders}
  if($login.userInfo.platformRole -ne $d.role){throw 'Wrong login identity'}
}
$evidence=[System.Collections.Generic.List[object]]::new()
function Check([bool]$Condition,[string]$Name){if(-not $Condition){throw "FAILED: $Name"};$evidence.Add(@{check=$Name;result='PASS'});Write-Host "PASS $Name"}
function Denied([string]$Path,[string]$Method,$Body,$Actor,[int]$Code=403){
  $params=@{Uri="$base$Path";Method=$Method;Headers=$Actor;SkipHttpErrorCheck=$true}
  if($null -ne $Body){$params.ContentType='application/json; charset=utf-8';$params.Body=[Text.Encoding]::UTF8.GetBytes(($Body|ConvertTo-Json -Depth 10))}
  $r=Invoke-RestMethod @params
  Check ($r.code -eq $Code) "$Method $Path returns $Code"
}
$prior=Get-Content "$PSScriptRoot/native-runtime/role-demo.json" -Raw|ConvertFrom-Json
$before=@{project=(Invoke-LocalApi "/task/project/$($fixture.projectId)" 'Get' $null $ph);tasks=@(Invoke-LocalApi '/task/list' 'Get' $null $h);reports=@(Invoke-LocalApi '/task/reports' 'Get' $null $h);messages=@(Invoke-LocalApi "/chat/messages/$($fixture.sessionId)" 'Get' $null $h)}
$before|ConvertTo-Json -Depth 30|Set-Content "$PSScriptRoot/native-runtime/before-role-check-$(Get-Date -Format yyyyMMdd-HHmmss-fff).json" -Encoding UTF8
Denied '/admin/dashboard/stats' 'Get' $null $uh
Denied '/admin/dashboard/stats' 'Get' $null $ph
Denied "/admin/roles/users/$($fixture.accounts[1].userId)" 'Put' @{platformRole='SYSTEM_ADMIN'} $uh
Denied '/admin/roles/users/1' 'Put' @{platformRole='MEMBER'} $h 400
Denied "/task/project/create?spaceId=$($fixture.spaceId)&name=Forbidden" 'Post' $null $uh
Denied "/task/project/$($fixture.projectId)" 'Put' @{name='Forbidden'} $uh
Denied "/kanban/$($fixture.projectId)/columns" 'Post' @{name='Forbidden'} $uh
Denied "/space/$($fixture.spaceId)/members" 'Post' @('1') $ph
Denied "/space/$($fixture.spaceId)/organization/roles" 'Post' @{name='Forbidden';permissions=@{view=$true;edit=$true;download=$true;share=$true;delete=$true}} $uh
Check $before.project.canManage 'Project administrator can manage owned project'
Denied "/task/project/$($fixture.projectId)" 'Put' @{managerId='1'} $ph
Denied "/task/project/$($fixture.projectId)" 'Put' @{managerId="$($fixture.accounts[1].userId)"} $h 400
Invoke-LocalApi "/task/project/$($fixture.projectId)" 'Put' @{description=$before.project.description;memberIds=@($before.project.memberIds);milestones=@($before.project.milestones)} $ph|Out-Null
Check ((Invoke-LocalApi "/task/project/$($fixture.projectId)" 'Get' $null $ph).description -eq $before.project.description) 'Project management round trip preserves settings'
$private=@(Invoke-LocalApi "/task/project/list?spaceId=$($fixture.spaceId)" 'Get' $null $h)|Where-Object name -eq '权限隔离验收项目'|Select-Object -First 1
if(-not $private){$private=@{projectId=(Invoke-LocalApi "/task/project/create?spaceId=$($fixture.spaceId)&name=$([uri]::EscapeDataString('权限隔离验收项目'))" 'Post' $null $h)}}
$privateBefore=Invoke-LocalApi "/task/project/$($private.projectId)" 'Get' $null $h
$privateBefore|ConvertTo-Json -Depth 20|Set-Content "$PSScriptRoot/native-runtime/before-role-transfer-$(Get-Date -Format yyyyMMdd-HHmmss-fff).json" -Encoding UTF8
try{
  Invoke-LocalApi "/task/project/$($private.projectId)" 'Put' @{managerId="$($fixture.accounts[0].userId)"} $h|Out-Null
  Check ((Invoke-LocalApi "/task/project/$($private.projectId)" 'Get' $null $ph).canManage) 'System administrator can appoint project manager'
}finally{Invoke-LocalApi "/task/project/$($private.projectId)" 'Put' @{managerId="$($privateBefore.managerId)";memberIds=@($privateBefore.memberIds)} $h|Out-Null}
$secretTitle='隔离项目专属验收任务'
$secret=@(Invoke-LocalApi "/task/list?projectId=$($private.projectId)" 'Get' $null $h)|Where-Object title -eq $secretTitle|Select-Object -First 1
if(-not $secret){$secret=@{taskId=(Invoke-LocalApi '/task/create' 'Post' @{title=$secretTitle;projectId="$($private.projectId)"} $h)}}
foreach($actor in @($ph,$uh)){
  Denied "/task/project/$($private.projectId)" 'Get' $null $actor
  Denied "/task/project/$($private.projectId)" 'Put' @{name='Forbidden'} $actor
  Denied "/task/$($secret.taskId)" 'Get' $null $actor
  $projects=@(Invoke-LocalApi "/task/project/list?spaceId=$($fixture.spaceId)" 'Get' $null $actor)
  Check ("$($private.projectId)" -notin @($projects.projectId)) 'Project list excludes unjoined project'
  $tasks=@(Invoke-LocalApi '/task/list' 'Get' $null $actor)
  Check ("$($secret.taskId)" -notin @($tasks.taskId)) 'Task list excludes private task'
  $search=@(Invoke-LocalApi "/search/global?scope=task&keyword=$([uri]::EscapeDataString($secretTitle))" 'Get' $null $actor)
  Check ($search.Count -eq 0) 'Search hides private project task'
  $stats=Invoke-LocalApi '/user/workspace/statistics' 'Get' $null $actor
  Check ($stats.tasks -eq $tasks.Count) 'Workspace statistics use same visible task count'
}
$myTask=@(Invoke-LocalApi '/task/list' 'Get' $null $uh)|Where-Object {"$($_.assigneeId)" -eq "$($fixture.accounts[1].userId)"}|Select-Object -First 1
$oldStatus=$myTask.status
try{Invoke-LocalApi "/task/$($myTask.taskId)/status?status=1" 'Put' $null $uh|Out-Null;Check ((Invoke-LocalApi "/task/$($myTask.taskId)" 'Get' $null $uh).status -eq 1) 'Member can update assigned task'}finally{Invoke-LocalApi "/task/$($myTask.taskId)/status?status=$oldStatus" 'Put' $null $uh|Out-Null}
Denied "/task/$($myTask.taskId)" 'Put' @{assigneeId='1'} $uh
Denied "/task/$($myTask.taskId)" 'Delete' $null $uh
$reports=@(Invoke-LocalApi '/task/reports' 'Get' $null $uh)
$report=$reports|Where-Object title -eq '角色权限验收汇报'|Select-Object -First 1
if(-not $report){Invoke-LocalApi '/task/reports' 'Post' @{projectId=$fixture.projectId;title='角色权限验收汇报';content='任务已完成初步验收，请确认交付标准。'} $uh|Out-Null;$report=@(Invoke-LocalApi '/task/reports' 'Get' $null $uh)|Where-Object title -eq '角色权限验收汇报'|Select-Object -First 1}
Denied "/task/reports/$($report.id)/review" 'Put' $null $uh
foreach($item in @(@{actor=$ph;content='项目管理员：已核对进展，请补充演示截图。'},@{actor=$uh;content='普通用户：收到，截图将随验收清单提交。'},@{actor=$h;content='系统管理员：本机演示环境已准备完成。'})){
  if($item.content -notin @($report.replies.content)){Invoke-LocalApi "/task/reports/$($report.id)/replies" 'Post' @{content=$item.content} $item.actor|Out-Null}
}
Invoke-LocalApi "/task/reports/$($report.id)/review" 'Put' $null $ph|Out-Null
$read=@(Invoke-LocalApi '/task/reports' 'Get' $null $uh)|Where-Object id -eq $report.id|Select-Object -First 1
Check ($read.status -eq 'reviewed' -and @($read.replies).Count -ge 3) 'Report submit, three-role replies and review persist'
foreach($actor in @($h,$ph,$uh)){
  $messages=@(Invoke-LocalApi "/chat/messages/$($fixture.sessionId)" 'Get' $null $actor)
  Check ($messages.Count -ge 2) 'All three identities can read shared group'
}
$pmId=$fixture.accounts[0].userId
try{
  Invoke-LocalApi "/admin/roles/users/$pmId" 'Put' @{platformRole='MEMBER'} $h|Out-Null
  Check ((Invoke-LocalApi '/user/info' 'Get' $null $ph).platformRole -eq 'MEMBER') 'Role change is visible with existing token'
  Denied "/task/project/$($fixture.projectId)" 'Put' @{description='Forbidden after demotion'} $ph
}finally{Invoke-LocalApi "/admin/roles/users/$pmId" 'Put' @{platformRole='PROJECT_ADMIN'} $h|Out-Null}
$exportPath="$PSScriptRoot/native-runtime/role-member-export.zip"
Invoke-WebRequest "$base/user/workspace/export?kinds=tasks" -Headers $uh -OutFile $exportPath
$archive=[IO.Compression.ZipFile]::OpenRead($exportPath)
try{$entry=$archive.GetEntry('tasks.json');$reader=[IO.StreamReader]::new($entry.Open());try{$exported=@($reader.ReadToEnd()|ConvertFrom-Json)}finally{$reader.Dispose()};Check ("$($secret.taskId)" -notin @($exported.task_id)) 'ZIP export excludes private task'}finally{$archive.Dispose()}
$evidence|ConvertTo-Json -Depth 10|Set-Content "$PSScriptRoot/native-runtime/role-api-verification.json" -Encoding UTF8
Write-Host "Role verification complete: $($evidence.Count) assertions passed. No credentials included in evidence."
