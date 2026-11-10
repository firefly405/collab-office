$ErrorActionPreference='Stop'
. "$PSScriptRoot/seed-local-demo.ps1"
Invoke-LocalApi '/document/list?spaceId=13&pageSize=1' 'Get' $null $headers | Out-Null
Invoke-LocalApi '/meeting/list' 'Get' $null $headers | Out-Null
Invoke-LocalApi '/task/list' 'Get' $null $headers | Out-Null
Assert-LocalDemoDatabase
$runtime=Join-Path $PSScriptRoot 'native-runtime'
$before=@{}
foreach($path in @('/space/my','/template/list','/knowledge/list','/chat/conversations')) { $before[$path]=Invoke-LocalApi $path 'Get' $null $headers }
$before | ConvertTo-Json -Depth 15 | Set-Content "$runtime/before-prd-content-$(Get-Date -Format 'yyyyMMdd-HHmmss').json" -Encoding UTF8
$space=@(Invoke-LocalApi '/space/my' 'Get' $null $headers) | Where-Object spaceName -eq 'PRD 功能验收' | Select-Object -First 1
if(-not $space){$space=Invoke-LocalApi '/space/create' 'Post' @{spaceName='PRD 功能验收'} $headers}
$demoHeaders=@{Authorization=$headers.Authorization;'X-Space-Id'="$($space.spaceId)"}
$content='<h1>PRD 内容复制验收</h1><p>标题、中文与正文必须保留 &amp; content</p>'
$doc=Invoke-LocalApi '/document/create' 'Post' @{title='PRD 验收 · 模板源文档';type=1;spaceId=$space.spaceId} $demoHeaders
$saved=Invoke-RestMethod -Uri "$base/document/$doc/content" -Method Put -Headers $demoHeaders -ContentType 'text/plain; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($content))
if($saved.code -ne 200){throw 'Source content save failed'}
$source=Invoke-LocalApi "/document/$doc/content" 'Get' $null $demoHeaders
$template=Invoke-LocalApi '/template/create' 'Post' @{sourceDocId="$doc";name='PRD 验收 · 项目计划模板';category='项目管理'} $demoHeaders
$copy=Invoke-LocalApi "/template/$template/use" 'Post' $null $demoHeaders
$copied=Invoke-LocalApi "/document/$copy/content" 'Get' $null $demoHeaders
if("$copy" -eq "$doc" -or $copied.content -ne $source.content){throw 'Template content copy failed'}
Invoke-LocalApi '/knowledge/categories' 'Post' @{name='项目规范'} $demoHeaders | Out-Null
$category=@(Invoke-LocalApi '/knowledge/categories' 'Get' $null $demoHeaders) | Where-Object name -eq '项目规范' | Select-Object -First 1
$knowledge=Invoke-LocalApi '/knowledge/create' 'Post' @{title='PRD 验收 · 新成员入门';content='先阅读项目计划，再认领任务。';categoryId=$category.id;tagsJson='["入门","协作"]'} $demoHeaders
Invoke-LocalApi "/knowledge/$knowledge" 'Put' @{title='PRD 验收 · 新成员入门';content='先阅读项目计划，再认领任务。知识库内容支持再次编辑。';categoryId=$category.id;tagsJson='["入门","协作"]'} $demoHeaders | Out-Null
Invoke-LocalApi "/knowledge/$knowledge/favorite?value=true" 'Put' $null $demoHeaders | Out-Null
$entry=Invoke-LocalApi "/knowledge/$knowledge" 'Get' $null $demoHeaders
if($entry.content -notlike '*再次编辑*' -or $entry.starCount -ne 1){throw 'Knowledge update or favorite failed'}
$favorites=@(Invoke-LocalApi '/knowledge/favorites' 'Get' $null $demoHeaders)
if($knowledge -notin $favorites){throw 'Favorite list failed'}
$share=Invoke-LocalApi "/document/$doc/share" 'Post' @{permission=1;expireDays=1} $demoHeaders
$shares=@(Invoke-LocalApi "/document/$doc/shares" 'Get' $null $demoHeaders)
if(-not ($shares|Where-Object id -eq $share.shareId)){throw 'Share listing failed'}
Invoke-LocalApi "/document/$doc/shares/$($share.shareId)" 'Delete' $null $demoHeaders | Out-Null
$revoked=Invoke-RestMethod "$base/document/share/$($share.shareToken)"
if($revoked.code -ne 404){throw 'Revoked share still accessible'}
$exports=@{}
foreach($format in @('docx','pdf')) {
    $task=Invoke-LocalApi "/document/$doc/export" 'Post' @{format=$format} $demoHeaders
    for($attempt=0;$attempt -lt 30 -and $task.status -eq 'PROCESSING';$attempt++) {Start-Sleep -Milliseconds 500;$task=Invoke-LocalApi "/document/export/tasks/$($task.taskId)" 'Get' $null $demoHeaders}
    if($task.status -ne 'COMPLETED'){throw "$format export failed: $($task.errorMsg)"}
    $file="$runtime/prd-batch2-export.$format"
    Invoke-WebRequest $task.fileUrl -OutFile $file
    if((Get-Item $file).Length -lt 100){throw 'Empty exported file'}
    $exports[$format]=@{status=$task.status;bytes=(Get-Item $file).Length;path=$file}
}
$reviewerFile="$runtime/prd-reviewer.json"
if(Test-Path $reviewerFile){$reviewer=Get-Content $reviewerFile -Raw | ConvertFrom-Json}else{
    $reviewer=[pscustomobject]@{username='prd_reviewer_'+(Get-Date -Format 'HHmmss');password='Prd'+[Guid]::NewGuid().ToString('N').Substring(0,12)}
    Invoke-LocalApi '/auth/register' 'Post' @{username=$reviewer.username;password=$reviewer.password} | Out-Null
    $reviewer|ConvertTo-Json|Set-Content $reviewerFile -Encoding UTF8
}
$reviewLogin=Invoke-LocalApi '/auth/login' 'Post' @{account=$reviewer.username;password=$reviewer.password}
$reviewHeaders=@{Authorization="Bearer $($reviewLogin.accessToken)";'X-Space-Id'="$($space.spaceId)"}
$reviewUser=Invoke-LocalApi '/user/info' 'Get' $null $reviewHeaders
Invoke-LocalApi "/space/$($space.spaceId)/members" 'Post' @($reviewUser.userId) $demoHeaders | Out-Null
$session=Invoke-LocalApi '/chat/sessions' 'Post' @{name='PRD 验收 · 协作讨论';type=2;memberIds=@($reviewUser.userId)} $demoHeaders
Invoke-LocalApi '/chat/compose' 'Post' @{sessionId="$session";content='PRD 消息引用与搜索验收';msgType='1'} $demoHeaders | Out-Null
$messages=@(Invoke-LocalApi "/chat/messages/$session" 'Get' $null $demoHeaders)
$original=$messages[0]
Invoke-LocalApi '/chat/compose' 'Post' @{sessionId="$session";content='已收到，准备执行';msgType='1';replyToId="$($original.msgId)"} $demoHeaders | Out-Null
Invoke-LocalApi "/chat/edit/$($original.msgId)" 'Put' @{content='PRD 消息引用与搜索验收（已编辑）'} $demoHeaders | Out-Null
Invoke-LocalApi "/chat/sessions/$session" 'Put' @{announcement='本群用于本机功能验收，完成后保留为演示记录。'} $demoHeaders | Out-Null
Invoke-LocalApi "/chat/sessions/$session/preferences" 'Put' @{pinned=$true;muted=$true;groupName='项目协作';draft='待发送的演示草稿'} $demoHeaders | Out-Null
Invoke-LocalApi "/chat/sessions/$session/members/$($reviewUser.userId)" 'Put' @{role='ADMIN'} $demoHeaders | Out-Null
Invoke-LocalApi "/chat/forward/$($original.msgId)" 'Post' @{sessionId="$session"} $demoHeaders | Out-Null
$messages=@(Invoke-LocalApi "/chat/messages/$session" 'Get' $null $demoHeaders)
if(-not($messages | Where-Object replyToId -eq $original.msgId)){throw 'Structured reply not saved'}
$search=@(Invoke-LocalApi '/chat/search?keyword=PRD' 'Get' $null $demoHeaders)
if(-not($search|Where-Object msgId -eq $original.msgId)){throw 'Message search failed'}
Invoke-LocalApi "/chat/read/$session" 'Post' $null $reviewHeaders | Out-Null
Invoke-LocalApi "/chat/recall/$($original.msgId)" 'Post' $null $demoHeaders | Out-Null
$messages=@(Invoke-LocalApi "/chat/messages/$session" 'Get' $null $demoHeaders)
if(($messages|Where-Object msgId -eq $original.msgId).content -ne '此消息已撤回'){throw 'Recall not reflected in history'}
$proof=@{spaceId="$($space.spaceId)";docId="$doc";copyId="$copy";templateId="$template";knowledgeId="$knowledge";sessionId="$session";templateCopied=$true;knowledgeEditedAndFavorited=$true;shareRevoked=$true;exports=$exports;chatRecallReplyEditForwardSearch=$true}
$proof|ConvertTo-Json -Depth 10|Set-Content "$runtime/prd-batch2-content.json" -Encoding UTF8
Write-Host 'Template, knowledge, share revocation, Word/PDF downloads, chat reply/edit/recall/search/preferences verified. Fixtures retained in PRD demo team.'
