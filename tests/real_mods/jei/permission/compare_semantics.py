"""Compare selected actual javap instruction bodies, retaining member comments and branch targets."""
import argparse,json,re,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'mouse_tweaks'))
from support import fresh,record,save
def body(path,signature):
 lines=path.read_text().splitlines();start=next(i for i,line in enumerate(lines) if signature in line);end=start+1
 while end<len(lines) and (end<=start+2 or lines[end].strip()):end+=1
 return [re.sub(r'#\d+','#CP',line.strip()) for line in lines[start:end] if re.match(r'^\s*\d+:',line)]
def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--abi',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();out=fresh(a.output)
 selected=[('net.minecraft.world.entity.Entity','MinecraftServer getServer('),('net.minecraft.world.entity.Entity','CommandSourceStack createCommandSourceStack('),('net.minecraft.server.level.ServerPlayer','protected int getPermissionLevel();'),('net.minecraft.server.level.ServerPlayer','boolean isCreative();'),('net.minecraft.server.MinecraftServer','int getProfilePermissions('),('net.minecraft.server.dedicated.DedicatedServer','int getOperatorUserPermissionLevel('),('net.minecraft.commands.CommandSourceStack','boolean hasPermission(int);'),('net.minecraft.server.players.PlayerList','boolean isOp('),('net.minecraft.server.players.ServerOpListEntry','int getLevel();'),('com.mojang.brigadier.tree.CommandNode','boolean canUse('),('com.mojang.brigadier.tree.CommandNode','getChild(java.lang.String);')]
 results=[]
 for cls,signature in selected:
  paths=[a.abi/(side+'-'+cls+'.stdout.log') for side in ['forge','neo']];bodies=[body(q,signature) for q in paths];assert all(bodies),(cls,signature);results.append({'class':cls,'method':signature,'equal':bodies[0]==bodies[1],'source':[record(q) for q in paths],'bodies':dict(zip(['forge','neo'],bodies))})
 save(out/'comparison.json',{'scope':'STATIC_SELECTED_BODY_COMPARISON; CP indices normalized, member comments/branch byte offsets preserved; no approval of every transitive/dynamic permission/OP service or real player state','methods':results});save(out/'result.json',{'status':'PASS_STATIC_COMPARISON' if all(x['equal'] for x in results) else 'PARTIAL_STATIC_COMPARISON','selected':len(results),'equal':sum(x['equal'] for x in results),'realConnectedPlayer':'NOT_RUN'});print('PERMISSION_NATIVE_BODIES',[(v['method'],v['equal']) for v in results])
if __name__=='__main__':main()
