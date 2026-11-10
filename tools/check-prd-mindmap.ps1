$ErrorActionPreference='Stop'
. "$PSScriptRoot/seed-local-demo.ps1"
$fixture=Get-Content "$PSScriptRoot/native-runtime/prd-batch2-content.json" -Raw | ConvertFrom-Json
$h=@{Authorization=$headers.Authorization;'X-Space-Id'="$($fixture.spaceId)"}
foreach($path in @('/document/list?spaceId=13&pageSize=1','/meeting/list','/task/list')){Invoke-LocalApi $path 'Get' $null $headers|Out-Null}
Assert-LocalDemoDatabase
@{documents=Invoke-LocalApi "/document/list?spaceId=$($fixture.spaceId)&pageSize=100" 'Get' $null $h}|ConvertTo-Json -Depth 30|Set-Content "$PSScriptRoot/native-runtime/before-prd-mindmap-$(Get-Date -Format 'yyyyMMdd-HHmmss').json" -Encoding UTF8
$doc=Invoke-LocalApi '/document/create' 'Post' @{title='PRD 验收 · 协作产品规划';type=3;spaceId=$fixture.spaceId} $h
$map=Invoke-LocalApi "/mindmap/$doc" 'Get' $null $h;$root=$map.rootNode.id
foreach($name in @('产品体验','协同能力','质量验收')){Invoke-LocalApi "/mindmap/$doc/node?parentId=$root&content=$([uri]::EscapeDataString($name))" 'Post' $null $h|Out-Null}
$map=Invoke-LocalApi "/mindmap/$doc" 'Get' $null $h;$child=$map.rootNode.children[0].id;$other=$map.rootNode.children[1].id
Invoke-LocalApi "/mindmap/$doc/node?parentId=$child&content=$([uri]::EscapeDataString('移动端布局'))" 'Post' $null $h|Out-Null
$map=Invoke-LocalApi "/mindmap/$doc" 'Get' $null $h;$grand=$map.rootNode.children[0].children[0].id
Invoke-LocalApi "/mindmap/$doc/nodes/$child/properties" 'Put' @{content='产品体验';parentId=$root;styleJson='{"color":"#1d2129","background":"#e8f8f2","borderColor":"#31a878","lineColor":"#31a878","fontSize":16,"tag":"重点","connector":"elbow"}';note='先验证主干流程，再检查窄屏';link='https://example.com';icon='flag'} $h|Out-Null
$stale=Invoke-RestMethod "$base/mindmap/$doc/nodes/$child/properties" -Method Put -Headers $h -ContentType 'application/json' -Body '{"expectedNote":"stale"}'
if($stale.code -ne 409){throw 'Stale node was overwritten'}
$cycle=Invoke-RestMethod "$base/mindmap/node/$child/move?targetParentId=$grand" -Method Put -Headers $h
if($cycle.code -ne 400){throw 'Node cycle accepted'}
Invoke-LocalApi "/mindmap/$doc/nodes/$grand/properties" 'Put' @{content='移动端布局';parentId=$other;styleJson='{}';note='';link='';icon=''} $h|Out-Null
$moved=Invoke-LocalApi "/mindmap/$doc" 'Get' $null $h
if($moved.rootNode.children[1].children[0].id -ne $grand){throw 'Property parent change failed'}
Invoke-LocalApi "/mindmap/node/$grand/move?targetParentId=$child" 'Put' $null $h|Out-Null
foreach($layout in @('org','fishbone','mindmap')){Invoke-LocalApi "/mindmap/$doc/layout?type=$layout" 'Put' $null $h|Out-Null;if((Invoke-LocalApi "/mindmap/$doc" 'Get' $null $h).layoutType -ne $layout){throw 'Layout not saved'}}
$snapshots=Invoke-LocalApi "/mindmap/$doc/snapshots" 'Get' $null $h
if(@($snapshots).Count -lt 5){throw 'Missing operation snapshots'}
Invoke-LocalApi "/mindmap/$doc/snapshots/$($snapshots[0].id)/rollback" 'Post' $null $h|Out-Null
# Restore only this newly created demonstration node after rollback verification.
Invoke-LocalApi "/mindmap/node/$grand/move?targetParentId=$child" 'Put' $null $h|Out-Null
$session=Invoke-LocalApi "/mindmap/$doc/presentation/control" 'Post' @{action='start'} $h
if(-not $session.active -or $session.total -ne 5){throw 'Presentation tree incorrect'}
$step=Invoke-LocalApi "/mindmap/$doc/presentation/control" 'Post' @{action='next'} $h
if($step.step -ne 1){throw 'Presentation next failed'}
Invoke-LocalApi "/mindmap/$doc/presentation/control" 'Post' @{action='annotate';annotation='本地演示验收'} $h|Out-Null
if((Invoke-LocalApi "/mindmap/$doc/presentation/state" 'Get' $null $h).annotation -ne '本地演示验收'){throw 'Annotation missing'}
Invoke-LocalApi "/mindmap/$doc/presentation/control" 'Post' @{action='stop'} $h|Out-Null
$template=Invoke-LocalApi '/template/create' 'Post' @{sourceDocId=$doc;name='PRD 验收 · 产品导图';category='思维导图'} $h
$copy=Invoke-LocalApi "/template/$template/use" 'Post' @{spaceId=$fixture.spaceId} $h
$copied=Invoke-LocalApi "/mindmap/$copy" 'Get' $null $h
if($copied.rootNode.children[0].note -ne '先验证主干流程，再检查窄屏'){throw 'Template node metadata missing'}
$image=[Convert]::ToBase64String([IO.File]::ReadAllBytes("$PSScriptRoot/native-runtime/prd-batch6-input.png"))
$pdf=Invoke-LocalApi "/mindmap/$doc/export" 'Post' @{image=$image} $h
$bytes=[Convert]::FromBase64String($pdf.base64);[IO.File]::WriteAllBytes("$PSScriptRoot/native-runtime/prd-batch6-export.pdf",$bytes)
if([Text.Encoding]::ASCII.GetString($bytes,0,4) -ne '%PDF'){throw 'Invalid PDF'}
@{docId=$doc;spaceId=$fixture.spaceId;rootId=$root;childId=$child;grandchildId=$grand;snapshots=@($snapshots).Count;staleCode=$stale.code;cycleCode=$cycle.code;presentationTotal=$session.total;templateId=$template;copy=$copy;pdfBytes=$bytes.Length}|ConvertTo-Json -Depth 10|Set-Content "$PSScriptRoot/native-runtime/prd-batch6-mindmap.json" -Encoding UTF8
Write-Host 'Mindmap properties, guarded moves, history, layouts, template and presentation verified.'
