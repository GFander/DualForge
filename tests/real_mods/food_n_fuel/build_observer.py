"""Build only the optional read-only Forge observer, never rebuild Food N' Fuel."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import zipfile


def build(args):
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    classes = output / 'classes'
    classes.mkdir()
    source = Path(__file__).with_name('NativeRegistryObserver.java')
    libraries = sorted((args.install.resolve() / 'libraries').rglob('*.jar'))
    command = [str(args.jdk_home.resolve() / 'bin/javac'), '--release', '21', '-proc:none',
               '-cp', os.pathsep.join(map(str, libraries)), '-d', str(classes), str(source)]
    (output / 'javac.command.json').write_text(json.dumps(command, indent=2) + '\n')
    result = subprocess.run(command, capture_output=True)
    for name, content in (('stdout', result.stdout), ('stderr', result.stderr)):
        (output / ('javac.' + name + '.log')).write_bytes(content)
    (output / 'javac.exit.json').write_text(json.dumps({'exitCode': result.returncode}) + '\n')
    result.check_returncode()
    jar = output / 'native-observer.jar'
    with zipfile.ZipFile(jar, 'w', zipfile.ZIP_DEFLATED) as archive:
        archive.writestr('META-INF/MANIFEST.MF', 'Manifest-Version: 1.0\nAutomatic-Module-Name: df007cobserver\n\n')
        archive.writestr('META-INF/mods.toml', 'modLoader="javafml"\nloaderVersion="[52,)"\nlicense="MIT"\n'
                        '[[mods]]\nmodId="df007cobserver"\nversion="1"\ndisplayName="Read-only native registry observer"\n')
        for file in sorted(classes.rglob('*.class')):
            archive.write(file, str(file.relative_to(classes)))
    records = [{'path': str(p), 'sha256': hashlib.sha256(p.read_bytes()).hexdigest(), 'bytes': p.stat().st_size}
               for p in [source, *libraries, jar]]
    (output / 'inputs-and-output.json').write_text(json.dumps(records, indent=2) + '\n')
    print(jar)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('install', 'output', 'jdk-home'):
        parser.add_argument('--' + name, type=Path, required=True)
    build(parser.parse_args())
