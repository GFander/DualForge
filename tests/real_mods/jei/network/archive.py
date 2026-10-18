"""Keep selected 010D evidence on SSD, excluding installed SDK/library duplicates."""
import argparse
import json
from pathlib import Path
import sys
import zipfile
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'mouse_tweaks'))
from support import ENV,record,save
SELECTED=['intake','abi-current','analysis-current-2','build-3','fixture-build-2','cohort-final','regressions-current']
def main():
    p=argparse.ArgumentParser(description=__doc__)
    for n in ['run','output']:p.add_argument('--'+n,type=Path,required=True)
    a=p.parse_args();root=a.run.resolve();target=a.output.resolve();assert root.is_relative_to(ENV/'runs') and target.parent==root and not target.exists()
    assert json.loads((root/'assessment-final.json').read_text())['SCOPED_AUTOMATED']=='PASS'
    files=sorted({p for name in SELECTED for p in (root/name).rglob('*') if p.is_file() and 'libraries' not in p.relative_to(root).parts}|set(root.glob('*.json')))
    rows=[{'member':str(p.relative_to(root)),**record(p)} for p in files];manifest=root/'evidence-current-manifest.json';assert not manifest.exists();save(manifest,{'scope':'selected new current evidence; SDK fingerprints retained; failures remain outside archive','selectedDirectories':SELECTED,'files':rows})
    with zipfile.ZipFile(target,'x',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        for p in files+[manifest]:z.write(p,str(p.relative_to(root)))
    with zipfile.ZipFile(target) as z:assert z.testzip() is None;count=len(z.namelist())
    save(root/'archive-receipt.json',{'status':'PASS','archive':record(target),'members':count,'manifest':record(manifest),'publication':'LOCAL_SSD_ONLY'})
    print('010D_EVIDENCE_ARCHIVE_PASS members='+str(count))
if __name__=='__main__':main()
