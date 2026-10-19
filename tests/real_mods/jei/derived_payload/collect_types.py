"""Fresh original type/process evidence; graph source bytes are revalidated, never inferred from names."""
import argparse,gzip,json,zipfile,hashlib,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'mouse_tweaks'))
from support import ENV,JDK,fresh,record,run,save

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--graph',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();out=fresh(a.output)
 original=ENV/'runs/convergence-010a-jei/inputs/jei-1.21.1-forge-19.27.0.340.jar';assert record(original)['sha256']=='f5e61a001d57dd044d6f2ba255daf955c50d90e4173f8b4933ab0eb8cd386ed5'
 rows=[json.loads(s) for s in gzip.open(a.graph,'rt')];source={r['name']:r for r in rows if r['artifact']=='forge-jei'}
 with zipfile.ZipFile(original) as z:
  for row in source.values():assert hashlib.sha256(z.read(row['member'])).hexdigest()==row['classSHA256']
 roots=[r for r in source.values() if any(m['name']=='process' for m in r['methods']) and '/network/packets/' in r['name']]
 needed={r['name'] for r in roots};needed.update(n for n in source if n.endswith(('/ServerPacketContext','/ClientPacketContext','/EnumStreamCodec','/GiveMode','/TransferOperation','/ServerCommandUtil','/ClientCheatPermissionHandler','/BasicRecipeTransferHandlerServer','/ServerCheatPermissionHandler')))
 graph=[]
 for n in sorted(needed):
  row=source[n];run(out,'source-'+n.replace('/','.'),[JDK/'bin/javap','-p','-c','-s','-v','-classpath',original,n.replace('/','.')])
  chain=[];seen=set();current=n
  while current in source:
   assert current not in seen;seen.add(current);r=source[current];chain.append({'name':current,'classSHA256':r['classSHA256'],'superclass':r['superclass'],'interfaces':r['interfaces']});current=r['superclass']
  graph.append({'name':n,'parentChain':chain,'hostAncestor':current,'fields':row['fields'],'declaredMethods':row['methods'],'recordComponents':row['recordComponents'],'methods':row['code'],'staticAndDynamicReferences':row['references']})
 save(out/'types-and-process-graph.json',{'scope':'STATIC_COMPLETE_SELECTED_CLASSFILES; direct/virtual references retained; dynamic dispatch and business are not automatically approved','original':record(original),'revalidatedSourceClassCount':len(source),'selected':graph})
 hostNames=['net.minecraft.network.protocol.common.custom.CustomPacketPayload','net.minecraft.network.FriendlyByteBuf','net.minecraft.world.item.ItemStack','net.minecraft.world.entity.player.Inventory','net.minecraft.network.codec.StreamCodec','net.minecraft.network.codec.ByteBufCodecs']
 bindings=[]
 for side in ['forge','neo']:
  deps=sorted((ENV/('runs/convergence-009c-20261017/client-abi/'+side+'/libraries')).rglob('*.jar'),key=lambda p:(not p.name.endswith('-server.jar'),str(p)))
  for n in hostNames:
   member=n.replace('.','/')+'.class'
   for jar in deps:
    with zipfile.ZipFile(jar) as z:
     if member in z.namelist():bindings.append({'side':side,'class':n,'jar':record(jar),'classSHA256':hashlib.sha256(z.read(member)).hexdigest()});break
   else:raise AssertionError(n)
   run(out,side+'-'+n,[JDK/'bin/javap','-p','-c','-s','-classpath',':'.join(map(str,deps)),n])
 save(out/'host-bindings.json',bindings);save(out/'result.json',{'status':'PASS','scope':'STATIC_SOURCE_AND_HOST_ABI','sourceClasses':len(source),'selected':len(graph),'original':record(original)});print('DERIVED_ABI_PASS selected='+str(len(graph)))
if __name__=='__main__':main()
