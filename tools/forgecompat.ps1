#requires -Version 7.0
[CmdletBinding()]
param(
    [ValidateSet('build','smoke','regress')][string]$Mode = 'regress',
    [string]$OutputRoot = '',
    [string]$JdkHome = 'C:\Users\Albert\.jdks\ms-21.0.10',
    [string]$Jar = '',
    [ValidateRange(1,600)][int]$MaxWaitSeconds = 120
)
$ErrorActionPreference = 'Stop'
$repo = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$source = Join-Path $repo 'spike001a\compat-002-0l\src'
$originalServer = Join-Path $repo 'spike000\neoforge-server'
$probe = Join-Path $repo 'spike000\probe-002-0l\build\forgeprobe.jar'
$utf8 = [Text.UTF8Encoding]::new($false)
$stage = if ($Mode -eq 'smoke') { 'NOT_RUN' } else { 'BUILD' }
$run = $null
$exitCode = 0
$result = [ordered]@{ task='Convergence-001'; mode=$Mode; status='NOT RUN'; category='NOT_RUN'; exitCode=40; runDirectory=$null }

function Save-Json($path, $value) { [IO.File]::WriteAllText($path, ($value | ConvertTo-Json -Depth 12), $utf8) }
function Fingerprints([string[]]$paths) {
    @(foreach ($path in $paths) { $item=Get-Item -LiteralPath $path; [pscustomobject]@{Path=$item.FullName; SHA256=(Get-FileHash -LiteralPath $path).Hash; Bytes=$item.Length} })
}
function Native([string]$name, [string]$exe, [string[]]$arguments) {
    Save-Json (Join-Path $run ($name+'.command.json')) @{executable=$exe; arguments=$arguments; workingDirectory=$repo}
    $info=[Diagnostics.ProcessStartInfo]::new(); $info.FileName=$exe; $info.WorkingDirectory=$repo
    $info.UseShellExecute=$false; $info.CreateNoWindow=$true; $info.RedirectStandardOutput=$true; $info.RedirectStandardError=$true
    foreach ($arg in $arguments) { $info.ArgumentList.Add($arg) }
    $process=[Diagnostics.Process]::new(); $process.StartInfo=$info
    $stdout=[IO.MemoryStream]::new(); $stderr=[IO.MemoryStream]::new()
    try {
        $null=$process.Start()
        $outTask=$process.StandardOutput.BaseStream.CopyToAsync($stdout); $errTask=$process.StandardError.BaseStream.CopyToAsync($stderr)
        $process.WaitForExit(); $null=$outTask.GetAwaiter().GetResult(); $null=$errTask.GetAwaiter().GetResult()
        [IO.File]::WriteAllBytes((Join-Path $run ($name+'.stdout.txt')), $stdout.ToArray())
        [IO.File]::WriteAllBytes((Join-Path $run ($name+'.stderr.txt')), $stderr.ToArray())
        Save-Json (Join-Path $run ($name+'.exit.json')) @{exitCode=$process.ExitCode}
        if ($process.ExitCode -ne 0) { throw "$name exited $($process.ExitCode); see native stderr" }
    } finally { $stdout.Dispose(); $stderr.Dispose(); $process.Dispose() }
}
function Jar-Contents([string]$path) {
    $zip=[IO.Compression.ZipFile]::OpenRead($path)
    try {
        @(foreach ($entry in $zip.Entries) { if ($entry.Name) {
            $stream=$entry.Open(); $sha=[Security.Cryptography.SHA256]::Create()
            try { $hash=[Convert]::ToHexString($sha.ComputeHash($stream)) } finally { $stream.Dispose(); $sha.Dispose() }
            [pscustomobject]@{Entry=$entry.FullName; SHA256=$hash; Bytes=$entry.Length}
        } })
    } finally { $zip.Dispose() }
}
function Invoke-IsolatedHarness($path, $parameters) {
    # Dot-source only inside this function: its own Java process can be stopped on harness error.
    try { . $path @parameters | Tee-Object -FilePath (Join-Path $run 'harness-output.txt') }
    finally {
        if ($proc) { try { if (-not $proc.HasExited) { $proc.Kill(); $null=$proc.WaitForExit(10000) } } catch { Write-Warning $_ } }
        foreach ($event in @($evOut,$evErr)) { if ($event) { Unregister-Event -SourceIdentifier $event.Name -ErrorAction SilentlyContinue } }
        foreach ($writer in @($swOut,$swErr)) { if ($writer) { try { $writer.Dispose() } catch { } } }
    }
}

