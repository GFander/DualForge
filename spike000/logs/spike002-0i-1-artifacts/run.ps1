$ErrorActionPreference='Stop'
. 'D:\DualForge\spike000\logs\spike002-0g-artifacts\build-evidence.ps1'
$evRoot=$PSScriptRoot
$sourceRoot='D:\DualForge\spike000\logs\spike002-0i-artifacts'
$base='D:\DualForge\spike000\forge-server\libraries'
$secure=$base+'\net\minecraftforge\securemodules\2.2.21\securemodules-2.2.21.jar'
$unsafeJar=$base+'\net\minecraftforge\unsafe\0.9.2\unsafe-0.9.2.jar'
$frozen=Get-Content -LiteralPath ($sourceRoot+'\inputs-before.json') -Raw|ConvertFrom-Json
$extra=@(@($secure,'A8254AC61485C95F652D1B3A92395FCEE375FA7D12AE996EFD672ED8BEABB3A0'),@($unsafeJar,'F5035AB2DD1C02AA354F948A28DAF2FF4215956194A98634F076AD71610FE3A4'))
foreach($i in $frozen){if((Get-FileHash -LiteralPath $i.Path).Hash -ne $i.SHA256){throw 'STOP old dependency drift'}}
foreach($i in $extra){if((Get-FileHash -LiteralPath $i[0]).Hash -ne $i[1]){throw 'STOP new dependency drift'}}
foreach($i in @(@('EmptyRegistrationFixture.class','3EDD9A21A53B0206349F30853D72F7A075E1AC850D67E75779C50642F6BA4B46'),@('EmptyRegistrationFixture$BareTarget.class','5525399924814351FBD166A7FDCF6CD085180C73F7DBAB092824EE93963C40BD'))){if((Get-FileHash -LiteralPath (Join-Path ($sourceRoot+'\classes') $i[0])).Hash -ne $i[1]){throw 'STOP class drift'}}
$paths=@($frozen|ForEach-Object{$_.Path})+@($secure,$unsafeJar)
Save-New 'inputs-before.json' ((Hashes $paths)|ConvertTo-Json -Depth 4)
Native 'securemodule-description' ($jdkBin+'\jar.exe') @('--describe-module','--file',$secure)
Native 'modlauncher-description' ($jdkBin+'\jar.exe') @('--describe-module','--file',($base+'\net\minecraftforge\modlauncher\10.2.4\modlauncher-10.2.4.jar'))
Native 'securemodule-entries' ($jdkBin+'\jar.exe') @('tf',$secure)
$oldCp=Get-Content -LiteralPath ($sourceRoot+'\classpath.txt') -Raw
$cp=$sourceRoot+'\classes;'+$oldCp+';'+$secure+';'+$unsafeJar
Save-New 'classpath.txt' $cp
Native 'java' ($jdkBin+'\java.exe') @('-classpath',$cp,'EmptyRegistrationFixture')
$text=Get-Content -LiteralPath (Join-Path $evRoot 'java.stdout.txt') -Raw
foreach($s in @('DECLARED_METHODS=0','CUSTOM_PUBLIC_METHODS=0 SUBSCRIBERS=0','BUS_CLASS=net.minecraftforge.eventbus.EventBus','BEFORE_REGISTER','AFTER_REGISTER')){if(-not $text.Contains($s)){throw ('STOP missing '+$s)}}
Save-New 'inputs-after.json' ((Hashes $paths)|ConvertTo-Json -Depth 4)
Write-Output 'PASS-002-0i-1: original Forge bare-object register returned, without changing fixture classes or builder options.'
