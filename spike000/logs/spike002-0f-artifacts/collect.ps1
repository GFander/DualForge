param([ValidateSet('core','extra','module-raw','verbose-raw','final')][string]$Phase)
$ErrorActionPreference = 'Stop'
$evRoot = $PSScriptRoot
$jdkBin = 'C:\Users\Albert\.jdks\ms-21.0.10\bin'
$forgeUniversal = 'D:\DualForge\spike000\forge-server\libraries\net\minecraftforge\forge\1.21.1-52.1.0\forge-1.21.1-52.1.0-universal.jar'
$forgeBus = 'D:\DualForge\spike000\forge-server\libraries\net\minecraftforge\eventbus\6.2.27\eventbus-6.2.27.jar'
$neoUniversal = 'D:\DualForge\spike000\neoforge-server\libraries\net\neoforged\neoforge\21.1.256\neoforge-21.1.256-universal.jar'
$neoBus = 'D:\DualForge\spike000\neoforge-server\libraries\net\neoforged\bus\8.0.5\bus-8.0.5.jar'
$compat = 'D:\DualForge\spike001a\compat-002-0d\build-treatment\forgecompat-002-0d.jar'
$probe = 'D:\DualForge\spike000\probe-002-0e\build\forgeprobe.jar'
$dump = 'D:\DualForge\spike000\logs\spike002-0e-treatment\ForgeProbe.rewritten.class'
$utf8 = [Text.UTF8Encoding]::new($false)
$refs = @(
 @($probe,'D61224F3014F0ADFF85D10EEDEA414763C231367F08EDB99CE99DD2C8935F1C9'),
 @($dump,'55D81709DC8C41A8FB25189BEF87D41F5F1920C721C02B2027F9EEE764924FE8'),
 @($compat,'4CD8195F7AA03B8B595F646CDEAC16454D03D5E3862B5E2F388C820CF2FA2FEF'),
 @($forgeUniversal,'88EF6625122CFDB9D7B3176BD61B8C0692A191BAD0EDD77FFDDC6A765EE4979C'),
 @($forgeBus,'D2A7BCECB80B33F90854A003DC295960263D78435D9D6016C427D711510CB999'),
 @($neoUniversal,'E0A08BAE5CEA95BE96C09770530F5643BB183AF8AF59C64694EB346CB4C93FA4'),
 @($neoBus,'705E24F37752809499A5CD0C7DD31EAABF3D933C5D3B2060168E94E9EF62593A'),
 @('D:\DualForge\spike000\run-server.ps1','84C7E2C06BF0A1D2AC0D0E2D5AD20482F1357668EC8807DAD642DA55944F77D0'),
 @('D:\DualForge\Dev Manuals\Spike_002-0f.md','75D287EDD1850DEFEDE17007F7CCAFED7E4CFD265FDD27547CB8FF8393E285E8')
)
function Save-New([string]$name,[string]$value) {
 $target = Join-Path $evRoot $name
 if (Test-Path -LiteralPath $target) { throw "STOP: evidence already exists: $target" }
 [IO.File]::WriteAllText($target,$value,$utf8)
}
function Check-Refs {
 @(foreach ($r in $refs) {
  $item = Get-Item -LiteralPath $r[0]
  $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $r[0]).Hash
  if ($hash -ne $r[1]) { throw "STOP: hash mismatch: $($r[0])" }
  [pscustomobject]@{Path=$r[0];SHA256=$hash;Bytes=$item.Length}
 })
}
function Invoke-Record([string]$name,[string]$exe,[string[]]$nativeArgs) {
 $rendered = '& ' + "'" + $exe.Replace("'","''") + "' " + (($nativeArgs | ForEach-Object {"'"+$_.Replace("'","''")+"'"}) -join ' ')
 Save-New ($name+'.command.txt') ($rendered+"`r`n")
 $psi = [Diagnostics.ProcessStartInfo]::new()
 $psi.FileName=$exe; $psi.WorkingDirectory='D:\DualForge'
 $psi.UseShellExecute=$false; $psi.CreateNoWindow=$true
 $psi.RedirectStandardOutput=$true; $psi.RedirectStandardError=$true
 $psi.StandardOutputEncoding=$utf8; $psi.StandardErrorEncoding=$utf8
 foreach($arg in $nativeArgs) { $psi.ArgumentList.Add($arg) }
 $p=[Diagnostics.Process]::new(); $p.StartInfo=$psi
 $null=$p.Start()
 $outBytes=[IO.MemoryStream]::new(); $errBytes=[IO.MemoryStream]::new()
 $outTask=$p.StandardOutput.BaseStream.CopyToAsync($outBytes)
 $errTask=$p.StandardError.BaseStream.CopyToAsync($errBytes)
 $p.WaitForExit()
 $null=$outTask.GetAwaiter().GetResult(); $null=$errTask.GetAwaiter().GetResult()
 foreach($pair in @(@('stdout',$outBytes),@('stderr',$errBytes))) {
  $rawPath=Join-Path $evRoot ($name+'.'+$pair[0]+'.txt')
  if(Test-Path -LiteralPath $rawPath) { throw "STOP: raw evidence exists: $rawPath" }
  [IO.File]::WriteAllBytes($rawPath,$pair[1].ToArray())
  $pair[1].Dispose()
 }
 Save-New ($name+'.exit.txt') ($p.ExitCode.ToString()+"`r`n")
 $code=$p.ExitCode; $p.Dispose()
 if($code -ne 0) { throw "STOP: tool exit $code : $name" }
}
if($Phase -eq 'module-raw') {
 $null=Check-Refs
 foreach($entry in @(@('forge-universal',$forgeUniversal),@('forge-bus',$forgeBus),@('neo-universal',$neoUniversal),@('neo-bus',$neoBus))) {
  Invoke-Record ($entry[0]+'-module-raw') (Join-Path $jdkBin 'jar.exe') @('--describe-module','--file',$entry[1])
 }
 Write-Output 'Module outputs captured as original bytes; previous decoded files preserved as superseded captures.'
}
if($Phase -eq 'verbose-raw') {
 $null=Check-Refs
 foreach($item in @(
  @('probe',$probe,'com.example.forgeprobe.ForgeProbe'),
  @('forge-MinecraftForge',($forgeUniversal+';'+$forgeBus),'net.minecraftforge.common.MinecraftForge'),
  @('forge-IEventBus',$forgeBus,'net.minecraftforge.eventbus.api.IEventBus'),
  @('compat-IEventBus',$compat,'net.minecraftforge.eventbus.api.IEventBus'),
  @('neo-NeoForge',($neoUniversal+';'+$neoBus),'net.neoforged.neoforge.common.NeoForge'),
  @('neo-IEventBus',$neoBus,'net.neoforged.bus.api.IEventBus')
 )) { Invoke-Record ($item[0]+'-verbose-raw') (Join-Path $jdkBin 'javap.exe') @('-p','-v','-classpath',$item[1],$item[2]) }
 Invoke-Record 'dump-verbose-raw' (Join-Path $jdkBin 'javap.exe') @('-p','-v',$dump)
 Write-Output 'Verbose outputs recaptured byte-for-byte; decoded predecessors retained and superseded.'
}
function Dump-Class([string]$name,[string]$cp,[string]$clazz) {
 Invoke-Record ($name+'-code') (Join-Path $jdkBin 'javap.exe') @('-p','-c','-s','-constants','-classpath',$cp,$clazz)
 Invoke-Record ($name+'-verbose') (Join-Path $jdkBin 'javap.exe') @('-p','-v','-classpath',$cp,$clazz)
}
if($Phase -eq 'core') {
 Save-New 'inputs-before.json' ((Check-Refs | ConvertTo-Json -Depth 4)+"`r`n")
 Invoke-Record 'javap-version' (Join-Path $jdkBin 'javap.exe') @('-version')
 Invoke-Record 'jar-version' (Join-Path $jdkBin 'jar.exe') @('--version')
 Dump-Class 'probe' $probe 'com.example.forgeprobe.ForgeProbe'
 Invoke-Record 'dump-code' (Join-Path $jdkBin 'javap.exe') @('-p','-c','-s','-constants',$dump)
 Invoke-Record 'dump-verbose' (Join-Path $jdkBin 'javap.exe') @('-p','-v',$dump)
 foreach($entry in @(@('forge-universal',$forgeUniversal),@('forge-bus',$forgeBus),@('neo-universal',$neoUniversal),@('neo-bus',$neoBus))) {
  Invoke-Record ($entry[0]+'-entries') (Join-Path $jdkBin 'jar.exe') @('tf',$entry[1])
  Invoke-Record ($entry[0]+'-module') (Join-Path $jdkBin 'jar.exe') @('--describe-module','--file',$entry[1])
  $zip=[IO.Compression.ZipFile]::OpenRead($entry[1])
  try {
   $mf=$zip.GetEntry('META-INF/MANIFEST.MF')
   if($null -eq $mf) { throw "STOP: missing manifest $($entry[0])" }
   $reader=[IO.StreamReader]::new($mf.Open(),$utf8)
   try { Save-New ($entry[0]+'-manifest.txt') $reader.ReadToEnd() } finally { $reader.Dispose() }
  } finally { $zip.Dispose() }
 }
 Dump-Class 'forge-MinecraftForge' ($forgeUniversal+';'+$forgeBus) 'net.minecraftforge.common.MinecraftForge'
 Dump-Class 'forge-IEventBus' $forgeBus 'net.minecraftforge.eventbus.api.IEventBus'
 Dump-Class 'compat-IEventBus' $compat 'net.minecraftforge.eventbus.api.IEventBus'
 $neoEntries=Get-Content -LiteralPath (Join-Path $evRoot 'neo-universal-entries.stdout.txt')
 Save-New 'neo-candidates.json' ((@(
  [pscustomobject]@{Class='net/neoforged/neoforge/common/NeoForge.class';Present=($neoEntries -ccontains 'net/neoforged/neoforge/common/NeoForge.class')},
  [pscustomobject]@{Class='net/neoforged/neoforge/common/MinecraftForge.class';Present=($neoEntries -ccontains 'net/neoforged/neoforge/common/MinecraftForge.class')}
 )|ConvertTo-Json)+"`r`n")
 foreach($candidate in @('NeoForge','MinecraftForge')) {
  if($neoEntries -ccontains ('net/neoforged/neoforge/common/'+$candidate+'.class')) {
   Dump-Class ('neo-'+$candidate) ($neoUniversal+';'+$neoBus) ('net.neoforged.neoforge.common.'+$candidate)
  }
 }
 Dump-Class 'neo-IEventBus' $neoBus 'net.neoforged.bus.api.IEventBus'
 Write-Output 'Core evidence saved; inspect initializer and implementation boundaries before extra collection.'
}
if($Phase -eq 'extra') {
 $null=Check-Refs
 foreach($extra in @(
  @('forge-BusBuilder',$forgeBus,'net.minecraftforge.eventbus.api.BusBuilder'),
  @('forge-BusBuilderImpl',$forgeBus,'net.minecraftforge.eventbus.BusBuilderImpl'),
  @('forge-EventBus',$forgeBus,'net.minecraftforge.eventbus.EventBus'),
  @('neo-BusBuilder',$neoBus,'net.neoforged.bus.api.BusBuilder'),
  @('neo-BusBuilderImpl',$neoBus,'net.neoforged.bus.BusBuilderImpl'),
  @('neo-EventBus',$neoBus,'net.neoforged.bus.EventBus')
 )) { Dump-Class $extra[0] $extra[1] $extra[2] }
 Write-Output 'Bounded builder and implementation evidence saved.'
}
if($Phase -eq 'final') {
 Save-New 'inputs-after.json' ((Check-Refs | ConvertTo-Json -Depth 4)+"`r`n")
 $items=@(Get-ChildItem -LiteralPath $evRoot -File | Sort-Object Name | ForEach-Object {
  [pscustomobject]@{Path=$_.Name;Bytes=$_.Length;SHA256=(Get-FileHash -Algorithm SHA256 -LiteralPath $_.FullName).Hash}
 })
 Save-New 'evidence-index.json' (($items|ConvertTo-Json -Depth 4)+"`r`n")
 Write-Output ('Frozen inputs unchanged; indexed '+$items.Count+' evidence files (index self excluded).')
}
