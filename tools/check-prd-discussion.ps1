$ErrorActionPreference='Stop'
. "$PSScriptRoot/seed-local-demo.ps1"
$fixture=Get-Content "$PSScriptRoot/native-runtime/prd-batch2-content.json" -Raw | ConvertFrom-Json
$demoHeaders=@{Authorization=$headers.Authorization;'X-Space-Id'="$($fixture.spaceId)"}
foreach($path in @('/document/list?spaceId=13&pageSize=1','/meeting/list','/task/list')) {Invoke-LocalApi $path 'Get' $null $headers | Out-Null}
Assert-LocalDemoDatabase
@{documents=Invoke-LocalApi "/document/list?spaceId=$($fixture.spaceId)&pageSize=100" 'Get' $null $demoHeaders} | ConvertTo-Json -Depth 20 | Set-Content "$PSScriptRoot/native-runtime/before-prd-discussion-$(Get-Date -Format 'yyyyMMdd-HHmmss').json" -Encoding UTF8
$doc=Invoke-LocalApi '/document/create' 'Post' @{title='PRD 验收 · 评论与版本';type=1;spaceId=$fixture.spaceId} $demoHeaders
function Save-FixtureContent([string]$Content) {
    $response=Invoke-RestMethod "$base/document/$doc/content" -Method Put -Headers $demoHeaders -ContentType 'text/html; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($Content))
    if($response.code -ne 200){throw "Save failed: $($response.message)"}
    return Invoke-LocalApi "/document/$doc/content" 'Get' $null $demoHeaders
}
$old=Save-FixtureContent '<h1>项目计划</h1><p>旧正文</p><h2>验收标准</h2><p>桌面和窄屏均可用。</p>'
Invoke-LocalApi "/document/$doc/versions/snapshot?name=$([uri]::EscapeDataString('需求评审版'))&remark=$([uri]::EscapeDataString('评审前基线'))" 'Post' $null $demoHeaders | Out-Null
$updated=Save-FixtureContent '<h1>项目计划</h1><p>新正文与协作验收</p><h2>验收标准</h2><p>桌面和窄屏均可用。</p>'
Invoke-LocalApi "/document/$doc/versions/$($updated.version)/metadata" 'Put' @{name='联调完成版';remark='评论、嵌套回复和版本对比'} $demoHeaders | Out-Null
$versions=@(Invoke-LocalApi "/document/$doc/versions?size=100" 'Get' $null $demoHeaders)
if(-not($versions|Where-Object versionName -eq '需求评审版') -or -not($versions|Where-Object versionName -eq '联调完成版')){throw 'Named snapshots missing from version list'}
$diff=Invoke-LocalApi "/document/$doc/versions/compare?v1=$($old.version)&v2=$($updated.version)" 'Get' $null $demoHeaders
if(-not($diff.diffSegments|Where-Object {$_.type -eq 'insert' -and $_.text -eq '新正文与协作验收'}) -or -not($diff.diffSegments|Where-Object {$_.type -eq 'delete' -and $_.text -eq '旧正文'})){throw 'Readable diff missing'}
$anchor=@{index=5;length=9;text='新正文与协作验收'} | ConvertTo-Json -Compress
$rootComment=Invoke-LocalApi "/document/$doc/comments" 'Post' @{docId=$doc;content='请核对这段验收说明。';selectionRange=$anchor;mentionUsers=@(1)} $demoHeaders
$reply=Invoke-LocalApi "/document/$doc/comments" 'Post' @{docId=$doc;content='已核对桌面布局。';parentId=$rootComment.commentId} $demoHeaders
$nested=Invoke-LocalApi "/document/$doc/comments" 'Post' @{docId=$doc;content='窄屏也已补充验证。';parentId=$reply.commentId} $demoHeaders
$threads=@(Invoke-LocalApi "/document/$doc/comments?filter=unresolved" 'Get' $null $demoHeaders)
if($threads.Count -ne 1 -or $threads[0].replies[0].replies[0].commentId -ne $nested.commentId -or -not $threads[0].selectionRange){throw 'Nested thread or stored anchor missing'}
Invoke-LocalApi "/document/$doc/comments/$($rootComment.commentId)/resolve" 'Put' $null $demoHeaders | Out-Null
$unresolved=@(Invoke-LocalApi "/document/$doc/comments?filter=unresolved" 'Get' $null $demoHeaders)
$resolved=@(Invoke-LocalApi "/document/$doc/comments?filter=resolved" 'Get' $null $demoHeaders)
if($unresolved.Count -ne 0 -or $resolved.Count -ne 1 -or $resolved[0].replies[0].replies.Count -ne 1){throw 'Root thread resolution filters inconsistent'}
Invoke-LocalApi "/document/$doc/versions/policy" 'Put' @{limit=20} $demoHeaders | Out-Null
$policy=Invoke-LocalApi "/document/$doc/versions/policy" 'Get' $null $demoHeaders
if($policy.limit -ne 20){throw 'Retention policy not saved'}
$details=Invoke-LocalApi "/document/$doc" 'Get' $null $demoHeaders
if(-not $details.canComment -or -not $details.canEdit){throw 'Owner permissions incorrect'}
$export=Invoke-LocalApi "/document/$doc/export" 'Post' @{format='txt';includeComments=$true;includeVersions=$true} $demoHeaders
for($attempt=0;$export.status -eq 'PROCESSING' -and $attempt -lt 30;$attempt++){Start-Sleep -Milliseconds 250;$export=Invoke-LocalApi "/document/export/tasks/$($export.taskId)" 'Get' $null $demoHeaders}
if($export.status -ne 'COMPLETED'){throw 'Appendix export did not complete'}
$exportPath="$PSScriptRoot/native-runtime/prd-batch4-appendices.txt"
Invoke-WebRequest $export.fileUrl -OutFile $exportPath
$exportText=Get-Content $exportPath -Raw -Encoding UTF8
foreach($expected in @('评论与批注','请核对这段验收说明','版本历史','旧正文','新正文与协作验收')){if(-not $exportText.Contains($expected)){throw "Export missing: $expected"}}
@{docId=$doc;spaceId=$fixture.spaceId;rootCommentId=$rootComment.commentId;replyId=$reply.commentId;nestedId=$nested.commentId;oldVersion=$old.version;newVersion=$updated.version;versions=$versions;diff=$diff;resolvedThreads=$resolved;unresolvedCount=$unresolved.Count;policy=$policy;appendixExportBytes=(Get-Item $exportPath).Length} | ConvertTo-Json -Depth 25 | Set-Content "$PSScriptRoot/native-runtime/prd-batch4-discussion.json" -Encoding UTF8
Write-Host 'Anchored nested comments, root filtering, named snapshots, readable diff and retention policy verified.'
