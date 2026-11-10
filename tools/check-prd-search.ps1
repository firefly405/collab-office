$ErrorActionPreference='Stop'
. "$PSScriptRoot/seed-local-demo.ps1"
$h=@{Authorization=$headers.Authorization;'X-Space-Id'='15'}
$all=Invoke-LocalApi "/search/global?keyword=$([uri]::EscapeDataString('验收'))" 'Get' $null $h
if(@($all).Count -lt 5){throw 'Missing known search results'}
$today=Get-Date -Format 'yyyy-MM-dd'
$filtered=Invoke-LocalApi "/search/global?keyword=$([uri]::EscapeDataString('验收'))&scope=document&from=$today&to=$today&creatorId=1" 'Get' $null $h
if(-not @($filtered).Count -or @($filtered|Where-Object {$_.creatorId -ne 1 -or $_.type -ne 'document' -or -not $_.createdAt.StartsWith($today)}).Count){throw 'Creator/date/type filters failed'}
$empty=Invoke-LocalApi "/search/global?keyword=$([uri]::EscapeDataString('验收'))&from=2000-01-01&to=2000-01-02" 'Get' $null $h
if(@($empty).Count){throw 'Date filter returned future records'}
$messages=Invoke-LocalApi "/search/global?keyword=$([uri]::EscapeDataString('验收'))&scope=message" 'Get' $null $h
if(-not @($messages).Count -or @($messages|Where-Object type -ne 'message').Count){throw 'Message search missing'}
$suggest=Invoke-LocalApi "/search/suggest?keyword=$([uri]::EscapeDataString('验收'))" 'Get' $null $h
if(-not @($suggest).Count -or @($suggest).Count -gt 5){throw 'Search suggestions invalid'}
$sheets=Invoke-LocalApi "/search/global?keyword=$([uri]::EscapeDataString('编辑器优化'))&scope=sheet" 'Get' $null $h
if(-not @($sheets|Where-Object id -eq '2110671241521270784').Count){throw 'Cell content search failed'}
$invalid=Invoke-RestMethod "$base/search/global?keyword=x&from=2026-02-02&to=2026-02-01" -Headers $h
if($invalid.code -ne 400){throw 'Invalid date range accepted'}
$foreignHeaders=@{Authorization=$headers.Authorization;'X-Space-Id'='13'}
$foreign=Invoke-LocalApi "/search/global?keyword=$([uri]::EscapeDataString('协作产品规划'))" 'Get' $null $foreignHeaders
if(@($foreign|Where-Object id -eq '2110678860000530432').Count){throw 'Cross-team result leaked'}
@{total=@($all).Count;filtered=@($filtered).Count;messages=@($messages).Count;messageExample=$messages[0];suggestions=$suggest;sheetContentFound=$true;crossTeamBlocked=$true;invalidCode=$invalid.code}|ConvertTo-Json -Depth 10|Set-Content "$PSScriptRoot/native-runtime/prd-batch7-search.json" -Encoding UTF8
Write-Host 'Search date/creator/type, messages, suggestions, cell content and team isolation verified.'
