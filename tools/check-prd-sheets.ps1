$ErrorActionPreference='Stop'
. "$PSScriptRoot/seed-local-demo.ps1"
$fixture=Get-Content "$PSScriptRoot/native-runtime/prd-batch2-content.json" -Raw | ConvertFrom-Json
$demoHeaders=@{Authorization=$headers.Authorization;'X-Space-Id'="$($fixture.spaceId)"}
foreach($path in @('/document/list?spaceId=13&pageSize=1','/meeting/list','/task/list')){Invoke-LocalApi $path 'Get' $null $headers | Out-Null}
Assert-LocalDemoDatabase
@{documents=Invoke-LocalApi "/document/list?spaceId=$($fixture.spaceId)&pageSize=100" 'Get' $null $demoHeaders} | ConvertTo-Json -Depth 20 | Set-Content "$PSScriptRoot/native-runtime/before-prd-sheets-$(Get-Date -Format 'yyyyMMdd-HHmmss').json" -Encoding UTF8
$doc=Invoke-LocalApi '/document/create' 'Post' @{title='PRD 验收 · 团队预算';type=2;spaceId=$fixture.spaceId} $demoHeaders
$detail=Invoke-LocalApi "/sheet/manage/$doc" 'Get' $null $demoHeaders
$child=$detail.sheets[0].id
function Write-Cells($Id,$Items){Invoke-LocalApi '/sheet/cell/batch' 'Put' @{sheetId=$Id;cells=$Items} $demoHeaders | Out-Null}
$items=@(@{sheetId=$child;cellKey='A1';content='部门'},@{sheetId=$child;cellKey='B1';content='事项'},@{sheetId=$child;cellKey='C1';content='预算'},@{sheetId=$child;cellKey='D1';content='含协作成本'},@{sheetId=$child;cellKey='F1';content='季度预算';formatJson='{"bl":1,"bg":"#e8f8f2","mc":{"r":0,"c":5,"rs":1,"cs":2}}'},@{sheetId=$child;cellKey='G1';content='';formatJson='{"mc":{"r":0,"c":5}}'})
$names=@('需求评审','编辑器优化','回归验证','权限联调','知识库整理');$values=@(300,800,200,500,150);$departments=@('产品','研发','测试','研发','产品')
for($i=0;$i -lt 5;$i++){$r=$i+2;$items+=@(@{sheetId=$child;cellKey="A$r";content=$departments[$i]},@{sheetId=$child;cellKey="B$r";content=$names[$i]},@{sheetId=$child;cellKey="C$r";content="$($values[$i])";formatJson='{"type":"number","min":0,"threshold":400,"comment":"预算按月复核"}'},@{sheetId=$child;cellKey="D$r";content="=C$r*2"})}
$items+=@(@{sheetId=$child;cellKey='C8';content='=SUM(C2:C6)'},@{sheetId=$child;cellKey='D8';content='=DATEDIF(DATE(2024,1,1),DATE(2026,1,1),"Y")'},@{sheetId=$child;cellKey='E8';content='=1/0'})
Write-Cells $child $items
$range=Invoke-LocalApi "/sheet/cell/range?sheetId=$child&range=A1:G10" 'Get' $null $demoHeaders
if($range.C8 -ne '1950' -or $range.D8 -ne '2' -or $range.E8 -ne '#DIV/0!'){throw 'Formula outputs incorrect'}
Invoke-LocalApi "/sheet/manage/$doc/sheet?name=$([uri]::EscapeDataString('汇总'))" 'Post' $null $demoHeaders | Out-Null
$detail=Invoke-LocalApi "/sheet/manage/$doc" 'Get' $null $demoHeaders;$summary=$detail.sheets[1].id
Write-Cells $summary @(@{sheetId=$summary;cellKey='A1';content='=SUM(Sheet1!C2:C6)'})
Invoke-LocalApi "/sheet/manage/$doc/sheet/0/rename?name=$([uri]::EscapeDataString('预算'))" 'Put' $null $demoHeaders | Out-Null
$cross=Invoke-LocalApi "/sheet/cell/range?sheetId=$summary&range=A1" 'Get' $null $demoHeaders
if($cross.A1 -ne '1950'){throw 'Cross-sheet rename/calculation failed'}
$stale=Invoke-RestMethod "$base/sheet/cell/batch" -Method Put -Headers $demoHeaders -ContentType 'application/json' -Body (@{sheetId=$child;cells=@(@{sheetId=$child;cellKey='C2';content='999';checkConflict=$true;expectedContent='stale'})}|ConvertTo-Json -Depth 8)
if($stale.code -ne 409){throw 'Stale overwrite was not rejected'}
$invalid=Invoke-RestMethod "$base/sheet/cell/batch" -Method Put -Headers $demoHeaders -ContentType 'application/json' -Body (@{sheetId=$child;cells=@(@{sheetId=$child;cellKey='C2';content='bad'})}|ConvertTo-Json -Depth 8)
if($invalid.code -ne 400){throw 'Invalid typed value was not rejected'}
Invoke-LocalApi "/sheet/$doc/sort" 'Post' @{sheetId=$child;range='A2:D6';rules=@(@{column='C';descending=$true},@{column='B';descending=$false})} $demoHeaders | Out-Null
$sorted=Invoke-LocalApi "/sheet/cell/range?sheetId=$child&range=A1:G10" 'Get' $null $demoHeaders
if($sorted.C2 -ne '800' -or $sorted.B2 -ne '编辑器优化' -or $sorted.D2 -ne '1600'){throw 'Sort did not move rows/formulas correctly'}
Invoke-LocalApi "/sheet/$doc/settings" 'Put' @{sheetId=$child;freezeRow=$true;freezeColumn=$true} $demoHeaders | Out-Null
$config=@{sheetId=$child;title='部门预算';labels='B2:B6';values='C2:C6'}|ConvertTo-Json -Compress
$chart=Invoke-LocalApi "/sheet/$doc/charts" 'Post' @{chartType='bar';configJson=$config} $demoHeaders
$foreign=Invoke-RestMethod "$base/sheet/$doc/charts/$($chart.id)" -Method Put -Headers $demoHeaders -ContentType 'application/json' -Body (@{chartType='bar';configJson=(@{sheetId=999999;labels='A1:A2';values='B1:B2'}|ConvertTo-Json -Compress)}|ConvertTo-Json -Compress)
if($foreign.code -eq 200){throw 'Foreign sheet chart config accepted'}
$exports=@{}
foreach($type in @('xlsx','csv','pdf')){$export=Invoke-LocalApi "/sheet/$doc/export?format=$type&sheetId=$child&columns=A,B,C,D" 'Get' $null $demoHeaders;$path="$PSScriptRoot/native-runtime/prd-batch5-export.$type";Invoke-WebRequest $export.downloadUrl -OutFile $path;$exports[$type]=(Get-Item $path).Length}
if(-not (Get-Content "$PSScriptRoot/native-runtime/prd-batch5-export.csv" -Raw -Encoding UTF8).Contains('编辑器优化')){throw 'CSV content missing'}
$imports=@()
foreach($path in @("$PSScriptRoot/fixtures/prd-sheet.csv","$PSScriptRoot/native-runtime/prd-batch5-import.xlsx","$PSScriptRoot/native-runtime/prd-batch5-import.xls")) {
    $result=Invoke-RestMethod "$base/sheet/import" -Method Post -Headers $demoHeaders -Form @{file=Get-Item $path;spaceId="$($fixture.spaceId)";startRow='1'}
    if($result.code -ne 200){throw "Import failed: $($result.message)"};$imports+= $result.data
    $imported=Invoke-LocalApi "/sheet/manage/$($result.data.sheetId)" 'Get' $null $demoHeaders
    $values=Invoke-LocalApi "/sheet/cell/range?sheetId=$($imported.sheets[0].id)&range=A1:C4" 'Get' $null $demoHeaders
    if($path.EndsWith('.csv')){if($values.B2 -ne '需求,评审'){throw 'CSV quoted delimiter lost'}}elseif($values.C1 -ne '24'){throw 'Excel imported formula not evaluated'}
}
@{docId=$doc;spaceId=$fixture.spaceId;sheetId=$child;summaryId=$summary;sorted=$sorted;crossSheet=$cross;staleRejected=$stale.code;invalidRejected=$invalid.code;chartId=$chart.id;exports=$exports;imports=$imports}|ConvertTo-Json -Depth 15|Set-Content "$PSScriptRoot/native-runtime/prd-batch5-sheets.json" -Encoding UTF8
Write-Host 'Sheet formulas, conflicts, sorting, chart isolation, CSV/XLS/XLSX imports and exports verified.'
