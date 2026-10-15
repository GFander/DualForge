#!/usr/bin/env python3
"""Compile/run independent ASM research tools; never start a game server."""
import argparse
import json
import os
from pathlib import Path
import subprocess
from inputs import save, sha


DEPENDENCIES = ['asm-9.10.1.jar', 'asm-tree-9.10.1.jar',
                'asm-commons-9.10.1.jar', 'asm-analysis-9.10.1.jar',
                'asm-util-9.10.1.jar', 'gson-2.10.1.jar']


def execute(command, receipt):
    receipt = Path(receipt)
    receipt.mkdir(parents=True, exist_ok=False)
    with (receipt / 'stdout.txt').open('w') as stdout, (receipt / 'stderr.txt').open('w') as stderr:
        result = subprocess.run(list(map(str, command)), stdout=stdout, stderr=stderr)
    save(receipt / 'command.json', {'argv': list(map(str, command)), 'exit': result.returncode})
    if result.returncode:
        raise RuntimeError(f'command failed ({result.returncode}); see {receipt}')


def compile_tools(java_home, libraries, work):
    jars = []
    for name in DEPENDENCIES:
        paths = sorted(Path(libraries).rglob(name))
        if len(paths) != 1:
            raise ValueError(f'exactly one {name} required, got {len(paths)}')
        jars.append(paths[0].resolve())
    sources = sorted(Path(__file__).parent.glob('*.java'))
    classes = Path(work) / 'classes'
    classes.mkdir(parents=True, exist_ok=False)
    cp = os.pathsep.join(map(str, jars))
    execute([Path(java_home) / 'bin/javac', '--release', '21', '-proc:none',
             '-cp', cp, '-d', classes, *sources], Path(work) / 'javac')
    save(Path(work) / 'inputs.json', [{'name': p.name, 'sha256': sha(p)} for p in [*sources, *jars]])
    return str(classes.resolve()) + os.pathsep + cp


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('mode', choices=['scan', 'usage'])
    p.add_argument('--java-home', type=Path, required=True)
    p.add_argument('--libraries', type=Path, required=True)
    p.add_argument('--inputs', type=Path, required=True, help='artifact ID TAB absolute JAR path per line')
    p.add_argument('--output', type=Path, required=True)
    p.add_argument('--work', type=Path, required=True, help='new empty command/build receipt directory')
    args = p.parse_args()
    if args.output.exists():
        p.error('output exists; choose a new path (failed attempts are preserved)')
    before = {row.split('\t', 1)[1]: sha(row.split('\t', 1)[1])
              for row in args.inputs.read_text().splitlines()}
    cp = compile_tools(args.java_home, args.libraries, args.work)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    execute([args.java_home / 'bin/java', '-Xmx1024m', '-cp', cp, 'Atlas',
             args.mode, args.inputs, args.output], args.work / args.mode)
    assert before == {path: sha(path) for path in before}, 'input bytes changed'
    save(args.work / 'result.json', {'status': 'PASS', 'mode': args.mode,
         'outputSHA256': sha(args.output), 'outputBytes': args.output.stat().st_size,
         'inputSHA256': before, 'inputUnchanged': True})
    (args.work / 'classpath.txt').write_text(cp)
    print(json.dumps({'status': 'PASS', 'output': str(args.output), 'sha256': sha(args.output)}))


if __name__ == '__main__':
    main()
