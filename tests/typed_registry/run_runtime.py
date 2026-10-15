"""Stock typed Registry lifecycle with exact installed SHA and delayed-callback assertions."""
import argparse,importlib.util,json,os,subprocess,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('w',ROOT/'tools/lib/unix_workflow.py');w=importlib.util.module_from_spec(spec);spec.loader.exec_module(w)
def main():
 p=argparse.ArgumentParser(description=__doc__)
 for n in ['fixture-index','install','jdk-home','baseline','output']:p.add_argument('--'+n,type=Path,required=True)
 p.add_argument('--host',choices=['forge','neoforge'],required=True);p.add_argument('--stage',choices=['control','treatment','baseline'],required=True);p.add_argument('--jar',type=Path);p.add_argument('--native',type=Path);a=p.parse_args()
 for k,v in vars(a).items():
  if isinstance(v,Path):setattr(a,k,v.resolve())
 art=json.loads(a.fixture_index.read_text())['artifacts'];mods=[Path(art[k]['Path']) for k in ['fixture',a.host+'Observer']]
 for key,jar in zip(['fixture',a.host+'Observer'],mods):assert w.digest(jar)==art[key]['SHA256']
 if a.host=='neoforge':assert a.jar and a.jar.name=='DualForge.jar';mods.insert(0,a.jar)
 if a.native:mods.append(a.native)
 run=a.output.parent/(a.output.name+'-invocation');run.mkdir(parents=True,exist_ok=False);env=os.environ.copy();env['DF4C2_REGISTER']='false' if a.stage=='control' else 'true'
 cmd=['python3','-B',ROOT/'tests/real_mods/food_n_fuel/run_stock.py','--host',a.host,'--install',a.install,'--jdk-home',a.jdk_home,'--output',a.output]
 for m in mods:cmd+=['--mod',m]
 inputs=w.fingerprints([*mods,a.fixture_index,Path(__file__)]);w.save(run/'inputs.json',inputs);w.save(run/'command.json',{'argv':list(map(str,cmd)),'cwd':str(ROOT),'fixtureEnvironment':{'DF4C2_REGISTER':env['DF4C2_REGISTER']}})
 with (run/'stdout.log').open('w') as out,(run/'stderr.log').open('w') as err:r=subprocess.run(list(map(str,cmd)),cwd=ROOT,env=env,stdout=out,stderr=err)
 raw=json.loads((a.output/'result.json').read_text());text=(a.output/'stdout.log').read_text();debug='\n'.join(f.read_text(errors='replace') for f in (a.output/'logs').glob('*.log'));alltext=text+debug+(a.output/'stderr.log').read_text();enabled=a.stage!='control'
 checks={'installedSHA':{i['path'].removeprefix('mods/'):i['sha256'] for i in raw['installedMods']}=={p.name:w.digest(p) for p in mods},'stockNoProperties':raw['zeroDualForgeParameters']}
 if a.stage=='baseline':checks.update(expectedRefusal=r.returncode!=0 and 'Registry helper rejected: ITEM_FIELD_DESCRIPTOR' in alltext,beforeConstruction='[DF4C2] CONSTRUCTED' not in text and 'Done (' not in text,realMetadata='ForgeModReader: found META-INF/mods.toml' in text and '{tabboundary}' in alltext)
 else:
  checks.update(runnerExit=r.returncode==0,lifecycle=raw['lifecycle']=='PASS',sameBaselineErrors=raw['severityRecords']==json.loads(a.baseline.read_text())['severityRecords'],constructedOnce=text.count('[DF4C2] CONSTRUCTED')==1,registryAndCallbacks=text.count('[DF4C2] TAB PASS enabled='+str(enabled).lower()+' itemAndTabSamePath=true icon='+str(int(enabled))+' display='+str(int(enabled)))==1,earlyRefused=text.count('[DF4C2] EARLY REFUSED ')==1,noDumpFailure='dump FAIL' not in alltext)
  for name in ['first','second','tab']:checks['supplier:'+name]=text.count('[DF4C2] SUPPLIER '+name+'\n')==int(enabled)
  for name in ['icon','display']:checks['callback:'+name]=text.count('[DF4C2] CALLBACK '+name+'\n')==int(enabled)
  if a.host=='neoforge':
   for name in ['EntryPoint','Items','Shelves','Consumer']:checks['target:'+name]=text.count('HELPER_TRANSFORM class=demo/shelves/'+name+' ')==1
   checks['publishBeforeDefine']=text.index('HELPER_PLANS_PUBLISHED')<text.index('HELPER_TRANSFORM class=demo/shelves/EntryPoint')<text.index('[DF4C2] CONSTRUCTED')
   identity=re.search(r'\[DF4C2\] MODBUS identity=(\d+)',text);bus=re.findall(r'ATTACHED modId=tabboundary .*nativeBus=(\d+)',text)
   checks['realModBus']=bool(identity) and (not enabled or bus==[identity[1],identity[1]])
   checks['typedBridgeItem']=len(re.findall(r'ATTACHED modId=tabboundary .*registry=minecraft:item ',text))==int(enabled);checks['typedBridgeTab']=len(re.findall(r'ATTACHED modId=tabboundary .*registry=minecraft:creative_mode_tab ',text))==int(enabled)
   checks['singleDiscovery']=(a.output/'logs/debug.log').read_text().count('Found mod file "tab-boundary.jar" [locator: dualforge locator')==1
   checks['builtinDelegate']='delegate -> builtin javafml for neoforge' in text
  if a.native:checks['nativeCoexistence']='[DF007-NATIVE] SERVER_STARTED' in text
 result={'status':'PASS' if all(checks.values()) else 'FAIL','host':a.host,'stage':a.stage,'checks':checks,'compatSHA256':w.digest(a.jar) if a.jar else None,'fixtureSHA256':art['fixture']['SHA256'],'observerSHA256':art[a.host+'Observer']['SHA256'],'installedJARs':{p.name:w.digest(p) for p in mods},'fixtureEnvironment':{'DF4C2_REGISTER':env['DF4C2_REGISTER']},'sourceCommit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'strictZeroErrors':False};w.unchanged(inputs);w.save(run/'result.json',result);print(json.dumps(result,indent=2));assert result['status']=='PASS'
if __name__=='__main__':main()
