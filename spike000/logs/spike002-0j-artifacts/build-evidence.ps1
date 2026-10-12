param([ValidateSet('prepare','build','final')][string]$Phase)
$ErrorActionPreference='Stop'
$evRoot=$PSScriptRoot
$work='D:\DualForge\spike001a\compat-002-0j'
$base='D:\DualForge\spike001a\compat-002-0g'
$jdkBin='C:\Users\Albert\.jdks\ms-21.0.10\bin'
$utf8=[Text.UTF8Encoding]::new($false)
$oldJar=$base+'\build\forgecompat-002-0g.jar'
$probe='D:\DualForge\spike000\probe-002-0e\build\forgeprobe.jar'
$refs=@(
 @($oldJar,'F0D51E46533A135A9ECA0FA859805E04E9FBB22E8759B1A29524319E37B4C739'),
 @($probe,'D61224F3014F0ADFF85D10EEDEA414763C231367F08EDB99CE99DD2C8935F1C9'),
 @('D:\DualForge\Dev Manuals\Spike_002-0j.md','F83ACB24E9D2F71CC5DC67F6D845C5EED24C9F02BC3E436A0C04397E97D79A4E'),
 @('D:\DualForge\spike000\logs\spike002-0g-treatment\ForgeProbe.rewritten.class','A9ABBB9440DD49A16BB5749D46744F01D96FAF2796B189920BD8788682705EF1')
)
function Save-New([string]$name,[string]$text) {
 $p=Join-Path $evRoot $name
 if(Test-Path -LiteralPath $p){throw "Evidence exists $p"}
 [IO.File]::WriteAllText($p,$text,$utf8)
}
function Hashes([string[]]$paths) {
 @(foreach($p in $paths){[pscustomobject]@{Path=$p;SHA256=(Get-FileHash -LiteralPath $p -Algorithm SHA256).Hash;Bytes=(Get-Item -LiteralPath $p).Length}})
}
function Source-Index([string]$root){
 @(Get-ChildItem -LiteralPath $root -File -Recurse | Sort-Object FullName | ForEach-Object{
  [pscustomobject]@{Path=$_.FullName.Substring($root.Length+1);SHA256=(Get-FileHash -LiteralPath $_.FullName).Hash;Bytes=$_.Length}
 })
}
function Check-Refs {
 foreach($r in $refs){if((Get-FileHash -LiteralPath $r[0]).Hash -ne $r[1]){throw "STOP reference $($r[0])"}}
}
function Native([string]$name,[string]$exe,[string[]]$nativeArgs,[int[]]$accepted=@(0)){
 $cmd='& '+"'"+$exe.Replace("'","''")+"' "+(($nativeArgs|ForEach-Object{"'"+$_.Replace("'","''")+"'"})-join ' ')
 Save-New ($name+'.command.txt') ($cmd+"`r`n")
 $psi=[Diagnostics.ProcessStartInfo]::new();$psi.FileName=$exe;$psi.WorkingDirectory='D:\DualForge'
 $psi.UseShellExecute=$false;$psi.CreateNoWindow=$true;$psi.RedirectStandardOutput=$true;$psi.RedirectStandardError=$true
 foreach($arg in $nativeArgs){$psi.ArgumentList.Add($arg)}
 $p=[Diagnostics.Process]::new();$p.StartInfo=$psi;$null=$p.Start()
 $out=[IO.MemoryStream]::new();$err=[IO.MemoryStream]::new()
 $ot=$p.StandardOutput.BaseStream.CopyToAsync($out);$et=$p.StandardError.BaseStream.CopyToAsync($err)
 $p.WaitForExit();$null=$ot.GetAwaiter().GetResult();$null=$et.GetAwaiter().GetResult()
 foreach($pair in @(@('stdout',$out),@('stderr',$err))){$dest=Join-Path $evRoot ($name+'.'+$pair[0]+'.txt');if(Test-Path -LiteralPath $dest){throw 'Existing native output'};[IO.File]::WriteAllBytes($dest,$pair[1].ToArray());$pair[1].Dispose()}
 $code=$p.ExitCode;$p.Dispose();Save-New ($name+'.exit.txt') ($code.ToString()+"`r`n")
 if($code -notin $accepted){throw "STOP tool $name exit $code"}
}
Check-Refs
if($Phase -eq 'prepare'){
 if(Test-Path -LiteralPath $work){throw 'STOP work exists'}
 $src=Source-Index ($base+'\src')
 $arch=@(Source-Index ($base+'\archive\src-after') | Where-Object Path -NE '_sha256.txt')
 if($src.Count -ne $arch.Count){throw 'STOP baseline source count'}
 foreach($s in $src){$a=@($arch|Where-Object Path -EQ $s.Path);if($a.Count -ne 1 -or $a[0].SHA256 -ne $s.SHA256){throw ('STOP source drift '+$s.Path)}}
 Save-New 'source-baseline.json' (($src|ConvertTo-Json -Depth 4)+"`r`n")
 New-Item -ItemType Directory -Path ($work+'\archive') | Out-Null
 Copy-Item -LiteralPath ($base+'\src') -Destination ($work+'\src') -Recurse
 Copy-Item -LiteralPath ($base+'\src') -Destination ($work+'\archive\src-before') -Recurse
 $harness='D:\DualForge\spike000\run-server.ps1'
 if((Get-FileHash -LiteralPath $harness).Hash -ne 'FBA0003CEDF843293008868948049F8E9B287A35F52C809D15DA236FF8CA9C8C'){throw 'STOP harness drift'}
 Copy-Item -LiteralPath $harness -Destination (Join-Path $evRoot 'harness-before.ps1')
 Copy-Item -LiteralPath 'D:\DualForge\spike000\logs\spike002-0b-2a-v1.1\ForgeProbe.rewritten.class' -Destination (Join-Path $evRoot 'historical-ForgeProbe.rewritten.class')
 if((Get-FileHash -LiteralPath (Join-Path $evRoot 'historical-ForgeProbe.rewritten.class')).Hash -ne $refs[3][1]){throw 'STOP shared dump drift'}
 $cp=(Get-Content -LiteralPath 'D:\DualForge\spike001a\compat\build\cp.txt' -Raw).Trim()
 $cp+=';D:\DualForge\spike000\neoforge-server\libraries\org\ow2\asm\asm\9.10.1\asm-9.10.1.jar;D:\DualForge\spike000\neoforge-server\libraries\org\ow2\asm\asm-tree\9.10.1\asm-tree-9.10.1.jar'
 $cp+=';D:\DualForge\spike000\neoforge-server\libraries\net\neoforged\bus\8.0.5\bus-8.0.5.jar'
 Save-New 'classpath.txt' $cp
 Save-New 'inputs-before.json' ((Hashes (@($refs|ForEach-Object{$_[0]})+@($harness)+@($cp -split ';')))|ConvertTo-Json -Depth 4)
 Native 'javac-version' ($jdkBin+'\javac.exe') @('-version')
 Native 'jar-version' ($jdkBin+'\jar.exe') @('--version')
 Write-Output ('Prepared baseline '+$src.Count+' source/resources; no original source modified.')
}
if($Phase -eq 'build'){
 Copy-Item -LiteralPath ($work+'\src') -Destination ($work+'\archive\src-after') -Recurse
 $src=Source-Index ($work+'\src');Save-New 'source-after.json' ($src|ConvertTo-Json -Depth 4)
 $before=Get-Content -LiteralPath (Join-Path $evRoot 'source-baseline.json') -Raw|ConvertFrom-Json
 $changes=@($src|Where-Object{$s=$_;@($before|Where-Object {$_.Path -eq $s.Path -and $_.SHA256 -eq $s.SHA256}).Count -eq 0})
 if($changes.Count -ne 2 -or @($changes|Where-Object Path -EQ 'main\java\com\example\forgecompat\ForgeProbeConstructorTransformer.java').Count -ne 1 -or @($changes|Where-Object Path -EQ 'main\java\com\example\forgecompat\ForgeEmptyRegistration.java').Count -ne 1){throw 'STOP wrong source changes'}
 Native 'source-diff' 'git' @('-c','core.autocrlf=false','diff','--no-index','--',($work+'\archive\src-before'),($work+'\archive\src-after')) @(1)
 Native 'harness-diff-step' 'git' @('-c','core.autocrlf=false','diff','--no-index','--',(Join-Path $evRoot 'harness-before.ps1'),'D:\DualForge\spike000\run-server.ps1') @(1)
 Native 'harness-diff-global' 'git' @('-c','core.autocrlf=false','diff','--no-index','--','D:\DualForge\spike000\logs\harness-baseline\run-server.ps1','D:\DualForge\spike000\run-server.ps1') @(1)
 $hOld=Get-Content -LiteralPath (Join-Path $evRoot 'harness-before.ps1') -Raw
 $hNew=Get-Content -LiteralPath 'D:\DualForge\spike000\run-server.ps1' -Raw
 $expected=$hOld.Replace("'spike002-0g-treatment')","'spike002-0g-treatment', 'spike002-0j-control', 'spike002-0j-treatment')")
 if($expected -cne $hNew){throw 'STOP unauthorized harness content'}
 New-Item -ItemType Directory -Path ($work+'\build\classes'),($work+'\build\jar-root')|Out-Null
 $cp=Get-Content -LiteralPath (Join-Path $evRoot 'classpath.txt') -Raw
 $sources=@(Get-ChildItem -LiteralPath ($work+'\src\main\java') -Recurse -Filter '*.java' -File|Sort-Object FullName|Select-Object -ExpandProperty FullName)
 Native 'javac' ($jdkBin+'\javac.exe') (@('--release','21','-encoding','UTF-8','-classpath',$cp,'-d',($work+'\build\classes'))+$sources)
 Copy-Item -Path ($work+'\build\classes\*') -Destination ($work+'\build\jar-root') -Recurse
 Copy-Item -Path ($work+'\src\main\resources\*') -Destination ($work+'\build\jar-root') -Recurse
 $newJar=$work+'\build\forgecompat-002-0j.jar'
 Native 'jar' ($jdkBin+'\jar.exe') @('--create','--file',$newJar,'--manifest',($work+'\src\main\resources\META-INF\MANIFEST.MF'),'-C',($work+'\build\jar-root'),'.')
 Native 'jar-entries' ($jdkBin+'\jar.exe') @('tf',$newJar)
 foreach($pair in @(@('old',$oldJar),@('new',$newJar))){Native ($pair[0]+'-transformer') ($jdkBin+'\javap.exe') @('-p','-c','-s','-constants','-classpath',$pair[1],'com.example.forgecompat.ForgeProbeConstructorTransformer')}
 $content=@(foreach($pair in @(@('old',$oldJar),@('new',$newJar))){$zip=[IO.Compression.ZipFile]::OpenRead($pair[1]);try{foreach($e in $zip.Entries){if($e.Name){$stream=$e.Open();$sha=[Security.Cryptography.SHA256]::Create();try{$hash=[Convert]::ToHexString($sha.ComputeHash($stream))}finally{$stream.Dispose();$sha.Dispose()};[pscustomobject]@{Jar=$pair[0];Entry=$e.FullName;SHA256=$hash;Bytes=$e.Length}}}}finally{$zip.Dispose()}})
 Save-New 'content-comparison.json' ($content|ConvertTo-Json -Depth 4)
 $old=@($content|Where-Object Jar -EQ old);$new=@($content|Where-Object Jar -EQ new)
 if($old.Count -ne 16 -or $new.Count -ne 17){throw 'STOP contents count'}
 $diffs=@(foreach($e in $new){$other=@($old|Where-Object Entry -EQ $e.Entry);if($other.Count -ne 1 -or $other[0].SHA256 -ne $e.SHA256){$e.Entry}})
 if($diffs.Count -ne 2 -or $diffs -notcontains 'com/example/forgecompat/ForgeProbeConstructorTransformer.class' -or $diffs -notcontains 'com/example/forgecompat/ForgeEmptyRegistration.class'){throw 'STOP content difference'}
 Save-New 'outputs.json' ((Hashes @($newJar,'D:\DualForge\spike000\run-server.ps1'))|ConvertTo-Json -Depth 4)
 Copy-Item -LiteralPath $newJar -Destination ($work+'\archive\forgecompat-002-0j.jar')
 Write-Output 'Build passed: 15 content entries unchanged, transformer changed and one helper added.'
}
if($Phase -eq 'final'){
 $initial=Get-Content -LiteralPath (Join-Path $evRoot 'inputs-before.json') -Raw|ConvertFrom-Json
 foreach($i in $initial){if($i.Path -eq 'D:\DualForge\spike000\run-server.ps1'){continue};if((Get-FileHash -LiteralPath $i.Path).Hash -ne $i.SHA256){throw "STOP final drift $($i.Path)"}}
 $outputs=Get-Content -LiteralPath (Join-Path $evRoot 'outputs.json') -Raw|ConvertFrom-Json
 foreach($o in $outputs){if((Get-FileHash -LiteralPath $o.Path).Hash -ne $o.SHA256){throw 'STOP output/harness drift'}}
 $sourceNow=Source-Index ($work+'\src');$sourceThen=Get-Content -LiteralPath (Join-Path $evRoot 'source-after.json') -Raw|ConvertFrom-Json
 foreach($s in $sourceNow){if(@($sourceThen|Where-Object{$_.Path -eq $s.Path -and $_.SHA256 -eq $s.SHA256}).Count -ne 1){throw 'STOP final source drift'}}
 Save-New 'inputs-after.json' ((Hashes @($initial|ForEach-Object{$_.Path}))|ConvertTo-Json -Depth 4)
 Write-Output 'Final frozen inputs and fixed new outputs verified.'
}
