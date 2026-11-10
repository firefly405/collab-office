$ErrorActionPreference='Stop'
. "$PSScriptRoot/seed-local-demo.ps1"
foreach($p in @('/document/list?spaceId=13&pageSize=1','/meeting/list','/task/list')){Invoke-LocalApi $p 'Get' $null $headers|Out-Null}
Assert-LocalDemoDatabase
$reviewer=Get-Content "$PSScriptRoot/native-runtime/prd-reviewer.json" -Raw|ConvertFrom-Json
$login=Invoke-LocalApi '/auth/login' 'Post' @{account=$reviewer.username;password=$reviewer.password} @{}
$rh=@{Authorization='Bearer '+$login.accessToken}
$reviewerInfo=Invoke-LocalApi '/user/info' 'Get' $null $rh
$uid=$reviewerInfo.userId
$spaces=Invoke-LocalApi '/space/my' 'Get' $null $headers
@{spaces=$spaces}|ConvertTo-Json -Depth 30|Set-Content "$PSScriptRoot/native-runtime/before-prd-permissions-$(Get-Date -Format yyyyMMdd-HHmmss).json" -Encoding UTF8
$space=$spaces|Where-Object spaceName -eq 'PRD 权限验收'|Select-Object -First 1
if(-not $space){$space=Invoke-LocalApi '/space/create' 'Post' @{spaceName='PRD 权限验收'} $headers}
$sid=$space.spaceId;$h=@{Authorization=$headers.Authorization;'X-Space-Id'="$sid"};$rh['X-Space-Id']="$sid";$org="/space/$sid/organization"
Invoke-LocalApi "/space/$sid/members" 'Post' @($uid) $h|Out-Null
function State{Invoke-LocalApi $org 'Get' $null $h}
function Expect-Code([string]$Path,[string]$Method,$Body,$Auth,[int]$Code){$p=@{Uri="$base$Path";Method=$Method;Headers=$Auth;ContentType='application/json'};if($null -ne $Body){$p.Body=[Text.Encoding]::UTF8.GetBytes(($Body|ConvertTo-Json -Depth 30 -Compress))};$r=Invoke-RestMethod @p;if($r.code -ne $Code){throw "$Method $Path expected $Code got $($r.code): $($r.message)"}}
$s=State
if(-not($s.departments|Where-Object name -eq '产品研发')){Invoke-LocalApi "$org/departments" 'Post' @{name='产品研发'} $h|Out-Null}
$root=(State).departments|Where-Object name -eq '产品研发'|Select-Object -First 1
if(-not((State).departments|Where-Object name -eq '交付小组')){Invoke-LocalApi "$org/departments" 'Post' @{name='交付小组';parentId=$root.id;leaderId=$uid} $h|Out-Null}
$child=(State).departments|Where-Object name -eq '交付小组'|Select-Object -First 1
Expect-Code "$org/departments" 'Post' @{id=$root.id;name='产品研发';parentId=$child.id} $h 400
$read=@{view=$true;edit=$false;download=$false;share=$false;delete=$false}
if(-not((State).roles|Where-Object name -eq '审阅成员')){Invoke-LocalApi "$org/roles" 'Post' @{name='审阅成员';permissions=$read} $h|Out-Null}
$role=(State).roles|Where-Object name -eq '审阅成员'|Select-Object -First 1
$doc=Invoke-LocalApi '/document/create' 'Post' @{title='PRD 验收 · 仅评论权限';type=1;spaceId=$sid} $h
Invoke-LocalApi "$org/documents/$doc" 'Put' @(@{userId=$uid;permission=4}) $h|Out-Null
$detail=Invoke-LocalApi "/document/$doc" 'Get' $null $rh
if(-not $detail.canComment -or $detail.canEdit){throw 'Comment-only contract wrong'}
Expect-Code "/document/$doc" 'Put' @{title='must not change'} $rh 403
$comment=Invoke-LocalApi "/document/$doc/comments" 'Post' @{docId=$doc;content='仅评论成员实际发表批注成功。'} $rh
$folder=Invoke-LocalApi '/file/folder?folderName=PRD-ACL-parent' 'Post' $null $h
$nested=Invoke-LocalApi "/file/folder?folderName=PRD-ACL-child&parentId=$folder" 'Post' $null $h
Invoke-LocalApi "$org/folders/$folder" 'Put' @(@{userId=$uid;permissions=$read}) $h|Out-Null
$folderDoc=Invoke-LocalApi '/document/create' 'Post' @{title='PRD 验收 · 文件夹继承';type=1;spaceId=$sid;folderId=$nested} $h
Invoke-LocalApi "/document/$folderDoc" 'Get' $null $rh|Out-Null
Expect-Code "/document/$folderDoc" 'Put' @{title='must not change'} $rh 403
Expect-Code "/document/$folderDoc/export" 'Post' @{format='txt'} $rh 403
Invoke-LocalApi "$org/members/$uid" 'Put' @{customRoleId=$role.id;deptId=$child.id} $h|Out-Null
Expect-Code "$org/roles/$($role.id)" 'Delete' $null $h 409
Expect-Code "/document/$doc/export" 'Post' @{format='txt'} $rh 403
Expect-Code "$org/departments" 'Post' @{name='must not create'} $rh 403
Invoke-LocalApi "$org/members/$uid" 'Put' @{expiresAt='2020-01-01T00:00:00'} $h|Out-Null
Expect-Code "/document/$doc" 'Get' $null $rh 403
Invoke-LocalApi "$org/members/$uid" 'Put' @{deptId=$child.id} $h|Out-Null
$temporary=Invoke-LocalApi '/space/create' 'Post' @{spaceName='PRD 转让解散验收 '+(Get-Date -Format HHmmss)} $headers
$tid=$temporary.spaceId;$th=@{Authorization=$headers.Authorization;'X-Space-Id'="$tid"};$torg="/space/$tid/organization"
Invoke-LocalApi "/space/$tid/members" 'Post' @($uid) $th|Out-Null
$tempDoc=Invoke-LocalApi '/document/create' 'Post' @{title='PRD 验收 · 解散后拒绝';type=1;spaceId=$tid} $th
$share=Invoke-LocalApi "/document/$tempDoc/share" 'Post' @{permission=1;expireDays=1} $th
Expect-Code "$torg/transfer" 'Post' @{userId=$uid} $th 400
if((Invoke-LocalApi $torg 'Get' $null $th).space.ownerId -ne 1){throw 'Rejected transfer changed team ownership'}
$recipientFile="$PSScriptRoot/native-runtime/prd-transfer-admin.json"
@{testSpaceId=$tid;recipientFixtureExists=(Test-Path $recipientFile)}|ConvertTo-Json|Set-Content "$PSScriptRoot/native-runtime/before-prd-transfer-$(Get-Date -Format yyyyMMdd-HHmmss).json" -Encoding UTF8
if(Test-Path $recipientFile){$recipient=Get-Content $recipientFile -Raw|ConvertFrom-Json}else{
    $recipient=@{username='prd_transfer_'+(Get-Date -Format yyyyMMddHHmmss);password='Demo'+[Guid]::NewGuid().ToString('N').Substring(0,12)}
    Invoke-LocalApi '/auth/register' 'Post' $recipient @{}|Out-Null
    $recipient|ConvertTo-Json|Set-Content $recipientFile -Encoding UTF8
}
$recipientLogin=Invoke-LocalApi '/auth/login' 'Post' @{account=$recipient.username;password=$recipient.password} @{}
$recipientHeaders=@{Authorization='Bearer '+$recipientLogin.accessToken;'X-Space-Id'="$tid"}
$recipientInfo=Invoke-LocalApi '/user/info' 'Get' $null $recipientHeaders
$recipientId=$recipientInfo.userId
if($recipientInfo.platformRole -ne 'MEMBER'){throw 'Transfer fixture must start as MEMBER'}
@{recipientId=$recipientId;platformRole=$recipientInfo.platformRole;spaces=(Invoke-LocalApi '/space/my' 'Get' $null $recipientHeaders)}|ConvertTo-Json -Depth 20|Set-Content "$PSScriptRoot/native-runtime/before-prd-transfer-recipient-$(Get-Date -Format yyyyMMdd-HHmmss).json" -Encoding UTF8
Invoke-LocalApi "/space/$tid/members" 'Post' @($recipientId) $th|Out-Null
Invoke-LocalApi "/admin/roles/users/$recipientId" 'Put' @{platformRole='SYSTEM_ADMIN'} $headers|Out-Null
try{
    Invoke-LocalApi "$torg/transfer" 'Post' @{userId=$recipientId} $th|Out-Null
    Expect-Code "$torg/transfer" 'Post' @{userId=1} $th 403
    Expect-Code "/admin/roles/users/$recipientId" 'Put' @{platformRole='MEMBER'} $headers 409
    Invoke-LocalApi "$torg/transfer" 'Post' @{userId=1} $recipientHeaders|Out-Null
}finally{
    if((Invoke-LocalApi $torg 'Get' $null $th).space.ownerId -eq $recipientId){Invoke-LocalApi "$torg/transfer" 'Post' @{userId=1} $recipientHeaders|Out-Null}
    $recipientSpaces=@(Invoke-LocalApi '/space/my' 'Get' $null $recipientHeaders)
    foreach($owned in @($recipientSpaces|Where-Object { $_.ownerId -eq $recipientId -and $_.spaceName -eq ($recipient.username+'的团队') })){
        $ownedHeaders=@{Authorization=$recipientHeaders.Authorization;'X-Space-Id'="$($owned.spaceId)"}
        Invoke-LocalApi "/space/$($owned.spaceId)/organization/dissolve" 'Post' @{confirm=$owned.spaceName} $ownedHeaders|Out-Null
    }
    Invoke-LocalApi "/admin/roles/users/$recipientId" 'Put' @{platformRole='MEMBER'} $headers|Out-Null
}
Invoke-LocalApi "$torg/dissolve" 'Post' @{confirm=$temporary.spaceName} $th|Out-Null
Expect-Code "/document/$tempDoc" 'Get' $null $th 403
Expect-Code "/document/share/$($share.shareToken)" 'Get' $null @{} 404
@{spaceId=$sid;reviewerId=$uid;commentDocument=$doc;folderDocument=$folderDoc;folder=$folder;childFolder=$nested;commentOnlyWriteDenied=$true;commentCreated=$comment.commentId;folderInheritanceEnforced=$true;roleExportDenied=$true;expiredDenied=$true;departmentCycleCode=400;roleInUseCode=409;transferAndReturn=$true;dissolvedSpace=$tid;dissolvedDocumentAndShareDenied=$true;auditCount=@((State).audit).Count}|ConvertTo-Json -Depth 20|Set-Content "$PSScriptRoot/native-runtime/prd-batch9-permissions.json" -Encoding UTF8
Write-Host 'Department tree, role ceiling, inherited folder permissions, actual comment-only member, expiry, transfer and dissolution verified.'
