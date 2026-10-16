"""Build independent native Forge and NeoForge probes of actual register semantics."""
import argparse,importlib.util,os
from pathlib import Path
from build_fixture import package
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('w',ROOT/'tools/lib/unix_workflow.py');w=importlib.util.module_from_spec(spec);spec.loader.exec_module(w)
def main():
 p=argparse.ArgumentParser(description=__doc__)
 for name in ['forge-install','neo-install','jdk-home','output']:p.add_argument('--'+name,type=Path,required=True)
 a=p.parse_args();a.output.mkdir(parents=True,exist_ok=False);art={}
 for host,install,prefix,bus,inherit in [('forge',a.forge_install,'net.minecraftforge','net.minecraftforge.eventbus',1),('neoforge',a.neo_install,'net.neoforged','net.neoforged.bus',0)]:
  d=a.output/host;d.mkdir();text=(Path(__file__).parent/'driver/SemanticsProbe.java.in').read_text().replace('HOST.fml',prefix+'.fml').replace('HOST.bus',bus).replace('HOST.event',prefix+('.event' if host=='forge' else '.neoforge.event')).replace('GLOBAL_BUS',prefix+('.common.MinecraftForge.EVENT_BUS' if host=='forge' else '.neoforge.common.NeoForge.EVENT_BUS')).replace('EXPECTED_INHERITED',str(inherit))
  if host=='neoforge':text=text.replace('import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;','import net.neoforged.fml.ModLoadingContext;').replace('FMLJavaModLoadingContext.get().getModEventBus()','ModLoadingContext.get().getActiveContainer().getEventBus()')
  source=d/'SemanticsProbe.java';source.write_text(text);classes=d/'classes';classes.mkdir();libs=sorted((install/'libraries').rglob('*.jar'),key=lambda p:(not p.name.endswith('server.jar'),str(p)));inputs=w.fingerprints([source,Path(__file__),*libs]);w.save(d/'inputs.json',inputs);w.native(d,'javac',[a.jdk_home/'bin/javac','--release','21','-g','-proc:none','-cp',os.pathsep.join(map(str,libs)),'-d',classes,source]);jar=d/'bus-semantics.jar';package(jar,classes,'bussemantics',host=='neoforge',True);art[host]=w.fingerprints([jar])[0];w.unchanged(inputs)
 w.save(a.output/'result.json',{'status':'PASS','artifacts':art,'scope':'Two separate native host probes; not a cross-host Forge fixture'})
if __name__=='__main__':main()
