"""Stock Forge/NeoForge helper lifecycle plus independently checked registry/cohort receipts."""
import argparse
import hashlib
import importlib.util
import json
import os
import re
from pathlib import Path
import subprocess

ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('w',ROOT/'tools/lib/unix_workflow.py');w=importlib.util.module_from_spec(spec);spec.loader.exec_module(w)

def main():
    p=argparse.ArgumentParser(description=__doc__)
    for n in ('fixture-index','install','jdk-home','output','baseline'):p.add_argument('--'+n,type=Path,required=True)
    p.add_argument('--jar',type=Path);p.add_argument('--native',type=Path);p.add_argument('--host',choices=['forge','neoforge'],required=True);p.add_argument('--stage',choices=['control','treatment'],required=True)
    a=p.parse_args()
    for name,value in vars(a).items():
        if isinstance(value,Path):setattr(a,name,value.resolve())
    index=json.loads(a.fixture_index.read_text());art=index['artifacts'];fixture=Path(art['fixture']['Path']);observer=Path(art[a.host+'Observer']['Path']);mods=[fixture,observer]
    for k in ['fixture',a.host+'Observer']:
        f=Path(art[k]['Path']);assert w.digest(f)==art[k]['SHA256'] and f.stat().st_size==art[k]['Bytes']
    if a.host=='neoforge':assert a.jar and a.jar.name=='DualForge.jar';mods.insert(0,a.jar)
    if a.native:mods.append(a.native)
    baseline=json.loads(a.baseline.read_text());assert baseline['lifecycle']=='PASS'
    cmd=['python3','-B',ROOT/'tests/real_mods/food_n_fuel/run_stock.py','--host',a.host,'--install',a.install,'--jdk-home',a.jdk_home,'--output',a.output]
    for jar in mods:cmd+=['--mod',jar]
    env=os.environ.copy();assert not env.get('DF4B_BRIDGE_NEGATIVES'),'Use the separate diagnostic run for bridge fault injection'
    env['DF4B_REGISTER']='true' if a.stage=='treatment' else 'false';run=a.output.parent/(a.output.name+'-invocation');run.mkdir(parents=True,exist_ok=False)
    inputs=w.fingerprints([*mods,Path(__file__),a.fixture_index]);w.save(run/'inputs.json',inputs)
    w.save(run/'command.json',{'argv':list(map(str,cmd)),'cwd':str(ROOT),'fixtureEnvironment':{'DF4B_REGISTER':env['DF4B_REGISTER']}})
    with (run/'stdout.log').open('w') as out,(run/'stderr.log').open('w') as err:r=subprocess.run(list(map(str,cmd)),cwd=ROOT,env=env,stdout=out,stderr=err)
    raw=json.loads((a.output/'result.json').read_text());text=(a.output/'stdout.log').read_text();enabled=a.stage=='treatment'
    checks={'runnerExit':r.returncode==0,'lifecycle':raw['lifecycle']=='PASS','sameBaselineErrors':raw['severityRecords']==baseline['severityRecords'],'oneConstructor':text.count('[DF4B] CONSTRUCTED')==1,'functionality':text.count('[DF4B] REGISTRY PASS enabled='+str(enabled).lower()+' owner=helperprobe')==1,'noDumpFail':'dump FAIL' not in text}
    checks['installedArtifactHashes']={v['path'].removeprefix('mods/'):v['sha256'] for v in raw['installedMods']}=={p.name:w.digest(p) for p in mods}
    for name in ['alpha','beta']:
        checks['supplier:'+name]=text.count('[DF4B] SUPPLIER '+name)==(1 if enabled else 0)
        checks['registry:'+name]='[DF4B] QUERY id=helperprobe:'+name+'_item enabled='+str(enabled).lower()+' suppliers='+str(1 if enabled else 0)+' identity=true' in text
    if a.host=='neoforge':
        checks['wholeGroupBeforeClassDefinition']=text.index('HELPER_PLANS_PUBLISHED')<text.index('HELPER_TRANSFORM class=example/helperprobe/HelperProbe')<text.index('[DF4B] CONSTRUCTED')
        for name in ['HelperProbe','ItemHelper','Consumer']:checks['transformed:'+name]=text.count('HELPER_TRANSFORM class=example/helperprobe/'+name+' ')==1
        checks['unusedUntouched']='HELPER_TRANSFORM class=example/helperprobe/Unused' not in text
        checks['bridgeAttachments']=text.count('[DUALFORGE-REGISTRY-BRIDGE] ATTACHED modId=helperprobe ')==(1 if enabled else 0)
        checks['unownedBusRefusals']=text.count('[DF4B] BRIDGE REFUSED unowned-source cases=2')==1
        bus=re.search(r'\[DF4B\] MODBUS owner=helperprobe identity=(\d+)',text)
        attached=re.search(r'\[DUALFORGE-REGISTRY-BRIDGE\] ATTACHED modId=helperprobe .*nativeBus=(\d+)',text)
        checks['independentModBusIdentity']=bool(bus) and (not enabled or bool(attached) and attached.group(1)==bus.group(1))
        checks['builtinDelegate']='delegate -> builtin javafml for neoforge' in text
    if a.native:checks['nativeCoexistence']='[DF007-NATIVE] SERVER_STARTED' in text
    result={'status':'PASS' if all(checks.values()) else 'FAIL','host':a.host,'stage':a.stage,'compatSHA256':w.digest(a.jar) if a.jar else None,'fixtureSHA256':w.digest(fixture),'observerSHA256':w.digest(observer),'installedJARs':{p.name:w.digest(p) for p in mods},'checks':checks,'strictZeroErrors':False,'sourceCommit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'runtimeDirectory':str(a.output),'fixtureEnvironment':{'DF4B_REGISTER':env['DF4B_REGISTER']}}
    w.unchanged(inputs);w.save(run/'result.json',result);print(json.dumps(result,indent=2));assert result['status']=='PASS'
if __name__=='__main__':main()
