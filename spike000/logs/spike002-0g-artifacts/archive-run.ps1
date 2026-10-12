param([Parameter(Mandatory=$true)][ValidateSet('control','treatment')][string]$Kind)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'build-evidence.ps1')
$label='spike002-0g-'+$Kind
$run='D:\DualForge\spike000\logs\'+$label
$dump=Join-Path $run 'ForgeProbe.rewritten.class'
if(Test-Path -LiteralPath $dump){throw 'STOP archived dump exists'}
if(-not(Test-Path -LiteralPath (Join-Path $run 'meta.json'))){throw 'STOP run incomplete'}
Copy-Item -LiteralPath 'D:\DualForge\spike000\logs\spike002-0b-2a-v1.1\ForgeProbe.rewritten.class' -Destination $dump
$mods=@(Get-ChildItem -LiteralPath 'D:\DualForge\spike000\neoforge-server\mods' -File)
$modsHashes=Hashes @($mods.FullName)
if($mods.Count -ne 2 -or @($modsHashes|Where-Object SHA256 -EQ 'D61224F3014F0ADFF85D10EEDEA414763C231367F08EDB99CE99DD2C8935F1C9').Count -ne 1 -or @($modsHashes|Where-Object SHA256 -EQ 'F0D51E46533A135A9ECA0FA859805E04E9FBB22E8759B1A29524319E37B4C739').Count -ne 1){throw 'STOP runtime mods'}
Save-New ($label+'-mods.json') ($modsHashes|ConvertTo-Json -Depth 4)
Save-New ($label+'-dump.json') ((Hashes @($dump))|ConvertTo-Json -Depth 4)
if($Kind -eq 'control' -and (Get-FileHash -LiteralPath $dump).Hash -ne '55D81709DC8C41A8FB25189BEF87D41F5F1920C721C02B2027F9EEE764924FE8'){throw 'STOP control dump changed'}
Native ($label+'-dump-code') ($jdkBin+'\javap.exe') @('-p','-c','-s','-constants',$dump)
Native ($label+'-dump-verbose') ($jdkBin+'\javap.exe') @('-p','-v',$dump)
Write-Output ($label+' dump sealed; runtime mods fixed.')
