"""Two-dist actual Reader/planner refusal model; never a client boot."""
import argparse
from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'mouse_tweaks'))
from support import ENV,JDK,fresh,record,run,save

def main():
    p=argparse.ArgumentParser(description=__doc__)
    for name in ['jar','fixture','output']:p.add_argument('--'+name,type=Path,required=True)
    a=p.parse_args();out=fresh(a.output);classes=out/'classes';classes.mkdir()
    source=Path(__file__).with_name('NetworkBoundaryContract.java')
    original=ENV/'runs/convergence-010a-jei/inputs/jei-1.21.1-forge-19.27.0.340.jar'
    deps=sorted((ENV/'runs/convergence-009c-20261017/client-abi/neo/libraries').rglob('*.jar'))
    host=next(p for p in deps if p.name=='neoforge-21.1.256-universal.jar')
    cp=':'.join(map(str,[a.jar,*deps]));save(out/'inputs.json',[record(p) for p in [source,a.jar,a.fixture,original,*deps]])
    run(out,'javac',[JDK/'bin/javac','--release','21','-proc:none','-cp',cp,'-d',classes,source])
    run(out,'boundary',[JDK/'bin/java','-Xverify:all','--add-opens','java.base/java.lang.invoke=ALL-UNNAMED','--add-exports','java.base/sun.security.util=ALL-UNNAMED','-cp',str(classes)+':'+cp,'io.github.gfander.dualforge.NetworkBoundaryContract',original,host,a.fixture])
    save(out/'result.json',{'status':'PASS','jar':record(a.jar),'scope':'TWO_DIST_READER_PLANNER_MODEL; no GAME layer or client launch'})
if __name__=='__main__':main()
