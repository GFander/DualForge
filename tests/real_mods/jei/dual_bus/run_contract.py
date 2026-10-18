"""Serial immutable-plan/negative and isolated native API validation against pinned libraries."""
import argparse
from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'mouse_tweaks'))
from support import ENV,JDK,fresh,record,run,save

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--jar',type=Path,required=True);p.add_argument('--fixtures',type=Path,required=True);p.add_argument('--output',type=Path,required=True)
    a=p.parse_args();out=fresh(a.output);src=Path(__file__).parent
    sources=[src/'PlanContract.java',src/'NativeBusContract.java']
    sdk=ENV/'runs/convergence-009c-20261017/client-abi'
    deps=sorted((sdk/'neo/libraries').rglob('*.jar'));classes=out/'classes';classes.mkdir()
    cp=':'.join(map(str,[a.jar,*deps]));save(out/'inputs.json',[record(p) for p in [a.jar,ENV/'inputs/fnf-forge-1.3-1.21.x.jar',*sources,*deps,*a.fixtures.glob('*/*.jar')]])
    run(out,'javac',[JDK/'bin/javac','--release','21','-proc:none','-cp',cp,'-d',classes,*sources])
    run(out,'plan',[JDK/'bin/java','-Xverify:all','-cp',str(classes)+':'+cp,'io.github.gfander.dualforge.PlanContract',a.fixtures/'busalpha/busalpha.jar',a.fixtures/'busbeta/busbeta.jar',sdk/'neo/libraries',out/'transformed',ENV/'inputs/fnf-forge-1.3-1.21.x.jar'])
    for side in ['forge','neo']:
        jars=sorted((sdk/side/'libraries').rglob('*.jar'))
        run(out,side+'-native-api',[JDK/'bin/java','-Xverify:all','-cp',':'.join(map(str,[classes,*jars])),'io.github.gfander.dualforge.NativeBusContract',side])
    save(out/'result.json',{'status':'PASS','jar':record(a.jar),'scope':'BYTECODE_PLAN_AND_ISOLATED_NATIVE_API; GAME layer is separate'})
if __name__=='__main__':main()
