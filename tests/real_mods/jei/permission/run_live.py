"""Serial native/compat registration plus stopped-world restart; no client or packets."""
import argparse
import importlib.util
import json
from pathlib import Path
import shutil
import sys
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'mouse_tweaks'))
from support import ENV,JDK,ROOT,fresh,record,run,save,sha

def restart():
    modified='--modified' in sys.argv
    if modified:sys.argv.remove('--modified')
    previous=Path(sys.argv[sys.argv.index('--previous')+1]);i=sys.argv.index('--previous');del sys.argv[i:i+2]
    assert json.loads((previous/'result.json').read_text())['lifecycle']=='PASS'
    spec=importlib.util.spec_from_file_location('network_stock',ROOT/'tests/real_mods/clumps/run_server.py');stock=importlib.util.module_from_spec(spec);spec.loader.exec_module(stock);original=stock.subprocess.Popen;copied={}
    def launch(argv,**kwargs):
        if argv==['sh','run.sh','nogui']:
            server=Path(kwargs['cwd']);before={str(p.relative_to(previous/'server')):sha(p) for name in ['world','config'] for p in (previous/'server'/name).rglob('*') if p.is_file()}
            for name in ['world','config']:shutil.copytree(previous/'server'/name,server/name)
            copied.update(before);assert all(sha(server/k)==v for k,v in before.items());save(server.parent/'restart-copy.json',{'source':str(previous),'files':before,'physicalCopyVerified':True,'worldPersistenceScope':'no players/packet delivery'})
            if modified:
                configs=[p for name in ['config','world/serverconfig'] for p in (server/name).glob('gatealpha-server.toml')];assert len(configs)==1
                config=configs[0];text=config.read_text();assert text.count('enabled = true')==1;old=record(config);config.write_text(text.replace('enabled = true','enabled = false'));save(server.parent/'restart-config-change.json',{'scope':'new copied run only; previous evidence untouched','before':old,'after':record(config),'expected':'alpha=false beta=true'})
        return original(argv,**kwargs)
    stock.subprocess.Popen=launch;sys.argv[0]=str(ROOT/'tests/real_mods/clumps/run_server.py');code=stock.main()
    assert copied and all(sha(previous/'server'/k)==v for k,v in copied.items())
    return code

def main():
    p=argparse.ArgumentParser(description=__doc__)
    for name in ['jar','fixtures','observers','output']:p.add_argument('--'+name,type=Path,required=True)
    a=p.parse_args();out=fresh(a.output);results={}
    save(out/'inputs.json',[record(q) for q in [a.jar,Path(__file__),*a.fixtures.glob('*/*.jar'),*a.observers.glob('*/*.jar')]])
    for side,host in [('forge','forge'),('neo','neoforge')]:
        base=['--host',host,'--install',ENV/('forge-52.1.0' if side=='forge' else 'neoforge-21.1.256'),'--jdk-home',JDK]
        mods=[a.fixtures/'gatealpha/gatealpha.jar',a.fixtures/'gatebeta/gatebeta.jar',a.observers/side/'permissionobserver.jar']
        if side=='neo':mods.insert(0,a.jar)
        for mod in mods:base+=['--mod',mod]
        previous=None
        for stage in ['first','restart']:
            label=side+'-'+stage;destination=out/label
            argv=[sys.executable,'-B',ROOT/'tests/real_mods/clumps/run_server.py',*base,'--output',destination] if previous is None else [sys.executable,'-B',Path(__file__),'--previous',previous,'--modified',*base,'--output',destination]
            run(out,label,argv);text=(destination/'stdout.log').read_text(errors='replace');actual=json.loads((destination/'result.json').read_text());assert actual['lifecycle']=='PASS',actual
            assert text.count('PERMISSION_OBSERVER_PASS')==1 and text.count('PERMISSION_COMPONENT_PASS cases=320')==1,label
            assert 'PERMISSION_OBSERVER_FAIL' not in text and 'AssertionError' not in text
            expected='creative='+('true' if stage=='first' else 'false')+' op=true give=false';assert 'PERMISSION_REGISTERED_CONFIG owner=org.example.gatealpha '+expected in text
            assert 'PERMISSION_REGISTERED_CONFIG owner=org.example.gatebeta creative=true op=true give=false' in text
            if side=='neo':
                assert text.count('PERMISSION_IDENTITY_NEGATIVE_PASS')==1 and text.count('DERIVED_READONLY_DEPENDENCY_PASS')==2
                for code in ['WRONG_MODULE','WRONG_CLASSLOADER','WRONG_MOD_CONTAINER','WRONG_SERVER_AUTHORITY','WRONG_PERMISSION_THREAD','WRONG_PLAYER_SOURCE']:assert 'DERIVED_OWNER_NEGATIVE '+code in text
            results[label]={'status':'PASS','lifecycle':actual,'evidence':str(destination),'results':[s for s in text.splitlines() if any(k in s for k in ['PERMISSION_COMPONENT_PASS','PERMISSION_REGISTERED_CONFIG','PERMISSION_OBSERVER_PASS','PERMISSION_IDENTITY_NEGATIVE_PASS','DERIVED_OWNER_NEGATIVE'])]}
            save(out/'result.json',{'status':'IN_PROGRESS','jar':record(a.jar),'results':results});print('PASS '+label,flush=True);previous=destination
    save(out/'result.json',{'status':'PASS','jar':record(a.jar),'results':results,'REAL_SERVER_PLAYER':'NOT_RUN','REAL_CONNECTED_PLAYER':'NOT_RUN','REAL_PACKET_DELIVERY':'NOT_RUN','scope':'real FML registration, ordinary unconnected Player predicate/counter component, fail-closed identity models, copied-world restart; no ServerPlayer created'})
if __name__=='__main__':
    if '--previous' in sys.argv:raise SystemExit(restart())
    main()
