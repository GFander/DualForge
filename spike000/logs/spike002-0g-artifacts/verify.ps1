$ErrorActionPreference='Stop'
$evRoot=$PSScriptRoot
$utf8=[Text.UTF8Encoding]::new($false)
$cCode=Get-Content -LiteralPath (Join-Path $evRoot 'spike002-0g-control-dump-code.stdout.txt') -Raw
$tCode=Get-Content -LiteralPath (Join-Path $evRoot 'spike002-0g-treatment-dump-code.stdout.txt') -Raw
$cLines=@(($cCode -split "`n"|Where-Object{$_ -match '^\s*\d+:'})|ForEach-Object{[regex]::Replace($_.Trim(),'#\d+','#')})
$tLines=@(($tCode -split "`n"|Where-Object{$_ -match '^\s*\d+:'})|ForEach-Object{[regex]::Replace($_.Trim(),'#\d+','#')})
if($cLines.Count -ne $tLines.Count){throw 'STOP instruction count changed'}
$diffs=@(for($i=0;$i -lt $cLines.Count;$i++){if($cLines[$i] -cne $tLines[$i]){[pscustomobject]@{Old=$cLines[$i];New=$tLines[$i]}}})
if($diffs.Count -ne 2 -or $diffs[0].Old -notmatch '^43: getstatic' -or $diffs[1].Old -notmatch '^47: invokeinterface'){throw 'STOP unexpected instruction changes'}
$expected=$cCode.Replace('net/minecraftforge/common/MinecraftForge.EVENT_BUS:Lnet/minecraftforge/eventbus/api/IEventBus;','net/neoforged/neoforge/common/NeoForge.EVENT_BUS:Lnet/neoforged/bus/api/IEventBus;').Replace('net/minecraftforge/eventbus/api/IEventBus.register:','net/neoforged/bus/api/IEventBus.register:')
if([regex]::Replace($expected,'#\d+','#') -cne [regex]::Replace($tCode,'#\d+','#')){throw 'STOP dump semantic difference beyond operands'}
$cRoot='D:\DualForge\spike000\logs\spike002-0g-control'
$tRoot='D:\DualForge\spike000\logs\spike002-0g-treatment'
$c=Get-Content -LiteralPath ($cRoot+'\spike002-0g-control.stdout.log') -Raw
$t=Get-Content -LiteralPath ($tRoot+'\spike002-0g-treatment.stdout.log') -Raw
foreach($signal in @('REWRITE hits=1','API original-pattern hits=1','FORGE PROBE CONSTRUCTOR REACHED','BUS = net.neoforged.bus.EventBus@','delegate -> builtin javafml for neoforge')){if(-not $c.Contains($signal) -or -not $t.Contains($signal)){throw ('STOP missing '+$signal)}}
if(-not $c.Contains('API_REWRITE enabled=false hits=0') -or -not $t.Contains('API_REWRITE enabled=true hits=1')){throw 'STOP switches'}
if(-not $c.Contains('java.lang.NoClassDefFoundError: net/minecraftforge/common/MinecraftForge')){throw 'STOP missing C P1'}
if($t.Contains('NoClassDefFoundError: net/minecraftforge/common/MinecraftForge')){throw 'STOP T P1 remains'}
if(-not $t.Contains('has no @SubscribeEvent methods, but register was called anyway.') -or -not $t.Contains('net.neoforged.bus.EventBus.register(EventBus.java:135)') -or -not $t.Contains('ForgeProbe.<init>(ForgeProbe.java:14)')){throw 'STOP S1/entry evidence'}
if($c.Contains('[FORGECOMPAT-XFORM] ERROR') -or $t.Contains('[FORGECOMPAT-XFORM] ERROR')){throw 'STOP transformer errors'}
foreach($side in @(@('control',$cRoot),@('treatment',$tRoot))){$meta=Get-Content -LiteralPath ($side[1]+'\meta.json') -Raw|ConvertFrom-Json;if($meta.status -ne 'EXITED_EARLY' -or $meta.exitCode -ne 0 -or $meta.doneLine){throw 'Unexpected termination'};if((Get-Item -LiteralPath ($side[1]+'\spike002-0g-'+$side[0]+'.stderr.log')).Length -ne 0){throw 'Unexpected stderr'}}
$out=Join-Path $evRoot 'semantic-and-run-verification.json'
if(Test-Path -LiteralPath $out){throw 'Already verified'}
[IO.File]::WriteAllText($out,([pscustomobject]@{InstructionCount=$cLines.Count;ChangedInstructionLines=$diffs;OnlyThreeOperandsChanged=$true;ControlP1=$true;TreatmentRegisterEntered=$true;TreatmentS1=$true;Result='PASS-002-0g-1'}|ConvertTo-Json -Depth 5),$utf8)
Write-Output 'PASS-002-0g-1 gates verified: only coupled three operands changed; register entry and S1 confirmed.'
