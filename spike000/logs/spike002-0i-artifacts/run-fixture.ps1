$ErrorActionPreference='Stop'
. 'D:\DualForge\spike000\logs\spike002-0g-artifacts\build-evidence.ps1'
$evRoot=$PSScriptRoot
$labRoot=$PSScriptRoot
$libs='D:\DualForge\spike000\forge-server\libraries'
$inputs=@(
 @('net\minecraftforge\eventbus\6.2.27\eventbus-6.2.27.jar','D2A7BCECB80B33F90854A003DC295960263D78435D9D6016C427D711510CB999'),
 @('net\minecraftforge\modlauncher\10.2.4\modlauncher-10.2.4.jar','FD5B1BEC9C1700052959DD2A3A934496C68CD7C0ECB18AED4D1FBA784122A134'),
 @('org\apache\logging\log4j\log4j-api\2.22.1\log4j-api-2.22.1.jar','5D7BEAE7FF15D8516D6517121D7F12A79A6AC180DF64B5FCEC55D5BE21056E53'),
 @('org\apache\logging\log4j\log4j-core\2.22.1\log4j-core-2.22.1.jar','46DCCECAC556623D8E2CE8648496824A82951D139062A4E61148AFF1A25ED18D'),
 @('org\ow2\asm\asm\9.7.1\asm-9.7.1.jar','8CADD43AC5EB6D09DE05FAECCA38B917A040BB9139C7EDEB4CC81C740B713281'),
 @('org\ow2\asm\asm-tree\9.7.1\asm-tree-9.7.1.jar','9929881F59EB6B840E86D54570C77B59CE721D104E6DFD7A40978991C2D3B41F'),
 @('net\jodah\typetools\0.6.3\typetools-0.6.3.jar','408300867804BE05D1083CC0287DD3A21295A9E76090E288013BA5A4182DC96D')
)
function Verify-Inputs {@(foreach($i in $inputs){$p=Join-Path $libs $i[0];$h=(Get-FileHash -LiteralPath $p).Hash;if($h -ne $i[1]){throw 'STOP dependency drift'};[pscustomobject]@{Path=$p;SHA256=$h;Bytes=(Get-Item -LiteralPath $p).Length}})}
$before=(Verify-Inputs)|ConvertTo-Json -Depth 4
$beforePath=Join-Path $labRoot 'inputs-before.json'
if(Test-Path -LiteralPath $beforePath){if([IO.File]::ReadAllText($beforePath,$utf8) -cne $before){throw 'STOP resumed input mismatch'}}else{Save-New 'inputs-before.json' $before}
Save-New 'fixture-source.json' ((Hashes @(($labRoot+'\EmptyRegistrationFixture.java'),'D:\DualForge\Dev Manuals\Spike_002-0i.md'))|ConvertTo-Json -Depth 4)
$cp=(@($inputs|ForEach-Object{Join-Path $libs $_[0]})-join ';')
Save-New 'classpath.txt' $cp
New-Item -ItemType Directory -Path ($labRoot+'\classes')|Out-Null
Native 'javac' ($jdkBin+'\javac.exe') @('--release','21','-encoding','UTF-8','-classpath',$cp,'-d',($labRoot+'\classes'),($labRoot+'\EmptyRegistrationFixture.java'))
Native 'fixture-bytecode' ($jdkBin+'\javap.exe') @('-p','-c','-s','-classpath',($labRoot+'\classes'),'EmptyRegistrationFixture')
Native 'java' ($jdkBin+'\java.exe') @('-classpath',($labRoot+'\classes;'+$cp),'EmptyRegistrationFixture')
$stdout=Get-Content -LiteralPath ($labRoot+'\java.stdout.txt') -Raw
foreach($s in @('DECLARED_METHODS=0','CUSTOM_PUBLIC_METHODS=0 SUBSCRIBERS=0','BUS_CLASS=net.minecraftforge.eventbus.EventBus','BEFORE_REGISTER','AFTER_REGISTER')){if(-not $stdout.Contains($s)){throw ('STOP marker '+$s)}}
Save-New 'inputs-after.json' ((Verify-Inputs)|ConvertTo-Json -Depth 4)
Write-Output 'PASS-002-0i-1: original Forge bus register returned for the fixed bare-target fixture.'
