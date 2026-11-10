$ErrorActionPreference='Stop'
. "$PSScriptRoot/seed-local-demo.ps1"
$fixture=Get-Content "$PSScriptRoot/native-runtime/prd-batch2-content.json" -Raw | ConvertFrom-Json
$demoHeaders=@{Authorization=$headers.Authorization;'X-Space-Id'="$($fixture.spaceId)"}
Invoke-LocalApi '/document/list?spaceId=13&pageSize=1' 'Get' $null $headers | Out-Null
Invoke-LocalApi '/meeting/list' 'Get' $null $headers | Out-Null
Invoke-LocalApi '/task/list' 'Get' $null $headers | Out-Null
Assert-LocalDemoDatabase
$docs=Invoke-LocalApi "/document/list?spaceId=$($fixture.spaceId)&pageSize=100" 'Get' $null $demoHeaders
$docs | ConvertTo-Json -Depth 10 | Set-Content "$PSScriptRoot/native-runtime/before-prd-organization-$(Get-Date -Format 'yyyyMMdd-HHmmss').json" -Encoding UTF8
$favorites=@(Invoke-LocalApi '/knowledge/favorites' 'Get' $null $demoHeaders)
if($fixture.knowledgeId -notin $favorites){throw 'Knowledge favorite listing failed'}
$copy=Invoke-LocalApi "/document/$($fixture.docId)/duplicate" 'Post' $null $demoHeaders
$source=Invoke-LocalApi "/document/$($fixture.docId)/content" 'Get' $null $demoHeaders
$copied=Invoke-LocalApi "/document/$copy/content" 'Get' $null $demoHeaders
if($source.content -ne $copied.content){throw 'Document duplication lost content'}
Invoke-LocalApi "/document/$copy/pin?value=true" 'Put' $null $demoHeaders | Out-Null
Invoke-LocalApi "/document/$copy" 'Put' @{tags='验收,项目'} $demoHeaders | Out-Null
$folder=Invoke-LocalApi '/file/folder?folderName=PRD-local-proof' 'Post' $null $demoHeaders
Invoke-LocalApi "/document/$copy/move?folderId=$folder" 'Put' $null $demoHeaders | Out-Null
$moved=(Invoke-LocalApi "/document/list?spaceId=$($fixture.spaceId)&folderId=$folder" 'Get' $null $demoHeaders).records | Where-Object docId -eq $copy
if(-not $moved -or $moved.isPinned -ne 1 -or $moved.tags -ne '验收,项目'){throw 'Folder, pin or tags failed'}
Invoke-LocalApi "/document/$copy/move" 'Put' $null $demoHeaders | Out-Null
$typed=@()
foreach($type in @(2,3,4)) {
    $id=Invoke-LocalApi '/document/create' 'Post' @{title="PRD 验收 · 类型$type 复制";type=$type;spaceId=$fixture.spaceId} $demoHeaders
    if($type -eq 2){
        $data=Invoke-LocalApi "/sheet/manage/$id" 'Get' $null $demoHeaders
        Invoke-LocalApi '/sheet/cell/batch' 'Put' @{sheetId=$data.sheets[0].id;cells=@(@{sheetId=$data.sheets[0].id;cellKey='A1';content='模板单元格'})} $demoHeaders | Out-Null
    } elseif($type -eq 3){
        $data=Invoke-LocalApi "/mindmap/$id" 'Get' $null $demoHeaders
        Invoke-LocalApi "/mindmap/$id/node?parentId=$($data.rootNode.id)&content=PRD-child" 'Post' $null $demoHeaders | Out-Null
    } else {
        Invoke-LocalApi "/whiteboard/$id/element" 'Post' @{type='text';positionJson='{"x":20,"y":20}';styleJson='{"color":"#3370ff"}';content='PRD-whiteboard'} $demoHeaders | Out-Null
    }
    $template=Invoke-LocalApi '/template/create' 'Post' @{sourceDocId="$id";name="PRD 验收 · 类型$type 模板";category='复制验收'} $demoHeaders
    $newId=Invoke-LocalApi "/template/$template/use" 'Post' $null $demoHeaders
    if($type -eq 2){
        $data=Invoke-LocalApi "/sheet/manage/$newId" 'Get' $null $demoHeaders
        $range=Invoke-LocalApi "/sheet/cell/range?sheetId=$($data.sheets[0].id)&range=A1:B2" 'Get' $null $demoHeaders
        if($range.A1 -ne '模板单元格'){throw 'Sheet template copy failed'}
    } elseif($type -eq 3){
        $data=Invoke-LocalApi "/mindmap/$newId" 'Get' $null $demoHeaders
        if($data.rootNode.children[0].content -ne 'PRD-child'){throw 'Mindmap template copy failed'}
    } else {
        $data=Invoke-LocalApi "/whiteboard/$newId" 'Get' $null $demoHeaders
        if($data.elements[0].content -ne 'PRD-whiteboard'){throw 'Whiteboard template copy failed'}
    }
    $typed+=@{type=$type;sourceId="$id";copyId="$newId";templateId="$template";verified=$true}
}
$messages=@(Invoke-LocalApi "/chat/messages/$($fixture.sessionId)" 'Get' $null $demoHeaders)
$message=$messages | Where-Object {-not $_.isRecalled} | Select-Object -First 1
Invoke-LocalApi "/chat/pin/$($message.msgId)?value=true" 'Put' $null $demoHeaders | Out-Null
$session=Invoke-LocalApi "/chat/sessions/$($fixture.sessionId)" 'Get' $null $demoHeaders
if(-not($session.pins | Where-Object msgId -eq $message.msgId)){throw 'Pinned group message missing'}
@{copyId="$copy";folderId="$folder";typedCopies=$typed;favoriteList=$true;moveRootAndFolder=$true;tagsAndPin=$true;groupPin=$true} | ConvertTo-Json -Depth 10 | Set-Content "$PSScriptRoot/native-runtime/prd-batch2-organization.json" -Encoding UTF8
Write-Host 'Document duplicate/move/tags/pin, knowledge favorites, three typed templates and group pin verified.'
