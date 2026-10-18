"""Compile a separate Forge diagnostic for the untouched JEI locale initializer."""
import argparse
from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'mouse_tweaks'))
from support import ENV,JDK,fresh,record,run,save

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',type=Path,required=True);a=p.parse_args();out=fresh(a.output)
    deps=sorted((ENV/'runs/convergence-009c-20261017/client-abi/forge/libraries').rglob('*.jar'))
    template=Path(__file__).with_name('LocaleObserver.java.in');src=out/'LocaleObserver.java';src.write_text(template.read_text());classes=out/'classes';classes.mkdir()
    save(out/'inputs.json',[record(p) for p in [template,Path(__file__),*deps]])
    run(out,'javac',[JDK/'bin/javac','--release','21','-proc:none','-cp',':'.join(map(str,deps)),'-d',classes,src])
    meta=classes/'META-INF';meta.mkdir();(meta/'mods.toml').write_text('modLoader="javafml"\nloaderVersion="[52,)"\nlicense="test-only"\n[[mods]]\nmodId="localeobserver"\nversion="1"\n[[dependencies.localeobserver]]\nmodId="jei"\nmandatory=true\nversionRange="[19,)"\nordering="BEFORE"\nside="BOTH"\n')
    run(out,'jar',[JDK/'bin/jar','--create','--file',out/'localeobserver.jar','-C',classes,'.'])
    save(out/'result.json',{'status':'PASS','scope':'FORGE_NATIVE_DIAGNOSTIC_BUILD','jar':record(out/'localeobserver.jar')})
if __name__=='__main__':main()
