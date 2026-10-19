"""Record the repository's required local checks without creating JVM output on SMB."""
import argparse
import re
from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'mouse_tweaks'))
from support import ROOT,fresh,run,save

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',type=Path,required=True);a=p.parse_args();out=fresh(a.output)
    total=0
    for name in ['events','registry','project_identity','multi_registry','block_registry','api_atlas','helper_registry','supplier_boundary','typed_registry','global_bus','real_mods/food_n_fuel/behavior']:
        label=name.replace('/','-')
        run(out,label,[sys.executable,'-B','-m','unittest','discover','-s',ROOT/'tests'/name,'-p','test_*.py','-v'])
        matches=re.findall(r'Ran (\d+) tests?',(out/(label+'.stderr.log')).read_text());assert len(matches)==1;total+=int(matches[0])
    assert total==106,total
    text=run(out,'identity',[sys.executable,'-B',ROOT/'tests/project_identity/check_identity.py']);assert '374' in text,text
    scripts=sorted(p for folder in ['tools','tests'] for p in (ROOT/folder).rglob('*.zsh'));assert len(scripts)==25
    for i,script in enumerate(scripts):run(out,'zsh-'+str(i),['zsh','-n',script])
    run(out,'whitespace',['git','-C',ROOT,'diff','--check'])
    for src in Path(__file__).parent.glob('*.py'):compile(src.read_text(),str(src),'exec')
    save(out/'result.json',{'status':'PASS','pythonTests':total,'zshScripts':len(scripts),'protectedFiles':374,'newPythonSyntax':'PASS'})
    print('LOCAL_CHECKS_PASS python='+str(total)+' zsh='+str(len(scripts))+' protected=374')
if __name__=='__main__':main()
