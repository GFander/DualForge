<#
    Spike-000 controlled NeoForge 21.1.256 server run.

    Runs the frozen NeoForge 21.1.256 dedicated server once, with a fully
    deterministic configuration, and captures:
      - launcher stdout / stderr
      - logs/latest.log, logs/debug.log
      - crash-reports/ (if any)
      - mods/ listing
      - meta.json (start/stop time, exit code, java version, status)

    The mods/ directory is emptied first, so the only difference between the
    baseline run and the forgeprobe run is the -ModsJar argument.
#>
param(
    [Parameter(Mandatory = $true)][ValidateSet('baseline', 'forgeprobe', 'spike001a-0', 'spike001b-1', 'spike001b-3', 'spike001b-3b', 'spike001b-5', 'spike001b-6', 'spike001c-0', 'spike001c-1', 'spike001c-2', 'spike002-0a', 'spike002-0b-2a-control', 'spike002-0b-2a', 'spike002-0b-2a-control-v1.1', 'spike002-0b-2a-v1.1', 'spike002-0c', 'spike002-0d-control', 'spike002-0d-treatment', 'spike002-0e-control', 'spike002-0e-treatment')][string]$Label,
    [string]$ModsJar = '',
    [string[]]$ExtraModsJars = @(),
    [string[]]$ExtraJvmArgs = @(),
    [string]$ServerDir = 'D:\DualForge\spike000\neoforge-server',
    [string]$OutDir = 'D:\DualForge\spike000\logs',
    [string]$JavaExe = 'C:\Users\Albert\.jdks\ms-21.0.10\bin\java.exe',
    [int]$MaxWaitSeconds = 600,
    [int]$PostDoneSeconds = 10
)

$ErrorActionPreference = 'Stop'

$runDir = Join-Path $OutDir $Label
if (Test-Path -LiteralPath $runDir) { Remove-Item -Recurse -Force -LiteralPath $runDir }
New-Item -ItemType Directory -Force -Path $runDir | Out-Null

# ---------------------------------------------------------------- config ----
# Identical, deterministic server configuration for every run.
Set-Content -LiteralPath (Join-Path $ServerDir 'eula.txt') -Value 'eula=true' -Encoding ascii

$props = @(
    'allow-flight=false',
    'allow-nether=false',
    'difficulty=peaceful',
    'enable-command-block=false',
    'enable-rcon=false',
    'enforce-secure-profile=false',
    'gamemode=creative',
    'generate-structures=false',
    'hardcore=false',
    'level-name=spike000world',
    'level-type=minecraft\:flat',
    'max-players=1',
    'motd=Spike-000',
    'online-mode=false',
    'player-idle-timeout=0',
    'prevent-proxy-connections=false',
    'pvp=false',
    'query.port=25565',
    'server-ip=',
    'server-port=25565',
    'simulation-distance=4',
    'spawn-animals=false',
    'spawn-monsters=false',
    'spawn-npcs=false',
    'sync-chunk-writes=false',
    'view-distance=4',
    'white-list=false'
)
Set-Content -LiteralPath (Join-Path $ServerDir 'server.properties') -Value $props -Encoding ascii
$jvmArgs = @('-Xmx3G', '-Xms1G') + $ExtraJvmArgs
Set-Content -LiteralPath (Join-Path $ServerDir 'user_jvm_args.txt') -Value ($jvmArgs -join ' ') -Encoding ascii

# ------------------------------------------------------------- mods state ---
$modsDir = Join-Path $ServerDir 'mods'
New-Item -ItemType Directory -Force -Path $modsDir | Out-Null
Get-ChildItem -LiteralPath $modsDir -Filter '*.jar' -File -ErrorAction SilentlyContinue |
    Remove-Item -Force
if ($ModsJar -ne '') {
    Copy-Item -LiteralPath $ModsJar -Destination (Join-Path $modsDir (Split-Path -Leaf $ModsJar)) -Force
}
foreach ($extra in $ExtraModsJars) {
    if ($extra -ne '') {
        Copy-Item -LiteralPath $extra -Destination (Join-Path $modsDir (Split-Path -Leaf $extra)) -Force
    }
}
$modsListing = @(Get-ChildItem -LiteralPath $modsDir -File -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Name)

# fresh log / crash dirs so "no crash report" is meaningful
foreach ($d in 'logs', 'crash-reports') {
    $p = Join-Path $ServerDir $d
    if (Test-Path -LiteralPath $p) { Remove-Item -Recurse -Force -LiteralPath $p }
}

# ---------------------------------------------------------------- launch ----
$stdoutPath = Join-Path $runDir "$Label.stdout.log"
$stderrPath = Join-Path $runDir "$Label.stderr.log"

$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = $JavaExe
$psi.Arguments = '@user_jvm_args.txt @libraries/net/neoforged/neoforge/21.1.256/win_args.txt nogui'
$psi.WorkingDirectory = $ServerDir
$psi.UseShellExecute = $false
$psi.RedirectStandardInput = $true
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$psi.StandardOutputEncoding = New-Object System.Text.UTF8Encoding($false)
$psi.StandardErrorEncoding = New-Object System.Text.UTF8Encoding($false)

