"""Three independently named native Forge fixtures; platform observers stay separate."""
import argparse,importlib.util,os,shutil
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('w',ROOT/'tools/lib/unix_workflow.py');w=importlib.util.module_from_spec(spec);spec.loader.exec_module(w)
spec=importlib.util.spec_from_file_location('b',ROOT/'tests/helper_registry/build_fixture.py');b=importlib.util.module_from_spec(spec);spec.loader.exec_module(b)
CASES={'ordinary':('basicboundary','lab.simple'),'data':('databoundary','different.pantry'),'subclass':('constructboundary','another.crystal')}
def main():
 p=argparse.ArgumentParser(description=__doc__)
 for n in ['forge-install','neo-install','jdk-home']:p.add_argument('--'+n,type=Path,required=True)
 a=p.parse_args();out=w.run_directory(ROOT/'out/convergence-007c4c1/fixture');binary=w.tools(a,out);forge=sorted((a.forge_install/'libraries').rglob('*.jar'));neo=sorted((a.neo_install/'libraries').rglob('*.jar'));forge.sort(key=lambda j:(not j.name.endswith('52.1.0-server.jar'),str(j)));assert not any('neoforged' in str(j) or j.name.lower()=='dualforge.jar' for j in forge)
 inputs=w.fingerprints([*forge,*neo,Path(__file__),*sorted((ROOT/'tests/helper_registry/fixture').rglob('*.java')),ROOT/'tests/helper_registry/driver/Observer.java.in']);w.save(out/'inputs.json',inputs)
 for case,(mod,pkg) in CASES.items():
  d=out/case;source=d/'source';source.mkdir(parents=True);classes=d/'classes';classes.mkdir();art={}
  for f in sorted((ROOT/'tests/helper_registry/fixture').rglob('*.java')):
   text=f.read_text().replace('example.helperprobe',pkg).replace('helperprobe',mod).replace('DF4B_REGISTER','DF4C1_REGISTER')
   if case=='data':text=text.replace('new Item(new Item.Properties())','new Item(Pantry.make().stacksTo(7))')
   if case=='subclass':text=text.replace('new Item(new Item.Properties())','new EnergyShard(new Item.Properties().stacksTo(11), 2468)')
   (source/f.name).write_text(text)
  if case=='data':(source/'Pantry.java').write_text('package '+pkg+'; import net.minecraft.world.food.FoodProperties; import net.minecraft.world.item.Item; public final class Pantry { public static final FoodProperties MEAL=new FoodProperties.Builder().nutrition(nutrition(3)).saturationModifier(0.45f).build(); static {System.out.println("[DF4C1] DATA INIT");} private Pantry(){} private static int nutrition(int n){return n*2+1;} public static Item.Properties make(){return new Item.Properties().food(MEAL);} }')
  if case=='subclass':(source/'EnergyShard.java').write_text('package '+pkg+'; import net.minecraft.world.item.*; import net.minecraft.world.item.crafting.RecipeType; public final class EnergyShard extends Item { private final int energy; static {System.out.println("[DF4C1] CLASS INIT");} public EnergyShard(Properties p,int energy){super(p);this.energy=energy;} @Override public int getBurnTime(ItemStack stack,RecipeType<?> recipe){return energy;} }')
  w.native(d,'fixture-javac',[binary/'javac','--release','21','-g','-proc:none','-cp',os.pathsep.join(map(str,forge)),'-d',classes,*sorted(source.glob('*.java'))]);jar=d/(mod+'.jar');b.package(jar,classes,mod);art['fixture']=w.fingerprints([jar])[0]
  for host,libs in [('forge',forge),('neoforge',neo)]:
   h=d/host;h.mkdir();template=(ROOT/'tests/helper_registry/driver/Observer.java.in').read_text().replace('example.helperprobe',pkg).replace('helperprobe',mod).replace('DF4B_REGISTER','DF4C1_REGISTER')
   if host=='forge':rep={'HOST.event':'net.minecraftforge.event','HOST.fml':'net.minecraftforge.fml','REGISTRY_PACKAGE':'net.minecraftforge.registries','GLOBAL_BUS':'net.minecraftforge.common.MinecraftForge.EVENT_BUS','EXACT_DEFERRED':'net.minecraftforge.registries.DeferredRegister$EventDispatcher','HOST_KIND':'forge'}
   else:rep={'HOST.event':'net.neoforged.neoforge.event','HOST.fml':'net.neoforged.fml','REGISTRY_PACKAGE':'net.neoforged.neoforge.registries','GLOBAL_BUS':'net.neoforged.neoforge.common.NeoForge.EVENT_BUS','EXACT_DEFERRED':'net.neoforged.neoforge.registries.DeferredRegister','HOST_KIND':'neoforge'}
   if case=='subclass':template=template.replace('item.getClass() != Item.class','!item.getClass().getName().equals("'+pkg+'.EnergyShard")')
   proof=''
   if case=='data':proof='if(new net.minecraft.world.item.ItemStack((Item)item).get(net.minecraft.core.component.DataComponents.FOOD).nutrition()!=7 || new net.minecraft.world.item.ItemStack((Item)item).getMaxStackSize()!=7)throw new AssertionError("Food business changed");'
   if case=='subclass':proof='if(!item.getClass().getMethod("getBurnTime",net.minecraft.world.item.ItemStack.class,net.minecraft.world.item.crafting.RecipeType.class).invoke(item,new net.minecraft.world.item.ItemStack((Item)item),null).equals(2468) || new net.minecraft.world.item.ItemStack((Item)item).getMaxStackSize()!=11)throw new AssertionError("Subclass business changed");'
   template=template.replace('previous = item;',proof+'System.out.println("[DF4C1] BUSINESS '+case+' value=true");previous = item;')
   for old,new in rep.items():template=template.replace(old,new)
   f=h/'Observer.java';f.write_text(template);target=h/'classes';target.mkdir();w.native(d,host+'-observer-javac',[binary/'javac','--release','21','-proc:none','-cp',os.pathsep.join(map(str,libs)),'-d',target,f]);j=h/(host+'-observer.jar');b.package(j,target,'helperobserver',host=='neoforge');art[host+'Observer']=w.fingerprints([j])[0]
  result={'status':'PASS','case':case,'modId':mod,'package':pkg,'artifacts':art,'purity':'Official Forge52.1.0 compile inputs only, no NeoForge/DualForge','source':w.fingerprints(sorted(source.glob('*.java'))),'classes':w.fingerprints(sorted(classes.rglob('*.class')))}
  for f in sorted(classes.rglob('*.class')):assert b'net/neoforged/' not in f.read_bytes() and b'io/github/gfander/dualforge' not in f.read_bytes()
  w.save(d/'result.json',result);print(case,d/'result.json',flush=True)
 w.unchanged(inputs);print('FIXTURES PASS',out,flush=True)
if __name__=='__main__':main()
