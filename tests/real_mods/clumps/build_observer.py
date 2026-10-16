"""Build an independent native event probe; compile only against official host APIs."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import zipfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--host', choices=['forge', 'neoforge'], required=True)
    parser.add_argument('--independent-fixture', action='store_true')
    for name in ['install', 'jdk-home', 'output']:
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=False)
    classes = args.output / 'classes'
    classes.mkdir()
    template = Path(__file__).with_name('EventObserver.java.in')
    forge = args.host == 'forge'
    text = template.read_text()
    if args.independent_fixture:
        text = text.replace('com.blamejared.clumps.ClumpsCommon', 'org.example.publisher.Shared')
        text = text.replace('com.blamejared.clumps.', 'org.example.publisher.')
        text = text.replace('pickupXPEvent', 'callback')
    for old, new in {
        'HOST.fml': 'net.minecraftforge.fml' if forge else 'net.neoforged.fml',
        'HOST.bus': 'net.minecraftforge.eventbus' if forge else 'net.neoforged.bus',
        'HOST.event': 'net.minecraftforge.event' if forge else 'net.neoforged.neoforge.event',
        'GLOBAL_BUS': 'net.minecraftforge.common.MinecraftForge.EVENT_BUS' if forge else 'net.neoforged.neoforge.common.NeoForge.EVENT_BUS',
        'CONSTRUCTOR_PARAMS': '' if forge else 'net.neoforged.bus.api.IEventBus bus',
    }.items():
        text = text.replace(old, new)
    source = args.output / 'EventObserver.java'
    source.write_text(text)
    libraries = sorted((args.install / 'libraries').rglob('*.jar'),
                       key=lambda p: (not p.name.endswith('-server.jar'), str(p)))
    inputs = [Path(__file__), template, source, *libraries]
    sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
    records = [{'path': str(p.resolve()), 'bytes': p.stat().st_size, 'sha256': sha(p)} for p in inputs]
    (args.output / 'inputs.json').write_text(json.dumps(records, indent=2) + '\n')
    cmd = [str(args.jdk_home / 'bin/javac'), '--release', '21', '-proc:none',
           '-cp', os.pathsep.join(map(str, libraries)), '-d', str(classes), str(source)]
    (args.output / 'command.json').write_text(json.dumps(cmd, indent=2) + '\n')
    result = subprocess.run(cmd, capture_output=True)
    (args.output / 'stdout.log').write_bytes(result.stdout)
    (args.output / 'stderr.log').write_bytes(result.stderr)
    (args.output / 'exit.json').write_text(json.dumps({'exitCode': result.returncode}) + '\n')
    result.check_returncode()
    jar = args.output / (args.host + '-event-observer.jar')
    with zipfile.ZipFile(jar, 'x', zipfile.ZIP_DEFLATED) as archive:
        archive.writestr('META-INF/' + ('mods.toml' if forge else 'neoforge.mods.toml'),
                        'modLoader="javafml"\nloaderVersion="' + ('[52,)' if forge else '[4,)') +
                        '"\nlicense="test fixture"\n[[mods]]\nmodId="clumpsobserver"\nversion="1"\n')
        for path in classes.rglob('*.class'):
            archive.write(path, path.relative_to(classes).as_posix())
    assert all(sha(Path(row['path'])) == row['sha256'] for row in records)
    (args.output / 'result.json').write_text(json.dumps(
        {'status': 'PASS', 'jar': str(jar), 'bytes': jar.stat().st_size, 'sha256': sha(jar)}, indent=2) + '\n')
    print(jar)


if __name__ == '__main__':
    main()
