$ErrorActionPreference='Stop'
. "$PSScriptRoot/seed-local-demo.ps1"
$h=@{Authorization=$headers.Authorization;'X-Space-Id'='15'}
Assert-LocalDemoDatabase
$settings=Invoke-LocalApi '/user/settings' 'Get' $null $h
$subs=Invoke-LocalApi '/notification/subscriptions' 'Get' $null $h
@{settings=$settings;subscriptions=$subs}|ConvertTo-Json -Depth 30|Set-Content "$PSScriptRoot/native-runtime/before-prd-preferences-$(Get-Date -Format yyyyMMdd-HHmmss).json" -Encoding UTF8
try{
    $changed=$settings|ConvertTo-Json -Depth 30|ConvertFrom-Json
    $changed.preferences.theme='dark';$changed.preferences.editorSize=17
    Invoke-LocalApi '/user/settings' 'Put' $changed $h|Out-Null
    $read=Invoke-LocalApi '/user/settings' 'Get' $null $h
    if($read.preferences.theme -ne 'dark' -or $read.preferences.editorSize -ne 17){throw 'Preferences not persisted'}
    $stats=Invoke-LocalApi '/user/workspace/statistics?personal=false' 'Get' $null $h
    if($stats.documents -lt 5 -or @($stats.trend).Count -ne 14){throw 'Invalid team statistics'}
    $tasks=Invoke-LocalApi '/task/list' 'Get' $null $h
    if($stats.completed -ne @($tasks|Where-Object status -eq 2).Count){throw 'Completed task statistics disagree with task list'}
    $zipPath="$PSScriptRoot/native-runtime/prd-batch8-workspace.zip"
    Invoke-WebRequest "$base/user/workspace/export?personal=false&kinds=documents,files,messages,tasks" -Headers $h -OutFile $zipPath
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip=[IO.Compression.ZipFile]::OpenRead($zipPath)
    try{if(-not $zip.GetEntry('manifest.json') -or -not $zip.GetEntry('documents/2110671241521270784.json') -or -not $zip.GetEntry('tasks.json') -or -not $zip.GetEntry('messages.json')){throw 'ZIP missing real content'};$entryCount=$zip.Entries.Count;$fileCount=@($zip.Entries|Where-Object {$_.FullName.StartsWith('files/') -and $_.FullName -ne 'files/index.json'}).Count}finally{$zip.Dispose()}
    $docId='2110678860000530432'
    Invoke-LocalApi '/notification/subscriptions' 'Post' @{kind='document';resourceId=$docId;frequency='realtime'} $h|Out-Null
    $newSub=@(Invoke-LocalApi '/notification/subscriptions' 'Get' $null $h|Where-Object resourceId -eq $docId)
    if(-not $newSub.Count){throw 'Subscription not persisted'}
    $foreign=Invoke-RestMethod "$base/notification/subscriptions" -Method Post -Headers $h -ContentType 'application/json' -Body (@{kind='document';resourceId='not-existing';frequency='realtime'}|ConvertTo-Json)
    if($foreign.code -ne 403){throw 'Subscription permission not enforced'}
    @{preferencesPersisted=$true;statistics=$stats;zipEntries=$entryCount;rawFiles=$fileCount;zipBytes=(Get-Item $zipPath).Length;subscriptionId=$newSub[0].id;unauthorizedSubscriptionCode=$foreign.code}|ConvertTo-Json -Depth 20|Set-Content "$PSScriptRoot/native-runtime/prd-batch8-preferences.json" -Encoding UTF8
}finally{Invoke-LocalApi '/user/settings' 'Put' $settings $h|Out-Null}
Write-Host 'Preferences, real statistics, complete ZIP entries and subscription ACL verified.'
