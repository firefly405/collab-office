# Local acceptance: creates retained fixtures in a separate team; never prints tokens.
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$seedAst = [System.Management.Automation.Language.Parser]::ParseFile("$PSScriptRoot/seed-local-demo.ps1", [ref]$null, [ref]$null)
$guard = $seedAst.Find({param($node) $node -is [System.Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq 'Assert-LocalDemoDatabase'}, $true)
Invoke-Expression $guard.Extent.Text
Assert-LocalDemoDatabase
$passwordLine = Get-Content "$root/.env" | Where-Object { $_ -match '^DB_PASSWORD=' } | Select-Object -First 1
$previousPassword = $env:MYSQL_PWD
try {
    $env:MYSQL_PWD = $passwordLine.Substring('DB_PASSWORD='.Length).Trim().Trim('"').Trim("'")
    $backupPath = "$PSScriptRoot/native-runtime/before-demo-acceptance-$(Get-Date -Format yyyyMMdd-HHmmss).sql"
    & 'C:/DevTools/mysql-8.0.43-winx64/bin/mysqldump.exe' --host=127.0.0.1 --port=13306 --user=root --single-transaction --routines --triggers --set-gtid-purged=OFF "--result-file=$backupPath" collab_office
    if ($LASTEXITCODE -ne 0) { throw 'Local backup failed. Acceptance stopped.' }
} finally { $env:MYSQL_PWD = $previousPassword }
. "$PSScriptRoot/seed-local-demo.ps1"
$qaUsersPath = Join-Path $PSScriptRoot 'native-runtime/acceptance-users.json'
function Api([string]$Path,[string]$Method='Get',$Body=$null,$Auth=$headers,[int]$Expected=200) {
    $parameters=@{Uri="$base$Path";Method=$Method;Headers=$Auth;SkipHttpErrorCheck=$true}
    if($null -ne $Body){$parameters.ContentType='application/json; charset=utf-8';$parameters.Body=[Text.Encoding]::UTF8.GetBytes((ConvertTo-Json -InputObject $Body -Depth 10 -Compress))}
    $response=Invoke-WebRequest @parameters
    $value=$null
    try{$value=$response.Content|ConvertFrom-Json}catch{}
    $code=if($value -and $null -ne $value.code){[int]$value.code}else{[int]$response.StatusCode}
    if($code -ne $Expected){throw "$Method $Path expected $Expected, received $code ($($value.message))"}
    return $value.data
}
function Assert($Condition,[string]$Description){if(-not $Condition){throw "FAIL $Description"};Write-Host "PASS $Description"}
if(Test-Path $qaUsersPath){$qa=Get-Content $qaUsersPath -Raw|ConvertFrom-Json}
else {
    $qa=@{}
    foreach($role in @('member','outsider')){
        $credentials=@{username='qa_'+$role+'_'+[Guid]::NewGuid().ToString('N').Substring(0,6);password='Qa'+[Guid]::NewGuid().ToString('N').Substring(0,14)}
        Api '/auth/register' 'Post' $credentials @{}|Out-Null
        $qa[$role]=$credentials
    }
    $qa|ConvertTo-Json -Depth 4|Set-Content $qaUsersPath -Encoding UTF8
}
$memberLogin=Api '/auth/login' 'Post' @{account=$qa.member.username;password=$qa.member.password} @{}
$outsiderLogin=Api '/auth/login' 'Post' @{account=$qa.outsider.username;password=$qa.outsider.password} @{}
$memberHeaders=@{Authorization="Bearer $($memberLogin.accessToken)"}
$outsiderHeaders=@{Authorization="Bearer $($outsiderLogin.accessToken)"}
$member=Api '/user/info' 'Get' $null $memberHeaders
$team=Api '/space/create' 'Post' @{spaceName='Acceptance '+(Get-Date -Format 'MMdd-HHmmss')}
$headers['X-Space-Id']="$($team.spaceId)"
$memberHeaders['X-Space-Id']="$($team.spaceId)"
$outsiderHeaders['X-Space-Id']="$($team.spaceId)"
Api "/space/$($team.spaceId)/members" 'Post' @($member.userId)|Out-Null
Assert (@(Api "/space/$($team.spaceId)/members").Count -eq 2) 'team invitation persisted'
Api "/space/$($team.spaceId)/members" 'Get' $null $outsiderHeaders 403|Out-Null

$docId=Api '/document/create' 'Post' @{title='Acceptance shared document';type=1;spaceId=$team.spaceId}
$html='<h1>Shared document</h1><p>Persisted acceptance content.</p>'
$saved=Invoke-RestMethod "$base/document/$docId/content" -Method Put -Headers $memberHeaders -ContentType 'text/html; charset=utf-8' -Body $html
Assert ($saved.code -eq 200 -and (Api "/document/$docId/content").content -eq $html) 'team document save and read-back'
Api "/document/$docId/content" 'Get' $null $outsiderHeaders 403|Out-Null
Api "/document/$docId/content" 'Put' @{} $outsiderHeaders 403|Out-Null
Api "/document/$docId/favorite?fav=true" 'Post'|Out-Null
$favorites=Api "/document/list?spaceId=$($team.spaceId)&filter=starred"
$otherFavorites=Api "/document/list?spaceId=$($team.spaceId)&filter=starred" 'Get' $null $memberHeaders
Assert (@($favorites.records).Count -eq 1 -and @($otherFavorites.records).Count -eq 0) 'favorites are personal'
Assert (@(Api "/document/$docId/versions").Count -ge 2) 'actual saved versions available'
$comment=Api "/document/$docId/comments" 'Post' @{docId=$docId;content='Review complete'} $memberHeaders
Api "/document/$docId/comments/$($comment.commentId)/resolve" 'Put'|Out-Null
Assert ((Api "/document/$docId/comments")[0].isResolved -eq 1) 'document comments resolve'

$folderId=Api '/file/folder?folderName=Acceptance' 'Post'
$fixture=Get-Item "$root/LOCAL-RUN.md"
$upload=Invoke-RestMethod "$base/file/upload" -Method Post -Headers $headers -Form @{file=$fixture;folderId="$folderId"}
Assert ($upload.code -eq 200) 'file upload'
$fileId=$upload.data.fileId
$download=Api "/file/download/$fileId"
$response=Invoke-WebRequest $download.downloadUrl
$bytes=$response.Content
if($bytes -is [string]){$bytes=[Text.Encoding]::UTF8.GetBytes($bytes)}
Assert ([Convert]::ToBase64String($bytes) -eq [Convert]::ToBase64String([IO.File]::ReadAllBytes($fixture.FullName))) 'file download exact bytes'
Api "/file/download/$fileId" 'Get' $null $outsiderHeaders 403|Out-Null
Api "/file/$fileId" 'Delete' $null $outsiderHeaders 403|Out-Null
$unsigned=($download.downloadUrl -split '\?')[0]
$unsignedResponse=Invoke-WebRequest $unsigned -SkipHttpErrorCheck
Assert ($unsignedResponse.StatusCode -eq 403) 'object storage refuses anonymous access'
Api "/file/$fileId/move" 'Put'|Out-Null
Assert (@(Api "/file/list?spaceId=$($team.spaceId)"|Where-Object fileId -eq $fileId).Count -eq 1) 'move file back to root folder'

$due=(Get-Date).AddDays(3).ToString('yyyy-MM-dd')
$projectId=Api "/task/project/create?spaceId=$($team.spaceId)&name=Acceptance-project" 'Post'
Api "/task/project/$projectId" 'Put' @{memberIds=@($user.userId,$member.userId)}|Out-Null
Assert ("$($member.userId)" -in @((Api "/task/project/$projectId").memberIds | ForEach-Object {"$_"})) 'task assignee joined project before assignment'
$taskId=Api '/task/create' 'Post' @{title='Acceptance task';projectId=$projectId;priority=1;dueDate=$due;assigneeId=$member.userId;description='Check persistence';status=1}
$task=Api "/task/$taskId"
Assert ($task.priority -eq 1 -and $task.dueDate -eq $due -and "$($task.assigneeId)" -eq "$($member.userId)") 'task priority, deadline and assignee persisted'
Api "/task/$taskId" 'Put' @{assigneeId=$null;dueDate=$null;status=2}|Out-Null
$task=Api "/task/$taskId"
Assert ($null -eq $task.assigneeId -and $null -eq $task.dueDate -and $task.progress -eq 100) 'task completion and optional-field clearing'
Api "/task/$taskId" 'Get' $null $outsiderHeaders 403|Out-Null
$invalid=Api '/task/create' 'Post' @{title='Invalid priority';priority=9} $headers 400
Assert (@(Api '/task/list'|Where-Object title -eq 'Invalid priority').Count -eq 0) 'invalid task creation rolls back'

$start=(Get-Date).AddDays(1).ToString('yyyy-MM-ddTHH:mm:ss')
$end=(Get-Date).AddDays(1).AddHours(1).ToString('yyyy-MM-ddTHH:mm:ss')
$meetingId=Api '/meeting/create' 'Post' @{title='Acceptance planning';startTime=$start;endTime=$end;agenda='Plan the next milestone';participantIds=@($member.userId)}
$meeting=Api "/meeting/$meetingId" 'Get' $null $memberHeaders
Assert ($meeting.agenda -eq 'Plan the next milestone' -and $meeting.endTime -eq $end.Replace('T',' ') -and @($meeting.participants).Count -eq 2) 'meeting JSON scheduling and participants'
$currentMinutes=Api "/meeting/$meetingId/minutes"
Api "/meeting/$meetingId/minutes" 'Put' @{minuteContent='Milestone agreed';revision=$currentMinutes.revision;todoListJson=@(@{id='acceptance-followup';title='Acceptance meeting follow-up';assigneeId="$($member.userId)";dueDate=$due})}|Out-Null
Assert ((Api "/meeting/$meetingId/minutes" 'Get' $null $memberHeaders).minuteContent -eq 'Milestone agreed') 'meeting minutes persisted'
$converted=Api '/task/from-meeting' 'Post' @{meetingId=$meetingId;projectId=$projectId;itemIds=@('acceptance-followup')}
$repeated=Api '/task/from-meeting' 'Post' @{meetingId=$meetingId;projectId=$projectId;itemIds=@('acceptance-followup')}
Assert ($converted.'acceptance-followup' -eq $repeated.'acceptance-followup' -and (Api "/task/$($converted.'acceptance-followup')").title -eq 'Acceptance meeting follow-up') 'meeting follow-up becomes one persisted task on repeated conversion'
Api "/meeting/$meetingId/minutes" 'Put' @{minuteContent='Forbidden'} $memberHeaders 403|Out-Null
Api "/meeting/$meetingId" 'Get' $null $outsiderHeaders 403|Out-Null

$sessionId=Api '/chat/sessions' 'Post' @{name='Acceptance group';type=2;memberIds=@($member.userId)}
$socket=[Net.WebSockets.ClientWebSocket]::new()
$timeout=[Threading.CancellationTokenSource]::new(20000)
function Receive-Frame {
    $buffer=[byte[]]::new(65536)
    $result=$socket.ReceiveAsync([ArraySegment[byte]]::new($buffer),$timeout.Token).GetAwaiter().GetResult()
    return [Text.Encoding]::UTF8.GetString($buffer,0,$result.Count)|ConvertFrom-Json
}
try{
    $socket.ConnectAsync([uri]"ws://127.0.0.1:3000/ws?token=$($memberLogin.accessToken)",$timeout.Token).GetAwaiter().GetResult()|Out-Null
    $connect=[Text.Encoding]::UTF8.GetBytes('{"type":"CONNECT"}')
    $socket.SendAsync([ArraySegment[byte]]::new($connect),[Net.WebSockets.WebSocketMessageType]::Text,$true,$timeout.Token).GetAwaiter().GetResult()|Out-Null
    $ack=Receive-Frame
    Assert ($ack.type -eq 'CONNECT_ACK') 'second user WebSocket connected'
    Api "/chat/send?sessionId=$sessionId&content=Acceptance-message&msgType=1" 'Post'|Out-Null
    do{$incoming=Receive-Frame}while($incoming.type -ne 'CHAT_RECEIVE')
    Assert ($incoming.data.content -eq 'Acceptance-message' -and $incoming.data.sessionId -eq $sessionId) 'real cross-account WebSocket message delivery'
}finally{$socket.Dispose();$timeout.Dispose()}
$conversation=@(Api '/chat/conversations' 'Get' $null $memberHeaders)|Where-Object sessionId -eq $sessionId
Assert ($conversation.unreadCount -eq 1) 'unread counter incremented'
Api "/chat/read/$sessionId" 'Post' $null $memberHeaders|Out-Null
$conversation=@(Api '/chat/conversations' 'Get' $null $memberHeaders)|Where-Object sessionId -eq $sessionId
Assert ($conversation.unreadCount -eq 0 -and @(Api "/chat/messages/$sessionId" 'Get' $null $memberHeaders).Count -eq 1) 'read receipt and message history persisted'
Api "/chat/messages/$sessionId" 'Get' $null $outsiderHeaders 403|Out-Null
$attachment=[uri]::EscapeDataString((@{fileId=$fileId;fileName=$fixture.Name}|ConvertTo-Json -Compress))
Api "/chat/send?sessionId=$sessionId&content=$attachment&msgType=3" 'Post'|Out-Null
Assert (@(Api "/chat/messages/$sessionId" 'Get' $null $memberHeaders|Where-Object msgType -eq 3).Count -eq 1) 'chat file attachment persisted'

$spreadsheetId=Api '/document/create' 'Post' @{title='Acceptance spreadsheet';type=2;spaceId=$team.spaceId}
$spreadsheet=Api "/sheet/manage/$spreadsheetId"
$innerSheetId=$spreadsheet.sheets[0].id
Api '/sheet/cell/batch' 'Put' @{sheetId=$innerSheetId;cells=@(@{sheetId=$innerSheetId;cellKey='A1';content='Saved cell'})} $memberHeaders|Out-Null
Assert ((Api "/sheet/cell/range?sheetId=$innerSheetId&range=A1:B2").A1 -eq 'Saved cell') 'spreadsheet values persisted with correct worksheet ID'
Api "/sheet/cell/range?sheetId=$innerSheetId&range=A1:B2" 'Get' $null $outsiderHeaders 403|Out-Null
Api "/sheet/manage/$spreadsheetId/sheet?name=Second" 'Post'|Out-Null
Assert (@((Api "/sheet/manage/$spreadsheetId").sheets).Count -eq 2) 'multiple worksheets persisted'
$excel=Api "/sheet/$spreadsheetId/export"
Assert ((Invoke-WebRequest $excel.downloadUrl).StatusCode -eq 200) 'spreadsheet Excel export downloadable'

$mapId=Api '/document/create' 'Post' @{title='Acceptance mindmap';type=3;spaceId=$team.spaceId}
$map=Api "/mindmap/$mapId"
Api "/mindmap/$mapId/node?parentId=$($map.rootNode.id)&content=Saved-node" 'Post'|Out-Null
$map=Api "/mindmap/$mapId"
$child=$map.rootNode.children[0]
Assert ($child.content -eq 'Saved-node') 'mindmap child node persisted'
Api "/mindmap/node/$($child.id)?content=Updated-node" 'Put'|Out-Null
Assert ((Api "/mindmap/$mapId").rootNode.children[0].content -eq 'Updated-node') 'mindmap node editing persisted'
Api "/mindmap/$mapId" 'Get' $null $outsiderHeaders 403|Out-Null

$export=Api "/document/$docId/export" 'Post' @{format='txt'}
for($i=0;$i -lt 20;$i++){
    $export=Api "/document/export/tasks/$($export.taskId)"
    if($export.status -ne 'PROCESSING'){break};Start-Sleep -Milliseconds 500
}
Assert ($export.status -eq 'COMPLETED' -and (Invoke-WebRequest $export.fileUrl).StatusCode -eq 200) 'document export downloadable'
Api "/document/export/tasks/$($export.taskId)" 'Get' $null $outsiderHeaders 403|Out-Null
$share=Api "/document/$docId/share" 'Post' @{permission=1;password='local-share-check';expireDays=7}
Api "/document/share/$($share.shareToken)" 'Get' $null @{} 401|Out-Null
Assert ((Api "/document/share/$($share.shareToken)/content?password=local-share-check" 'Get' $null @{}).content -eq $html) 'password-protected read-only share'
Api "/document/$docId" 'Delete'|Out-Null
Api "/document/share/$($share.shareToken)/content?password=local-share-check" 'Get' $null @{} 404|Out-Null
Assert (@((Api "/document/list?spaceId=$($team.spaceId)&status=0").records).Count -eq 1) 'document recycle bin'
Api "/document/$docId/recover" 'Post'|Out-Null

$searchResults=@(Api '/search/global?keyword=Acceptance')
Assert (@($searchResults|Where-Object id -eq $docId).Count -eq 1) 'global search returns accessible document'
Api '/search/global?keyword=Acceptance' 'Get' $null $outsiderHeaders 403|Out-Null

Start-Sleep -Milliseconds 500
$notifications=@(Api '/notification/list' 'Get' $null $memberHeaders)
$invitation=$notifications|Where-Object relatedId -eq $meetingId|Select-Object -First 1
Assert ($null -ne $invitation) 'meeting invitation delivered to station notifications'
Api '/notification/read-all' 'Put' $null $memberHeaders|Out-Null
Assert ((Api '/notification/unread-count' 'Get' $null $memberHeaders) -eq 0) 'mark all notifications read'
Api "/space/$($team.spaceId)/members/role" 'Put' @{spaceId=$team.spaceId;userId=$member.userId;roleType=3}|Out-Null
Api "/document/$docId/content" 'Put' @{} $memberHeaders 403|Out-Null
Assert ((Api "/document/$docId" 'Get' $null $memberHeaders).canEdit -eq $false) 'visitor is read-only'
Api "/space/$($team.spaceId)/members/role" 'Put' @{spaceId=$team.spaceId;userId=$member.userId;roleType=2}|Out-Null
Api "/meeting/$meetingId" 'Delete'|Out-Null
Assert ((Api "/meeting/$meetingId").status -eq 4) 'test meeting cancelled with valid status'
@{spaceId=$team.spaceId;docId=$docId;taskId=$taskId;fileId=$fileId;meetingId=$meetingId;sessionId=$sessionId}|ConvertTo-Json|Set-Content "$PSScriptRoot/native-runtime/acceptance-fixtures.json" -Encoding UTF8
Write-Host 'All collaboration acceptance checks passed.'
