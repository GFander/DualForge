$ErrorActionPreference='Stop'
. 'D:\DualForge\spike000\logs\spike002-0j-artifacts\build-evidence.ps1'
$evRoot=$PSScriptRoot
$forge='D:\DualForge\spike000\forge-server\libraries\net\minecraftforge\eventbus\6.2.27\eventbus-6.2.27.jar'
$neo='D:\DualForge\spike000\neoforge-server\libraries\net\neoforged\bus\8.0.5\bus-8.0.5.jar'
$universal='D:\DualForge\spike000\neoforge-server\libraries\net\neoforged\neoforge\21.1.256\neoforge-21.1.256-universal.jar'
$refs=@(@($forge,'D2A7BCECB80B33F90854A003DC295960263D78435D9D6016C427D711510CB999'),@($neo,'705E24F37752809499A5CD0C7DD31EAABF3D933C5D3B2060168E94E9EF62593A'),@($universal,'E0A08BAE5CEA95BE96C09770530F5643BB183AF8AF59C64694EB346CB4C93FA4'))
Check-Refs
Save-New 'inputs-before.json' ((Hashes @($refs|ForEach-Object{$_[0]}))|ConvertTo-Json -Depth 4)
foreach($pair in @(@('forge',$forge,'net.minecraftforge.eventbus.api'),@('neo',$neo,'net.neoforged.bus.api'))){
 foreach($cls in @('SubscribeEvent','Event','EventPriority')){
  Native ($pair[0]+'-'+$cls) ($jdkBin+'\javap.exe') @('-p','-c','-s','-constants','-v','-classpath',$pair[1],($pair[2]+'.'+$cls))
 }
}
Native 'neo-EventBus' ($jdkBin+'\javap.exe') @('-p','-c','-s','-constants','-classpath',$neo,'net.neoforged.bus.EventBus')
Native 'neo-SubscribeEventListener' ($jdkBin+'\javap.exe') @('-p','-c','-s','-constants','-classpath',$neo,'net.neoforged.bus.SubscribeEventListener')
Native 'neo-NeoForge' ($jdkBin+'\javap.exe') @('-p','-c','-s','-constants','-classpath',($universal+';'+$neo),'net.neoforged.neoforge.common.NeoForge')
Check-Refs
Save-New 'inputs-after.json' ((Hashes @($refs|ForEach-Object{$_[0]}))|ConvertTo-Json -Depth 4)
Write-Output 'All nine raw javap captures completed, input hashes unchanged.'
