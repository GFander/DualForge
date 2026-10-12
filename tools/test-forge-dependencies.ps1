#requires -Version 7.0
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$CompatJar,
    [ValidateSet('before','after')][string]$Stage='after',
    [switch]$BoundaryOnly,
    [string]$OutputRoot=''
)
$ErrorActionPreference='Stop'
$repo=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$jdk='C:\Users\Albert\.jdks\ms-21.0.10\bin'
$original=Join-Path $repo 'spike000\neoforge-server'
$probe=Join-Path $repo 'spike000\probe-002-0l\build\forgeprobe.jar'
$fixtures=Join-Path $repo 'tests\dependencies'
$utf8=[Text.UTF8Encoding]::new($false)
if(-not $OutputRoot){$OutputRoot=Join-Path $repo 'out\convergence-002\dependencies'}
$output=[IO.Path]::GetFullPath($OutputRoot)
if(-not $output.StartsWith(($repo+'\out\'),[StringComparison]::OrdinalIgnoreCase)){throw 'OutputRoot must be under workspace out'}
$ancestor=$output
while($ancestor -and $ancestor.StartsWith($repo,[StringComparison]::OrdinalIgnoreCase)){
    if((Test-Path -LiteralPath $ancestor) -and ((Get-Item -LiteralPath $ancestor).Attributes -band [IO.FileAttributes]::ReparsePoint)){throw 'Output reparse point'}
    $ancestor=Split-Path -Parent $ancestor
}
$run=Join-Path $output ((Get-Date -Format 'yyyyMMdd-HHmmss')+'-'+[guid]::NewGuid().ToString('N').Substring(0,8))
New-Item -ItemType Directory -Path $run | Out-Null
function Json($path,$value){[IO.File]::WriteAllText($path,($value | ConvertTo-Json -Depth 12),$utf8)}
function Hashes($paths){@(foreach($path in $paths){[pscustomobject]@{Path=$path; SHA256=(Get-FileHash -LiteralPath $path).Hash}})}
function Native($name,$exe,$arguments){
    Json (Join-Path $run ($name+'.command.json')) @{exe=$exe;arguments=$arguments}
    & $exe @arguments 1> (Join-Path $run ($name+'.stdout.log')) 2> (Join-Path $run ($name+'.stderr.log'))
    Json (Join-Path $run ($name+'.exit.json')) @{exitCode=$LASTEXITCODE}
    if($LASTEXITCODE -ne 0){throw "$name failed; see native logs"}
}
function Harness($path,$parameters){
    try{. $path @parameters | Tee-Object -FilePath ($parameters.OutDir+'\harness-output.txt')}
    finally{
        if($proc){try{if(-not $proc.HasExited){$proc.Kill();$null=$proc.WaitForExit(10000)}}catch{Write-Warning $_}}
        foreach($event in @($evOut,$evErr)){if($event){Unregister-Event -SourceIdentifier $event.Name -ErrorAction SilentlyContinue}}
        foreach($writer in @($swOut,$swErr)){if($writer){try{$writer.Dispose()}catch{}}}
    }
}
$result=[ordered]@{task='Convergence-002';stage=$Stage;status='NOT RUN';directory=$run;cases=@()}
try{
    $CompatJar=(Resolve-Path -LiteralPath $CompatJar).Path
    if((Split-Path -Leaf $CompatJar) -ne 'forgecompat-002-0l.jar'){throw 'Keep current module filename'}
    $libs=@(Get-ChildItem -LiteralPath ($original+'\libraries') -Filter '*.jar' -Recurse -File | Sort-Object FullName | Select-Object -ExpandProperty FullName)
    $inputs=Hashes (@($CompatJar,$probe,$PSCommandPath)+@(Get-ChildItem -LiteralPath $fixtures -File | Select-Object -ExpandProperty FullName)+$libs)
    Json (Join-Path $run 'inputs.json') $inputs
    $cp=(@($CompatJar)+$libs) -join ';'
    $classes=Join-Path $run 'classes';New-Item -ItemType Directory -Path $classes | Out-Null
    Native 'javac' ($jdk+'\javac.exe') @('--release','21','-encoding','UTF-8','-classpath',$cp,'-d',$classes,($fixtures+'\DependencyContractTest.java'))
    # SecureJar is on the classpath only in this standalone boundary-test JVM.
    # The real server continues to use its UNCHANGED named-module win_args.txt.
    Native 'boundary' ($jdk+'\java.exe') @('--add-opens','java.base/java.lang.invoke=ALL-UNNAMED','--add-exports','java.base/sun.security.util=ALL-UNNAMED','-classpath',($classes+';'+$cp),'com.example.forgecompat.DependencyContractTest',$Stage,$fixtures,$probe)
    $result.boundary='PASS'
    if(-not $BoundaryOnly){
        if(Get-Process java -ErrorAction SilentlyContinue){throw 'Other Java process; NOT RUN'}
        if(@([Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners() | Where-Object Port -eq 25565).Count){throw 'Port in use; NOT RUN'}
        $roots=@('spike000','spike001a','spike001b5','Dev Manuals') | ForEach-Object {Join-Path $repo $_}
        $protected=Hashes @(Get-ChildItem -LiteralPath $roots -Recurse -File | Select-Object -ExpandProperty FullName)
        Json (Join-Path $run 'protected-before.json') $protected
        $caseNames=if($Stage -eq 'before'){@('D-missing-required')}else{@('C-satisfied','D-missing-required','E-missing-optional','F-wrong-version')}
        foreach($name in $caseNames){
            $caseDir=Join-Path $run $name;New-Item -ItemType Directory -Path $caseDir | Out-Null
            $fixture=Join-Path $caseDir 'forgeprobe.jar';$jarRoot=Join-Path $caseDir 'jar-root'
            [IO.Compression.ZipFile]::ExtractToDirectory($probe,$jarRoot)
            Copy-Item -LiteralPath ($fixtures+'\'+$name+'.toml') -Destination ($jarRoot+'\META-INF\mods.toml')
            # JDK jar preserves Java JarInputStream compatibility; no classes are recompiled.
            Native ($name+'-jar') ($jdk+'\jar.exe') @('--create','--file',$fixture,'--manifest',($jarRoot+'\META-INF\MANIFEST.MF'),'-C',$jarRoot,'.')
            $classProof=@();$oldZip=[IO.Compression.ZipFile]::OpenRead($probe);$newZip=[IO.Compression.ZipFile]::OpenRead($fixture)
            try{foreach($entry in $oldZip.Entries | Where-Object {$_.FullName.EndsWith('.class')}){
                $a=$entry.Open();$b=$newZip.GetEntry($entry.FullName).Open();$sha=[Security.Cryptography.SHA256]::Create()
                try{$same=([Convert]::ToHexString($sha.ComputeHash($a)) -eq [Convert]::ToHexString($sha.ComputeHash($b)));$classProof+=@{entry=$entry.FullName;unchanged=$same};if(-not $same){throw 'Fixture changed probe bytecode'}}finally{$a.Dispose();$b.Dispose();$sha.Dispose()}
            }}finally{$oldZip.Dispose();$newZip.Dispose()}
            Json (Join-Path $caseDir 'fixture.json') @{input=($fixtures+'\'+$name+'.toml');jar=(Hashes @($fixture));classes=$classProof}
            $server=Join-Path $caseDir 'server';New-Item -ItemType Directory -Path $server | Out-Null
            foreach($folder in @('libraries','config','defaultconfigs','spike000world')){
                $from=Join-Path $original $folder
                if(((Get-Item -LiteralPath $from).Attributes -band [IO.FileAttributes]::ReparsePoint) -or @(Get-ChildItem -LiteralPath $from -Recurse -Force | Where-Object {$_.Attributes -band [IO.FileAttributes]::ReparsePoint}).Count){throw 'Server input link'}
                Copy-Item -LiteralPath $from -Destination (Join-Path $server $folder) -Recurse
            }
            $harness=Join-Path $caseDir 'run-server.ps1';Copy-Item -LiteralPath ($repo+'\spike000\run-server.ps1') -Destination $harness
            $scratch=@('spike000\logs\spike002-0b-2a-v1.1\ForgeProbe.rewritten.class','spike000\logs\spike002-0l-current\ForgeProbe.class','spike000\logs\spike002-0l-current\ProbeEvent.class') | ForEach-Object {Join-Path $repo $_}
            $cache=@(for($i=0;$i -lt $scratch.Count;$i++){$p=$scratch[$i];$exists=Test-Path -LiteralPath $p;$backup=Join-Path $caseDir ('cache-'+$i+'.class');if($exists){$item=Get-Item -LiteralPath $p;if($item.Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'Scratch link'};Copy-Item -LiteralPath $p -Destination $backup};[pscustomobject]@{Path=$p;Existed=$exists;Backup=$backup;Creation=if($exists){$item.CreationTimeUtc}else{$null};Write=if($exists){$item.LastWriteTimeUtc}else{$null}}})
            Json (Join-Path $caseDir 'cache-before.json') $cache
            $label='spike002-0l-treatment';$logs=Join-Path $caseDir 'logs';New-Item -ItemType Directory -Path $logs | Out-Null
            $parameters=@{Label=$label;ServerDir=$server;OutDir=$logs;JavaExe=($jdk+'\java.exe');ModsJar=$fixture;ExtraModsJars=@($CompatJar);ExtraJvmArgs=@('-Dforgecompat.2a.rewrite=true','-Dforgecompat.0g.apiRewrite=true','-Dforgecompat.0j.emptyRegistration=true','-Dforgecompat.0l.subscriberRewrite=true');MaxWaitSeconds=120}
            Json (Join-Path $caseDir 'run-command.json') @{parameters=$parameters;harness=(Hashes @($harness))}
            try{Harness $harness $parameters}finally{
                foreach($c in $cache){if(Test-Path -LiteralPath $c.Path){Copy-Item -LiteralPath $c.Path -Destination (Join-Path $caseDir ('observed-'+(Split-Path -Leaf $c.Path)))}
                    if($c.Existed){Copy-Item -LiteralPath $c.Backup -Destination $c.Path -Force;[IO.File]::SetCreationTimeUtc($c.Path,$c.Creation);[IO.File]::SetLastWriteTimeUtc($c.Path,$c.Write);$restored=Get-Item -LiteralPath $c.Path;if((Get-FileHash -LiteralPath $c.Path).Hash -ne (Get-FileHash -LiteralPath $c.Backup).Hash -or $restored.CreationTimeUtc -ne $c.Creation -or $restored.LastWriteTimeUtc -ne $c.Write){throw 'Cache restore failed'}}elseif(Test-Path -LiteralPath $c.Path){Remove-Item -LiteralPath $c.Path}
                }
                if(@(Get-ChildItem -LiteralPath $roots -Recurse -File).Count -ne $protected.Count){throw 'Protected file set changed'}
                foreach($p in $protected){if((Get-FileHash -LiteralPath $p.Path).Hash -ne $p.SHA256){throw "Protected file changed: $($p.Path)"}}
                Json (Join-Path $caseDir 'preservation.json') @{status='PASS';files=$protected.Count;cachesRestored=3;contentsAndTimestamps=$true}
            }
            $logDir=Join-Path $logs $label;$meta=Get-Content -LiteralPath ($logDir+'\meta.json') -Raw | ConvertFrom-Json
            $stdout=Get-Content -LiteralPath ($logDir+'\'+$label+'.stdout.log') -Raw
            $debug=Get-Content -LiteralPath ($logDir+'\'+$label+'.debug.log') -Raw
            $assert=[ordered]@{exactFixtureInstalled=((Get-FileHash -LiteralPath ($server+'\mods\forgeprobe.jar')).Hash -eq (Get-FileHash -LiteralPath $fixture).Hash);exactCompatInstalled=((Get-FileHash -LiteralPath ($server+'\mods\forgecompat-002-0l.jar')).Hash -eq (Get-FileHash -LiteralPath $CompatJar).Hash)}
            $negative=($Stage -eq 'after' -and ($name -eq 'D-missing-required' -or $name -eq 'F-wrong-version'))
            if($negative){
                $target=if($name.StartsWith('D-')){'convergence_missing'}else{'minecraft'}
                $actual=if($name.StartsWith('D-')){'[MISSING]'}else{'1.21.1'}
                $assert.specificDependencyRejected=($debug -match 'Missing or unsupported mandatory dependencies' -and $debug.Contains("Mod ID: '$target', Requested by: 'forgeprobe'") -and $debug.Contains("Actual version: '$actual'"))
                $assert.preLoadingFatal=($stdout -match 'Error during pre-loading phase' -and $stdout -match 'requires' -and $stdout -match $target)
                $assert.noTargetInstantiation=($stdout -notmatch 'FORGE PROBE CONSTRUCTOR REACHED|register returned target=com.example.forgeprobe.ForgeProbe|loadMod called for forgeprobe')
                $assert.noDone=($null -eq $meta.doneLine -and $stdout -notmatch 'Done \(')
                $assert.exitedBeforeDone=($meta.status -eq 'EXITED_EARLY')
            }else{
                $assert.doneAndStopped=($meta.status -eq 'DONE_STOPPED_GRACEFULLY' -and $meta.exitCode -eq 0)
                foreach($pattern in @('FORGE PROBE CONSTRUCTOR REACHED','register returned target=com.example.forgeprobe.ForgeProbe declaredMethods=1','NeoForge mod loading, version 21.1.256','SUBSCRIBER enabled=true annotationHits=1','EVENT_PROFILE enabled=true superHits=1 constructorHits=1')){$assert[$pattern]=[bool]($stdout -match $pattern)}
                $assert.noErrors=($stdout -notmatch '\b(ERROR|FATAL)\b|dump FAIL' -and $debug -notmatch '\b(ERROR|FATAL)\b|dump FAIL')
            }
            Json (Join-Path $caseDir 'assertions.json') $assert
            if($assert.Values -contains $false){throw "Case $name failed assertions"}
            $result.cases+=@{name=$name;status='PASS';negative=$negative;meta=$meta;assertions=$assert}
            Write-Output "CASE $name PASS (expected rejection=$negative)"
        }
    }
    foreach($input in $inputs){if((Get-FileHash -LiteralPath $input.Path).Hash -ne $input.SHA256){throw 'Test input changed'}}
    $result.status=if($Stage -eq 'before'){'REPRODUCED'}else{'PASS'}
}catch{$result.status='FAIL';$result.error=($_ | Out-String);[IO.File]::WriteAllText((Join-Path $run 'failure.txt'),$result.error,$utf8)}
finally{Json (Join-Path $run 'result.json') $result;Write-Output "RESULT $($result.status) $run"}
if($result.status -eq 'FAIL'){exit 1}
