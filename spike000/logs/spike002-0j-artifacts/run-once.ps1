param([Parameter(Mandatory=$true)][ValidateSet('control','treatment')][string]$Kind)
$ErrorActionPreference='Stop'
$evRoot=$PSScriptRoot
$base='D:\DualForge\spike000'
$label='spike002-0j-'+$Kind
$run=Join-Path ($base+'\logs') $label
$probe=$base+'\probe-002-0e\build\forgeprobe.jar'
$compat='D:\DualForge\spike001a\compat-002-0j\build\forgecompat-002-0j.jar'
$harness=$base+'\run-server.ps1'
if(Test-Path -LiteralPath $run){throw 'STOP existing run directory'}
if(Get-Process java -ErrorAction SilentlyContinue){throw 'STOP java process exists'}
foreach($pair in @(
 @($probe,'D61224F3014F0ADFF85D10EEDEA414763C231367F08EDB99CE99DD2C8935F1C9'),
 @($compat,'D724FB489A3A1F1180A5FBA09DFF2FECD01FBD6F8AE33EE727851AB211FA44AB'),
 @($harness,'33049C802F8AF4E79665A541DAB9A9DFAAE0CFCFEE8CDA8702A8309DD5DBF64A'),
 @(($base+'\neoforge-server\libraries\net\neoforged\neoforge\21.1.256\neoforge-21.1.256-universal.jar'),'E0A08BAE5CEA95BE96C09770530F5643BB183AF8AF59C64694EB346CB4C93FA4'),
 @(($base+'\neoforge-server\libraries\net\neoforged\bus\8.0.5\bus-8.0.5.jar'),'705E24F37752809499A5CD0C7DD31EAABF3D933C5D3B2060168E94E9EF62593A')
)){if((Get-FileHash -LiteralPath $pair[0]).Hash -ne $pair[1]){throw ('STOP input drift '+$pair[0])}}
$server=$base+'\neoforge-server'
foreach($relative in @('mods','logs','crash-reports')){
 $target=[IO.Path]::GetFullPath((Join-Path $server $relative))
 if(-not $target.StartsWith($server+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'STOP cleanup outside server'}
 if(Test-Path -LiteralPath $target){if((Get-Item -LiteralPath $target).Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'STOP reparse cleanup'}}
}
$previous=if($Kind -eq 'control'){$base+'\logs\spike002-0g-treatment'}else{$base+'\logs\spike002-0j-control'}
$previousLabel=Split-Path -Leaf $previous
foreach($file in @('latest','debug')){
 $suffix=if($file -eq 'latest'){'.log'}else{'.debug.log'}
 $live=$server+'\logs\'+$file+'.log';$saved=Join-Path $previous ($previousLabel+$suffix)
 if((Get-FileHash -LiteralPath $live).Hash -ne (Get-FileHash -LiteralPath $saved).Hash){throw 'STOP unarchived server logs'}
}
if(Test-Path -LiteralPath ($server+'\crash-reports')){
 foreach($crash in Get-ChildItem -LiteralPath ($server+'\crash-reports') -File){$saved=Join-Path ($previous+'\crash-reports') $crash.Name;if(-not(Test-Path -LiteralPath $saved) -or (Get-FileHash -LiteralPath $saved).Hash -ne (Get-FileHash -LiteralPath $crash.FullName).Hash){throw 'STOP unarchived crash'}}
}
$switch=if($Kind -eq 'control'){'false'}else{'true'}
$jvm=@('-Dforgecompat.2a.rewrite=true','-Dforgecompat.0g.apiRewrite=true',('-Dforgecompat.0j.emptyRegistration='+$switch))
$utf8=[Text.UTF8Encoding]::new($false)
$command="& '$harness' -Label '$label' -ExtraJvmArgs @('-Dforgecompat.2a.rewrite=true', '-Dforgecompat.0g.apiRewrite=true', '-Dforgecompat.0j.emptyRegistration=$switch') -ModsJar '$probe' -ExtraModsJars '$compat'"
$cmdFile=Join-Path $evRoot ($label+'.command.txt')
if(Test-Path -LiteralPath $cmdFile){throw 'STOP command already recorded'}
[IO.File]::WriteAllText($cmdFile,$command+"`r`n",$utf8)
$code=0
try {
 $output=@(& $harness -Label $label -ExtraJvmArgs $jvm -ModsJar $probe -ExtraModsJars $compat 2>&1)
 [IO.File]::WriteAllText((Join-Path $evRoot ($label+'.harness-output.txt')),($output -join "`r`n")+"`r`n",$utf8)
 $output
} catch {
 $code=1;[IO.File]::WriteAllText((Join-Path $evRoot ($label+'.harness-error.txt')),($_|Out-String),$utf8);throw
} finally {[IO.File]::WriteAllText((Join-Path $evRoot ($label+'.harness-exit.txt')),$code.ToString()+"`r`n",$utf8)}
