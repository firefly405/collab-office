$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$seedAst=[System.Management.Automation.Language.Parser]::ParseFile("$PSScriptRoot/seed-local-demo.ps1",[ref]$null,[ref]$null)
$guard=$seedAst.Find({param($node) $node -is [System.Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq 'Assert-LocalDemoDatabase'},$true)
Invoke-Expression $guard.Extent.Text
Assert-LocalDemoDatabase
. "$PSScriptRoot/seed-local-demo.ps1"
function Read-RoleSnapshot {
    $fixture=Get-Content "$PSScriptRoot/native-runtime/role-demo.json" -Raw|ConvertFrom-Json
    $actor=@{Authorization=$headers.Authorization;'X-Space-Id'=$fixture.spaceId}
    return [ordered]@{
        spaceId=$fixture.spaceId
        projectId=$fixture.projectId
        sessionId=$fixture.sessionId
        accounts=@($fixture.accounts.userId|Sort-Object)
        projects=@(Invoke-LocalApi "/task/project/list?spaceId=$($fixture.spaceId)" 'Get' $null $actor|ForEach-Object projectId|Sort-Object)
        tasks=@(Invoke-LocalApi '/task/list' 'Get' $null $actor|ForEach-Object taskId|Sort-Object)
        reports=@(Invoke-LocalApi '/task/reports' 'Get' $null $actor|ForEach-Object id|Sort-Object)
        documents=@((Invoke-LocalApi "/document/list?spaceId=$($fixture.spaceId)&pageSize=100" 'Get' $null $actor).records|ForEach-Object docId|Sort-Object)
        conversations=@(Invoke-LocalApi '/chat/conversations' 'Get' $null $actor|ForEach-Object sessionId|Sort-Object)
        messages=@(Invoke-LocalApi "/chat/messages/$($fixture.sessionId)" 'Get' $null $actor|ForEach-Object msgId|Sort-Object)
    }
}
$before=Read-RoleSnapshot
foreach($kind in @('accounts','projects','tasks','reports','documents','conversations','messages')) {
    if(-not $before[$kind].Count -or @($before[$kind]|Where-Object {"$_" -eq ''}).Count){throw "Missing $kind IDs; seed cannot be verified"}
}
& "$PSScriptRoot/seed-role-demo.ps1"
if($LASTEXITCODE -ne 0){throw 'Role seed failed'}
$after=Read-RoleSnapshot
if(($before|ConvertTo-Json -Depth 10 -Compress) -cne ($after|ConvertTo-Json -Depth 10 -Compress)){throw 'Repeated role seed changed resource IDs'}
@{before=$before;after=$after;idempotent=$true;verifiedAt=(Get-Date).ToString('o')}|ConvertTo-Json -Depth 20|Set-Content "$PSScriptRoot/native-runtime/role-seed-verification.json" -Encoding UTF8
Write-Host 'Role seed idempotency verified: account, team, project, task, report, document, conversation and message IDs unchanged.'
