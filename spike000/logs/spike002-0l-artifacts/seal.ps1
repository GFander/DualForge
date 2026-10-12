$ErrorActionPreference='Stop'
$evRoot=$PSScriptRoot
$utf8=[Text.UTF8Encoding]::new($false)
function Save-NewText([string]$p,[string]$text){if(Test-Path -LiteralPath $p){throw ('Already exists '+$p)};[IO.File]::WriteAllText($p,$text,$utf8)}
function Normalize-Verbose([string]$p){
 $text=Get-Content -LiteralPath $p -Raw
 (($text -split '\r?\n'|Where-Object{$_ -notmatch '^Classfile |^  Last modified |^  SHA-256 checksum '}) -join "`n").Replace('net/minecraftforge/eventbus/api/SubscribeEvent','net/neoforged/bus/api/SubscribeEvent').Replace('net.minecraftforge.eventbus.api.SubscribeEvent','net.neoforged.bus.api.SubscribeEvent').Replace('net/minecraftforge/eventbus/api/Event','net/neoforged/bus/api/Event').Replace('net.minecraftforge.eventbus.api.Event','net.neoforged.bus.api.Event')
}
foreach($cls in @('ForgeProbe','ProbeEvent')){
 $c=Join-Path $evRoot ('spike002-0l-control-'+$cls+'-verbose.stdout.txt')
 $t=Join-Path $evRoot ('spike002-0l-treatment-'+$cls+'-verbose.stdout.txt')
 if((Normalize-Verbose $c) -cne (Normalize-Verbose $t)){throw ('STOP extra verbose class differences '+$cls)}
}
$jvmC=Get-Content -LiteralPath (Join-Path $evRoot 'spike002-0l-control.actual-jvm-args.txt') -Raw
$jvmT=Get-Content -LiteralPath (Join-Path $evRoot 'spike002-0l-treatment.actual-jvm-args.txt') -Raw
if($jvmC.Replace('forgecompat.0l.subscriberRewrite=false','forgecompat.0l.subscriberRewrite=true') -cne $jvmT){throw 'STOP extra JVM differences'}
$tRoot='D:\DualForge\spike000\logs\spike002-0l-treatment'
foreach($suffix in @('.stdout.log','.log','.debug.log')){if((Get-Content -LiteralPath ($tRoot+'\spike002-0l-treatment'+$suffix) -Raw) -match '\b(ERROR|FATAL)\b'){throw 'STOP T ERROR/FATAL'}}
if((Get-Item -LiteralPath ($tRoot+'\spike002-0l-treatment.stderr.log')).Length -ne 0){throw 'STOP stderr nonempty'}
if(Get-ChildItem -LiteralPath ($tRoot+'\crash-reports') -File -Recurse -ErrorAction SilentlyContinue){throw 'STOP T crash'}
if(Get-Process java -ErrorAction SilentlyContinue){throw 'STOP Java remains'}
Save-NewText (Join-Path $evRoot 'complete-review-gates.txt') "Complete C/T verbose equivalence after ONLY the allowed namespace substitutions and file metadata exclusion; actual JVM args sole toggle; T ERROR/FATAL absent, stderr zero, no crash and Java exited.`r`n"
foreach($round in @('0k','0l','0m')){
 $dir='D:\DualForge\spike000\logs\spike002-'+$round+'-artifacts'
 $items=@(Get-ChildItem -LiteralPath $dir -File -Recurse|Sort-Object FullName|ForEach-Object{[pscustomobject]@{Path=$_.FullName;SHA256=(Get-FileHash -LiteralPath $_.FullName).Hash;Bytes=$_.Length}})
 Save-NewText ($dir+'\evidence-index.json') ($items|ConvertTo-Json -Depth 4)
 Write-Output ($round+': '+$items.Count+' evidence files sealed')
}
$runtime=@(Get-ChildItem -LiteralPath 'D:\DualForge\spike000\logs\spike002-0l-control',$tRoot -File -Recurse|Sort-Object FullName|ForEach-Object{[pscustomobject]@{Path=$_.FullName;SHA256=(Get-FileHash -LiteralPath $_.FullName).Hash;Bytes=$_.Length}})
Save-NewText (Join-Path $evRoot 'runtime-index.json') ($runtime|ConvertTo-Json -Depth 4)
foreach($index in @((Join-Path $evRoot 'runtime-index.json'),'D:\DualForge\spike000\logs\spike002-0k-artifacts\evidence-index.json',(Join-Path $evRoot 'evidence-index.json'),'D:\DualForge\spike000\logs\spike002-0m-artifacts\evidence-index.json')){
 foreach($i in (Get-Content -LiteralPath $index -Raw|ConvertFrom-Json)){if((Get-FileHash -LiteralPath $i.Path).Hash -ne $i.SHA256){throw 'STOP sealed hash mismatch'}}
}
Write-Output ($runtime.Count.ToString()+' runtime files sealed and all four indexes reverified.')
