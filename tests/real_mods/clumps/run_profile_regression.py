"""Compile unchanged active fixture sources as a new cohort and run original Profile contracts.

The fixed Probe and native coexistence input are supplied separately; neither is
rebuilt. New fixture JARs do not inherit any historical game/client result.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[3]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ['jar', 'forge-install', 'neo-install', 'jdk-home', 'fixed-inputs', 'fnf', 'output']:
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=False)
    sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
    forge = sorted((args.forge_install / 'libraries').rglob('*.jar'), key=lambda p: (not p.name.endswith('-server.jar'), str(p)))
    neo = sorted((args.neo_install / 'libraries').rglob('*.jar'), key=lambda p: (not p.name.endswith('-server.jar'), str(p)))
    records = [args.fixed_inputs / 'forgeprobe.jar', args.fixed_inputs / 'native-bootstrap.jar']

    def run(label, cmd):
        (args.output / (label + '.command.json')).write_text(json.dumps(list(map(str, cmd)), indent=2) + '\n')
        result = subprocess.run(list(map(str, cmd)), cwd=ROOT, capture_output=True)
        (args.output / (label + '.stdout.log')).write_bytes(result.stdout)
        (args.output / (label + '.stderr.log')).write_bytes(result.stderr)
        (args.output / (label + '.exit.json')).write_text(json.dumps({'exitCode': result.returncode}) + '\n')
        assert result.returncode == 0, label + ' failed; see stderr'

    for label, directory in [('normal', 'dropin_items/src'), ('legacy', 'registry/src'),
                             ('block', 'block_registry/src'), ('context', 'entry_context/fixture')]:
        source = ROOT / 'tests' / directory
        records.extend(p for p in source.rglob('*') if p.is_file())
        classes = args.output / (label + '-classes')
        classes.mkdir()
        run(label + '-javac', [args.jdk_home / 'bin/javac', '--release', '21', '-g', '-proc:none',
                              '-cp', os.pathsep.join(map(str, forge)), '-d', classes, *sorted(source.rglob('*.java'))])
        if label != 'context':
            with zipfile.ZipFile(args.output / (label + '.jar'), 'x', zipfile.ZIP_DEFLATED) as archive:
                for p in classes.rglob('*.class'):
                    archive.write(p, p.relative_to(classes).as_posix())
                for p in (source / 'main/resources').rglob('*'):
                    if p.is_file():
                        archive.write(p, p.relative_to(source / 'main/resources').as_posix())
        else:
            for mod, package, name in [('contextprobe', 'context', 'ContextProbe'), ('contextpeer', 'contextpeer', 'ContextPeer')]:
                with zipfile.ZipFile(args.output / (mod + '.jar'), 'x', zipfile.ZIP_DEFLATED) as archive:
                    archive.writestr('META-INF/MANIFEST.MF', 'Manifest-Version: 1.0\n\n')
                    archive.writestr('META-INF/mods.toml', 'modLoader="javafml"\nloaderVersion="[52,)"\nlicense="test fixture"\n[[mods]]\nmodId="' + mod + '"\nversion="1"\n')
                    path = 'com/example/' + package + '/' + name + '.class'
                    archive.write(classes / path, path)
    with zipfile.ZipFile(args.output / 'normal.jar') as archive:
        import re
        mod = re.search(r'modId\s*=\s*"([^"]+)"', archive.read('META-INF/mods.toml').decode())[1]
    matching = args.output / (mod + '.jar')
    shutil.copyfile(args.output / 'normal.jar', matching)
    sources = [ROOT / 'tests/dropin_items/NormalItemBytecodeTest.java', ROOT / 'tests/entry_context/ContextProfileContract.java']
    records.extend([Path(__file__), *sources, args.jar, args.fnf, *forge, *neo])
    inputs = [{'path': str(p.resolve()), 'bytes': p.stat().st_size, 'sha256': sha(p)} for p in records]
    (args.output / 'inputs.json').write_text(json.dumps(inputs, indent=2) + '\n')
    classes = args.output / 'contract-classes'
    classes.mkdir()
    cp = os.pathsep.join(map(str, [args.jar, *neo]))
    run('contract-javac', [args.jdk_home / 'bin/javac', '--release', '21', '-proc:none', '-cp', cp, '-d', classes, *sources])
    prefix = [args.jdk_home / 'bin/java', '--add-opens', 'java.base/java.lang.invoke=ALL-UNNAMED',
              '--add-exports', 'java.base/sun.security.util=ALL-UNNAMED', '-cp', str(classes) + os.pathsep + cp]
    native = args.fixed_inputs / 'native-bootstrap.jar'
    probe = args.fixed_inputs / 'forgeprobe.jar'
    run('normal', [*prefix, 'io.github.gfander.dualforge.NormalItemBytecodeTest', args.output / 'normal.jar',
                   native, args.output / 'normal-bytes', matching])
    run('context', [*prefix, 'io.github.gfander.dualforge.ContextProfileContract', args.output / 'contextprobe.jar',
                    args.fnf, args.output / 'normal.jar', args.output / 'legacy.jar', args.output / 'block.jar',
                    probe, native, args.output / 'contextpeer.jar', args.output / 'context-bytes'])
    assert all(sha(Path(row['path'])) == row['sha256'] for row in inputs)
    result = {'status': 'PASS', 'compatSHA256': sha(args.jar),
              'scope': 'newly compiled active fixture cohort; original bytecode contracts; no historical dynamic result inherited'}
    (args.output / 'result.json').write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
