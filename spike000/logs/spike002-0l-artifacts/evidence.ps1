param([ValidateSet('prepare','build','archive-control','archive-treatment','final')][string]$Phase)
$ErrorActionPreference='Stop'
$requestedPhase=$Phase
. 'D:\DualForge\spike000\logs\spike002-0j-artifacts\build-evidence.ps1'
$Phase=$requestedPhase
$evRoot=$PSScriptRoot
$compatBase='D:\DualForge\spike001a\compat-002-0j'
$probeBase='D:\DualForge\spike000\probe-002-0e'
$compatWork='D:\DualForge\spike001a\compat-002-0l'
$probeWork='D:\DualForge\spike000\probe-002-0l'
$harness='D:\DualForge\spike000\run-server.ps1'
$spec='D:\DualForge\Dev Manuals\Spike_002-0l.md'
$forgeBus='D:\DualForge\spike000\forge-server\libraries\net\minecraftforge\eventbus\6.2.27\eventbus-6.2.27.jar'
$forgeUniversal='D:\DualForge\spike000\forge-server\libraries\net\minecraftforge\forge\1.21.1-52.1.0\forge-1.21.1-52.1.0-universal.jar'
$forgeLanguage='D:\DualForge\spike000\forge-server\libraries\net\minecraftforge\javafmllanguage\1.21.1-52.1.0\javafmllanguage-1.21.1-52.1.0.jar'
$refs=@(
 @(($compatBase+'\build\forgecompat-002-0j.jar'),'D724FB489A3A1F1180A5FBA09DFF2FECD01FBD6F8AE33EE727851AB211FA44AB'),
 @(($probeBase+'\build\forgeprobe.jar'),'D61224F3014F0ADFF85D10EEDEA414763C231367F08EDB99CE99DD2C8935F1C9'),
 @($forgeBus,'D2A7BCECB80B33F90854A003DC295960263D78435D9D6016C427D711510CB999'),
 @($forgeUniversal,'88EF6625122CFDB9D7B3176BD61B8C0692A191BAD0EDD77FFDDC6A765EE4979C'),
 @($forgeLanguage,'D27E1F3149EA08FC0A80779FE24FA8585692C5042A34BAD153E55662486ACC15')
)
Check-Refs
function Content-Index([string]$jar,[string]$name){
 $zip=[IO.Compression.ZipFile]::OpenRead($jar)
 try { foreach($entry in $zip.Entries){if($entry.Name){$s=$entry.Open();$sha=[Security.Cryptography.SHA256]::Create();try{$hash=[Convert]::ToHexString($sha.ComputeHash($s))}finally{$s.Dispose();$sha.Dispose()};[pscustomobject]@{Jar=$name;Entry=$entry.FullName;SHA256=$hash;Bytes=$entry.Length}}} }finally{$zip.Dispose()}
}
if($Phase -eq 'prepare'){
 foreach($pair in @(@($compatBase,$compatWork),@($probeBase,$probeWork))){
  if(Test-Path -LiteralPath $pair[1]){throw 'STOP existing workspace'}
  $current=Source-Index ($pair[0]+'\src');$arch=Source-Index ($pair[0]+'\archive\src-after')
  foreach($s in $current){if(@($arch|Where-Object{$_.Path -eq $s.Path -and $_.SHA256 -eq $s.SHA256}).Count -ne 1){throw 'STOP baseline source drift'}}
  New-Item -ItemType Directory -Path ($pair[1]+'\archive')|Out-Null
  Copy-Item -LiteralPath ($pair[0]+'\src') -Destination ($pair[1]+'\src') -Recurse
  Copy-Item -LiteralPath ($pair[0]+'\src') -Destination ($pair[1]+'\archive\src-before') -Recurse
 }
 if((Get-FileHash -LiteralPath $harness).Hash -ne '33049C802F8AF4E79665A541DAB9A9DFAAE0CFCFEE8CDA8702A8309DD5DBF64A'){throw 'STOP harness drift'}
 Copy-Item -LiteralPath $harness -Destination (Join-Path $evRoot 'harness-before.ps1')
 $cp=Get-Content -LiteralPath 'D:\DualForge\spike000\logs\spike002-0j-artifacts\classpath.txt' -Raw
 Save-New 'compat-classpath.txt' $cp
 Save-New 'probe-classpath.txt' (@($forgeLanguage,$forgeBus,$forgeUniversal)-join ';')
 $originalSource=@(Get-ChildItem -LiteralPath ($compatBase+'\src'),($probeBase+'\src') -File -Recurse|Select-Object -ExpandProperty FullName)
 Save-New 'inputs-before.json' ((Hashes (@($refs|ForEach-Object{$_[0]})+@($cp.Trim() -split ';')+@($spec)+$originalSource))|ConvertTo-Json -Depth 4)
 Native 'java-version' ($jdkBin+'\java.exe') @('-version')
 Write-Output 'Prepared two isolated copies and before archives; original inputs untouched.'
}
if($Phase -eq 'build'){
 foreach($pair in @(@('compat',$compatWork,'forgecompat-002-0l.jar'),@('probe',$probeWork,'forgeprobe.jar'))){
  $root=$pair[1];Copy-Item -LiteralPath ($root+'\src') -Destination ($root+'\archive\src-after') -Recurse
  Save-New ($pair[0]+'-source-before.json') ((Source-Index ($root+'\archive\src-before'))|ConvertTo-Json -Depth 4)
  Save-New ($pair[0]+'-source-after.json') ((Source-Index ($root+'\src'))|ConvertTo-Json -Depth 4)
  Native ($pair[0]+'-source-diff') 'git' @('-c','core.autocrlf=false','diff','--no-index','--',($root+'\archive\src-before'),($root+'\archive\src-after')) @(1)
  New-Item -ItemType Directory -Path ($root+'\build\classes'),($root+'\build\jar-root')|Out-Null
  $cp=Get-Content -LiteralPath (Join-Path $evRoot ($pair[0]+'-classpath.txt')) -Raw
  $sources=@(Get-ChildItem -LiteralPath ($root+'\src\main\java') -File -Recurse -Filter '*.java'|Sort-Object FullName|Select-Object -ExpandProperty FullName)
  Native ($pair[0]+'-javac') ($jdkBin+'\javac.exe') (@('--release','21','-encoding','UTF-8','-classpath',$cp,'-d',($root+'\build\classes'))+$sources)
  Copy-Item -Path ($root+'\build\classes\*') -Destination ($root+'\build\jar-root') -Recurse
  Copy-Item -Path ($root+'\src\main\resources\*') -Destination ($root+'\build\jar-root') -Recurse
  $jar=$root+'\build\'+$pair[2]
  Native ($pair[0]+'-jar') ($jdkBin+'\jar.exe') @('--create','--file',$jar,'--manifest',($root+'\src\main\resources\META-INF\MANIFEST.MF'),'-C',($root+'\build\jar-root'),'.')
  Native ($pair[0]+'-entries') ($jdkBin+'\jar.exe') @('tf',$jar)
  Copy-Item -LiteralPath $jar -Destination ($root+'\archive\'+$pair[2])
 }
 $content=@(Content-Index $refs[0][0] 'compat-before')+@(Content-Index ($compatWork+'\build\forgecompat-002-0l.jar') 'compat-after')+@(Content-Index $refs[1][0] 'probe-before')+@(Content-Index ($probeWork+'\build\forgeprobe.jar') 'probe-after')
 Save-New 'content-comparison.json' ($content|ConvertTo-Json -Depth 4)
 $old=@($content|Where-Object Jar -EQ 'compat-before');$new=@($content|Where-Object Jar -EQ 'compat-after')
 if($old.Count -ne 17 -or $new.Count -ne 17){throw 'STOP compat contents count'}
 $changes=@(foreach($item in $new){if(@($old|Where-Object{$_.Entry -eq $item.Entry -and $_.SHA256 -eq $item.SHA256}).Count -ne 1){$item.Entry}})
 if($changes.Count -ne 2 -or $changes -notcontains 'com/example/forgecompat/ForgeProbeConstructorTransformer.class' -or $changes -notcontains 'com/example/forgecompat/ForgeEmptyRegistration.class'){throw 'STOP compat unauthorized content changes'}
 foreach($entry in @('META-INF/MANIFEST.MF','META-INF/mods.toml')){$r=@($content|Where-Object{$_.Jar -like 'probe-*' -and $_.Entry -eq $entry});if($r.Count -ne 2 -or $r[0].SHA256 -ne $r[1].SHA256){throw 'STOP probe resource changed'}}
 foreach($pair in @(@('probe-old',$refs[1][0]),@('probe-new',($probeWork+'\build\forgeprobe.jar')))){
  Native ($pair[0]+'-code') ($jdkBin+'\javap.exe') @('-p','-c','-s','-constants','-classpath',$pair[1],'com.example.forgeprobe.ForgeProbe')
  Native ($pair[0]+'-verbose') ($jdkBin+'\javap.exe') @('-p','-v','-classpath',$pair[1],'com.example.forgeprobe.ForgeProbe')
 }
 Native 'event-new-code' ($jdkBin+'\javap.exe') @('-p','-c','-s','-constants','-classpath',($probeWork+'\build\forgeprobe.jar'),'com.example.forgeprobe.ProbeEvent')
 Native 'event-new-verbose' ($jdkBin+'\javap.exe') @('-p','-v','-classpath',($probeWork+'\build\forgeprobe.jar'),'com.example.forgeprobe.ProbeEvent')
 foreach($cls in @('ForgeProbeConstructorTransformer','ForgeEmptyRegistration')){Native ($cls+'-code') ($jdkBin+'\javap.exe') @('-p','-c','-s','-constants','-classpath',($compatWork+'\build\forgecompat-002-0l.jar'),('com.example.forgecompat.'+$cls))}
 $oldH=Get-Content -LiteralPath (Join-Path $evRoot 'harness-before.ps1') -Raw
 if($oldH.Replace("'spike002-0j-treatment')","'spike002-0j-treatment', 'spike002-0l-control', 'spike002-0l-treatment')") -cne (Get-Content -LiteralPath $harness -Raw)){throw 'STOP harness changed beyond appended labels'}
 Native 'harness-diff' 'git' @('-c','core.autocrlf=false','diff','--no-index','--',(Join-Path $evRoot 'harness-before.ps1'),$harness) @(1)
 Save-New 'outputs.json' ((Hashes @(($compatWork+'\build\forgecompat-002-0l.jar'),($probeWork+'\build\forgeprobe.jar'),$harness))|ConvertTo-Json -Depth 4)
 Write-Output 'Built both fixed inputs; 15 compat entries identical, two allowed changed. Verify semantic bytecode before C.'
}
if($Phase -like 'archive-*'){
 $label='spike002-0l-'+$Phase.Substring(8);$run='D:\DualForge\spike000\logs\'+$label
 if(-not(Test-Path -LiteralPath ($run+'\meta.json'))){throw 'STOP missing meta'}
 foreach($cls in @('ForgeProbe','ProbeEvent')){
  $dump='D:\DualForge\spike000\logs\spike002-0l-current\'+$cls+'.class'
  if(-not(Test-Path -LiteralPath $dump)){throw ('STOP absent reached dump '+$cls)}
  $dest=$run+'\'+$cls+'.class';if(Test-Path -LiteralPath $dest){throw 'STOP archive exists'}
  Copy-Item -LiteralPath $dump -Destination $dest
  Native ($label+'-'+$cls+'-code') ($jdkBin+'\javap.exe') @('-p','-c','-s','-constants',$dest)
  Native ($label+'-'+$cls+'-verbose') ($jdkBin+'\javap.exe') @('-p','-v',$dest)
 }
 Save-New ($label+'-runtime-mods.json') ((Hashes @(Get-ChildItem -LiteralPath 'D:\DualForge\spike000\neoforge-server\mods' -File|Select-Object -ExpandProperty FullName))|ConvertTo-Json -Depth 4)
 Save-New ($label+'-dumps.json') ((Hashes @(($run+'\ForgeProbe.class'),($run+'\ProbeEvent.class')))|ConvertTo-Json -Depth 4)
}
if($Phase -eq 'final'){
 foreach($json in @('inputs-before.json','outputs.json')){foreach($i in (Get-Content -LiteralPath (Join-Path $evRoot $json) -Raw|ConvertFrom-Json)){if((Get-FileHash -LiteralPath $i.Path).Hash -ne $i.SHA256){throw ('STOP final drift '+$i.Path)}}}
 foreach($pair in @(@('compat',$compatWork),@('probe',$probeWork))){$now=Source-Index ($pair[1]+'\src');$then=Get-Content -LiteralPath (Join-Path $evRoot ($pair[0]+'-source-after.json')) -Raw|ConvertFrom-Json;if($now.Count -ne $then.Count){throw 'STOP source count changed'};foreach($s in $now){if(@($then|Where-Object{$_.Path -eq $s.Path -and $_.SHA256 -eq $s.SHA256}).Count -ne 1){throw 'STOP source changed after build'}}}
 Save-New 'final-check.txt' 'Frozen inputs, original sources, output jars, harness and new sources unchanged.'
 Write-Output 'Final immutability gates passed.'
}
