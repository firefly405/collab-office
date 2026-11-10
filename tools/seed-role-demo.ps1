$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$seedAst=[System.Management.Automation.Language.Parser]::ParseFile("$PSScriptRoot/seed-local-demo.ps1",[ref]$null,[ref]$null)
$guard=$seedAst.Find({param($node) $node -is [System.Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq 'Assert-LocalDemoDatabase'},$true)
Invoke-Expression $guard.Extent.Text
Assert-LocalDemoDatabase
. "$PSScriptRoot/seed-local-demo.ps1"
if($user.platformRole -ne 'SYSTEM_ADMIN'){throw 'Deploy role support and prepare-role-schema.ps1 first'}
$manifestPath="$PSScriptRoot/native-runtime/role-demo.json"
$previous=if(Test-Path $manifestPath){Get-Content $manifestPath -Raw|ConvertFrom-Json}else{$null}
$dbPrevious=$env:MYSQL_PWD
try{
  $env:MYSQL_PWD=((Get-Content "$root/.env"|Where-Object {$_ -match '^DB_PASSWORD='}|Select-Object -First 1)-replace '^DB_PASSWORD=','').Trim().Trim('"').Trim("'")
  if(-not $env:MYSQL_PWD){throw 'Local DB credential missing'}
  $backup="$PSScriptRoot/native-runtime/before-role-seed-$(Get-Date -Format yyyyMMdd-HHmmss-fff).sql"
  & C:/DevTools/mysql-8.0.43-winx64/bin/mysqldump.exe --host=127.0.0.1 --port=13306 --user=root --single-transaction --set-gtid-purged=OFF "--result-file=$backup" collab_office
  if($LASTEXITCODE -ne 0){throw 'Backup failed; no demo data will be written'}
}finally{$env:MYSQL_PWD=$dbPrevious}
$definitions=@(@{username='pm_demo';password='project123';role='PROJECT_ADMIN'},@{username='user_demo';password='user12345';role='MEMBER'})
$accounts=@()
foreach($d in $definitions){
  $existing=@(Invoke-LocalApi "/user/directory?keyword=$($d.username)" 'Get' $null $headers)|Where-Object username -eq $d.username|Select-Object -First 1
  if($existing -and (-not $previous -or "$($existing.userId)" -notin @($previous.accounts|ForEach-Object {"$($_.userId)"}))){throw "Account name collision: $($d.username); existing account will not be changed"}
  if(-not $existing){$registered=Invoke-LocalApi '/auth/register' 'Post' @{username=$d.username;password=$d.password};$id=$registered.userId}else{$id=$existing.userId}
  $accounts+=@{username=$d.username;password=$d.password;role=$d.role;userId="$id"}
  @{accounts=$accounts;spaceId=$previous.spaceId;projectId=$previous.projectId;sessionId=$previous.sessionId}|ConvertTo-Json -Depth 8|Set-Content $manifestPath -Encoding UTF8
  Invoke-LocalApi "/admin/roles/users/$id" 'Put' @{platformRole=$d.role} $headers|Out-Null
}
$team=@(Invoke-LocalApi '/space/my' 'Get' $null $headers)|Where-Object spaceName -eq '角色协作演示'|Select-Object -First 1
if(-not $team){$team=Invoke-LocalApi '/space/create' 'Post' @{spaceName='角色协作演示'} $headers}
$h=@{Authorization=$headers.Authorization;'X-Space-Id'="$($team.spaceId)"}
Invoke-LocalApi "/space/$($team.spaceId)/members" 'Post' @($accounts|ForEach-Object {$_.userId}) $h|Out-Null
$pm=Invoke-LocalApi '/auth/login' 'Post' @{account=$accounts[0].username;password=$accounts[0].password}
$member=Invoke-LocalApi '/auth/login' 'Post' @{account=$accounts[1].username;password=$accounts[1].password}
$ph=@{Authorization="Bearer $($pm.accessToken)";'X-Space-Id'="$($team.spaceId)"}
$uh=@{Authorization="Bearer $($member.accessToken)";'X-Space-Id'="$($team.spaceId)"}
$project=@(Invoke-LocalApi "/task/project/list?spaceId=$($team.spaceId)" 'Get' $null $ph)|Where-Object name -eq '毕业设计演示交付'|Select-Object -First 1
if(-not $project){$project=@{projectId=(Invoke-LocalApi "/task/project/create?spaceId=$($team.spaceId)&name=$([uri]::EscapeDataString('毕业设计演示交付'))" 'Post' $null $ph)}}
$detail=Invoke-LocalApi "/task/project/$($project.projectId)" 'Get' $null $ph
$ids=@($detail.memberIds|ForEach-Object {"$_"})+@("$($user.userId)")+@($accounts|ForEach-Object {$_.userId})|Select-Object -Unique
if(@($detail.memberIds).Count -lt 3){Invoke-LocalApi "/task/project/$($project.projectId)" 'Put' @{memberIds=@($ids);description='产品评审、功能交付和答辩演示。';milestones=@(@{title='需求确认';dueDate=(Get-Date).AddDays(-1).ToString('yyyy-MM-dd');done=$true},@{title='演示验收';dueDate=(Get-Date).AddDays(3).ToString('yyyy-MM-dd');done=$false})} $ph|Out-Null}
$taskTitles=@('完成协作功能验收','整理答辩演示材料','复核项目交付清单')
$existingTasks=@(Invoke-LocalApi '/task/list' 'Get' $null $ph)
for($i=0;$i -lt $taskTitles.Count;$i++){
  if($existingTasks|Where-Object title -eq $taskTitles[$i]){continue}
  Invoke-LocalApi '/task/create' 'Post' @{title=$taskTitles[$i];projectId="$($project.projectId)";assigneeId=$accounts[1].userId;description='完成后提交工作汇报，项目管理员统一验收。';status=$i;priority=3;dueDate=(Get-Date).AddDays($i+1).ToString('yyyy-MM-dd')} $ph|Out-Null
}
$reports=@(Invoke-LocalApi '/task/reports' 'Get' $null $uh)
if(-not ($reports|Where-Object title -eq '本周进展与验收计划')){Invoke-LocalApi '/task/reports' 'Post' @{projectId="$($project.projectId)";title='本周进展与验收计划';content="已完成文档整理和任务拆解。`n待确认：答辩演示的先后顺序。`n下一步：逐项验收并补充截图。"} $uh|Out-Null}
$docs=Invoke-LocalApi "/document/list?spaceId=$($team.spaceId)&pageSize=100" 'Get' $null $h
if(-not ($docs.records|Where-Object title -eq '团队协作与验收清单')){
  $doc=Invoke-LocalApi '/document/create' 'Post' @{title='团队协作与验收清单';type=1;spaceId=$team.spaceId} $ph
  $res=Invoke-RestMethod "$base/document/$doc/content" -Method Put -Headers $ph -ContentType 'text/html; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes('<h1>团队协作与验收清单</h1><p>项目管理员拆解任务并安排里程碑，成员执行任务后提交进展汇报。</p><ul><li>检查文档保存与版本</li><li>核对任务状态与负责人</li><li>提交汇报并处理反馈</li></ul>'))
  if($res.code -ne 200){throw 'Document save failed'}
}
$session=@(Invoke-LocalApi '/chat/conversations' 'Get' $null $ph)|Where-Object name -eq '项目交付协作群'|Select-Object -First 1
if(-not $session){$session=@{sessionId=(Invoke-LocalApi '/chat/sessions' 'Post' @{name='项目交付协作群';type=2;memberIds=@("$($user.userId)",$accounts[1].userId)} $ph)}
  Invoke-LocalApi "/chat/send?sessionId=$($session.sessionId)&content=$([uri]::EscapeDataString('本周优先完成协作验收，进展请提交到工作汇报。'))" 'Post' $null $ph|Out-Null
  Invoke-LocalApi "/chat/send?sessionId=$($session.sessionId)&content=$([uri]::EscapeDataString('收到，我会更新任务状态并提交本周汇报。'))" 'Post' $null $uh|Out-Null
}
@{spaceId="$($team.spaceId)";projectId="$($project.projectId)";sessionId="$($session.sessionId)";accounts=$accounts}|ConvertTo-Json -Depth 8|Set-Content $manifestPath -Encoding UTF8
Write-Host 'Role demo ready: local accounts, shared team, managed project, assigned tasks, report and chat. Credentials retained in ignored runtime directory.'
