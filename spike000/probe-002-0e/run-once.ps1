param([Parameter(Mandatory=$true)][ValidateSet('control','treatment')][string]$Kind)
$ErrorActionPreference='Stop'
$base='D:\DualForge\spike000'
$label='spike002-0e-'+$Kind
$evidence=Join-Path $base 'logs\spike002-0e-artifacts'
$probe=if($Kind -eq 'control'){Join-Path $base 'probe\build\forgeprobe.jar'}else{Join-Path $base 'probe-002-0e\build\forgeprobe.jar'}
$probeHash=if($Kind -eq 'control'){'A93A823C988BFD6A8F6EA132D189D9DE732B0E5951F4BF74980D51BB63374E22'}else{'D61224F3014F0ADFF85D10EEDEA414763C231367F08EDB99CE99DD2C8935F1C9'}
$compat='D:\DualForge\spike001a\compat-002-0d\build-treatment\forgecompat-002-0d.jar'
$harness=Join-Path $base 'run-server.ps1'
if(Test-Path -LiteralPath (Join-Path (Join-Path $base 'logs') $label)){throw 'STOP: existing run directory'}
if((Get-FileHash -LiteralPath $probe).Hash -ne $probeHash){throw 'STOP: probe drift'}
if((Get-FileHash -LiteralPath $compat).Hash -ne '4CD8195F7AA03B8B595F646CDEAC16454D03D5E3862B5E2F388C820CF2FA2FEF'){throw 'STOP: compat drift'}
if((Get-FileHash -LiteralPath $harness).Hash -ne '84C7E2C06BF0A1D2AC0D0E2D5AD20482F1357668EC8807DAD642DA55944F77D0'){throw 'STOP: harness drift'}
if(Get-Process java -ErrorAction SilentlyContinue){throw 'STOP: Java process exists'}
$server=Join-Path $base 'neoforge-server'
foreach($relative in @('mods','logs','crash-reports')){
    $target=[IO.Path]::GetFullPath((Join-Path $server $relative))
    if(-not $target.StartsWith($server+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'STOP: cleanup target outside server'}
    if(Test-Path -LiteralPath $target){if((Get-Item -LiteralPath $target).Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'STOP: cleanup target is reparse point'}}
}
$utf8=New-Object System.Text.UTF8Encoding($false)
$command="& '$harness' -Label '$label' -ExtraJvmArgs '-Dforgecompat.2a.rewrite=true' -ModsJar '$probe' -ExtraModsJars '$compat'"
[IO.File]::WriteAllText((Join-Path $evidence ($label+'.command.txt')),$command+"`r`n",$utf8)
$exit=0
try{
    $output=@(& $harness -Label $label -ExtraJvmArgs '-Dforgecompat.2a.rewrite=true' -ModsJar $probe -ExtraModsJars $compat 2>&1)
    [IO.File]::WriteAllText((Join-Path $evidence ($label+'.harness-output.txt')),($output -join "`r`n")+"`r`n",$utf8)
    $output
}catch{
    $exit=1
    [IO.File]::WriteAllText((Join-Path $evidence ($label+'.harness-error.txt')),($_|Out-String),$utf8)
    throw
}finally{
    [IO.File]::WriteAllText((Join-Path $evidence ($label+'.harness-exit.txt')),[string]$exit+"`r`n",$utf8)
}
