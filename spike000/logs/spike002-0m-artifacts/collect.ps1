$ErrorActionPreference='Stop'
. 'D:\DualForge\spike000\logs\spike002-0j-artifacts\build-evidence.ps1'
$evRoot=$PSScriptRoot
$forge='D:\DualForge\spike000\forge-server\libraries\net\minecraftforge\eventbus\6.2.27\eventbus-6.2.27.jar'
$neo='D:\DualForge\spike000\neoforge-server\libraries\net\neoforged\bus\8.0.5\bus-8.0.5.jar'
$universal='D:\DualForge\spike000\neoforge-server\libraries\net\neoforged\neoforge\21.1.256\neoforge-21.1.256-universal.jar'
$refs=@(@($forge,'D2A7BCECB80B33F90854A003DC295960263D78435D9D6016C427D711510CB999'),@($neo,'705E24F37752809499A5CD0C7DD31EAABF3D933C5D3B2060168E94E9EF62593A'),@($universal,'E0A08BAE5CEA95BE96C09770530F5643BB183AF8AF59C64694EB346CB4C93FA4'),@('D:\DualForge\spike001a\compat-002-0l\build\forgecompat-002-0l.jar','D49CAEA48A61C57DA5384E053D7E8DE2834760172E6948C5167BF836BFD0E679'),@('D:\DualForge\spike000\probe-002-0l\build\forgeprobe.jar','A774D2F737B17F089046014AB55A53767DBAD008CD44DCCE52C727B87E559AE8'))
Check-Refs
Save-New 'inputs-before.json' ((Hashes @($refs|ForEach-Object{$_[0]}))|ConvertTo-Json -Depth 4)
foreach($cls in @('EventBus','ListenerList','EventListenerFactory','SubscribeEventListener')){Native ('neo-'+$cls) ($jdkBin+'\javap.exe') @('-p','-c','-s','-constants','-classpath',$neo,('net.neoforged.bus.'+$cls))}
foreach($cls in @('common.NeoForge','internal.CommonModLoader','server.loading.ServerModLoader')){Native ('neo-'+($cls -replace '\.','-')) ($jdkBin+'\javap.exe') @('-p','-c','-s','-constants','-classpath',$universal,('net.neoforged.neoforge.'+$cls))}
Native 'forge-EventBus' ($jdkBin+'\javap.exe') @('-p','-c','-s','-constants','-classpath',$forge,'net.minecraftforge.eventbus.EventBus')
Native 'forge-IEventBus' ($jdkBin+'\javap.exe') @('-p','-c','-s','-constants','-classpath',$forge,'net.minecraftforge.eventbus.api.IEventBus')
Check-Refs
Save-New 'inputs-after.json' ((Hashes @($refs|ForEach-Object{$_[0]}))|ConvertTo-Json -Depth 4)
Write-Output 'Nine static captures complete; fixed inputs unchanged; no server run.'
