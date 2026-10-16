"""Build a separate NeoForge observer for labeled bridge white-box fault injection."""
import argparse,importlib.util,os,json
from pathlib import Path
from build_fixture import package
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('w',ROOT/'tools/lib/unix_workflow.py');w=importlib.util.module_from_spec(spec);spec.loader.exec_module(w)
def main():
 p=argparse.ArgumentParser(description=__doc__)
 for n in ['neo-install','jdk-home','fixture-index','output']:p.add_argument('--'+n,type=Path,required=True)
 a=p.parse_args();a.output.mkdir(parents=True,exist_ok=False);source=a.output/'Observer.java';text=(Path(__file__).parent/'driver/Observer.java.in').read_text()
 for k,v in {'HOST.fml':'net.neoforged.fml','HOST.event':'net.neoforged.neoforge.event','HOST.bus':'net.neoforged.bus','REGISTRY_PACKAGE':'net.neoforged.neoforge.registries','GLOBAL_BUS':'net.neoforged.neoforge.common.NeoForge.EVENT_BUS'}.items():text=text.replace(k,v)
 text=text.replace('original.println(text);','original.println(text);if(text.equals("[DF4C3] CONSTRUCTED quiet"))BridgeDiagnostics.run();').replace('phase="DONE";original.println','BridgeDiagnostics.completed();phase="DONE";original.println');source.write_text(text);diag=a.output/'BridgeDiagnostics.java';diag.write_text((Path(__file__).parent/'driver/BridgeDiagnostics.java.in').read_text());libs=sorted((a.neo_install/'libraries').rglob('*.jar'),key=lambda p:(p.name!='neoforge-21.1.256-server.jar',str(p)));inputs=w.fingerprints([source,diag,Path(__file__),a.fixture_index,*libs]);w.save(a.output/'inputs.json',inputs);classes=a.output/'classes';classes.mkdir();w.native(a.output,'javac',[a.jdk_home/'bin/javac','--release','21','-g','-proc:none','-cp',os.pathsep.join(map(str,libs)),'-d',classes,source,diag]);jar=a.output/'neoforge-global-observer.jar';package(jar,classes,'globalobserver',True,True);index=json.loads(a.fixture_index.read_text());index['artifacts']['neoforgeObserver']=w.fingerprints([jar])[0];index['scope']='Separate diagnostics only; not the standard C/T observer';w.save(a.output/'fixture-index.json',index);w.unchanged(inputs);print('DIAGNOSTIC BUILD PASS',a.output)
if __name__=='__main__':main()
