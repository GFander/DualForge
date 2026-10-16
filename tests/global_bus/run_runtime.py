"""Real global EventBus and typed Registry lifecycle, with exact installed cohort identity."""
import argparse,importlib.util,json,os,subprocess,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('w',ROOT/'tools/lib/unix_workflow.py');w=importlib.util.module_from_spec(spec);spec.loader.exec_module(w)
def main():
 p=argparse.ArgumentParser(description=__doc__)
 for n in ['fixture-index','install','jdk-home','baseline','output']:p.add_argument('--'+n,type=Path,required=True)
 p.add_argument('--host',choices=['forge','neoforge'],required=True);p.add_argument('--variant',choices=['quiet','signal','combined'],required=True);p.add_argument('--stage',choices=['control','treatment'],default='treatment');p.add_argument('--jar',type=Path);p.add_argument('--native',type=Path);a=p.parse_args()
 for k,v in vars(a).items():
  if isinstance(v,Path):setattr(a,k,v.resolve())
 art=json.loads(a.fixture_index.read_text())['artifacts'];mods=[Path(art[k]['Path']) for k in [a.variant,a.host+'Observer']]
 for key,j in zip([a.variant,a.host+'Observer'],mods):assert w.digest(j)==art[key]['SHA256']
 if a.host=='neoforge':assert a.jar and a.jar.name=='DualForge.jar';mods.insert(0,a.jar)
 if a.native:mods.append(a.native)
 run=a.output.parent/(a.output.name+'-invocation');run.mkdir(parents=True,exist_ok=False);env=os.environ.copy();env['DF4C3_VARIANT']=a.variant;env['DF4C3_REGISTER']='false' if a.stage=='control' else 'true';cmd=['python3','-B',ROOT/'tests/real_mods/food_n_fuel/run_stock.py','--host',a.host,'--install',a.install,'--jdk-home',a.jdk_home,'--output',a.output]
 for m in mods:cmd+=['--mod',m]
 inputs=w.fingerprints([*mods,a.fixture_index,Path(__file__)]);w.save(run/'inputs.json',inputs);w.save(run/'command.json',{'argv':list(map(str,cmd)),'cwd':str(ROOT),'fixtureEnvironment':{k:env[k] for k in ['DF4C3_VARIANT','DF4C3_REGISTER']}})
 with (run/'stdout.log').open('w') as out,(run/'stderr.log').open('w') as err:r=subprocess.run(list(map(str,cmd)),cwd=ROOT,env=env,stdout=out,stderr=err)
 raw=json.loads((a.output/'result.json').read_text());text=(a.output/'stdout.log').read_text();debug='\n'.join(f.read_text(errors='replace') for f in (a.output/'logs').glob('*.log'));alltext=text+debug+(a.output/'stderr.log').read_text();enabled=a.stage=='treatment';identity=re.search(r'\[DF4C3\] IDENTITIES global=(\d+) mod=(\d+) object=(\d+)',text)
 checks={'installedSHA':{i['path'].removeprefix('mods/'):i['sha256'] for i in raw['installedMods']}=={p.name:w.digest(p) for p in mods},'stockNoProperties':raw['zeroDualForgeParameters'],'runnerExit':r.returncode==0,'lifecycle':raw['lifecycle']=='PASS','baselineErrors':raw['severityRecords']==json.loads(a.baseline.read_text())['severityRecords'],'constructedOnce':text.count('[DF4C3] CONSTRUCTED '+a.variant)==1,'handlerCount':text.count('[DF4C3] HANDLER '+a.variant+'\n')==int(a.variant!='quiet'),'runtimeProof':text.count('[DF4C3] PASS variant='+a.variant+' handlers='+str(int(a.variant!='quiet')))==1,'distinctActualBuses':bool(identity) and identity[1]!=identity[2],'noDumpFail':'dump FAIL' not in alltext}
 if a.variant=='combined':checks.update(registryProof=text.count('[DF4C3] REGISTRY PASS enabled='+str(enabled).lower())==1,earlyRefusal=text.count('[DF4C3] EARLY REFUSED')==1)
 if a.host=='neoforge':
  result=re.search(r'\[DUALFORGE-GLOBAL-BRIDGE\] REGISTERED modId=\w+ object=(\d+) globalBus=(\d+) listeners=(\d+) outcome=(\w+) plan=([0-9a-f]+)',text)
  checks['officialGlobalInvocation']=bool(result) and bool(identity) and (a.variant=='quiet' and 'nativeSourceObjects=0' in text or a.variant!='quiet' and result[1]==identity[3]) and result[2]==identity[1] and int(result[3])==int(a.variant!='quiet') and result[4]==('EMPTY_SOURCE_TRANSLATION' if a.variant=='quiet' else 'NATIVE_RETURN')
  names=['EntryPoint','Items','Shelves','Consumer'] if a.variant=='combined' else ['EntryPoint']
  for name in names:checks['target:'+name]=text.count('HELPER_TRANSFORM class=org/example/'+a.variant+'bus/'+name+' ')==1
  checks['publishBeforeDefine']=text.index('HELPER_PLANS_PUBLISHED')<text.index('HELPER_TRANSFORM class=org/example/'+a.variant+'bus/EntryPoint')<text.index('[DF4C3] CONSTRUCTED') if 'HELPER_PLANS_PUBLISHED' in text and 'HELPER_TRANSFORM class=' in text else False
  checks['builtinDelegate']='delegate -> builtin javafml for neoforge' in text
  if a.variant=='combined':
   buses=re.findall(r'ATTACHED modId=catalogbus .*nativeBus=(\d+)',text);checks['actualModBridge']=buses==[identity[2],identity[2]] if enabled and identity else not enabled and not buses
 if a.native:checks['nativeCoexistence']='[DF007-NATIVE] SERVER_STARTED' in text
 result={'status':'PASS' if all(checks.values()) else 'FAIL','checks':checks,'host':a.host,'variant':a.variant,'stage':a.stage,'compatSHA256':w.digest(a.jar) if a.jar else None,'fixtureSHA256':art[a.variant]['SHA256'],'observerSHA256':art[a.host+'Observer']['SHA256'],'installedJARs':{m.name:w.digest(m) for m in mods},'fixtureEnvironment':{k:env[k] for k in ['DF4C3_VARIANT','DF4C3_REGISTER']},'strictZeroErrors':False,'sourceCommit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()};w.unchanged(inputs);w.save(run/'result.json',result);print(json.dumps(result,indent=2));assert result['status']=='PASS'
if __name__=='__main__':main()
