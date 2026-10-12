param([ValidateSet('prepare','build','archive-control','archive-treatment','final')][string]$Phase)
$ErrorActionPreference = 'Stop'
$work = 'D:\DualForge\spike000\probe-002-0e'
$evidence = 'D:\DualForge\spike000\logs\spike002-0e-artifacts'
$jdk = 'C:\Users\Albert\.jdks\ms-21.0.10\bin'
$oldJar = 'D:\DualForge\spike000\probe\build\forgeprobe.jar'
$compatJar = 'D:\DualForge\spike001a\compat-002-0d\build-treatment\forgecompat-002-0d.jar'
$dump = 'D:\DualForge\spike000\logs\spike002-0b-2a-v1.1\ForgeProbe.rewritten.class'
$utf8 = New-Object System.Text.UTF8Encoding($false)
function Save-Text([string]$path, [object]$value) {
    [IO.File]::WriteAllText($path, (($value | Out-String -Width 4096).TrimEnd() + "`r`n"), $utf8)
}
function Fingerprint([string]$path) {
    [pscustomobject]@{Path=$path; SHA256=(Get-FileHash -LiteralPath $path).Hash; Bytes=(Get-Item -LiteralPath $path).Length}
}
function Invoke-Evidence([string]$name,[string]$exe,[string[]]$invocationArgs,[int[]]$accepted=@(0)) {
    $command = '& ' + "'" + $exe.Replace("'","''") + "' " + (($invocationArgs | ForEach-Object { "'" + $_.Replace("'","''") + "'" }) -join ' ')
    Save-Text (Join-Path $evidence ($name+'.command.txt')) $command
    $output = @(& $exe @invocationArgs 2>&1)
    $code = $LASTEXITCODE
    Save-Text (Join-Path $evidence ($name+'.txt')) $output
    Save-Text (Join-Path $evidence ($name+'.exit.txt')) $code
    if($code -notin $accepted){throw "STOP: $name exit $code"}
    return $output
}
function Source-Manifest([string]$root,[string]$destination){
    $records = @(Get-ChildItem -LiteralPath $root -File -Recurse | Sort-Object FullName | ForEach-Object {
        [pscustomobject]@{Path=$_.FullName.Substring($root.Length+1);SHA256=(Get-FileHash -LiteralPath $_.FullName).Hash;Bytes=$_.Length}
    })
    Save-Text $destination ($records | ConvertTo-Json -Depth 4)
}
$refs = @(
    @($compatJar,'4CD8195F7AA03B8B595F646CDEAC16454D03D5E3862B5E2F388C820CF2FA2FEF'),
    @($oldJar,'A93A823C988BFD6A8F6EA132D189D9DE732B0E5951F4BF74980D51BB63374E22'),
    @('D:\DualForge\spike000\probe\src\main\java\com\example\forgeprobe\ForgeProbe.java','6092CC3D402F6D0132A8F15C6A298BC1AE8D4EFFBD8CD91B01C9D734FC1EBBF5'),
    @('D:\DualForge\spike000\probe\src\main\resources\META-INF\mods.toml','B32CF69B88FBE053C2580FC739B30FEDC43D96984C72FF78CD6725C7F3A1CE6E'),
    @('D:\DualForge\spike000\probe\build\jar-root\META-INF\mods.toml','B32CF69B88FBE053C2580FC739B30FEDC43D96984C72FF78CD6725C7F3A1CE6E'),
    @('D:\DualForge\spike000\probe\build\verify\META-INF\mods.toml','B32CF69B88FBE053C2580FC739B30FEDC43D96984C72FF78CD6725C7F3A1CE6E'),
    @('D:\DualForge\spike000\neoforge-server\libraries\net\neoforged\fancymodloader\loader\4.0.45\loader-4.0.45.jar','8390398E67DF23E72D85ED809685BE2752C8EC545B7CBB6AD4B70FDC81E4DCD4'),
    @('D:\DualForge\spike000\forge-server\libraries\net\minecraftforge\javafmllanguage\1.21.1-52.1.0\javafmllanguage-1.21.1-52.1.0.jar','D27E1F3149EA08FC0A80779FE24FA8585692C5042A34BAD153E55662486ACC15'),
    @('D:\DualForge\spike000\forge-server\libraries\net\minecraftforge\eventbus\6.2.27\eventbus-6.2.27.jar','D2A7BCECB80B33F90854A003DC295960263D78435D9D6016C427D711510CB999'),
    @('D:\DualForge\spike000\forge-server\libraries\net\minecraftforge\forge\1.21.1-52.1.0\forge-1.21.1-52.1.0-universal.jar','88EF6625122CFDB9D7B3176BD61B8C0692A191BAD0EDD77FFDDC6A765EE4979C'),
    @('D:\DualForge\spike000\logs\harness-baseline\run-server.ps1','F5319A54086C5BF7EBC8441774F358BC10B74F99C90471A526CAA2D5707FADCA'),
    @('D:\DualForge\spike001a\compat\build\forgecompat-002-0b-2a-v1.1.jar','E27BB9659303DDEDF2EDE02D10B49CAFC586549C0EDA003A0165DF525F97A436'),
    @('D:\DualForge\Dev Manuals\Spike_002-0e.md','072ADCCB6A063E1C037836769058958E08D376E807F774EC7C69F052AB7C6F2A')
)
if($Phase -eq 'prepare'){
    if(Test-Path -LiteralPath $evidence){throw 'STOP: evidence directory already exists'}
    New-Item -ItemType Directory -Path $evidence | Out-Null
    $records=@(foreach($r in $refs){$v=Fingerprint $r[0];if($v.SHA256 -ne $r[1]){throw ('STOP: hash '+$r[0])};$v})
    $records += Fingerprint 'D:\DualForge\spike000\run-server.ps1'
    if($records[-1].SHA256 -ne $refs[10][1]){throw 'STOP: live harness differs'}
    $records += Fingerprint $dump
    if($records[-1].SHA256 -ne '5E2F78DB2B0909D7368A185F618C3D080C00B732279B105CD5B8010FD66D9295'){throw 'STOP: historical dump differs'}
    Save-Text (Join-Path $evidence '2Ea-frozen-refs-recheck.txt') ($records|ConvertTo-Json -Depth 4)
    foreach($name in @('javac','javap','jar')){$opt=if($name -eq 'jar'){'--version'}else{'-version'};$null=Invoke-Evidence ('2Ea-'+$name+'-version') (Join-Path $jdk ($name+'.exe')) @($opt)}
    foreach($index in @(7,8,9)){$null=Invoke-Evidence ('2Ea-entries-'+$index) (Join-Path $jdk 'jar.exe') @('tf',$refs[$index][0])}
    $null=Invoke-Evidence '2Ea-MinecraftForge' (Join-Path $jdk 'javap.exe') @('-p','-classpath',$refs[9][0],'net.minecraftforge.common.MinecraftForge')
    $null=Invoke-Evidence '2Ea-IEventBus-original' (Join-Path $jdk 'javap.exe') @('-p','-classpath',$refs[8][0],'net.minecraftforge.eventbus.api.IEventBus')
    $null=Invoke-Evidence '2Ea-IEventBus-shim' (Join-Path $jdk 'javap.exe') @('-p','-classpath',$compatJar,'net.minecraftforge.eventbus.api.IEventBus')
    Copy-Item -LiteralPath $dump -Destination (Join-Path $evidence 'historical-ForgeProbe.rewritten.class')
    New-Item -ItemType Directory -Path (Join-Path $work 'archive') | Out-Null
    Copy-Item -LiteralPath $oldJar -Destination (Join-Path $work 'archive\forgeprobe-frozen-A93A.jar')
    Copy-Item -LiteralPath 'D:\DualForge\spike000\probe\src' -Destination (Join-Path $work 'archive\src-before') -Recurse
    Copy-Item -LiteralPath 'D:\DualForge\spike000\probe\src' -Destination (Join-Path $work 'src') -Recurse
    Source-Manifest (Join-Path $work 'archive\src-before') (Join-Path $work 'archive\src-before-sha256.json')
    Write-Output '2E-a passed; historical dump and source-before archived. Ready for authorized source patch.'
}
if($Phase -eq 'build'){
    Copy-Item -LiteralPath (Join-Path $work 'src') -Destination (Join-Path $work 'archive\src-after') -Recurse
    Source-Manifest (Join-Path $work 'archive\src-after') (Join-Path $work 'archive\src-after-sha256.json')
    $null=Invoke-Evidence '2Eb-source-diff' 'git' @('-c','core.autocrlf=false','diff','--no-index','--',(Join-Path $work 'archive\src-before'),(Join-Path $work 'archive\src-after')) @(1)
    $harness='D:\DualForge\spike000\run-server.ps1'
    $null=Invoke-Evidence '2Eb-harness-diff' 'git' @('-c','core.autocrlf=false','diff','--no-index','--',$refs[10][0],$harness) @(1)
    if((Get-FileHash -LiteralPath $harness).Hash -ne '84C7E2C06BF0A1D2AC0D0E2D5AD20482F1357668EC8807DAD642DA55944F77D0'){throw 'STOP: harness post-patch hash mismatch'}
    New-Item -ItemType Directory -Path (Join-Path $work 'build\classes'),(Join-Path $work 'build\jar-root') | Out-Null
    $classpath=@($refs[7][0],$refs[8][0],$refs[9][0]) -join ';'
    $null=Invoke-Evidence '2Ec-javac' (Join-Path $jdk 'javac.exe') @('--release','21','-encoding','UTF-8','-classpath',$classpath,'-d',(Join-Path $work 'build\classes'),(Join-Path $work 'src\main\java\com\example\forgeprobe\ForgeProbe.java'))
    Copy-Item -LiteralPath (Join-Path $work 'build\classes\com') -Destination (Join-Path $work 'build\jar-root\com') -Recurse
    Copy-Item -LiteralPath (Join-Path $work 'src\main\resources\META-INF') -Destination (Join-Path $work 'build\jar-root\META-INF') -Recurse
    $newJar=Join-Path $work 'build\forgeprobe.jar'
    $null=Invoke-Evidence '2Ec-jar' (Join-Path $jdk 'jar.exe') @('--create','--file',$newJar,'--manifest',(Join-Path $work 'src\main\resources\META-INF\MANIFEST.MF'),'-C',(Join-Path $work 'build\jar-root'),'.')
    Copy-Item -LiteralPath $newJar -Destination (Join-Path $work 'archive\forgeprobe-002-0e.jar')
    $newEntries=Invoke-Evidence '2Ec-new-entries' (Join-Path $jdk 'jar.exe') @('tf',$newJar)
    $oldEntries=Invoke-Evidence '2Ec-old-entries' (Join-Path $jdk 'jar.exe') @('tf',$oldJar)
    if(($newEntries -join "`n") -cne ($oldEntries -join "`n") -or $newEntries.Count -ne 7){throw 'STOP: jar entries differ'}
    foreach($pair in @(@('old',$oldJar),@('new',$newJar))){
        $null=Invoke-Evidence ('2Ed-'+$pair[0]+'-javap') (Join-Path $jdk 'javap.exe') @('-p','-c','-constants','-classpath',$pair[1],'com.example.forgeprobe.ForgeProbe')
        $null=Invoke-Evidence ('2Ed-'+$pair[0]+'-lines') (Join-Path $jdk 'javap.exe') @('-p','-l','-classpath',$pair[1],'com.example.forgeprobe.ForgeProbe')
        $null=Invoke-Evidence ('2Ed-'+$pair[0]+'-verbose') (Join-Path $jdk 'javap.exe') @('-p','-v','-classpath',$pair[1],'com.example.forgeprobe.ForgeProbe')
    }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $content=@(foreach($pair in @(@('old',$oldJar),@('new',$newJar))){
        $zip=[IO.Compression.ZipFile]::OpenRead($pair[1]);try {foreach($entry in $zip.Entries){if($entry.Name){$stream=$entry.Open();$sha=[Security.Cryptography.SHA256]::Create();try{$hash=[BitConverter]::ToString($sha.ComputeHash($stream)).Replace('-','')}finally{$stream.Dispose();$sha.Dispose()};[pscustomobject]@{Jar=$pair[0];Entry=$entry.FullName;Bytes=$entry.Length;SHA256=$hash}}}}finally{$zip.Dispose()}
    })
    Save-Text (Join-Path $evidence '2Ed-content-compare.json') ($content|ConvertTo-Json -Depth 4)
    foreach($entry in @('META-INF/MANIFEST.MF','META-INF/mods.toml')){$a=@($content|Where-Object Entry -eq $entry);if($a.Count -ne 2 -or $a[0].SHA256 -ne $a[1].SHA256){throw ('STOP: resource differs '+$entry)}}
    Save-Text (Join-Path $evidence '2Ec-output-fingerprints.json') (@((Fingerprint $newJar),(Fingerprint (Join-Path $work 'archive\forgeprobe-002-0e.jar')),(Fingerprint $harness))|ConvertTo-Json -Depth 4)
    Write-Output 'Build completed; inspect bytecode evidence before running.'
}
if($Phase -in @('archive-control','archive-treatment')){
    $label='spike002-0e-'+$Phase.Substring(8)
    $run='D:\DualForge\spike000\logs\'+$label
    if(-not(Test-Path -LiteralPath (Join-Path $run 'meta.json'))){throw 'STOP: no run meta'}
    Copy-Item -LiteralPath $dump -Destination (Join-Path $run 'ForgeProbe.rewritten.class')
    Save-Text (Join-Path $evidence ($label+'-dump.json')) ((Fingerprint (Join-Path $run 'ForgeProbe.rewritten.class'))|ConvertTo-Json)
    $mods=@(Get-ChildItem -LiteralPath 'D:\DualForge\spike000\neoforge-server\mods' -File|ForEach-Object {Fingerprint $_.FullName})
    Save-Text (Join-Path $evidence ($label+'-runtime-mods.json')) ($mods|ConvertTo-Json -Depth 4)
    $expectedProbe=if($Phase -eq 'archive-control'){(Get-FileHash -LiteralPath $oldJar).Hash}else{(Get-FileHash -LiteralPath (Join-Path $work 'build\forgeprobe.jar')).Hash}
    if($mods.Count -ne 2 -or @($mods|Where-Object SHA256 -eq $expectedProbe).Count -ne 1 -or @($mods|Where-Object SHA256 -eq $refs[0][1]).Count -ne 1){throw 'STOP: runtime mods differ'}
    if($Phase -eq 'archive-control' -and (Get-FileHash -LiteralPath $dump).Hash -ne '5E2F78DB2B0909D7368A185F618C3D080C00B732279B105CD5B8010FD66D9295'){throw 'STOP: control dump differs'}
    Write-Output ($label+' dump archived, runtime mods verified.')
}
if($Phase -eq 'final'){
    $records=@(foreach($r in $refs){$v=Fingerprint $r[0];if($v.SHA256 -ne $r[1]){throw ('STOP: final hash '+$r[0])};$v})
    $records += Fingerprint 'D:\DualForge\spike000\run-server.ps1'
    if($records[-1].SHA256 -ne '84C7E2C06BF0A1D2AC0D0E2D5AD20482F1357668EC8807DAD642DA55944F77D0'){throw 'STOP: final harness differs'}
    $records += Fingerprint (Join-Path $evidence 'historical-ForgeProbe.rewritten.class')
    if($records[-1].SHA256 -ne '5E2F78DB2B0909D7368A185F618C3D080C00B732279B105CD5B8010FD66D9295'){throw 'STOP: archive dump differs'}
    Save-Text (Join-Path $evidence '2Ef-final-frozen-refs-recheck.json') ($records|ConvertTo-Json -Depth 4)
    Write-Output 'All frozen input and historical dump hashes unchanged; authorized harness hash verified.'
}
