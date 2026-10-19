"""Stock runner with recorded pre-launch config/world copy; JVM arguments remain official."""
import argparse
import importlib.util
import json
from pathlib import Path
import shutil
import sys
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'mouse_tweaks'))
from support import ROOT,record,save,sha

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--previous',type=Path,required=True)
    p.add_argument('--change-values',action='store_true')
    a,rest=p.parse_known_args()
    previous=a.previous.resolve()
    assert json.loads((previous/'result.json').read_text())['lifecycle']=='PASS'
    spec=importlib.util.spec_from_file_location('stock010b',ROOT/'tests/real_mods/clumps/run_server.py')
    stock=importlib.util.module_from_spec(spec);spec.loader.exec_module(stock)
    original=stock.subprocess.Popen
    def launch(argv,**kwargs):
        if argv!=['sh','run.sh','nogui']:
            return original(argv,**kwargs)
        server=Path(kwargs['cwd']);output=server.parent
        assert previous.parent==output.parent and previous!=output
        before={str(p.relative_to(previous/'server')):sha(p) for base in ['config','world'] for p in (previous/'server'/base).rglob('*') if p.is_file()}
        old=json.loads((previous/'installed-inputs.json').read_text())
        expected={r['path']:r['sha256'] for r in old if r['path'].startswith('mods/')}
        assert expected=={str(p.relative_to(server)):sha(p) for p in (server/'mods').iterdir()}
        for name in ['config','world']:shutil.copytree(previous/'server'/name,server/name)
        assert all(sha(server/k)==v for k,v in before.items())
        edited=[]
        if a.change_values:
            for filename,oldtext,newtext in [('originalcfg-server.toml','enableCheatModeForOp = true','enableCheatModeForOp = false')]:
                found=list(server.rglob(filename));assert len(found)==1,found
                f=found[0];text=f.read_text();assert text.count(oldtext)==1
                f.write_text(text.replace(oldtext,newtext));edited.append(record(f))
        save(output/'config-world-copy.json',{'source':str(previous),'sourceFiles':before,'physicalCopyVerified':True,'editsBeforeLaunch':edited,'mechanism':'copy stopped prior experiment; edit official TOML; stock restart'})
        assert all(sha(previous/'server'/k)==v for k,v in before.items())
        return original(argv,**kwargs)
    stock.subprocess.Popen=launch
    sys.argv=[str(ROOT/'tests/real_mods/clumps/run_server.py'),*rest]
    code=stock.main()
    copy=Path(rest[rest.index('--output')+1])/'config-world-copy.json'
    data=json.loads(copy.read_text());data['sourceUnchangedAfterRestart']=all(sha(previous/'server'/k)==v for k,v in data['sourceFiles'].items());save(copy,data)
    assert data['sourceUnchangedAfterRestart']
    raise SystemExit(code)
if __name__=='__main__':main()
