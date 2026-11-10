$ErrorActionPreference = 'Stop'
$target = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot 'native-runtime'))
$toolsRoot = [IO.Path]::GetFullPath($PSScriptRoot)
if (-not $target.StartsWith($toolsRoot + [IO.Path]::DirectorySeparatorChar)) { throw 'Unexpected runtime path; refusing cleanup.' }
if ((Split-Path $target -Leaf) -ne 'native-runtime') { throw 'Unexpected runtime directory name; refusing cleanup.' }
if (Test-Path -LiteralPath $target) { Remove-Item -LiteralPath $target -Recurse -Force }
[pscustomobject]@{RuntimeExists=(Test-Path -LiteralPath $target);Path=$target}
