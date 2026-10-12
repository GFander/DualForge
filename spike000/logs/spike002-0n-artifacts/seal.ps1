$ErrorActionPreference='Stop'
. 'D:\DualForge\spike000\logs\spike002-0j-artifacts\build-evidence.ps1'
$evRoot=$PSScriptRoot
$work='D:\DualForge\spike000\subscriber-002-0n'
$asm='D:\DualForge\spike000\neoforge-server\libraries\org\ow2\asm\asm\9.10.1\asm-9.10.1.jar'
$tree='D:\DualForge\spike000\neoforge-server\libraries\org\ow2\asm\asm-tree\9.10.1\asm-tree-9.10.1.jar'
New-Item -ItemType Directory -Path ($evRoot+'\verifier-classes')|Out-Null
Native 'semantic-verifier-javac' ($jdkBin+'\javac.exe') @('--release','21','-encoding','UTF-8','-classpath',($asm+';'+$tree),'-d',($evRoot+'\verifier-classes'),($evRoot+'\SemanticVerifier.java'))
Native 'semantic-verifier' ($jdkBin+'\java.exe') @('-classpath',($evRoot+'\verifier-classes;'+$asm+';'+$tree),'SemanticVerifier',($work+'\build\original\com\example\forgeprobe\SubscriberFixture.class'),($work+'\build\runtime\com\example\forgeprobe\SubscriberFixture.class'))
$c=Get-Content -LiteralPath (Join-Path $evRoot 'control.command.txt') -Raw
$t=Get-Content -LiteralPath (Join-Path $evRoot 'treatment.command.txt') -Raw
if($c.Replace('forgecompat.0n.post=false','forgecompat.0n.post=true') -cne $t){throw 'STOP command changes beyond post flag'}
foreach($kind in @('control','treatment')){
 if((Get-Content -LiteralPath (Join-Path $evRoot ($kind+'.exit.txt')) -Raw).Trim() -ne '0'){throw 'STOP experiment nonzero exit'}
 if((Get-Item -LiteralPath (Join-Path $evRoot ($kind+'.stderr.txt'))).Length -ne 0){throw 'STOP experiment stderr'}
}
Save-New 'complete-review-gates.txt' 'Complete canonical class equality except one annotation descriptor; original event bytes identical; actual command only one flag; both exits 0 and stderr empty.'
$index=@(Get-ChildItem -LiteralPath $evRoot -File -Recurse|Sort-Object FullName|ForEach-Object{[pscustomobject]@{Path=$_.FullName;SHA256=(Get-FileHash -LiteralPath $_.FullName).Hash;Bytes=$_.Length}})
Save-New 'evidence-index.json' ($index|ConvertTo-Json -Depth 4)
foreach($i in $index){if((Get-FileHash -LiteralPath $i.Path).Hash -ne $i.SHA256){throw 'STOP evidence drift'}}
Write-Output ($index.Count.ToString()+' evidence files sealed and verified.')
