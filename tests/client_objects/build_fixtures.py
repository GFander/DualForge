"""Clean Forge compilation of canonical Ice Ether object IDs; no production edits."""
import argparse, importlib.util, json, os, re, shutil, zipfile
from pathlib import Path

SOURCE = Path(__file__).resolve().parent
REPO = SOURCE.parents[1]
spec = importlib.util.spec_from_file_location('registry', REPO / 'tests/registry/run_registry.py')
r = importlib.util.module_from_spec(spec); spec.loader.exec_module(r)
w = r.w
ROLES = {'items': ('multi-registry-probe', 'multi-registry-driver', 'com.example.multiregistrycheck.MultiRegistryProbe', 'com.example.multiregistrydriver.MultiRegistryDriver'),
         'block': ('block-registry-probe', 'block-registry-driver', 'com.example.blockregistrycheck.BlockRegistryProbe', 'com.example.blockregistrydriver.BlockRegistryDriver')}

def build(args):
    run = w.run_directory(REPO / 'out/client-objects/fixture' / args.role)
    binary = w.tools(args, run)
    forge = [args.forge_dependencies / name for name, _ in r.identity.FORGE]
    for path, (_, digest) in zip(forge, r.identity.FORGE): assert w.digest(path) == digest
    libs = sorted((args.server_input / 'libraries').rglob('*.jar'))
    vanilla = args.server_input / 'libraries/net/minecraft/server/1.21.1-20240808.144430/server-1.21.1-20240808.144430-srg.jar'
    with zipfile.ZipFile(vanilla) as jar:
        assert all(b'net/neoforged/' not in jar.read(n) and b'net/minecraftforge/' not in jar.read(n) for n in jar.namelist() if n.endswith('.class'))
    support = [p for p in libs if p.relative_to(args.server_input / 'libraries').as_posix().startswith(('com/google/', 'com/mojang/', 'it/unimi/', 'org/slf4j/', 'org/apache/logging/log4j/', 'org/jetbrains/', 'org/joml/'))]
    forgecp = [*forge, vanilla, *support]
    neo = args.server_input / 'libraries/net/neoforged/neoforge/21.1.256/neoforge-21.1.256-server.jar'
    base = SOURCE / args.role
    inputs = w.fingerprints([*sorted(p for p in base.rglob('*') if p.is_file()), *forgecp, *libs])
    w.save(run / 'inputs.json', inputs); w.save(run / 'forge-compile-inputs.json', w.fingerprints(forgecp))
    w.save(run / 'invocation.json', {k: str(v) for k, v in vars(args).items()})
    shutil.copytree(base, run / 'source')
    fixture_name, driver_name, entry, driver = ROLES[args.role]
    jars = {}
    for name, folder, cp, cls in [(fixture_name, base / 'src', forgecp, entry), (driver_name, base / 'driver', [neo, *libs], driver)]:
        classes = run / (name + '-classes'); classes.mkdir()
        w.native(run, name + '-javac', [binary / 'javac', '--release', '21', '-encoding', 'UTF-8', '-classpath', os.pathsep.join(map(str, cp)), '-d', classes, *sorted(folder.rglob('*.java'))])
        root = run / (name + '-root'); shutil.copytree(classes, root); shutil.copytree(folder / 'main/resources', root, dirs_exist_ok=True)
        jar = run / (name + '.jar')
        w.native(run, name + '-jar', [binary / 'jar', '--create', '--file', jar, '--manifest', root / 'META-INF/MANIFEST.MF', '-C', root, '.'])
        jars[name] = r.fingerprint(jar); w.save(run / (name + '-contents.json'), w.jar_contents(jar))
        w.native(run, 'javap-' + name, [binary / 'javap', '-v', '-p', '-c', '-classpath', jar, cls])
        if name == fixture_name:
            with zipfile.ZipFile(jar) as z:
                assert all(b'net/neoforged/' not in z.read(n) and b'io/github/gfander/dualforge/' not in z.read(n) for n in z.namelist() if n.endswith('.class'))
    fixture = run / (fixture_name + '.jar')
    with zipfile.ZipFile(fixture) as old, zipfile.ZipFile(run / 'native-control.jar', 'w') as native:
        for name in old.namelist(): native.writestr(name, old.read(name))
        native.writestr('META-INF/neoforge.mods.toml', old.read('META-INF/mods.toml').replace(b'[52,)', b'[4,)'))
    jars['native-control'] = r.fingerprint(run / 'native-control.jar')
    evidence = (run / ('javap-' + driver_name + '.stdout.log')).read_text()
    assert 'Registry.register:' not in evidence and 'RegisterEvent.register:' not in evidence
    assert not re.search(r'\bnew\s+#\d+\s+// class net/minecraft/(?:world/item/(?:Item|BlockItem)|world/level/block/Block)\b', evidence)
    w.unchanged(inputs)
    result = {'status': 'PASS', 'role': args.role, 'directory': str(run), 'jars': jars, 'source': w.fingerprints(sorted(p for p in base.rglob('*') if p.is_file())), 'nativeForgeOnly': True}
    w.save(run / 'result.json', result)
    index = REPO / 'out/client-objects' / args.role / 'fixture.json'
    index.parent.mkdir(parents=True, exist_ok=True); w.save(index, result)
    print(json.dumps(result, indent=2))

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--role', choices=ROLES, required=True)
    for name in ('server-input', 'forge-dependencies', 'jdk-home'): parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    for name in ('server_input', 'forge_dependencies', 'jdk_home'): setattr(args, name, getattr(args, name).resolve())
    build(args)
