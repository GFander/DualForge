"""Run entry classification/Context ABI contracts on one supplied immutable compat JAR."""
import argparse
import importlib.util
import os
from pathlib import Path
import subprocess

REPO=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('w',REPO/'tools/lib/unix_workflow.py')
w=importlib.util.module_from_spec(spec);spec.loader.exec_module(w)


def main():
    p=argparse.ArgumentParser(description=__doc__)
    for name in ('jar','server-input','jdk-home','fixture','fnf','normal','legacy','block','probe','native','peer'):
        p.add_argument('--'+name,type=Path,required=True)
    a=p.parse_args()
    for proc in Path('/proc').iterdir():
        try:
            if (proc/'exe').resolve().name in ('java','javac'):raise RuntimeError('All JVM work must be serial')
        except (PermissionError,FileNotFoundError):pass
    d=w.run_directory(REPO/'out/convergence-007c3/contract');w.save(d/'invocation.json',{k:str(v) for k,v in vars(a).items()})
    sources=list(Path(__file__).parent.glob('*.java'));libs=sorted((a.server_input/'libraries').rglob('*.jar'));asm=[p for p in libs if p.name in ('asm-9.10.1.jar','asm-tree-9.10.1.jar')]
    inputs=w.fingerprints([a.jar,a.fixture,a.fnf,a.normal,a.legacy,a.block,a.probe,a.native,a.peer,*sources,Path(__file__),*libs]);w.save(d/'inputs.json',inputs)
    result={'status':'FAIL','compatSHA256':w.digest(a.jar),'testedCommit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=REPO,text=True).strip(),'offlineContext':'explicit FML VersionInfo/dist, no GAME layer or runtime-container emulation'}
    try:
        binary=w.tools(a,d);cp=os.pathsep.join(map(str,[a.jar,*asm,*libs]));classes=d/'classes';classes.mkdir()
        w.native(d,'javac',[binary/'javac','--release','21','-proc:none','-cp',cp,'-d',classes,*sources])
        w.native(d,'contract',[binary/'java','--add-opens','java.base/java.lang.invoke=ALL-UNNAMED','--add-exports','java.base/sun.security.util=ALL-UNNAMED','-cp',str(classes)+os.pathsep+cp,'io.github.gfander.dualforge.ContextProfileContract',a.fixture,a.fnf,a.normal,a.legacy,a.block,a.probe,a.native,a.peer,d/'bytes'])
        assert 'CONTEXT PROFILE CONTRACT PASS' in (d/'contract.stdout.log').read_text();w.unchanged(inputs);result['status']='PASS'
    finally:w.save(d/'result.json',result);print('RESULT',result['status'],d)


if __name__=='__main__':main()
