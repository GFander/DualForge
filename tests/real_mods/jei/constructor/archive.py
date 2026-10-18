"""Archive selected current evidence and manifests; omit SDK/library copies and earlier failed runs."""
import argparse
import json
from pathlib import Path
import sys
import zipfile
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'mouse_tweaks'))
from support import ENV,record,save

SELECTED=['intake','abi-current','locale-api-current','native-observer-2','jei-native-locale-2','build-current','fixture-build-2','observers-final','contracts-current','dual-contract-current','live-verified','dual-live-current','original-config-fixture','config-observers','original-config-live','boundary-atomic','jei-current','owner-negative-current','regressions-current','mouse-current','helper-current','local-current']

def main():
    p=argparse.ArgumentParser(description=__doc__)
    for name in ['run','output']:p.add_argument('--'+name,type=Path,required=True)
    a=p.parse_args();root=a.run.resolve();target=a.output.resolve();assert root.is_relative_to(ENV/'runs') and target.parent==root and not target.exists()
    assert json.loads((root/'assessment-final.json').read_text())['SCOPED_AUTOMATED']=='PASS'
    files=sorted({p for name in SELECTED for p in (root/name).rglob('*') if p.is_file() and 'libraries' not in p.relative_to(root).parts}|{p for p in root.glob('*.json') if p.is_file()})
    rows=[{'member':str(p.relative_to(root)),**record(p)} for p in files]
    manifest=root/'evidence-current-manifest.json';assert not manifest.exists();save(manifest,{'scope':'selected current raw evidence; no SDK/library bytes; earlier failures retained outside archive','selectedDirectories':SELECTED,'files':rows})
    with zipfile.ZipFile(target,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as archive:
        for p in files+[manifest]:archive.write(p,str(p.relative_to(root)))
    with zipfile.ZipFile(target) as archive:
        assert archive.testzip() is None;members=len(archive.namelist())
    save(root/'archive-receipt.json',{'status':'PASS','archive':record(target),'members':members,'manifest':record(manifest),'publication':'LOCAL_SSD_ONLY'})
    print('EVIDENCE_ARCHIVE_PASS members='+str(members))
if __name__=='__main__':main()
