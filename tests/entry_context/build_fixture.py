"""Clean-compile unchanged-source Forge Context inputs and separate read-only observers."""
import argparse
import importlib.util
import os
from pathlib import Path
import shutil
import zipfile

REPO = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('w', REPO / 'tools/lib/unix_workflow.py')
w = importlib.util.module_from_spec(spec); spec.loader.exec_module(w)


def package(path, classes, names, metadata, native=False):
    with zipfile.ZipFile(path, 'w', zipfile.ZIP_DEFLATED) as z:
        z.writestr('META-INF/MANIFEST.MF', 'Manifest-Version: 1.0\n\n')
        z.writestr('META-INF/' + ('neoforge.mods.toml' if native else 'mods.toml'), metadata)
        for name in names:
            z.write(classes / name, name)


def toml(mod_id, native=False, platform=True):
    text = 'modLoader="javafml"\nloaderVersion="'+('[4,)' if native else '[52,)')+'"\nlicense="MIT"\n[[mods]]\nmodId="'+mod_id+'"\nversion="1.0.0"\n'
    if platform:
        text += '\n[[dependencies.'+mod_id+']]\nmodId="forge"\nmandatory=true\nversionRange="[52,53)"\nordering="NONE"\nside="BOTH"\n'
    return text


def main():
    p=argparse.ArgumentParser(description=__doc__)
    for key in ('forge-install','neo-install','jdk-home'):p.add_argument('--'+key,type=Path,required=True)
    a=p.parse_args()
    for proc in Path('/proc').iterdir():
        try:
            if (proc/'exe').resolve().name in ('java','javac'):raise RuntimeError('All JVM work must be serial')
        except (PermissionError,FileNotFoundError):pass
    out=w.run_directory(REPO/'out/convergence-007c3/fixture');binary=w.tools(a,out)
    root=Path(__file__).parent;shutil.copytree(root/'fixture',out/'source');shutil.copytree(root/'driver',out/'driver-source')
    forge=sorted((a.forge_install/'libraries').rglob('*.jar'));neo=sorted((a.neo_install/'libraries').rglob('*.jar'))
    assert forge and neo and not any('neoforge-21.' in v.name or v.name.lower()=='dualforge.jar' for v in forge)
    sources=sorted((root/'fixture').rglob('*.java'));drivers=sorted((root/'driver').glob('*.java'))
    inputs=w.fingerprints([*sources,*drivers,Path(__file__),*forge,*neo]);w.save(out/'inputs.json',inputs)
    classes=out/'classes';classes.mkdir()
    w.native(out,'forge-fixture-javac',[binary/'javac','--release','21','-g','-proc:none','-cp',os.pathsep.join(map(str,forge)),'-d',classes,*sources])
    artifacts={}
    for name,mod_id,package_name in [('ContextProbe','contextprobe','context'),('ContextPeer','contextpeer','contextpeer')]:
        file='com/example/'+package_name+'/'+name+'.class';data=(classes/file).read_bytes()
        assert b'net/neoforged/' not in data and b'io/github/gfander/dualforge' not in data
        jar=out/(mod_id+'.jar');package(jar,classes,[file],toml(mod_id));artifacts[mod_id]=w.fingerprints([jar])[0]
    for host,libs in [('Forge',forge),('Neo',neo)]:
        target=out/(host.lower()+'-observer-classes');target.mkdir()
        source=root/'driver'/(host+'ContextObserver.java')
        w.native(out,host.lower()+'-observer-javac',[binary/'javac','--release','21','-proc:none','-cp',os.pathsep.join(map(str,libs)),'-d',target,source])
        names=[p.relative_to(target).as_posix() for p in target.rglob('*.class')];jar=out/(host.lower()+'-context-observer.jar')
        package(jar,target,names,toml(host.lower()+'contextobserver',host=='Neo',False),host=='Neo');artifacts[host.lower()+'-observer']=w.fingerprints([jar])[0]
    w.unchanged(inputs);w.save(out/'result.json',{'status':'PASS','compilePurity':'Forge fixtures and native Forge observer use official Forge52.1.0 installation only; Neo observer separately uses official NeoForge installation, no compat compile input','artifacts':artifacts,'classFingerprints':w.fingerprints(sorted(classes.rglob('*.class'))),'source':w.fingerprints(sources)})
    print('CONTEXT FIXTURE PASS',out)


if __name__=='__main__':main()
