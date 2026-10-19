"""Revalidate fixed release class bytes and collect permission/source/host implementations serially."""
import argparse,gzip,json,hashlib,sys,zipfile
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'mouse_tweaks'))
from support import ENV,JDK,fresh,record,run,save,sha

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',type=Path,required=True);a=p.parse_args();out=fresh(a.output)
 jar=ENV/'runs/convergence-010a-jei/inputs/jei-1.21.1-forge-19.27.0.340.jar';assert sha(jar)=='f5e61a001d57dd044d6f2ba255daf955c50d90e4173f8b4933ab0eb8cd386ed5'
 scan=ENV/'runs/convergence-010a-jei/static/usage.jsonl.gz';source={r['name']:r for r in map(json.loads,gzip.open(scan,'rt')) if r['artifact']=='forge-jei'}
 with zipfile.ZipFile(jar) as z:
  for row in source.values():assert hashlib.sha256(z.read(row['member'])).hexdigest()==row['classSHA256']
 selected=['mezz/jei/common/util/ServerCommandUtil','mezz/jei/common/config/IServerConfig','mezz/jei/library/config/ServerConfig','mezz/jei/common/network/ServerPacketContext','mezz/jei/common/network/packets/PacketDeletePlayerItem','mezz/jei/common/network/packets/PacketCheatPermission','mezz/jei/forge/network/NetworkHandler']
 selected=[s for s in selected if s in source];save(out/'source-graph.json',{'jar':record(jar),'revalidated':len(source),'classes':[source[s] for s in selected]})
 for n in selected:run(out,'source-'+n.replace('/','.'),[JDK/'bin/javap','-p','-c','-s','-v','-classpath',jar,n.replace('/','.')])
 names=['net.minecraft.network.Connection','net.minecraft.server.network.ServerCommonPacketListenerImpl','net.minecraft.world.level.Level','net.minecraft.server.level.ServerPlayerGameMode','net.minecraft.world.entity.Entity','net.minecraft.server.level.ServerLevel','net.minecraft.world.entity.player.Player','net.minecraft.server.level.ServerPlayer','net.minecraft.server.MinecraftServer','net.minecraft.server.dedicated.DedicatedServer','net.minecraft.commands.CommandSourceStack','net.minecraft.commands.Commands','net.minecraft.server.players.PlayerList','net.minecraft.server.players.ServerOpList','net.minecraft.server.players.ServerOpListEntry','net.minecraft.server.network.ServerGamePacketListenerImpl','net.minecraft.world.inventory.AbstractContainerMenu','net.minecraft.world.item.ItemStack','net.minecraft.server.commands.GiveCommand','com.mojang.brigadier.tree.CommandNode','com.mojang.brigadier.tree.RootCommandNode','com.mojang.brigadier.CommandDispatcher']
 bindings=[]
 for side in ['forge','neo']:
  jars=sorted((ENV/('runs/convergence-009c-20261017/client-abi/'+side+'/libraries')).rglob('*.jar'),key=lambda p:(not p.name.endswith('-server.jar'),str(p)));save(out/(side+'-sdk.json'),[record(p) for p in jars]);classes={}
  for path in jars:
   with zipfile.ZipFile(path) as z:
    for name in names:
     member=name.replace('.','/')+'.class'
     if name not in classes and member in z.namelist():
      data=z.read(member);target=out/side/member;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(data);classes[name]=data;bindings.append({'side':side,'name':name,'jar':record(path),'class':record(target)})
  assert set(classes)==set(names)
  for n in names:run(out,side+'-'+n,[JDK/'bin/javap','-p','-c','-s','-classpath',':'.join(map(str,jars)),n])
 save(out/'host-bindings.json',bindings);save(out/'result.json',{'status':'PASS_STATIC_COLLECTION','original':record(jar),'sourceClassesRevalidated':len(source),'sourceSelected':len(selected),'hostClasses':len(bindings),'identity':'real connected player NOT_RUN; no inferred permissions from ABI alone'});print('PERMISSION_ABI_COLLECTED',len(selected),len(bindings))
if __name__=='__main__':main()
