"""Same immutable Forge JARs: native/Neo owner isolation and real TOML restart persistence."""
import argparse
import json
import os
from pathlib import Path
import subprocess
import sys
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'mouse_tweaks'))
from support import ENV,JDK,ROOT,fresh,record,run,save

def main():
    p=argparse.ArgumentParser(description=__doc__)
    for name in ['jar','fixtures','observers','output']:p.add_argument('--'+name,type=Path,required=True)
    a=p.parse_args();out=fresh(a.output);tools=Path(__file__).parent
    save(out/'inputs.json',[record(p) for p in [a.jar,*a.fixtures.glob('*.jar'),*a.observers.glob('*/*.jar'),Path(__file__),tools/'run_config_restart.py',ROOT/'tests/real_mods/clumps/run_server.py']])
    results={}
    for side,host in [('forge','forge'),('neo','neoforge')]:
        base=['--host',host,'--install',ENV/('forge-52.1.0' if host=='forge' else 'neoforge-21.1.256'),'--jdk-home',JDK]
        mods=[a.fixtures/'originalcfg.jar',a.observers/side/'configobserver.jar']
        if side=='neo':mods.insert(0,a.jar)
        for mod in mods:base+=['--mod',mod]
        previous=None
        for stage in ['default','modified','persisted']:
            label=side+'-'+stage;destination=out/label
            if stage=='default':argv=[sys.executable,'-B',ROOT/'tests/real_mods/clumps/run_server.py',*base,'--output',destination]
            else:argv=[sys.executable,'-B',tools/'run_config_restart.py','--previous',previous,*(['--change-values'] if stage=='modified' else []),*base,'--output',destination]
            env=os.environ.copy()
            command=list(map(str,argv));save(out/(label+'.command.json'),{'argv':command,'cwd':str(out),'diagnosticEnvironment':{'DF010B_CONFIG_ONLY':env.get('DF010B_CONFIG_ONLY')}})
            proc=subprocess.run(command,cwd=out,env=env,capture_output=True)
            (out/(label+'.stdout.log')).write_bytes(proc.stdout);(out/(label+'.stderr.log')).write_bytes(proc.stderr);save(out/(label+'.exit.json'),{'exitCode':proc.returncode})
            assert proc.returncode==0,label+' stock runtime failure'
            text=(destination/'stdout.log').read_text(errors='replace')
            assert text.count('ORIGINAL_CONFIG_INTEGRATION_PASS')==1,label+' missing actual original config factory/owner proof'
            expected='true,true,false' if stage=='default' else 'false,true,false'
            assert 'actualFactoryRegistration=true values='+expected in text,label+' incorrect actual config values'
            configs={}
            for filename in ['originalcfg-server.toml','configobserver-server.toml']:
                found=list((destination/'server').rglob(filename));assert len(found)==1,found
                configs[filename]={'relativePath':str(found[0].relative_to(destination/'server')),'file':record(found[0])}
            assert Path(configs['originalcfg-server.toml']['relativePath']).parent==Path(configs['configobserver-server.toml']['relativePath']).parent,'source SERVER must use the actual native SERVER policy'
            result=json.loads((destination/'result.json').read_text());assert result['lifecycle']=='PASS'
            results[label]={'status':'PASS','expected':expected,'evidence':str(destination),'lifecycle':result,'actualConfigs':configs,'scope':'actual native lifecycle + diagnostics' if stage=='default' else 'real host config load after stopped-world copy/restart'}
            save(out/'result.json',{'status':'IN_PROGRESS','jar':record(a.jar),'results':results});previous=destination
            print('PASS '+label,flush=True)
    save(out/'result.json',{'status':'PASS','jar':record(a.jar),'results':results,'GUI':'NOT_RUN'})
if __name__=='__main__':main()
