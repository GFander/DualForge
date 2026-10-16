"""Compile original Forge global-registration fixtures; observers are separate host mods."""
import argparse,importlib.util,os,zipfile,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('w',ROOT/'tools/lib/unix_workflow.py');w=importlib.util.module_from_spec(spec);spec.loader.exec_module(w)
def package(jar,classes,mod,native=False,observer=False):
 toml='modLoader="javafml"\nloaderVersion="'+('[4,)' if native else '[52,)')+'"\nlicense="test fixture"\n[[mods]]\nmodId="'+mod+'"\nversion="1"\n'
 if not observer:toml+='[[dependencies.'+mod+']]\nmodId="globalobserver"\nmandatory=true\nversionRange="[1,)"\nordering="AFTER"\nside="BOTH"\n'
 with zipfile.ZipFile(jar,'x',zipfile.ZIP_DEFLATED) as z:
  z.writestr('META-INF/'+('neoforge.mods.toml' if native else 'mods.toml'),toml)
  for f in sorted(classes.rglob('*.class')):z.write(f,f.relative_to(classes).as_posix())
def main():
 p=argparse.ArgumentParser(description=__doc__)
 for n in ['forge-install','neo-install','jdk-home','output']:p.add_argument('--'+n,type=Path,required=True)
 p.add_argument("--existing-fixture-index",type=Path);a=p.parse_args();out=a.output.resolve();out.mkdir(parents=True,exist_ok=False);binary=w.tools(a,out)
 forge=sorted((a.forge_install/'libraries').rglob('*.jar'),key=lambda p:(not p.name.endswith('52.1.0-server.jar'),str(p)));neo=sorted((a.neo_install/'libraries').rglob('*.jar'),key=lambda p:(p.name!='neoforge-21.1.256-server.jar',str(p)));assert not any('neoforge' in str(p) for p in forge)
 sources=sorted((Path(__file__).parent/'fixture').rglob('*.java'));inputs=w.fingerprints([*sources,*forge,*neo,Path(__file__),Path(__file__).parent/'driver/Observer.java.in']);
 if a.existing_fixture_index:inputs+=w.fingerprints([a.existing_fixture_index,*[Path(v['Path']) for k,v in json.loads(a.existing_fixture_index.read_text())['artifacts'].items() if k in ['quiet','signal','combined']]])
 w.save(out/'inputs.json',inputs);art={} if not a.existing_fixture_index else {k:v for k,v in json.loads(a.existing_fixture_index.read_text())['artifacts'].items() if k in ['quiet','signal','combined']}
 for variant,mod in [('quiet','quietbus'),('signal','signalbus'),('combined','catalogbus')]:
  if a.existing_fixture_index:continue
  d=out/variant;d.mkdir();classes=d/'classes';classes.mkdir();src=sorted((Path(__file__).parent/'fixture'/variant).rglob('*.java'));w.native(d,'javac',[binary/'javac','--release','21','-g','-proc:none','-cp',os.pathsep.join(map(str,forge)),'-d',classes,*src]);jar=d/(mod+'.jar');package(jar,classes,mod);art[variant]=w.fingerprints([jar])[0]
  for f in sorted(classes.rglob('*.class')):
   assert b'net/neoforged/' not in f.read_bytes() and b'io/github/gfander/dualforge' not in f.read_bytes();w.native(d,'javap-'+f.stem,[binary/'javap','-p','-v','-c',f])
 for host,libs in [('forge',forge),('neoforge',neo)]:
  d=out/host;d.mkdir();text=(Path(__file__).parent/'driver/Observer.java.in').read_text()
  for k,v in {'HOST.fml':'net.minecraftforge.fml' if host=='forge' else 'net.neoforged.fml','HOST.event':'net.minecraftforge.event' if host=='forge' else 'net.neoforged.neoforge.event','HOST.bus':'net.minecraftforge.eventbus' if host=='forge' else 'net.neoforged.bus','REGISTRY_PACKAGE':'net.minecraftforge.registries' if host=='forge' else 'net.neoforged.neoforge.registries','GLOBAL_BUS':'net.minecraftforge.common.MinecraftForge.EVENT_BUS' if host=='forge' else 'net.neoforged.neoforge.common.NeoForge.EVENT_BUS'}.items():text=text.replace(k,v)
  f=d/'Observer.java';f.write_text(text);classes=d/'classes';classes.mkdir();w.native(d,'javac',[binary/'javac','--release','21','-g','-proc:none','-cp',os.pathsep.join(map(str,libs)),'-d',classes,f]);jar=d/(host+'-global-observer.jar');package(jar,classes,'globalobserver',host=='neoforge',True);art[host+'Observer']=w.fingerprints([jar])[0]
 w.unchanged(inputs);w.save(out/'result.json',{'status':'PASS','artifacts':art,'source':w.fingerprints(sources),'classes':w.fingerprints(sorted(out.rglob('*.class'))),'purity':'Fixtures compile against official Forge52.1.0 only; observers separate'});print('GLOBAL FIXTURE PASS',out)
if __name__=='__main__':main()
