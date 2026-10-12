param([Parameter(Mandatory=$true)][ValidateSet('control','treatment')][string]$Kind)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'build-evidence.ps1')
$label='spike002-0j-'+$Kind
$run='D:\DualForge\spike000\logs\'+$label
$dump=Join-Path $run 'ForgeProbe.rewritten.class'
if(Test-Path -LiteralPath $dump){throw 'STOP archived dump exists'}
if(-not(Test-Path -LiteralPath (Join-Path $run 'meta.json'))){throw 'STOP run incomplete'}
Copy-Item -LiteralPath 'D:\DualForge\spike000\logs\spike002-0b-2a-v1.1\ForgeProbe.rewritten.class' -Destination $dump
$mods=@(Get-ChildItem -LiteralPath 'D:\DualForge\spike000\neoforge-server\mods' -File)
$modsHashes=Hashes @($mods.FullName)
if($mods.Count -ne 2 -or @($modsHashes|Where-Object SHA256 -EQ 'D61224F3014F0ADFF85D10EEDEA414763C231367F08EDB99CE99DD2C8935F1C9').Count -ne 1 -or @($modsHashes|Where-Object SHA256 -EQ 'D724FB489A3A1F1180A5FBA09DFF2FECD01FBD6F8AE33EE727851AB211FA44AB').Count -ne 1){throw 'STOP runtime mods'}
Save-New ($label+'-mods.json') ($modsHashes|ConvertTo-Json -Depth 4)
Save-New ($label+'-dump.json') ((Hashes @($dump))|ConvertTo-Json -Depth 4)
if($Kind -eq 'control' -and (Get-FileHash -LiteralPath $dump).Hash -ne 'A9ABBB9440DD49A16BB5749D46744F01D96FAF2796B189920BD8788682705EF1'){throw 'STOP control dump changed'}
Native ($label+'-dump-code') ($jdkBin+'\javap.exe') @('-p','-c','-s','-constants',$dump)
Native ($label+'-dump-verbose') ($jdkBin+'\javap.exe') @('-p','-v',$dump)
Write-Output ($label+' dump sealed; runtime mods fixed.')
