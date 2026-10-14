"""Compile the original Forge drop-in fixture and a separate query-only NeoForge observer."""
import argparse
import importlib.util
import json
import os
from pathlib import Path
import shutil
import zipfile

REPO = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('registry', REPO / 'tests/registry/run_registry.py')
r = importlib.util.module_from_spec(spec)
spec.loader.exec_module(r)
w = r.w


def build(args):
    run = w.run_directory(REPO / 'out/convergence-007b/fixture')
    binary = w.tools(args, run)
    source = REPO / 'tests/dropin_items'
    libraries = sorted((args.server_input / 'libraries').rglob('*.jar'))
    forge = [args.forge_dependencies / name for name, _ in r.identity.FORGE]
    for path, (_, expected) in zip(forge, r.identity.FORGE):
        assert w.digest(path) == expected
    vanilla = args.server_input / 'libraries/net/minecraft/server/1.21.1-20240808.144430/server-1.21.1-20240808.144430-srg.jar'
    with zipfile.ZipFile(vanilla) as jar:
        assert all(b'net/neoforged/' not in jar.read(n) and b'net/minecraftforge/' not in jar.read(n)
                   for n in jar.namelist() if n.endswith('.class'))
    support = [p for p in libraries if p.relative_to(args.server_input / 'libraries').as_posix().startswith(
               ('com/google/', 'com/mojang/', 'it/unimi/', 'org/slf4j/', 'org/apache/logging/log4j/', 'org/jetbrains/', 'org/joml/'))]
    forgecp = [*forge, vanilla, *support]
    inputs = w.fingerprints([p for p in source.rglob('*') if p.is_file()] + forgecp + libraries)
    w.save(run / 'inputs.json', inputs)
    w.save(run / 'forge-compile-inputs.json', w.fingerprints(forgecp))
    shutil.copytree(source, run / 'source')
    jars = {}
    for name, folder, cp in [('dropin-items', source / 'src', forgecp),
                             ('dropin-observer', source / 'driver', libraries)]:
        if name == 'dropin-items' and args.existing_forge_jar:
            original = args.existing_forge_jar.resolve()
            frozen = json.loads((original.parent / 'result.json').read_text())
            assert frozen['jars'][name]['SHA256'] == w.digest(original)
            for record in frozen['source']:
                path = Path(record['Path'])
                if path.is_relative_to(source / 'src'):
                    assert w.digest(path) == record['SHA256'], 'Frozen Forge source/resource changed'
            jars[name] = r.fingerprint(original)
            continue
        classes = run / (name + '-classes')
        classes.mkdir()
        w.native(run, name + '-javac', [binary / 'javac', '--release', '21', '-encoding', 'UTF-8',
                 '-cp', os.pathsep.join(map(str, cp)), '-d', classes, *sorted(folder.rglob('*.java'))])
        root = run / (name + '-root')
        shutil.copytree(classes, root)
        shutil.copytree(folder / 'main/resources', root, dirs_exist_ok=True)
        jar = run / (name + '.jar')
        w.native(run, name + '-jar', [binary / 'jar', '--create', '--file', jar, '--manifest',
                 root / 'META-INF/MANIFEST.MF', '-C', root, '.'])
        jars[name] = r.fingerprint(jar)
        w.save(run / (name + '-contents.json'), w.jar_contents(jar))
    forgejar = Path(jars['dropin-items']['Path'])
    with zipfile.ZipFile(forgejar) as jar:
        for name in jar.namelist():
            if name.endswith('.class'):
                data = jar.read(name)
                assert b'net/neoforged/' not in data and b'io/github/gfander/dualforge/' not in data
                assert not any(key in data for key in (b'DF005_REGISTER', b'DF006_REGISTER', b'DF006B_REGISTER', b'getBoolean'))
    for name, jar, cls in [('entry', forgejar, 'com.example.dropin.DropInItems'),
                           ('driver', run / 'dropin-observer.jar', 'com.example.dropindriver.DropInDriver')]:
        w.native(run, 'javap-' + name, [binary / 'javap', '-v', '-p', '-c', '-classpath', jar, cls])
    driver = (run / 'javap-driver.stdout.log').read_text()
    assert 'registries.DeferredRegister.register:' not in driver and 'Registry.register:' not in driver
    classes = run / 'asm-classes'
    classes.mkdir()
    cp = os.pathsep.join(map(str, libraries))
    w.native(run, 'asm-javac', [binary / 'javac', '--release', '21', '-cp', cp, '-d', classes,
             REPO / 'tests/multi_registry/contract/AsmEvidence.java'])
    w.native(run, 'asm-nodes', [binary / 'java', '-cp', str(classes) + os.pathsep + cp,
             'io.github.gfander.dualforge.test.AsmEvidence', forgejar, 'com/example/dropin/DropInItems.class'])
    w.unchanged(inputs)
    result = {'status': 'PASS', 'jars': jars, 'nativeForgeOnly': True, 'businessSwitches': [],
              'source': w.fingerprints([p for p in source.rglob('*') if p.is_file()])}
    w.save(run / 'result.json', result)
    w.save(REPO / 'out/convergence-007b/fixture.json', result)
    print('RESULT PASS', run)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for key in ('server-input', 'forge-dependencies', 'jdk-home'):
        parser.add_argument('--' + key, type=Path, required=True)
    parser.add_argument('--existing-forge-jar', type=Path,
                        help='Preserve the frozen original Forge JAR while rebuilding only the query observer')
    build(parser.parse_args())
