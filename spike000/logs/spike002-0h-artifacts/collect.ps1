$ErrorActionPreference='Stop'
$evRoot=$PSScriptRoot
$utf8=[Text.UTF8Encoding]::new($false)
$refs=@(
 @('D:\DualForge\spike000\forge-server\libraries\net\minecraftforge\eventbus\6.2.27\eventbus-6.2.27.jar','D2A7BCECB80B33F90854A003DC295960263D78435D9D6016C427D711510CB999'),
 @('D:\DualForge\spike000\neoforge-server\libraries\net\neoforged\bus\8.0.5\bus-8.0.5.jar','705E24F37752809499A5CD0C7DD31EAABF3D933C5D3B2060168E94E9EF62593A'),
 @('D:\DualForge\spike000\probe-002-0e\build\forgeprobe.jar','D61224F3014F0ADFF85D10EEDEA414763C231367F08EDB99CE99DD2C8935F1C9'),
 @('D:\DualForge\spike001a\compat-002-0g\build\forgecompat-002-0g.jar','F0D51E46533A135A9ECA0FA859805E04E9FBB22E8759B1A29524319E37B4C739'),
 @('D:\DualForge\spike000\logs\spike002-0g-treatment\ForgeProbe.rewritten.class','A9ABBB9440DD49A16BB5749D46744F01D96FAF2796B189920BD8788682705EF1'),
 @('D:\DualForge\spike000\run-server.ps1','FBA0003CEDF843293008868948049F8E9B287A35F52C809D15DA236FF8CA9C8C'),
 @('D:\DualForge\Dev Manuals\Spike_002-0h.md','F745576DAF91C255482C44BF49835E94AAA0313B1A9DF59664E44B1E5E385BF1')
)
function Save-New([string]$name,[string]$body){$p=Join-Path $evRoot $name;if(Test-Path -LiteralPath $p){throw 'STOP evidence exists'};[IO.File]::WriteAllText($p,$body,$utf8)}
function Check-Refs{@(foreach($r in $refs){$h=(Get-FileHash -LiteralPath $r[0]).Hash;if($h -ne $r[1]){throw "STOP drift $($r[0])"};[pscustomobject]@{Path=$r[0];SHA256=$h;Bytes=(Get-Item -LiteralPath $r[0]).Length}})}
Save-New 'inputs-before.json' ((Check-Refs)|ConvertTo-Json -Depth 4)
$fRoot='D:\DualForge\spike000\logs\spike002-0f-artifacts'
$fIndex=Get-Content -LiteralPath ($fRoot+'\evidence-index.json') -Raw|ConvertFrom-Json
$reuse=@('forge-EventBus-code.stdout.txt','neo-EventBus-code.stdout.txt','probe-verbose-raw.stdout.txt','javap-version.stdout.txt','jar-version.stdout.txt')
$records=@(foreach($name in $reuse){$record=@($fIndex|Where-Object Path -EQ $name);$p=Join-Path $fRoot $name;if($record.Count -ne 1 -or (Get-FileHash -LiteralPath $p).Hash -ne $record[0].SHA256){throw 'STOP reuse fingerprint'};[pscustomobject]@{Source=$p;SHA256=$record[0].SHA256;Bytes=$record[0].Bytes;Reused=$true}})
$gRoot='D:\DualForge\spike000\logs\spike002-0g-treatment'
$gIndex=Get-Content -LiteralPath 'D:\DualForge\spike000\logs\spike002-0g-artifacts\run-files-index.json' -Raw|ConvertFrom-Json
$gLog=$gRoot+'\spike002-0g-treatment.log';$gRecord=@($gIndex|Where-Object Path -EQ $gLog)
if($gRecord.Count -ne 1 -or (Get-FileHash -LiteralPath $gLog).Hash -ne $gRecord[0].SHA256){throw 'STOP prior log drift'}
$records+=[pscustomobject]@{Source=$gLog;SHA256=$gRecord[0].SHA256;Bytes=$gRecord[0].Bytes;Reused=$true}
Save-New 'reused-sources.json' ($records|ConvertTo-Json -Depth 4)
$forge=[IO.File]::ReadAllText($fRoot+'\forge-EventBus-code.stdout.txt',$utf8)
$method=[regex]::Match($forge,'(?ms)^  private void registerObject\(java\.lang\.Object\);.*?(?=^  private void parentTypes)')
if(-not $method.Success){throw 'STOP missing full original method'}
Save-New 'forge-registerObject-full-excerpt.txt' $method.Value
$neo=[IO.File]::ReadAllText($fRoot+'\neo-EventBus-code.stdout.txt',$utf8)
$method=[regex]::Match($neo,'(?ms)^  public void register\(java\.lang\.Object\);.*?(?=^  private static void checkSupertypes)')
if(-not $method.Success){throw 'STOP missing full Neo method'}
Save-New 'neo-register-full-excerpt.txt' $method.Value
Save-New 'inputs-after.json' ((Check-Refs)|ConvertTo-Json -Depth 4)
$files=@(Get-ChildItem -LiteralPath $evRoot -File|Sort-Object Name|ForEach-Object{[pscustomobject]@{Path=$_.Name;Bytes=$_.Length;SHA256=(Get-FileHash -LiteralPath $_.FullName).Hash}})
Save-New 'evidence-index.json' ($files|ConvertTo-Json -Depth 4)
Write-Output ('Verified '+$records.Count+' reused sources and '+$refs.Count+' unchanged inputs; no tools rerun or JVM application launched.')
