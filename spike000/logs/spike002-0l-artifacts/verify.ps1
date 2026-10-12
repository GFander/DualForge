param([ValidateSet('pre','post')][string]$Phase)
$ErrorActionPreference='Stop'
$evRoot=$PSScriptRoot
$utf8=[Text.UTF8Encoding]::new($false)
function Instructions([string]$name){@([regex]::Matches((Get-Content -LiteralPath (Join-Path $evRoot $name) -Raw),'(?m)^\s+\d+:\s+.*$')|ForEach-Object{$_.Value.Trim() -replace '#\d+','#CP'})}
if($Phase -eq 'pre'){
 $old=Instructions 'probe-old-code.stdout.txt';$new=Instructions 'probe-new-code.stdout.txt'
 if($new.Count -ne ($old.Count+1) -or ($old -join "`n") -cne ($new[0..($old.Count-1)] -join "`n") -or $new[-1] -ne '0: return'){throw 'STOP original constructor semantics changed'}
 $event=Instructions 'event-new-code.stdout.txt'
 if($event.Count -ne 3 -or $event[0] -ne '0: aload_0' -or $event[1] -notmatch '1: invokespecial.*net/minecraftforge/eventbus/api/Event' -or $event[2] -ne '4: return'){throw 'STOP unexpected event instructions'}
 [IO.File]::WriteAllText((Join-Path $evRoot 'semantic-before-run.txt'),"Old constructor all $($old.Count) instructions unchanged; only added onProbe RETURN and three-instruction concrete event constructor.`r`n",$utf8)
 Write-Output 'Pre-run semantic gates passed.'
}
if($Phase -eq 'post'){
 $c=Instructions 'spike002-0l-control-ForgeProbe-code.stdout.txt';$t=Instructions 'spike002-0l-treatment-ForgeProbe-code.stdout.txt'
 if(($c -join "`n") -cne ($t -join "`n")){throw 'STOP subscriber instructions differ C/T'}
 $ce=Instructions 'spike002-0l-control-ProbeEvent-code.stdout.txt';$te=Instructions 'spike002-0l-treatment-ProbeEvent-code.stdout.txt'
 if(($ce -join "`n").Replace('net/minecraftforge/eventbus/api/Event','net/neoforged/bus/api/Event') -cne ($te -join "`n")){throw 'STOP unexpected event instruction changes'}
 $base='D:\DualForge\spike000\logs'
 $cs=Get-Content -LiteralPath ($base+'\spike002-0l-control\spike002-0l-control.stdout.log') -Raw
 $ts=Get-Content -LiteralPath ($base+'\spike002-0l-treatment\spike002-0l-treatment.stdout.log') -Raw
 if($cs -match 'register returned|legacy-empty-noop' -or $ts -notmatch 'SUBSCRIBER enabled=true annotationHits=1' -or $ts -notmatch 'EVENT_PROFILE enabled=true superHits=1 constructorHits=1' -or $ts -notmatch 'register returned target=com.example.forgeprobe.ForgeProbe declaredMethods=1' -or $ts -match 'legacy-empty-noop|FORGECOMPAT-XFORM\] ERROR|dump FAIL'){throw 'STOP runtime profile mismatch'}
 $meta=Get-Content -LiteralPath ($base+'\spike002-0l-treatment\meta.json') -Raw|ConvertFrom-Json
 if($meta.status -ne 'DONE_STOPPED_GRACEFULLY' -or $meta.exitCode -ne 0){throw 'STOP treatment not complete'}
 [IO.File]::WriteAllText((Join-Path $evRoot 'semantic-after-run.txt'),"C/T ForgeProbe instructions identical; only method annotation namespace differs. ProbeEvent parent class and super call owner are the two event reference changes. Runtime T real register returned and Done gracefully; no no-op.`r`n",$utf8)
 Write-Output 'Post-run semantic/runtime gates passed.'
}
