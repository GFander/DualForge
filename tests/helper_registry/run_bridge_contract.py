"""Isolated bridge rejection diagnostics; never used as the standard C/T launch."""
import argparse
import importlib.util
import json
import os
from pathlib import Path
import subprocess
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('w',ROOT/'tools/lib/unix_workflow.py');w=importlib.util.module_from_spec(spec);spec.loader.exec_module(w)

def main():
    p=argparse.ArgumentParser(description=__doc__)
    for key in ('jar','fixture-index','peer','install','jdk-home','baseline','output'):p.add_argument('--'+key,type=Path,required=True)
    a=p.parse_args()
    for key,value in vars(a).items():setattr(a,key,value.resolve())
    art=json.loads(a.fixture_index.read_text())['artifacts'];mods=[a.jar,Path(art['fixture']['Path']),Path(art['neoforgeObserver']['Path']),a.peer]
    inputs=w.fingerprints([*mods,Path(__file__),a.fixture_index]);run=a.output.parent/(a.output.name+'-invocation');run.mkdir(parents=True,exist_ok=False);w.save(run/'inputs.json',inputs)
    cmd=['python3','-B',ROOT/'tests/real_mods/food_n_fuel/run_stock.py','--host','neoforge','--install',a.install,'--jdk-home',a.jdk_home,'--output',a.output]
    for mod in mods:cmd+=['--mod',mod]
    env=os.environ.copy();env.update(DF4B_REGISTER='true',DF4B_BRIDGE_NEGATIVES='true')
    w.save(run/'command.json',{'argv':list(map(str,cmd)),'cwd':str(ROOT),'fixtureEnvironment':{k:env[k] for k in ('DF4B_REGISTER','DF4B_BRIDGE_NEGATIVES')},'whiteBoxDiagnostics':'Real containers; private view construction and restored active-state mutation only for rejection tests'})
    with (run/'stdout.log').open('w') as out,(run/'stderr.log').open('w') as err:r=subprocess.run(list(map(str,cmd)),cwd=ROOT,env=env,stdout=out,stderr=err)
    raw=json.loads((a.output/'result.json').read_text());text=(a.output/'stdout.log').read_text();baseline=json.loads(a.baseline.read_text())
    reasons={'duplicate-attachment':'DUPLICATE_REGISTRY_ATTACHMENT','foreign-helper-module':'foreign helper module','wrong-active-container':'foreign or escaped registry context','outside-constructor':'registry bridge outside owned entry constructor','two-forge-mods-cross-bus':'foreign or escaped registry context','escaped-context':'foreign or escaped registry context'}
    checks={'lifecycle':r.returncode==0 and raw['lifecycle']=='PASS','sameBaselineErrors':raw['severityRecords']==baseline['severityRecords'],'unownedSources':text.count('[DF4B] BRIDGE REFUSED unowned-source cases=2')==1,'functionalityRetained':'[DF4B] REGISTRY PASS enabled=true owner=helperprobe' in text}
    for label,reason in reasons.items():checks[label]=any(line.startswith('[DF4B] BRIDGE NEGATIVE PASS '+label+' reason=') and reason in line for line in text.splitlines())
    w.unchanged(inputs);result={'status':'PASS' if all(checks.values()) else 'FAIL','compatSHA256':w.digest(a.jar),'fixtureSHA256':w.digest(mods[1]),'checks':checks,'negativeCases':8,'strictZeroErrors':False,'runtimeDirectory':str(a.output)};w.save(run/'result.json',result);print(json.dumps(result,indent=2));assert result['status']=='PASS'
if __name__=='__main__':main()
