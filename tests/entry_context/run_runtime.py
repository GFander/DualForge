"""Stock no-argument Context lifecycles and read-only per-mod host Bus identity checks."""
import argparse
import importlib.util
import json
from pathlib import Path
import subprocess

REPO=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('w',REPO/'tools/lib/unix_workflow.py')
w=importlib.util.module_from_spec(spec);spec.loader.exec_module(w)


def main():
    p=argparse.ArgumentParser(description=__doc__)
    for name in ('jar','install','jdk-home','native','fixture-index'):p.add_argument('--'+name,type=Path,required=True)
    a=p.parse_args();assert a.jar.name=='DualForge.jar'
    frozen=json.loads(a.fixture_index.read_text());artifacts=frozen['artifacts'];jars={k:Path(v['Path']) for k,v in artifacts.items()}
    for name,path in jars.items():assert path.stat().st_size==artifacts[name]['Bytes'] and w.digest(path)==artifacts[name]['SHA256']
    d=w.run_directory(REPO/'out/convergence-007c3/runtime');inputs=w.fingerprints([a.jar,a.native,a.fixture_index,Path(__file__),*jars.values()]);w.save(d/'inputs.json',inputs);w.save(d/'invocation.json',{k:str(v) for k,v in vars(a).items()})
    result={'status':'FAIL','compatSHA256':w.digest(a.jar),'testedCommit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=REPO,text=True).strip(),'cases':{}}
    try:
        baseline=None
        for name,mods in [('N0',[]),('single',[a.jar,jars['contextprobe']]),('owned-buses',[a.jar,jars['contextprobe'],jars['contextpeer'],jars['neo-observer'],a.native])]:
            cmd=['python3','-B',REPO/'tests/real_mods/food_n_fuel/run_stock.py','--host','neoforge','--install',a.install,'--output',d/name,'--jdk-home',a.jdk_home]
            for mod in mods:cmd+=['--mod',mod]
            w.native(d,name,cmd);raw=json.loads((d/name/'result.json').read_text());out=(d/name/'stdout.log').read_text()
            if baseline is None:baseline=raw['severityRecords']
            checks={'lifecycle':raw['lifecycle']=='PASS','stockJvm':raw['zeroDualForgeParameters'],'noNewErrors':raw['severityRecords']==baseline,'sourceInputsUnchanged':raw['assertions']['inputsUnchanged']}
            if name!='N0':checks.update(exactCompat=any(v['path']=='mods/DualForge.jar' and v['sha256']==result['compatSHA256'] for v in raw['installedMods']),originalFixture=any(v['sha256']==artifacts['contextprobe']['SHA256'] for v in raw['installedMods']),constructorOnce=out.count('[DF007C3] CONTEXT CONSTRUCTED contextprobe')==1)
            if name=='owned-buses':checks.update(bothCorrectIndependentNativeBuses='[DF007C3] CONTEXT PASS owners=2 distinct=true' in out,nativeForeignContextRefused='[DF007C3] FOREIGN_CONTEXT REJECTED native-constructor' in out,peerConstructorOnce=out.count('[DF007C3] CONTEXT CONSTRUCTED contextpeer')==1,nativeCoexist='[DF007-NATIVE] SERVER_STARTED' in out)
            assert all(checks.values()),(name,checks)
            w.save(d/name/'gate-assertions.json',{'status':'PASS','checks':checks,'compatSHA256':None if name=='N0' else result['compatSHA256'],'viewIdentity':'Read-only source view delegate/owner checked against real FML container; not raw view==native bus or Event API functionality'})
            result['cases'][name]={'status':'PASS','raw':str(d/name/'result.json')};print('PASS',name,flush=True)
        w.unchanged(inputs);result.update(status='PASS',originalForgeJarsReused=True,nativeProofReference=frozen.get('originalNativeResult'))
    finally:w.save(d/'result.json',result);print('RESULT',result['status'],d)


if __name__=='__main__':main()
