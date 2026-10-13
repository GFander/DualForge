"""Package the verified 006A server cohort for separate manual Mac acceptance; no client execution."""
import argparse,hashlib,json,shutil,subprocess,zipfile
from pathlib import Path
SOURCE=Path(__file__).resolve().parent
REPO=SOURCE.parents[1]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def build(args):
    report=json.loads(args.validation.read_text())
    assert report['006A-S']=='PASS' and report['sameArtifactCohort']
    for name,path in [('compat',args.compat),('fixture',args.fixture)]:
        assert report[name]['Path']==str(path.resolve()) and report[name]['SHA256']==sha(path)
    for stage,path in report['stages'].items():
        result=json.loads(Path(path).read_text())
        assert result['status']=='PASS' and result['compat']['SHA256']==sha(args.compat)
    with zipfile.ZipFile(args.fixture) as archive:
        entry=archive.read('com/example/multiregistrycheck/MultiRegistryProbe.class')
        assert b'net/neoforged/' not in entry and b'net/minecraftforge/registries/DeferredRegister' in entry
        assert b'Automatic-Module-Name: multiregistryprobe' in archive.read('META-INF/MANIFEST.MF')
    out=args.output_dir.resolve();assert out.is_relative_to(REPO/'out');out.mkdir(parents=True,exist_ok=False)
    root=out/'dualforge-006a-mac-test-kit';(root/'mods').mkdir(parents=True)
    shutil.copyfile(args.compat,root/'mods/dualforge.jar');shutil.copyfile(args.fixture,root/'mods/multi-registry-probe.jar')
    for name in ('MAC-SETUP.md','JVM-ARGS.txt','CHECKLIST.md'):shutil.copyfile(SOURCE/name,root/name)
    payload=sorted(p for p in root.rglob('*') if p.is_file())
    manifest={'task':'Convergence-006A','clientStatus':'NOT RUN','serverStatus':'PASS','minecraft':'1.21.1','neoforge':'21.1.256','javaMajor':21,'sourceCommit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=REPO,text=True).strip(),'compat':report['compat'],'fixture':report['fixture'],'testDriverIncluded':False,'uploadUrl':None,'files':{str(p.relative_to(root)):sha(p) for p in payload}}
    (root/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
    (root/'SHA256SUMS').write_text(''.join(f'{sha(p)}  {p.relative_to(root)}\n' for p in sorted(root.rglob('*')) if p.is_file()))
    target=out/'dualforge-006a-mac-test-kit.zip'
    with zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED) as archive:
        for p in sorted(root.rglob('*')):
            if p.is_file():archive.write(p,str(p.relative_to(out)))
    with zipfile.ZipFile(target) as archive:
        assert archive.testzip() is None
        for line in archive.read(root.name+'/SHA256SUMS').decode().splitlines():
            digest,name=line.split('  ',1);assert hashlib.sha256(archive.read(root.name+'/'+name)).hexdigest()==digest
    result={'status':'PASS','scope':'artifact packaging, not Mac execution','sourceCommit':manifest['sourceCommit'],'zip':str(target),'SHA256':sha(target),'bytes':target.stat().st_size,'clientStatus':'NOT RUN','uploaded':False,'contents':len(archive.namelist())}
    (out/'result.json').write_text(json.dumps(result,indent=2)+'\n');(out/(target.name+'.sha256')).write_text(sha(target)+'  '+target.name+'\n');print(json.dumps(result,indent=2))
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    for name in ('compat','fixture','validation','output-dir'):p.add_argument('--'+name,type=Path,required=True)
    build(p.parse_args())
