"""Compile an independently named original Forge event publisher fixture."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import zipfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ['forge-install', 'jdk-home', 'output']:
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=False)
    classes = args.output / 'classes'
    classes.mkdir()
    sources = sorted(Path(__file__).with_name('fixture').rglob('*.java'))
    libraries = sorted((args.forge_install / 'libraries').rglob('*.jar'),
                       key=lambda p: (not p.name.endswith('-server.jar'), str(p)))
    sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
    records = [{'path': str(p.resolve()), 'bytes': p.stat().st_size, 'sha256': sha(p)}
               for p in [Path(__file__), *sources, *libraries]]
    (args.output / 'inputs.json').write_text(json.dumps(records, indent=2) + '\n')
    cmd = [str(args.jdk_home / 'bin/javac'), '--release', '21', '-g', '-proc:none',
           '-cp', os.pathsep.join(map(str, libraries)), '-d', str(classes), *map(str, sources)]
    (args.output / 'command.json').write_text(json.dumps(cmd, indent=2) + '\n')
    result = subprocess.run(cmd, capture_output=True)
    (args.output / 'stdout.log').write_bytes(result.stdout)
    (args.output / 'stderr.log').write_bytes(result.stderr)
    (args.output / 'exit.json').write_text(json.dumps({'exitCode': result.returncode}) + '\n')
    result.check_returncode()
    jar = args.output / 'independent-publisher.jar'
    with zipfile.ZipFile(jar, 'x', zipfile.ZIP_DEFLATED) as archive:
        archive.writestr('META-INF/mods.toml', 'modLoader="javafml"\nloaderVersion="[52,)"\nlicense="test fixture"\n[[mods]]\nmodId="otherpublisher"\nversion="1"\n')
        archive.writestr('META-INF/services/org.example.publisher.platform.EventPort', 'org.example.publisher.platform.PublisherPort\n')
        for source in classes.rglob('*.class'):
            archive.write(source, source.relative_to(classes).as_posix())
    assert all(sha(Path(row['path'])) == row['sha256'] for row in records)
    (args.output / 'result.json').write_text(json.dumps({'status': 'PASS', 'jar': str(jar), 'sha256': sha(jar)}, indent=2) + '\n')
    print(jar)


if __name__ == '__main__':
    main()
