"""Compile one native observer against official host-only APIs, never modify source mods."""
import argparse,hashlib,json,os,subprocess,zipfile
from pathlib import Path

def main():
 p=argparse.ArgumentParser(description=__doc__)
 for n in ['host','install','jdk-home','output']:p.add_argument('--'+n,required=True,type=str if n=='host' else Path)
 a=p.parse_args();assert a.host in ['forge','neoforge'];out=a.output.resolve();out.mkdir(parents=True,exist_ok=False);classes=out/'classes';classes.mkdir();template=Path(__file__).with_name('BehaviorObserver.java.in');text=template.read_text();forge=a.host=='forge'
 replaces={'HOST.fml':'net.minecraftforge.fml' if forge else 'net.neoforged.fml','HOST.event':'net.minecraftforge.event' if forge else 'net.neoforged.neoforge.event','HOST.registry':'net.minecraftforge.registries' if forge else 'net.neoforged.neoforge.registries','GLOBAL_BUS':'net.minecraftforge.common.MinecraftForge.EVENT_BUS' if forge else 'net.neoforged.neoforge.common.NeoForge.EVENT_BUS','CONSTRUCTOR_PARAMS':'' if forge else 'net.neoforged.bus.api.IEventBus bus','MOD_BUS':'net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus()' if forge else 'bus'}
 for k,v in replaces.items():text=text.replace(k,v)
 src=out/'BehaviorObserver.java';src.write_text(text);libs=sorted((a.install.resolve()/'libraries').rglob('*.jar'),key=lambda p:(not p.name.endswith('-server.jar'),str(p)));h=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();records=[{'path':str(f.resolve()),'bytes':f.stat().st_size,'sha256':h(f)} for f in [Path(__file__),template,src,*libs]];(out/'inputs.json').write_text(json.dumps(records,indent=2)+'\n')
 cmd=[str(a.jdk_home.resolve()/'bin/javac'),'--release','21','-proc:none','-cp',os.pathsep.join(map(str,libs)),'-d',str(classes),str(src)];(out/'command.json').write_text(json.dumps(cmd,indent=2)+'\n');r=subprocess.run(cmd,capture_output=True);(out/'stdout.log').write_bytes(r.stdout);(out/'stderr.log').write_bytes(r.stderr);(out/'exit.json').write_text(json.dumps({'exitCode':r.returncode})+'\n');r.check_returncode()
 jar=out/(a.host+'-behavior-observer.jar')
 with zipfile.ZipFile(jar,'x',zipfile.ZIP_DEFLATED) as z:
  z.writestr('META-INF/'+('mods.toml' if forge else 'neoforge.mods.toml'),'modLoader="javafml"\nloaderVersion="'+('[52,)' if forge else '[4,)')+'"\nlicense="test fixture"\n[[mods]]\nmodId="fnfbehaviorobserver"\nversion="1"\n')
  for f in classes.rglob('*.class'):z.write(f,f.relative_to(classes).as_posix())
 assert all(h(Path(v['path']))==v['sha256'] for v in records)
 (out/'result.json').write_text(json.dumps({'jar':str(jar),'sha256':h(jar),'bytes':jar.stat().st_size,'sourceSHA256':h(src),'templateSHA256':h(template),'sourceCommit':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()},indent=2)+'\n');print(jar)
if __name__=='__main__':main()
