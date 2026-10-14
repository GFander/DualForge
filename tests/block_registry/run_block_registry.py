"""Controlled Block registry stages; physical copies and serial JVMs use the existing harness."""
import argparse,importlib.util,json,re,traceback
from pathlib import Path
REPO=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('registry',REPO/'tests/registry/run_registry.py');r=importlib.util.module_from_spec(spec);spec.loader.exec_module(r)
w=r.w
ROOT=REPO/'out/convergence-006b'
ENTRY='com.example.blockregistrycheck.BlockRegistryProbe'

def baseline(args,run,binary):
    w.server_preflight(args,args.jar)
    artifacts=[args.jar,args.fixture,args.driver];w.save(run/'artifacts.json',w.fingerprints(artifacts))
    meta,logs,server=w.harness(args,run,binary,args.fixture,args.jar,installed_mod_name=args.fixture.name,extra_mods=(args.driver,),extra_jvm_args=('-DDF006B_REGISTER=true',))
    return {'status':'OBSERVED','compat':r.fingerprint(args.jar),'fixture':r.fingerprint(args.fixture),'driver':r.fingerprint(args.driver),'meta':meta,'logs':str(logs),'note':'Actual pre-change observation; root-cause assertions are recorded only after reading raw logs.'}

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--stage',choices=('baseline',),required=True)
    for key in ('jar','fixture','driver','server-input','jdk-home','java-network-args'):p.add_argument('--'+key,type=Path,required=key!='java-network-args')
    p.add_argument('--allow-server-start',action='store_true');p.add_argument('--public-key-relay',action='store_true')
    p.add_argument('--max-wait-seconds',type=int,default=120);p.add_argument('--post-done-seconds',type=int,default=10)
    args=p.parse_args()
    for key,value in vars(args).items():
        if isinstance(value,Path):setattr(args,key,value.resolve())
    run=w.run_directory(ROOT/args.stage);w.save(run/'invocation.json',{k:str(v) if isinstance(v,Path) else v for k,v in vars(args).items()})
    result={'status':'FAIL','stage':args.stage,'directory':str(run)}
    try:
        frozen=json.loads((ROOT/'fixture.json').read_text())['jars']
        for key,name in [('fixture','block-registry-probe'),('driver','block-registry-driver')]:assert w.digest(getattr(args,key))==frozen[name]['SHA256']
        binary=w.tools(args,run);result.update(baseline(args,run,binary))
    except Exception as error:
        result.update(error=str(error));(run/'failure.txt').write_text(traceback.format_exc());raise
    finally:w.save(run/'result.json',result);print('RESULT',result['status'],run)
if __name__=='__main__':main()
