$ErrorActionPreference='Stop'
$evRoot=$PSScriptRoot
$c=Get-Content -LiteralPath ($evRoot+'\spike002-0j-control-dump-code.stdout.txt') -Raw
$t=Get-Content -LiteralPath ($evRoot+'\spike002-0j-treatment-dump-code.stdout.txt') -Raw
$a=@(($c -split "`n"|Where-Object{$_ -match '^\s*\d+:'})|ForEach-Object{[regex]::Replace($_.Trim(),'#\d+','#')})
$b=@(($t -split "`n"|Where-Object{$_ -match '^\s*\d+:'})|ForEach-Object{[regex]::Replace($_.Trim(),'#\d+','#')})
if($a.Count -ne 20 -or $b.Count -ne 20){throw 'STOP instruction count'}
$diffs=@(for($i=0;$i -lt $a.Count;$i++){if($a[$i] -cne $b[$i]){[pscustomobject]@{Old=$a[$i];New=$b[$i]}}})
if($diffs.Count -ne 2 -or $diffs[0].Old -notmatch '^47: invokeinterface' -or $diffs[0].New -notmatch '^47: invokestatic.*ForgeEmptyRegistration.register:\(Lnet/neoforged/bus/api/IEventBus;Ljava/lang/Object;\)V' -or $diffs[1].Old -ne '52: return' -or $diffs[1].New -ne '50: return'){throw 'STOP call/layout difference'}
$cRoot='D:\DualForge\spike000\logs\spike002-0j-control';$tRoot='D:\DualForge\spike000\logs\spike002-0j-treatment'
$cLog=Get-Content -LiteralPath ($cRoot+'\spike002-0j-control.stdout.log') -Raw
$tLog=Get-Content -LiteralPath ($tRoot+'\spike002-0j-treatment.stdout.log') -Raw
foreach($s in @('REWRITE hits=1','API_REWRITE enabled=true hits=1','FORGE PROBE CONSTRUCTOR REACHED','BUS = net.neoforged.bus.EventBus@','delegate -> builtin javafml for neoforge')){if(-not $cLog.Contains($s) -or -not $tLog.Contains($s)){throw 'STOP common signals'}}
if(-not $cLog.Contains('EMPTY_ROUTE enabled=false hits=0') -or -not $cLog.Contains('has no @SubscribeEvent methods') -or -not $cLog.Contains('net.neoforged.bus.EventBus.register(EventBus.java:135)')){throw 'STOP C S1'}
if(-not $tLog.Contains('EMPTY_ROUTE enabled=true hits=1') -or -not $tLog.Contains('[FORGECOMPAT-EMPTY] legacy-empty-noop target=com.example.forgeprobe.ForgeProbe declaredMethods=0 customPublic=0')){throw 'STOP T profile'}
if($tLog.Contains('[FORGECOMPAT-EMPTY] delegate to NeoForge register') -or $tLog.Contains('has no @SubscribeEvent methods') -or $tLog.Contains('[FORGECOMPAT-XFORM] ERROR')){throw 'STOP T invalid path'}
foreach($file in @('spike002-0j-treatment.stdout.log','spike002-0j-treatment.log','spike002-0j-treatment.debug.log')){if((Get-Content -LiteralPath (Join-Path $tRoot $file) -Raw) -match '/(?:ERROR|FATAL)\]'){throw 'STOP unexpected T exception'}}
$cm=Get-Content -LiteralPath ($cRoot+'\meta.json') -Raw|ConvertFrom-Json
$tm=Get-Content -LiteralPath ($tRoot+'\meta.json') -Raw|ConvertFrom-Json
if($cm.status -ne 'EXITED_EARLY' -or $cm.doneLine -or $tm.status -ne 'DONE_STOPPED_GRACEFULLY' -or $tm.exitCode -ne 0 -or -not $tm.doneLine){throw 'STOP termination'}
if((Get-Item -LiteralPath ($tRoot+'\spike002-0j-treatment.stderr.log')).Length -ne 0 -or (Test-Path -LiteralPath ($tRoot+'\crash-reports'))){throw 'STOP T stderr/crash'}
$utf8=[Text.UTF8Encoding]::new($false)
$p=Join-Path $evRoot 'semantic-and-run-verification.json';if(Test-Path -LiteralPath $p){throw 'Already verified'}
[IO.File]::WriteAllText($p,([pscustomobject]@{InstructionCount=20;Differences=$diffs;ControlS1=$true;TreatmentEmptyProfile=$true;TreatmentDone=$true;Result='VALID-002-0j-1'}|ConvertTo-Json -Depth 4),$utf8)
Write-Output 'VALID-002-0j-1: control S1; treatment guarded empty return and Done; only call plus two-byte layout changed.'