try {
    if (-not $OutputRoot) { $OutputRoot=Join-Path $repo 'out\forgecompat' }
    $output=[IO.Path]::GetFullPath($OutputRoot)
    $allowed=(Join-Path $repo 'out')+'\'
    if (-not $output.StartsWith($allowed,[StringComparison]::OrdinalIgnoreCase)) { throw 'OutputRoot must be below this workspace out directory' }
    $ancestor=$output
    while ($ancestor -and $ancestor.StartsWith($repo,[StringComparison]::OrdinalIgnoreCase)) {
        if (Test-Path -LiteralPath $ancestor) {
            if ((Get-Item -LiteralPath $ancestor).Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'Output ancestor is a reparse point' }
        }
        $ancestor=Split-Path -Parent $ancestor
    }
    $run=Join-Path $output ((Get-Date -Format 'yyyyMMdd-HHmmss')+'-'+[guid]::NewGuid().ToString('N').Substring(0,8))
    New-Item -ItemType Directory -Path $run | Out-Null
    $result.runDirectory=$run
    Save-Json (Join-Path $run 'invocation.json') @{mode=$Mode; source=$source; outputRoot=$output; jdk=$JdkHome; jar=$Jar; maxWaitSeconds=$MaxWaitSeconds; entry=(Fingerprints @($PSCommandPath))}
    $tools=@('java','javac','jar') | ForEach-Object { Join-Path $JdkHome ('bin\'+$_+'.exe') }
    foreach ($tool in $tools) { if (-not (Test-Path -LiteralPath $tool -PathType Leaf)) { throw "Missing tool $tool" } }
    Native 'java-version' $tools[0] @('-version')
    $version=(Get-Content -LiteralPath (Join-Path $run 'java-version.stderr.txt') -Raw)+(Get-Content -LiteralPath (Join-Path $run 'java-version.stdout.txt') -Raw)
    if ($version -notmatch 'version "21\.0\.10"' -or $version -notmatch 'Microsoft') { throw 'Requires the fixed Microsoft JDK 21.0.10' }
    $dependencies=@(
        'net\neoforged\fancymodloader\loader\4.0.45\loader-4.0.45.jar',
        'cpw\mods\securejarhandler\3.0.8\securejarhandler-3.0.8.jar',
        'cpw\mods\modlauncher\11.0.5\modlauncher-11.0.5.jar',
        'org\slf4j\slf4j-api\2.0.9\slf4j-api-2.0.9.jar',
        'org\apache\maven\maven-artifact\3.8.5\maven-artifact-3.8.5.jar',
        'org\ow2\asm\asm\9.10.1\asm-9.10.1.jar',
        'org\ow2\asm\asm-tree\9.10.1\asm-tree-9.10.1.jar',
        'net\neoforged\bus\8.0.5\bus-8.0.5.jar'
    ) | ForEach-Object { Join-Path ($originalServer+'\libraries') $_ }
    foreach ($dependency in $dependencies) { if (-not (Test-Path -LiteralPath $dependency -PathType Leaf)) { throw "Missing dependency $dependency" } }

    if ($Mode -ne 'smoke') {
        Write-Output "BUILD from $source into $run"
        $inputs=@(Get-ChildItem -LiteralPath $source -File -Recurse | Sort-Object FullName)
        if (@($inputs | Where-Object Extension -eq '.class').Count) { throw 'Precompiled class in source tree; clean source build required' }
        $inputHashes=Fingerprints (@($inputs.FullName)+@($dependencies)+@($tools))
        Save-Json (Join-Path $run 'build-inputs.json') $inputHashes
        $classes=Join-Path $run 'classes'; $jarRoot=Join-Path $run 'jar-root'
        New-Item -ItemType Directory -Path $classes,$jarRoot | Out-Null
        $javaSources=@(Get-ChildItem -LiteralPath ($source+'\main\java') -File -Recurse -Filter '*.java' | Sort-Object FullName | Select-Object -ExpandProperty FullName)
        if (-not $javaSources.Count) { throw 'No Java source files' }
        Native 'javac' $tools[1] (@('--release','21','-encoding','UTF-8','-classpath',($dependencies -join ';'),'-d',$classes)+$javaSources)
        Copy-Item -Path ($classes+'\*') -Destination $jarRoot -Recurse
        Copy-Item -Path ($source+'\main\resources\*') -Destination $jarRoot -Recurse
        $Jar=Join-Path $run 'forgecompat-002-0l.jar'
        Native 'jar' $tools[2] @('--create','--file',$Jar,'--manifest',($source+'\main\resources\META-INF\MANIFEST.MF'),'-C',$jarRoot,'.')
        foreach ($input in $inputHashes) { if ((Get-FileHash -LiteralPath $input.Path).Hash -ne $input.SHA256) { throw "Build input changed: $($input.Path)" } }
        $contents=Jar-Contents $Jar; Save-Json (Join-Path $run 'jar-contents.json') $contents
        $oldJar=Join-Path $repo 'spike001a\compat-002-0l\build\forgecompat-002-0l.jar'
        if (Test-Path -LiteralPath $oldJar) {
            $oldContents=Jar-Contents $oldJar
            $differences=@($contents | Where-Object { $entry=$_; @($oldContents | Where-Object {$_.Entry -eq $entry.Entry -and $_.SHA256 -eq $entry.SHA256}).Count -ne 1 })
            Save-Json (Join-Path $run 'historical-content-comparison.json') @{historical=(Fingerprints @($oldJar)); newContentCount=$contents.Count; historicalContentCount=$oldContents.Count; changedOrAdded=$differences; removed=@($oldContents | Where-Object Entry -notin $contents.Entry)}
        }
        $result.build=@{status='PASS'; source=$source; javaCount=$javaSources.Count; sourceAndResourceCount=$inputs.Count; jar=(Fingerprints @($Jar))[0]; cleanOutput=$true}
        Save-Json (Join-Path $run 'build-result.json') $result.build
    }

    if ($Mode -ne 'build') {
        $stage='NOT_RUN'
        if (-not $Jar -or -not (Test-Path -LiteralPath $Jar -PathType Leaf)) { throw 'Smoke requires an existing built jar via -Jar or regress mode' }
        if ((Split-Path -Leaf $Jar) -ne 'forgecompat-002-0l.jar') { throw 'Keep the baseline jar filename: its automatic module version is filename-derived' }
        if (-not (Test-Path -LiteralPath $probe)) { throw 'Missing fixed 0l probe' }
        if (Get-Process java -ErrorAction SilentlyContinue) { throw 'A Java process exists; isolate without interrupting another run' }
        if (@([Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners() | Where-Object Port -eq 25565).Count) { throw 'Port 25565 is in use; smoke NOT RUN' }
        $scratch=@(
            (Join-Path $repo 'spike000\logs\spike002-0b-2a-v1.1\ForgeProbe.rewritten.class'),
            (Join-Path $repo 'spike000\logs\spike002-0l-current\ForgeProbe.class'),
            (Join-Path $repo 'spike000\logs\spike002-0l-current\ProbeEvent.class')
        )
        $protectedRoots=@('spike000','spike001a','spike001b5','Dev Manuals') | ForEach-Object { Join-Path $repo $_ }
        $protectedFiles=@(Get-ChildItem -LiteralPath $protectedRoots -File -Recurse | Select-Object -ExpandProperty FullName)
        $protected=Fingerprints $protectedFiles
        Save-Json (Join-Path $run 'preserved-before.json') $protected
        $smokeInputs=Fingerprints @($Jar,$probe); Save-Json (Join-Path $run 'smoke-inputs.json') $smokeInputs
        $server=Join-Path $run 'server'
        New-Item -ItemType Directory -Path $server | Out-Null
        foreach ($folder in @('libraries','config','defaultconfigs','spike000world')) {
            $from=Join-Path $originalServer $folder
            if (-not (Test-Path -LiteralPath $from)) { throw "Missing isolated server input $from" }
            if (@(Get-ChildItem -LiteralPath $from -Force -Recurse | Where-Object {$_.Attributes -band [IO.FileAttributes]::ReparsePoint}).Count -or ((Get-Item -LiteralPath $from).Attributes -band [IO.FileAttributes]::ReparsePoint)) { throw 'Server copy input contains a reparse point' }
            Copy-Item -LiteralPath $from -Destination (Join-Path $server $folder) -Recurse
        }
        $harness=Join-Path $run 'run-server.ps1'
        Copy-Item -LiteralPath (Join-Path $repo 'spike000\run-server.ps1') -Destination $harness
        $cached=@(for ($i=0; $i -lt $scratch.Count; $i++) {
            $path=$scratch[$i]; $exists=Test-Path -LiteralPath $path
            if ($exists -and ((Get-Item -LiteralPath $path).Attributes -band [IO.FileAttributes]::ReparsePoint)) { throw 'Legacy scratch is a reparse point' }
            $backup=Join-Path $run ('scratch-'+$i+'.original.class')
            if ($exists) { $item=Get-Item -LiteralPath $path; Copy-Item -LiteralPath $path -Destination $backup }
            [pscustomobject]@{Path=$path; Existed=$exists; Backup=$backup; LastWriteTimeUtc=if($exists){$item.LastWriteTimeUtc}else{$null}; CreationTimeUtc=if($exists){$item.CreationTimeUtc}else{$null}}
        })
        Save-Json (Join-Path $run 'scratch-backups.json') $cached
        $stage='STARTUP'
        $label='spike002-0l-treatment'; $logRoot=Join-Path $run 'logs'; $logDir=Join-Path $logRoot $label
        $parameters=@{Label=$label; ServerDir=$server; OutDir=$logRoot; JavaExe=$tools[0]; ModsJar=$probe; ExtraModsJars=@($Jar); ExtraJvmArgs=@('-Dforgecompat.2a.rewrite=true','-Dforgecompat.0g.apiRewrite=true','-Dforgecompat.0j.emptyRegistration=true','-Dforgecompat.0l.subscriberRewrite=true'); MaxWaitSeconds=$MaxWaitSeconds}
        Save-Json (Join-Path $run 'smoke-command.json') @{harness=$harness; parameters=$parameters; harnessSHA256=(Get-FileHash -LiteralPath $harness).Hash}
        try {
            Write-Output "SMOKE in isolated $server"
            Invoke-IsolatedHarness $harness $parameters
        } finally {
            foreach ($cache in $cached) {
                $dumpName=Split-Path -Leaf $cache.Path
                if (Test-Path -LiteralPath $cache.Path) { Copy-Item -LiteralPath $cache.Path -Destination (Join-Path $run ('observed-'+$dumpName)) }
                if ($cache.Existed) {
                    Copy-Item -LiteralPath $cache.Backup -Destination $cache.Path -Force
                    [IO.File]::SetLastWriteTimeUtc($cache.Path,$cache.LastWriteTimeUtc); [IO.File]::SetCreationTimeUtc($cache.Path,$cache.CreationTimeUtc)
                    $restored=Get-Item -LiteralPath $cache.Path
                    if ((Get-FileHash -LiteralPath $cache.Path).Hash -ne (Get-FileHash -LiteralPath $cache.Backup).Hash -or $restored.LastWriteTimeUtc -ne $cache.LastWriteTimeUtc -or $restored.CreationTimeUtc -ne $cache.CreationTimeUtc) { throw "Scratch restoration failed: $($cache.Path)" }
                } elseif (Test-Path -LiteralPath $cache.Path) { Remove-Item -LiteralPath $cache.Path }
            }
            $currentFiles=@(Get-ChildItem -LiteralPath $protectedRoots -File -Recurse | Select-Object -ExpandProperty FullName)
            if ($currentFiles.Count -ne $protectedFiles.Count) { throw 'Original workspace file set changed during smoke' }
            foreach ($item in $protected) { if ((Get-FileHash -LiteralPath $item.Path).Hash -ne $item.SHA256) { throw "Original file changed: $($item.Path)" } }
            foreach ($item in $smokeInputs) { if ((Get-FileHash -LiteralPath $item.Path).Hash -ne $item.SHA256) { throw 'Smoke input changed' } }
            Save-Json (Join-Path $run 'preservation-result.json') @{status='PASS'; filesVerified=$protected.Count; scratchRestored=$cached.Count; scratchContentsAndTimestampsVerified=$true}
        }
        $meta=Get-Content -LiteralPath (Join-Path $logDir 'meta.json') -Raw | ConvertFrom-Json
        if ($meta.status -ne 'DONE_STOPPED_GRACEFULLY' -or $meta.exitCode -ne 0) { throw "Startup failed: $($meta.status), JVM exit $($meta.exitCode)" }
        $stage='ASSERTION'
        $stdout=Get-Content -LiteralPath (Join-Path $logDir ($label+'.stdout.log')) -Raw
        $debug=Get-Content -LiteralPath (Join-Path $logDir ($label+'.debug.log')) -Raw
        $assertions=[ordered]@{}
        foreach ($pattern in @('FORGE PROBE CONSTRUCTOR REACHED','SUBSCRIBER enabled=true annotationHits=1','EVENT_PROFILE enabled=true superHits=1 constructorHits=1','register returned target=com.example.forgeprobe.ForgeProbe declaredMethods=1','delegate -> builtin javafml for neoforge','NeoForge mod loading, version 21.1.256','Done \(')) { $assertions[$pattern]=[bool]($stdout -match $pattern) }
        $assertions['no legacy empty no-op']=($stdout -notmatch 'legacy-empty-noop')
        $assertions['no ERROR/FATAL or dump FAIL']=$true
        foreach ($suffix in @('.stdout.log','.log','.debug.log')) { if ((Get-Content -LiteralPath (Join-Path $logDir ($label+$suffix)) -Raw) -match '\b(ERROR|FATAL)\b|dump FAIL') { $assertions['no ERROR/FATAL or dump FAIL']=$false } }
        $assertions['stderr empty']=((Get-Item -LiteralPath (Join-Path $logDir ($label+'.stderr.log'))).Length -eq 0)
        $assertions['no crash report']=(@(Get-ChildItem -LiteralPath (Join-Path $logDir 'crash-reports') -File -Recurse -ErrorAction SilentlyContinue).Count -eq 0)
        $mods=@(Get-ChildItem -LiteralPath (Join-Path $server 'mods') -File)
        $assertions['exact built jar and fixed probe loaded']=($mods.Count -eq 2 -and (Get-FileHash -LiteralPath (Join-Path $server 'mods\forgecompat-002-0l.jar')).Hash -eq $smokeInputs[0].SHA256 -and (Get-FileHash -LiteralPath (Join-Path $server 'mods\forgeprobe.jar')).Hash -eq $smokeInputs[1].SHA256 -and $meta.extraModsJars -contains $Jar)
        $assertions['isolated artifact service discovery']=($debug -match ('Found additional transformation services from discovery services: \['+[regex]::Escape((Join-Path $server 'mods\forgecompat-002-0l.jar'))+'\]') -and $debug -match 'Loaded service forgecompat-transform' -and $debug -match 'ForgeCompatLanguageLoader from forgecompat-002-0l.jar')
        Save-Json (Join-Path $run 'assertions.json') $assertions
        Save-Json (Join-Path $run 'loaded-mods.json') (Fingerprints @($mods.FullName))
        if ($assertions.Values -contains $false) { throw 'Smoke assertion failed; see assertions.json and complete logs' }
        $result.smoke=@{status='PASS'; meta=$meta; assertions=$assertions; scope='fixed 0l profile; native NeoForge loading; not general event compatibility'}
    }
    $result.status='PASS'; $result.category='PASS'; $result.exitCode=0
} catch {
    $exitCode=switch ($stage) { 'BUILD' {10}; 'STARTUP' {20}; 'ASSERTION' {30}; default {40} }
    $result.status=if ($exitCode -eq 40 -and $result.build) {'PARTIAL'} elseif ($exitCode -eq 40) {'NOT RUN'} else {'FAIL'}
    $result.category=$stage; $result.exitCode=$exitCode; $result.error=($_ | Out-String)
    if ($run) { [IO.File]::WriteAllText((Join-Path $run 'failure.txt'),$result.error,$utf8) }
    Write-Output ($result.status+': '+$_.Exception.Message)
} finally {
    if ($run) { Save-Json (Join-Path $run 'result.json') $result; Write-Output ("RESULT $($result.status) exit=$($result.exitCode) $run") }
}
exit $exitCode
