"""Reclaim only verified duplicate 010E runtime libraries; keep pinned SDKs and all run evidence."""
import argparse
import json
from pathlib import Path
import shutil
import subprocess
import sys
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'mouse_tweaks'))
from support import ENV,record,save,sha

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--run',type=Path,required=True);p.add_argument('--receipt',type=Path,required=True);a=p.parse_args();root=a.run.resolve()
    assert root==ENV/'runs/convergence-010e' and not a.receipt.exists()
    assert subprocess.run(['pgrep','-x','java'],capture_output=True).returncode==1,'never touch a running game installation'
    receipts=[]
    for result in sorted(root.rglob('result.json')):
        directory=result.parent;library=directory/'server/libraries'
        if not library.is_dir():continue
        data=json.loads(result.read_text());assert data.get('checks',{}).get('inputsUnchanged') is True,result
        manifest=json.loads((directory/'installed-inputs.json').read_text());rows=[row for row in manifest if row['path'].startswith('libraries/')]
        files=sorted(p for p in library.rglob('*') if p.is_file());assert len(files)==len(rows)
        # Select by the exact source file bytes, including layouts whose artifact directory differs.
        choices=[sdk for sdk in [ENV/'forge-52.1.0',ENV/'neoforge-21.1.256'] if all((sdk/row['path']).is_file() for row in rows)]
        matching=[sdk for sdk in choices if all(sha(sdk/row['path'])==row['sha256'] for row in rows)];assert len(matching)==1,(directory,matching)
        assert all(sha(directory/'server'/row['path'])==row['sha256'] for row in rows)
        size=sum(f.stat().st_size for f in files);receipts.append({'run':str(directory),'sdk':str(matching[0]),'inputManifest':record(directory/'installed-inputs.json'),'files':len(files),'reclaimedBytes':size,'restoration':'copy sdk/libraries back to this run/server/libraries; immutable bytes verified before removal'})
        assert library.resolve().is_relative_to(root) and library.name=='libraries' and library.parent.name=='server';shutil.rmtree(library)
    save(a.receipt,{'status':'PASS','scope':'only this task generated and fully verified duplicate runtime SDK copies; no mods/world/logs/input SDK/history deletion','runs':receipts,'reclaimedBytes':sum(r['reclaimedBytes'] for r in receipts)})
    print('010E_DUPLICATE_LIBRARY_RETENTION_PASS runs='+str(len(receipts)))
if __name__=='__main__':main()