$proc = New-Object System.Diagnostics.Process
$proc.StartInfo = $psi

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$swOut = New-Object System.IO.StreamWriter($stdoutPath, $false, $utf8NoBom)
$swErr = New-Object System.IO.StreamWriter($stderrPath, $false, $utf8NoBom)
$swOut.AutoFlush = $true
$swErr.AutoFlush = $true

$startedAt = Get-Date
$null = $proc.Start()

$evOut = Register-ObjectEvent -InputObject $proc -EventName OutputDataReceived -MessageData $swOut -Action {
    if ($null -ne $EventArgs.Data) { $Event.MessageData.WriteLine($EventArgs.Data) }
}
$evErr = Register-ObjectEvent -InputObject $proc -EventName ErrorDataReceived -MessageData $swErr -Action {
    if ($null -ne $EventArgs.Data) { $Event.MessageData.WriteLine($EventArgs.Data) }
}
$proc.BeginOutputReadLine()
$proc.BeginErrorReadLine()

Write-Output "[$Label] started pid=$($proc.Id) at $($startedAt.ToString('o'))"
Write-Output "[$Label] mods/ = $($modsListing -join ', ')"

# ------------------------------------------------------------------ poll ----
$latestLog = Join-Path $ServerDir 'logs\latest.log'
$status = 'UNKNOWN'
$doneLine = $null
$deadline = (Get-Date).AddSeconds($MaxWaitSeconds)

while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 2
    if ($proc.HasExited) { $status = 'EXITED_EARLY'; break }
    if (Test-Path -LiteralPath $latestLog) {
        $txt = Get-Content -LiteralPath $latestLog -Raw -ErrorAction SilentlyContinue
        if ($txt -and $txt.Contains('Done (')) {
            $status = 'DONE'
            $doneLine = ($txt -split "`n" | Where-Object { $_ -match 'Done \(' } | Select-Object -First 1)
            break
        }
    }
}

if ($status -eq 'UNKNOWN') { $status = 'TIMEOUT' }

# ------------------------------------------------------------------ stop ----
if (-not $proc.HasExited) {
    if ($status -eq 'DONE') {
        Start-Sleep -Seconds $PostDoneSeconds
        try { $proc.StandardInput.WriteLine('stop'); $proc.StandardInput.Flush() } catch { }
        if ($proc.WaitForExit(120000)) { $status = 'DONE_STOPPED_GRACEFULLY' }
        else { $status = 'DONE_FORCED_KILL'; try { $proc.Kill() } catch { } }
    }
    else {
        $status = "$status`_FORCED_KILL"
        try { $proc.Kill() } catch { }
    }
}
$null = $proc.WaitForExit(30000)
Start-Sleep -Seconds 2

$exitCode = $null
try { $exitCode = $proc.ExitCode } catch { }
$endedAt = Get-Date

Unregister-Event -SourceIdentifier $evOut.Name -ErrorAction SilentlyContinue
Unregister-Event -SourceIdentifier $evErr.Name -ErrorAction SilentlyContinue
$swOut.Flush(); $swOut.Dispose()
$swErr.Flush(); $swErr.Dispose()

# ------------------------------------------------------------- artifacts ----
$serverLogs = Join-Path $ServerDir 'logs'
if (Test-Path -LiteralPath (Join-Path $serverLogs 'latest.log')) {
    Copy-Item -LiteralPath (Join-Path $serverLogs 'latest.log') -Destination (Join-Path $runDir "$Label.log") -Force
}
if (Test-Path -LiteralPath (Join-Path $serverLogs 'debug.log')) {
    Copy-Item -LiteralPath (Join-Path $serverLogs 'debug.log') -Destination (Join-Path $runDir "$Label.debug.log") -Force
}
$crashDir = Join-Path $ServerDir 'crash-reports'
if (Test-Path -LiteralPath $crashDir) {
    Copy-Item -LiteralPath $crashDir -Destination (Join-Path $runDir 'crash-reports') -Recurse -Force
}
$modsListing | Set-Content -LiteralPath (Join-Path $runDir "$Label.mods-list.txt") -Encoding utf8

$savedEap = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
$javaVersion = (& $JavaExe -version 2>&1 | Out-String).Trim()
$ErrorActionPreference = $savedEap

$meta = [ordered]@{
    label            = $Label
    status           = $status
    exitCode         = $exitCode
    startedAt        = $startedAt.ToString('o')
    endedAt          = $endedAt.ToString('o')
    durationSeconds  = [math]::Round(($endedAt - $startedAt).TotalSeconds, 1)
    doneLine         = $doneLine
    modsDir          = $modsListing
    modsJar          = $ModsJar
    extraModsJars    = $ExtraModsJars
    commandLine      = "$JavaExe $($psi.Arguments)"
    workingDirectory = $ServerDir
    javaVersion      = $javaVersion
    stdoutLog        = $stdoutPath
    stderrLog        = $stderrPath
    latestLog        = (Join-Path $runDir "$Label.log")
}
$meta | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $runDir 'meta.json') -Encoding utf8

Write-Output "[$Label] status=$status exitCode=$exitCode duration=$($meta.durationSeconds)s"
Write-Output "[$Label] doneLine=$doneLine"
Write-Output "[$Label] artifacts -> $runDir"
