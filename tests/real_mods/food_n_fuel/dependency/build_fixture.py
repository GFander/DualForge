"""Clean-compile independent Forge dependency controls; frozen inputs remain unchanged."""
import argparse
import importlib.util
import os
from pathlib import Path
import shutil
import zipfile

REPO = Path(__file__).resolve().parents[4]
spec = importlib.util.spec_from_file_location('identity', REPO / 'tests/identity/run_identity.py')
identity = importlib.util.module_from_spec(spec); spec.loader.exec_module(identity)
w = identity.w


def dependency(owner, target, version, required=True, side='BOTH', ordering='NONE'):
    return f'\n[[dependencies.{owner}]]\nmodId="{target}"\nmandatory={str(required).lower()}\nversionRange="{version}"\nside="{side}"\nordering="{ordering}"\n'


def metadata(owner, forge_range='[0,)', side='BOTH', ordering='NONE', extra='', minecraft='[1.21.1]', required=True):
    return (f'modLoader="javafml"\nloaderVersion="[52,)"\nlicense="MIT"\n[[mods]]\nmodId="{owner}"\nversion="1.0.0"\n'
            + dependency(owner, 'minecraft', minecraft) + dependency(owner, 'forge', forge_range, required, side, ordering) + extra)


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--forge-dependencies', type=Path, required=True)
    p.add_argument('--jdk-home', type=Path, required=True)
    p.add_argument('--native', type=Path, required=True)
    a = p.parse_args()
    for proc in Path('/proc').iterdir():
        try:
            if (proc / 'exe').resolve().name in ('java','javac'):
                raise RuntimeError('All JVM work must be serial')
        except (FileNotFoundError,PermissionError):
            pass
    run = w.run_directory(REPO / 'out/convergence-007c2/fixture')
    source = Path(__file__).with_name('fixture')
    forge = [a.forge_dependencies / name for name,_ in identity.FORGE]
    for path,(_,expected) in zip(forge,identity.FORGE):
        assert w.digest(path)==expected
    records = w.fingerprints([*source.rglob('*.java'),*forge,a.native,Path(__file__)])
    w.save(run/'inputs.json',records);shutil.copytree(source,run/'source')
    binary=w.tools(a,run);classes=run/'classes';classes.mkdir()
    w.native(run,'javac',[binary/'javac','--release','21','-encoding','UTF-8','-cp',os.pathsep.join(map(str,forge)),'-d',classes,*sorted(source.rglob('*.java'))])
    # Compile classpath contains only the three pinned Forge artifacts, never NeoForge/DualForge.
    variants={
        'a-valid':('a',metadata('dependency_a')),
        'a-bounded':('a',metadata('dependency_a','[52,53)')),
        'b-unsupported':('b',metadata('dependency_b','[53,)')),
        'a-missing':('a',metadata('dependency_a',extra=dependency('dependency_a','non_existing_mod','[1,)'))),
        'a-minecraft':('a',metadata('dependency_a',minecraft='[1.20.1]')),
        'a-client-missing':('a',metadata('dependency_a',extra=dependency('dependency_a','non_existing_mod','[1,)',side='CLIENT'))),
        'a-optional-missing':('a',metadata('dependency_a',extra=dependency('dependency_a','non_existing_mod','[1,)',False))),
        'a-client-platform':('a',metadata('dependency_a','[53,)','CLIENT','AFTER')),
        'a-ordering-platform':('a',metadata('dependency_a','[0,)','BOTH','AFTER')),
    }
    with zipfile.ZipFile(a.native) as z:
        native_text=z.read('META-INF/neoforge.mods.toml').decode()
        import tomllib
        native_id=tomllib.loads(native_text)['mods'][0]['modId']
        print('NATIVE TARGET',native_id)
    variants.update({
        'a-low-required':('a',metadata('dependency_a',extra=dependency('dependency_a',native_id,'[2,)'))),
        'a-low-optional':('a',metadata('dependency_a',extra=dependency('dependency_a',native_id,'[2,)',False))),
        'a-after-native':('a',metadata('dependency_a',extra=dependency('dependency_a',native_id,'[1,)',True,ordering='AFTER'))),
    })
    jars={}
    for name,(suffix,text) in variants.items():
        jar=run/(name+'.jar')
        with zipfile.ZipFile(jar,'w') as z:
            z.writestr('META-INF/MANIFEST.MF',f'Manifest-Version: 1.0\nAutomatic-Module-Name: dependency_{suffix}\n\n')
            z.writestr('META-INF/mods.toml',text)
            for c in sorted((classes/('com/example/dependency'+suffix)).glob('*.class')):
                data=c.read_bytes();assert b'net/neoforged/' not in data and b'io/github/gfander/dualforge' not in data
                z.writestr(str(c.relative_to(classes)),data)
        jars[name]=w.fingerprints([jar])[0]
    for name,required in [('native-required-forge',True),('native-optional-forge',False)]:
        jar=run/(name+'.jar')
        extra=f'\n[[dependencies.{native_id}]]\nmodId="forge"\ntype="'+('required' if required else 'optional')+'"\nversionRange="[53,)"\nordering="NONE"\nside="BOTH"\n'
        with zipfile.ZipFile(a.native) as src,zipfile.ZipFile(jar,'w') as z:
            for n in src.infolist():z.writestr(n,src.read(n.filename) if n.filename!='META-INF/neoforge.mods.toml' else (native_text+extra).encode())
        jars[name]=w.fingerprints([jar])[0]
    w.unchanged(records)
    w.save(run/'result.json',{'status':'PASS','compilePurity':'Forge52.1.0 only, no NeoForge/DualForge','nativeId':native_id,'jars':jars,'sources':records})
    print('RESULT PASS',run)


if __name__=='__main__':main()
