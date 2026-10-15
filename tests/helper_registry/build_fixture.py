"""Clean-build a native Forge helper fixture and separate platform observers."""
import argparse
import importlib.util
import os
from pathlib import Path
import shutil
import zipfile

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('workflow', ROOT/'tools/lib/unix_workflow.py')
w = importlib.util.module_from_spec(spec); spec.loader.exec_module(w)

def package(path, classes, mod, native=False):
    text = 'modLoader="javafml"\nloaderVersion="'+('[4,)' if native else '[52,)')+'"\nlicense="test fixture"\n[[mods]]\nmodId="'+mod+'"\nversion="1"\n'
    if mod == 'helperprobe':
        text += '\n[[dependencies.helperprobe]]\nmodId="forge"\nmandatory=true\nversionRange="[52,53)"\nordering="NONE"\nside="BOTH"\n'
    with zipfile.ZipFile(path, 'w', zipfile.ZIP_DEFLATED) as z:
        z.writestr('META-INF/'+('neoforge.mods.toml' if native else 'mods.toml'), text)
        for p in sorted(classes.rglob('*.class')):
            z.writestr(zipfile.ZipInfo(p.relative_to(classes).as_posix(), (2020,1,1,0,0,0)), p.read_bytes())

def main():
    p=argparse.ArgumentParser(description=__doc__)
    for name in ('forge-install','neo-install','jdk-home'):p.add_argument('--'+name,type=Path,required=True)
    a=p.parse_args();out=w.run_directory(ROOT/'out/convergence-007c4b/fixture');binary=w.tools(a,out)
    sources=Path(__file__).parent/'fixture';shutil.copytree(sources,out/'source')
    forge=sorted((a.forge_install/'libraries').rglob('*.jar'));neo=sorted((a.neo_install/'libraries').rglob('*.jar'))
    assert forge and neo and not any('neoforge-21.' in p.name or p.name.lower()=='dualforge.jar' for p in forge)
    inputs=w.fingerprints([*sources.rglob('*.java'),*forge,*neo,Path(__file__),Path(__file__).parent/'driver/Observer.java.in'])
    w.save(out/'inputs.json',inputs);classes=out/'classes';classes.mkdir()
    w.native(out,'fixture-javac',[binary/'javac','--release','21','-g','-proc:none','-cp',os.pathsep.join(map(str,forge)),'-d',classes,*sorted(sources.rglob('*.java'))])
    jar=out/'helper-probe.jar';package(jar,classes,'helperprobe');artifacts={'fixture':w.fingerprints([jar])[0]}
    for host,libs in [('forge',forge),('neoforge',neo)]:
        d=out/host;d.mkdir();template=(Path(__file__).parent/'driver/Observer.java.in').read_text()
        if host=='forge':replacements={'HOST.event':'net.minecraftforge.event','HOST.fml':'net.minecraftforge.fml','REGISTRY_PACKAGE':'net.minecraftforge.registries','GLOBAL_BUS':'net.minecraftforge.common.MinecraftForge.EVENT_BUS','EXACT_DEFERRED':'net.minecraftforge.registries.DeferredRegister$EventDispatcher','HOST_KIND':'forge'}
        else:replacements={'HOST.event':'net.neoforged.neoforge.event','HOST.fml':'net.neoforged.fml','REGISTRY_PACKAGE':'net.neoforged.neoforge.registries','GLOBAL_BUS':'net.neoforged.neoforge.common.NeoForge.EVENT_BUS','EXACT_DEFERRED':'net.neoforged.neoforge.registries.DeferredRegister','HOST_KIND':'neoforge'}
        for old,new in replacements.items():template=template.replace(old,new)
        source=d/'Observer.java';source.write_text(template);target=d/'classes';target.mkdir()
        w.native(out,host+'-observer-javac',[binary/'javac','--release','21','-proc:none','-cp',os.pathsep.join(map(str,libs)),'-d',target,source])
        j=d/(host+'-helper-observer.jar');package(j,target,'helperobserver',host=='neoforge');artifacts[host+'Observer']=w.fingerprints([j])[0]
    for f in sorted(classes.rglob('*.class')):
        w.native(out,'javap-'+f.stem,[binary/'javap','-p','-v','-s','-c',f])
        assert b'net/neoforged/' not in f.read_bytes() and b'io/github/gfander/dualforge' not in f.read_bytes()
    w.unchanged(inputs);result={'status':'PASS','purity':'fixture compiled with official Forge52.1.0 inputs only; platform observers compiled separately','artifacts':artifacts,'source':w.fingerprints(sorted((out/'source').rglob('*.java'))),'classes':w.fingerprints(sorted(classes.rglob('*.class')))};w.save(out/'result.json',result);print(out,flush=True)

if __name__=='__main__':main()
