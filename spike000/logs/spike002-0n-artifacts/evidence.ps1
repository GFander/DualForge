param([ValidateSet('prepare','build','finish-build','control','treatment','final')][string]$Phase)
$ErrorActionPreference='Stop'
$requestedPhase=$Phase
. 'D:\DualForge\spike000\logs\spike002-0j-artifacts\build-evidence.ps1'
$Phase=$requestedPhase
$evRoot=$PSScriptRoot
$work='D:\DualForge\spike000\subscriber-002-0n'
$server='D:\DualForge\spike000\neoforge-server'
$lib=$server+'\libraries'
$event='D:\DualForge\spike000\logs\spike002-0l-treatment\ProbeEvent.class'
$probe='D:\DualForge\spike000\probe-002-0l\build\forgeprobe.jar'
$forgeBus='D:\DualForge\spike000\forge-server\libraries\net\minecraftforge\eventbus\6.2.27\eventbus-6.2.27.jar'
$neoBus=$lib+'\net\neoforged\bus\8.0.5\bus-8.0.5.jar'
$asm=$lib+'\org\ow2\asm\asm\9.10.1\asm-9.10.1.jar'
$asmTree=$lib+'\org\ow2\asm\asm-tree\9.10.1\asm-tree-9.10.1.jar'
$runtimeDeps=@($neoBus,$asm,($lib+'\org\apache\logging\log4j\log4j-api\2.22.1\log4j-api-2.22.1.jar'),($lib+'\org\apache\logging\log4j\log4j-core\2.22.1\log4j-core-2.22.1.jar'),($lib+'\net\jodah\typetools\0.6.3\typetools-0.6.3.jar'))
$refs=@(@($event,'15089EC00BC3EA3867E4746DF6FDEB612CBE6C899D52F725BDD8313A7FEAD6AD'),@($probe,'A774D2F737B17F089046014AB55A53767DBAD008CD44DCCE52C727B87E559AE8'),@($forgeBus,'D2A7BCECB80B33F90854A003DC295960263D78435D9D6016C427D711510CB999'),@($neoBus,'705E24F37752809499A5CD0C7DD31EAABF3D933C5D3B2060168E94E9EF62593A'),@('D:\DualForge\Dev Manuals\Spike_002-0n.md','DAFE0D5B3AAA708342941E26555DF739888F7583C39C5A1C63874F58BB60276C'))
Check-Refs
function Check-Frozen {
 foreach($i in (Get-Content -LiteralPath (Join-Path $evRoot 'inputs-before.json') -Raw|ConvertFrom-Json)){if((Get-FileHash -LiteralPath $i.Path).Hash -ne $i.SHA256){throw ('STOP frozen input changed '+$i.Path)}}
}
if($Phase -eq 'prepare'){
 if(Test-Path -LiteralPath ($work+'\build')){throw 'STOP existing build'}
 $sourcePaths=@(Get-ChildItem -LiteralPath ($work+'\src') -File -Recurse|Select-Object -ExpandProperty FullName)
 if($sourcePaths.Count -ne 3){throw 'STOP source count'}
 $serverSnapshot=@(($server+'\mods'),($server+'\logs'))
 $hostFiles=@(Get-ChildItem -LiteralPath $serverSnapshot -File -Recurse|Select-Object -ExpandProperty FullName)
 Save-New 'inputs-before.json' ((Hashes (@($refs|ForEach-Object{$_[0]})+$runtimeDeps+@($asmTree,'D:\DualForge\spike000\run-server.ps1')+$sourcePaths+$hostFiles))|ConvertTo-Json -Depth 4)
 Save-New 'sources.json' ((Source-Index ($work+'\src'))|ConvertTo-Json -Depth 4)
 New-Item -ItemType Directory -Path ($work+'\archive'),($work+'\build\original'),($work+'\build\rewriter'),($work+'\build\runtime\com\example\forgeprobe')|Out-Null
 Copy-Item -LiteralPath ($work+'\src') -Destination ($work+'\archive\src-frozen') -Recurse
 Copy-Item -LiteralPath $event -Destination ($work+'\build\runtime\com\example\forgeprobe\ProbeEvent.class')
 Copy-Item -LiteralPath $event -Destination ($work+'\archive\ProbeEvent-translated-0l.class')
 Native 'java-version' ($jdkBin+'\java.exe') @('-version')
 Write-Output 'Prepared fixed fixture sources, exact copied event and dependency/host snapshots.'
}
if($Phase -eq 'build'){
 Check-Frozen
 Native 'listener-javac' ($jdkBin+'\javac.exe') @('--release','21','-encoding','UTF-8','-classpath',($probe+';'+$forgeBus),'-d',($work+'\build\original'),($work+'\src\com\example\forgeprobe\SubscriberFixture.java'))
 Native 'rewriter-javac' ($jdkBin+'\javac.exe') @('--release','21','-encoding','UTF-8','-classpath',($asm+';'+$asmTree),'-d',($work+'\build\rewriter'),($work+'\src\ProfileRewriter.java'))
 $original=$work+'\build\original\com\example\forgeprobe\SubscriberFixture.class'
 $translated=$work+'\build\runtime\com\example\forgeprobe\SubscriberFixture.class'
 Native 'rewrite' ($jdkBin+'\java.exe') @('-classpath',($work+'\build\rewriter;'+$asm+';'+$asmTree),'ProfileRewriter',$original,$translated)
 foreach($pair in @(@('listener-original',$original),@('listener-translated',$translated))){
  Native ($pair[0]+'-code') ($jdkBin+'\javap.exe') @('-p','-c','-s','-constants',$pair[1])
  Native ($pair[0]+'-verbose') ($jdkBin+'\javap.exe') @('-p','-v',$pair[1])
 }
}
if($Phase -in @('build','finish-build')){
 Check-Frozen
 $original=$work+'\build\original\com\example\forgeprobe\SubscriberFixture.class'
 $translated=$work+'\build\runtime\com\example\forgeprobe\SubscriberFixture.class'
 function Code([string]$name){@([regex]::Matches((Get-Content -LiteralPath (Join-Path $evRoot $name) -Raw),'(?m)^\s+\d+:\s+.*$')|ForEach-Object{$_.Value.Trim() -replace '#\d+','#CP' -replace '\s+//',' //'}) -join "`n"}
 if((Code 'listener-original-code.stdout.txt') -cne (Code 'listener-translated-code.stdout.txt')){throw 'STOP rewrite changed instructions'}
 $before=Get-Content -LiteralPath (Join-Path $evRoot 'listener-original-verbose.stdout.txt') -Raw
 $after=Get-Content -LiteralPath (Join-Path $evRoot 'listener-translated-verbose.stdout.txt') -Raw
 # ClassWriter reindexes the constant pool. Full originals retained; explicit annotation and field/method/instruction gates below.
 if($before -notmatch 'net.minecraftforge.eventbus.api.SubscribeEvent' -or $after -notmatch 'net.neoforged.bus.api.SubscribeEvent' -or $after -match 'net[./]minecraftforge'){throw 'STOP annotation references not cleanly translated'}
 Save-New 'translation-gates.txt' 'All opcode operands equal after CP-id normalization; default method annotation namespace rewritten once. Full code/verbose originals retained.'
 Copy-Item -LiteralPath $original -Destination ($work+'\archive\SubscriberFixture.original.class')
 Copy-Item -LiteralPath $translated -Destination ($work+'\archive\SubscriberFixture.translated.class')
 $cp=(@($work+'\build\runtime')+$runtimeDeps) -join ';'
 Native 'driver-javac' ($jdkBin+'\javac.exe') @('--release','21','-encoding','UTF-8','-classpath',$cp,'-d',($work+'\build\runtime'),($work+'\src\com\example\forgeprobe\FixtureDriver.java'))
 $jar=$work+'\build\subscriber-002-0n.jar'
 Native 'jar' ($jdkBin+'\jar.exe') @('--create','--file',$jar,'-C',($work+'\build\runtime'),'.')
 Native 'jar-entries' ($jdkBin+'\jar.exe') @('tf',$jar)
 foreach($cls in @('FixtureDriver','SubscriberFixture','ProbeEvent')){Native ($cls+'-runtime-code') ($jdkBin+'\javap.exe') @('-p','-c','-s','-constants','-classpath',$jar,('com.example.forgeprobe.'+$cls))}
 Save-New 'runtime-classpath.txt' ((@($jar)+$runtimeDeps) -join ';')
 Save-New 'outputs.json' ((Hashes @($jar,$original,$translated,($work+'\build\runtime\com\example\forgeprobe\ProbeEvent.class')))|ConvertTo-Json -Depth 4)
 Copy-Item -LiteralPath $jar -Destination ($work+'\archive\subscriber-002-0n.jar')
 Write-Output 'Fixture built; original/translated class and driver bytecode ready for inspection.'
}
if($Phase -in @('control','treatment')){
 Check-Frozen
 foreach($i in (Get-Content -LiteralPath (Join-Path $evRoot 'outputs.json') -Raw|ConvertFrom-Json)){if((Get-FileHash -LiteralPath $i.Path).Hash -ne $i.SHA256){throw 'STOP output drift'}}
 if($Phase -eq 'treatment'){
  $c=Get-Content -LiteralPath (Join-Path $evRoot 'control.stdout.txt') -Raw
  if($c -notmatch 'register returned hits=0' -or $c -notmatch 'post skipped' -or $c -notmatch 'FINAL hits=0 expected=0' -or $c -match 'HANDLER REACHED'){throw 'STOP failed control gate'}
 }
 $flag=if($Phase -eq 'control'){'false'}else{'true'}
 $cp=Get-Content -LiteralPath (Join-Path $evRoot 'runtime-classpath.txt') -Raw
 if($cp -match 'minecraftforge|compat-002|probe-002'){throw 'STOP forbidden runtime dependency'}
 Native $Phase ($jdkBin+'\java.exe') @(('-Dforgecompat.0n.post='+$flag),'-classpath',$cp,'com.example.forgeprobe.FixtureDriver')
 Write-Output ($Phase+' completed; inspect full raw output before next phase.')
}
if($Phase -eq 'final'){
 Check-Frozen
 foreach($i in (Get-Content -LiteralPath (Join-Path $evRoot 'outputs.json') -Raw|ConvertFrom-Json)){if((Get-FileHash -LiteralPath $i.Path).Hash -ne $i.SHA256){throw 'STOP final output drift'}}
 $c=Get-Content -LiteralPath (Join-Path $evRoot 'control.stdout.txt') -Raw
 $t=Get-Content -LiteralPath (Join-Path $evRoot 'treatment.stdout.txt') -Raw
 if($c -match 'HANDLER REACHED' -or ([regex]::Matches($t,'HANDLER REACHED hits=1')).Count -ne 1 -or $t -notmatch 'register returned hits=0' -or $t -notmatch 'post returned sameEvent=true' -or $t -notmatch 'FINAL hits=1 expected=1'){throw 'STOP C/T result mismatch'}
 if((Get-FileHash -LiteralPath ($work+'\build\runtime\com\example\forgeprobe\ProbeEvent.class')).Hash -ne $refs[0][1]){throw 'STOP copied event altered'}
 Save-New 'final-check.txt' 'C0/T1 exact callback, sameEvent=true, all original/fixed/host/source/dependency/output hashes unchanged; no server/harness modifications.'
 Write-Output 'All frozen gates pass; isolated callback delivery observed.'
}
