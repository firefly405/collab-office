# Requires PowerShell 7 and a running local stack. Retains the preview fixture.
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/seed-local-demo.ps1"
$fixture = "$PSScriptRoot/native-runtime/office-check.html"
$uploaded = Invoke-RestMethod "$base/file/upload" -Method Post -Headers $headers -Form @{ file = Get-Item $fixture }
if ($uploaded.code -ne 200) { throw 'Preview fixture upload failed.' }
$previewPath = "/file/preview/$($uploaded.data.fileId)"
$preview = Invoke-LocalApi $previewPath 'Post' $null $headers
$deadline = (Get-Date).AddSeconds(60)
while ($preview.status -eq 'pending' -and (Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 1
    $preview = Invoke-LocalApi $previewPath 'Post' $null $headers
}
if ($preview.status -ne 'ready') { throw 'PDF preview did not become ready.' }
$pdf = Invoke-WebRequest $preview.url -UseBasicParsing
$bytes = $pdf.Content
if ($bytes -is [string]) { $bytes = [Text.Encoding]::UTF8.GetBytes($bytes) }
if ($bytes.Length -lt 1000 -or [Text.Encoding]::ASCII.GetString($bytes, 0, 4) -ne '%PDF') {
    throw 'Preview is missing or is a placeholder PDF.'
}
Write-Host "PASS real LibreOffice PDF preview ($($bytes.Length) bytes)"
