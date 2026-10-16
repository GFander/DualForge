"""Compile and run the event initializer's positive and fail-closed bytecode contract."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ['jar', 'clumps', 'fixture', 'neo-install', 'jdk-home', 'output']:
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=False)
    libraries = sorted((args.neo_install / 'libraries').rglob('*.jar'))
    source = Path(__file__).with_name('EventInitializerContract.java')
    sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
    inputs = [Path(__file__), source, args.jar, args.clumps, args.fixture, *libraries]
    records = [{'path': str(p.resolve()), 'sha256': sha(p)} for p in inputs]
    (args.output / 'inputs.json').write_text(json.dumps(records, indent=2) + '\n')
    classes = args.output / 'classes'
    classes.mkdir()
    cp = os.pathsep.join(map(str, [args.jar, *libraries]))
    host = args.neo_install / 'libraries/net/neoforged/neoforge/21.1.256/neoforge-21.1.256-universal.jar'
    commands = [
        ('javac', [str(args.jdk_home / 'bin/javac'), '--release', '21', '-proc:none', '-cp', cp, '-d', str(classes), str(source)]),
        ('contract', [str(args.jdk_home / 'bin/java'), '-Xverify:all', '-cp', str(classes) + os.pathsep + cp,
                      'io.github.gfander.dualforge.EventInitializerContract', str(args.clumps), str(args.fixture), str(host),
                      str(args.output / 'branch-diagnostics')]),
    ]
    for name, cmd in commands:
        (args.output / (name + '.command.json')).write_text(json.dumps(cmd, indent=2) + '\n')
        result = subprocess.run(cmd, capture_output=True)
        (args.output / (name + '.stdout.log')).write_bytes(result.stdout)
        (args.output / (name + '.stderr.log')).write_bytes(result.stderr)
        (args.output / (name + '.exit.json')).write_text(json.dumps({'exitCode': result.returncode}) + '\n')
        result.check_returncode()
    text = (args.output / 'contract.stdout.log').read_text()
    match = re.search(r'EVENT INITIALIZER CONTRACT PASS assertions=(\d+) negatives=(\d+)', text)
    assert match and all(sha(Path(row['path'])) == row['sha256'] for row in records)
    result = {'status': 'PASS', 'assertions': int(match[1]), 'negatives': int(match[2]), 'compatSHA256': sha(args.jar)}
    (args.output / 'result.json').write_text(json.dumps(result, indent=2) + '\n')
    print(result)


if __name__ == '__main__':
    main()
