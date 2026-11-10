# Run with PowerShell 7. Creates retained demo content in the isolated local DB.
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/seed-local-demo.ps1"
$root = Split-Path $PSScriptRoot -Parent
$spaceId = @(Invoke-LocalApi '/space/my' 'Get' $null $headers)[0].spaceId
$headers['X-Space-Id'] = "$spaceId"
$documentList = Invoke-LocalApi "/document/list?spaceId=$spaceId" 'Get' $null $headers
$document = @($documentList.records | Where-Object title -eq 'Welcome to Nexus Office') | Select-Object -First 1
if ($document) { $docId = $document.docId }
else { $docId = Invoke-LocalApi '/document/create' 'Post' @{ title = 'Welcome to Nexus Office'; type = 1; spaceId = $spaceId } $headers }
$html = '<h1>Welcome to Nexus Office</h1><p>Your local workspace is ready.</p>'
$saved = Invoke-RestMethod "$base/document/$docId/content" -Method Put -Headers $headers -ContentType 'application/json' -Body $html
if ($saved.code -ne 200) { throw 'Document save failed.' }
$content = Invoke-LocalApi "/document/$docId/content" 'Get' $null $headers
if ($content.content -ne $html) { throw 'Document read-back did not match saved content.' }
Write-Host 'PASS document create/save/read-back'

$project = @(Invoke-LocalApi "/task/project/list?spaceId=$spaceId" 'Get' $null $headers) | Where-Object name -eq 'Demo Project' | Select-Object -First 1
if ($project) { $projectId = $project.projectId }
else { $projectId = Invoke-LocalApi "/task/project/create?name=Demo%20Project&spaceId=$spaceId" 'Post' $null $headers }
$existingTask = @(Invoke-LocalApi "/task/list?projectId=$projectId" 'Get' $null $headers) | Where-Object title -eq 'Try the workspace' | Select-Object -First 1
if ($existingTask) { $taskId = $existingTask.taskId }
else { $taskId = Invoke-LocalApi "/task/create?title=Try%20the%20workspace&projectId=$projectId" 'Post' $null $headers }
$task = Invoke-LocalApi "/task/$taskId" 'Get' $null $headers
if ($task.title -ne 'Try the workspace') { throw 'Task read-back failed.' }
Write-Host 'PASS project/task create/read-back'

$fixture = Join-Path $root 'LOCAL-RUN.md'
$uploaded = Invoke-RestMethod "$base/file/upload" -Method Post -Headers $headers -Form @{ file = Get-Item $fixture }
if ($uploaded.code -ne 200) { throw 'File upload failed.' }
$download = Invoke-LocalApi "/file/download/$($uploaded.data.fileId)" 'Get' $null $headers
$downloaded = Invoke-WebRequest $download.downloadUrl -UseBasicParsing
$bytes = $downloaded.Content
if ($bytes -is [string]) { $bytes = [Text.Encoding]::UTF8.GetBytes($bytes) }
if ([Convert]::ToBase64String($bytes) -ne [Convert]::ToBase64String([IO.File]::ReadAllBytes($fixture))) { throw 'Downloaded file differs from uploaded file.' }
Write-Host 'PASS file upload/download byte comparison'

foreach ($path in @("/document/list?spaceId=$spaceId", '/chat/conversations', '/meeting/list', '/task/list', "/file/list?spaceId=$spaceId", '/notification/list', '/search/health', '/search/global?keyword=Welcome', "/collaboration/$docId/yjs/max-seq")) {
    Invoke-LocalApi $path 'Get' $null $headers | Out-Null
    Write-Host "PASS $path"
}
$socket = New-Object Net.WebSockets.ClientWebSocket
$deadline = New-Object Threading.CancellationTokenSource(10000)
try {
    $socket.ConnectAsync([uri]"ws://127.0.0.1:3000/ws?token=$($login.accessToken)", $deadline.Token).GetAwaiter().GetResult() | Out-Null
    $payload = [Text.Encoding]::UTF8.GetBytes('{"type":"CONNECT"}')
    $socket.SendAsync([ArraySegment[byte]]::new($payload), [Net.WebSockets.WebSocketMessageType]::Text, $true, $deadline.Token).GetAwaiter().GetResult() | Out-Null
    $buffer = New-Object byte[] 4096
    $received = $socket.ReceiveAsync([ArraySegment[byte]]::new($buffer), $deadline.Token).GetAwaiter().GetResult()
    $ack = [Text.Encoding]::UTF8.GetString($buffer, 0, $received.Count) | ConvertFrom-Json
    if ($ack.type -ne 'CONNECT_ACK') { throw 'WebSocket did not acknowledge authenticated connection.' }
    Write-Host 'PASS authenticated WebSocket through frontend proxy'
} finally { $socket.Dispose(); $deadline.Dispose() }
Write-Host 'Local workflow checks passed.'
