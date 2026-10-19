"""Finite permission plan, original source predicate and independent refusal mutations."""
import argparse,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'mouse_tweaks'))
from support import ENV,JDK,fresh,record,run,save
def main():
 p=argparse.ArgumentParser(description=__doc__)
 for n in ['jar','fixtures','abi','output']:p.add_argument('--'+n,type=Path,required=True)
 a=p.parse_args();out=fresh(a.output);deps=sorted((ENV/'runs/convergence-009c-20261017/client-abi/neo/libraries').rglob('*.jar'));src=Path(__file__).with_name('PermissionContract.java');classes=out/'classes';classes.mkdir();cp=':'.join(map(str,[a.jar,*deps]));original=ENV/'runs/convergence-010a-jei/inputs/jei-1.21.1-forge-19.27.0.340.jar'
 save(out/'inputs.json',[record(q) for q in [a.jar,src,original,*a.fixtures.glob('*/*.jar'),*deps]])
 run(out,'javac',[JDK/'bin/javac','--release','21','-proc:none','-cp',cp,'-d',classes,src]);run(out,'plan',[JDK/'bin/java','-Xverify:all','-cp',str(classes)+':'+cp,'io.github.gfander.dualforge.PermissionContract',a.fixtures/'gatealpha/gatealpha.jar',a.fixtures/'gatebeta/gatebeta.jar',ENV/'runs/convergence-009c-20261017/client-abi/neo/libraries',a.abi/'neo',original]);save(out/'result.json',{'status':'PASS','jar':record(a.jar),'scope':'STATIC_SOURCE/CONTRACT_MODEL; no real player/connection'})
if __name__=='__main__':main()
