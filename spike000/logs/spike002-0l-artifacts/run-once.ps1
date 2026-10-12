param([Parameter(Mandatory=$true)][ValidateSet('control','treatment')][string]$Kind)
$ErrorActionPreference='Stop'
$evRoot=$PSScriptRoot
$base='D:\DualForge\spike000'
$label='spike002-0l-'+$Kind
$run=Join-Path ($base+'\logs') $label
$harness=$base+'\run-server.ps1'
$probe=$base+'\probe-002-0l\build\forgeprobe.jar'
$compat='D:\DualForge\spike001a\compat-002-0l\build\forgecompat-002-0l.jar'
if(Test-Path -LiteralPath $run){throw 'STOP existing label'}
if(Get-Process java -ErrorAction SilentlyContinue){throw 'STOP Java process exists'}
foreach($json in @('inputs-before.json','outputs.json')){foreach($i in (Get-Content -LiteralPath (Join-Path $evRoot $json) -Raw|ConvertFrom-Json)){if((Get-FileHash -LiteralPath $i.Path).Hash -ne $i.SHA256){throw ('STOP preflight input drift '+$i.Path)}}}
if($Kind -eq 'treatment'){
 $c=Get-Content -LiteralPath ($base+'\logs\spike002-0l-control\spike002-0l-control.stdout.log') -Raw
 if($c -notmatch 'SUBSCRIBER enabled=false annotationHits=0' -or $c -notmatch 'EVENT_PROFILE enabled=false superHits=0 constructorHits=0' -or $c -notmatch 'NoClassDefFoundError: net/minecraftforge/eventbus/api/Event' -or $c -match 'register returned|FORGECOMPAT-XFORM\] ERROR|dump FAIL'){throw 'STOP control not within frozen expected P1 gate'}
}
$server=$base+'\neoforge-server'
foreach($relative in @('mods','logs','crash-reports')){
 $target=[IO.Path]::GetFullPath((Join-Path $server $relative))
 if(-not $target.StartsWith($server+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'STOP cleanup outside server'}
 if(Test-Path -LiteralPath $target){if((Get-Item -LiteralPath $target).Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'STOP reparse cleanup'}}
}
$previous=if($Kind -eq 'control'){$base+'\logs\spike002-0j-treatment'}else{$base+'\logs\spike002-0l-control'}
$previousLabel=Split-Path -Leaf $previous
foreach($file in @('latest','debug')){
 $suffix=if($file -eq 'latest'){'.log'}else{'.debug.log'}
 $live=$server+'\logs\'+$file+'.log';$saved=Join-Path $previous ($previousLabel+$suffix)
 if((Get-FileHash -LiteralPath $live).Hash -ne (Get-FileHash -LiteralPath $saved).Hash){throw 'STOP unarchived live logs'}
}
if(Test-Path -LiteralPath ($server+'\crash-reports')){foreach($crash in Get-ChildItem -LiteralPath ($server+'\crash-reports') -File){$saved=Join-Path ($previous+'\crash-reports') $crash.Name;if(-not(Test-Path -LiteralPath $saved) -or (Get-FileHash -LiteralPath $saved).Hash -ne (Get-FileHash -LiteralPath $crash.FullName).Hash){throw 'STOP unarchived crash'}}}
$switch=if($Kind -eq 'control'){'false'}else{'true'}
$jvm=@('-Dforgecompat.2a.rewrite=true','-Dforgecompat.0g.apiRewrite=true','-Dforgecompat.0j.emptyRegistration=true',('-Dforgecompat.0l.subscriberRewrite='+$switch))
$utf8=[Text.UTF8Encoding]::new($false)
$command="& '$harness' -Label '$label' -ExtraJvmArgs @('-Dforgecompat.2a.rewrite=true', '-Dforgecompat.0g.apiRewrite=true', '-Dforgecompat.0j.emptyRegistration=true', '-Dforgecompat.0l.subscriberRewrite=$switch') -ModsJar '$probe' -ExtraModsJars '$compat'"
$cmdFile=Join-Path $evRoot ($label+'.command.txt');if(Test-Path -LiteralPath $cmdFile){throw 'STOP recorded command exists'}
[IO.File]::WriteAllText($cmdFile,$command+"`r`n",$utf8)
$code=0
try{
 $output=@(& $harness -Label $label -ExtraJvmArgs $jvm -ModsJar $probe -ExtraModsJars $compat 2>&1)
 [IO.File]::WriteAllText((Join-Path $evRoot ($label+'.harness-output.txt')),($output -join "`r`n")+"`r`n",$utf8)
 Copy-Item -LiteralPath ($server+'\user_jvm_args.txt') -Destination (Join-Path $evRoot ($label+'.actual-jvm-args.txt'))
 $output
}catch{$code=1;[IO.File]::WriteAllText((Join-Path $evRoot ($label+'.harness-error.txt')),($_|Out-String),$utf8);throw}
finally{[IO.File]::WriteAllText((Join-Path $evRoot ($label+'.harness-exit.txt')),$code.ToString()+"`r`n",$utf8)}
