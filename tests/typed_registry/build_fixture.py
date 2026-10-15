"""Build pure Forge typed-Registry input and independent platform observers."""
import argparse,importlib.util,os,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('w',ROOT/'tools/lib/unix_workflow.py');w=importlib.util.module_from_spec(spec);spec.loader.exec_module(w)
def pack(jar,classes,mod,native=False):
 toml='modLoader="javafml"\nloaderVersion="'+('[4,)' if native else '[52,)')+'"\nlicense="test fixture"\n[[mods]]\nmodId="'+mod+'"\nversion="1"\n'
 if mod=='tabboundary':toml+='[[dependencies.tabboundary]]\nmodId="tabobserver"\nmandatory=true\nversionRange="[1,)"\nordering="AFTER"\nside="BOTH"\n'
 with zipfile.ZipFile(jar,'x',zipfile.ZIP_DEFLATED) as z:
  z.writestr('META-INF/'+('neoforge.mods.toml' if native else 'mods.toml'),toml)
  for p in sorted(classes.rglob('*.class')):z.writestr(p.relative_to(classes).as_posix(),p.read_bytes())
def main():
 p=argparse.ArgumentParser(description=__doc__)
 for n in ['forge-install','neo-install','jdk-home']:p.add_argument('--'+n,type=Path,required=True)
 a=p.parse_args();out=w.run_directory(ROOT/'out/convergence-007c4c2/fixture');binary=w.tools(a,out);source=Path(__file__).parent/'fixture';forge=sorted((a.forge_install/'libraries').rglob('*.jar'),key=lambda p:(not p.name.endswith('52.1.0-server.jar'),str(p)));neo=sorted((a.neo_install/'libraries').rglob('*.jar'),key=lambda p:(p.name!='neoforge-21.1.256-server.jar',str(p)));assert not any('neoforge' in p.name for p in forge);inputs=w.fingerprints([*forge,*neo,*source.rglob('*.java'),Path(__file__).parent/'driver/Observer.java.in',Path(__file__)]);w.save(out/'inputs.json',inputs);classes=out/'classes';classes.mkdir();w.native(out,'fixture-javac',[binary/'javac','--release','21','-g','-proc:none','-cp',os.pathsep.join(map(str,forge)),'-d',classes,*sorted(source.rglob('*.java'))]);jar=out/'tab-boundary.jar';pack(jar,classes,'tabboundary');art={'fixture':w.fingerprints([jar])[0]}
 for host,libs in [('forge',forge),('neoforge',neo)]:
  d=out/host;d.mkdir();text=(Path(__file__).parent/'driver/Observer.java.in').read_text();rep={'HOST.fml':'net.minecraftforge.fml' if host=='forge' else 'net.neoforged.fml','HOST.event':'net.minecraftforge.event' if host=='forge' else 'net.neoforged.neoforge.event','REGISTRY_PACKAGE':'net.minecraftforge.registries' if host=='forge' else 'net.neoforged.neoforge.registries','GLOBAL_BUS':'net.minecraftforge.common.MinecraftForge.EVENT_BUS' if host=='forge' else 'net.neoforged.neoforge.common.NeoForge.EVENT_BUS'}
  for k,v in rep.items():text=text.replace(k,v)
  f=d/'Observer.java';f.write_text(text);c=d/'classes';c.mkdir();w.native(out,host+'-observer-javac',[binary/'javac','--release','21','-proc:none','-cp',os.pathsep.join(map(str,libs)),'-d',c,f]);j=d/(host+'-tab-observer.jar');pack(j,c,'tabobserver',host=='neoforge');art[host+'Observer']=w.fingerprints([j])[0]
 for f in classes.rglob('*.class'):assert b'net/neoforged' not in f.read_bytes() and b'io/github/gfander/dualforge' not in f.read_bytes();w.native(out,'javap-'+f.stem,[binary/'javap','-p','-v','-c',f])
 w.unchanged(inputs);w.save(out/'result.json',{'status':'PASS','artifacts':art,'source':w.fingerprints(sorted(source.rglob('*.java'))),'classes':w.fingerprints(sorted(classes.rglob('*.class'))),'purity':'Official Forge52.1.0 only; platform observers separate'});print('FIXTURE PASS',out)
if __name__=='__main__':main()
