"""Serial static comparison, exact planner refusal and native FML observation."""
import argparse
import json
from pathlib import Path
import re
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / 'mouse_tweaks'))
from support import ENV, JDK, ROOT, fresh, libraries, record, run, save


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run', type=Path, required=True)
    args = parser.parse_args()
    output = fresh(args.run / 'boundary')
    tools = Path(__file__).parent
    compat = args.run / 'build-baseline/dualforge.jar'
    original = args.run / 'inputs/jei-1.21.1-forge-19.27.0.340.jar'
    run(output, 'analysis', [sys.executable, '-B', tools / 'analyze.py', '--run', args.run,
                           '--output', args.run / 'analysis'])
    classes = output / 'classes'
    classes.mkdir()
    deps = libraries('neo')
    cp = ':'.join(map(str, [compat, *deps]))
    source = tools / 'BoundaryContract.java'
    save(output / 'inputs.json', [record(p) for p in [source, compat, original, *deps]])
    run(output, 'javac', [JDK / 'bin/javac', '--release', '21', '-proc:none', '-cp', cp,
                         '-d', classes, source])
    text = run(output, 'contract', [JDK / 'bin/java', '-Xverify:all',
        '--add-opens', 'java.base/java.lang.invoke=ALL-UNNAMED',
        '--add-exports', 'java.base/sun.security.util=ALL-UNNAMED',
        '-cp', str(classes) + ':' + cp, 'io.github.gfander.dualforge.BoundaryContract', original])
    match = re.search(r'JEI BOUNDARY CONTRACT PASS assertions=(\d+)', text)
    assert match
    observer = fresh(args.run / 'native-observer-build')
    classes = observer / 'classes'
    classes.mkdir()
    sdk = ENV / 'runs/convergence-009c-20261017/client-abi/forge/libraries'
    deps = sorted(sdk.rglob('*.jar'))
    source = tools / 'NativeBaselineObserver.java'
    save(observer / 'inputs.json', [record(p) for p in [source, *deps]])
    run(observer, 'javac', [JDK / 'bin/javac', '--release', '21', '-proc:none',
        '-cp', ':'.join(map(str, deps)), '-d', classes, source])
    metadata = classes / 'META-INF'
    metadata.mkdir()
    (metadata / 'mods.toml').write_text('modLoader="javafml"\nloaderVersion="[52,)"\n'
        'license="test-only"\n[[mods]]\nmodId="jeibaselineobserver"\nversion="1"\n')
    jar = observer / 'native-observer.jar'
    run(observer, 'jar', [JDK / 'bin/jar', '--create', '--file', jar, '-C', classes, '.'])
    run(output, 'native-observed', [sys.executable, '-B', ROOT / 'tests/real_mods/clumps/run_server.py',
        '--host', 'forge', '--install', ENV / 'forge-52.1.0', '--jdk-home', JDK,
        '--output', args.run / 'native-observed', '--mod', original, '--mod', jar])
    game_log = (args.run / 'native-observed/stdout.log').read_text(errors='replace')
    matches = [line for line in game_log.splitlines() if 'JEI_NATIVE_OBSERVER_PASS' in line]
    assert len(matches) == 1, 'Missing once-only diagnostic observation'
    save(output / 'result.json', {'status': 'PASS', 'compat': record(compat), 'original': record(original),
        'plannerRefusalAssertions': int(match[1]), 'nativeObservation': matches[0],
        'scope': 'Static evidence + exact fail-closed model + separate native Forge FML observation',
        'JEI_NEO_RUNTIME': 'FAIL_AT_BASELINE_PREPLAN', 'JEI_GUI': 'NOT_RUN'})
    print('JEI EVIDENCE/REFUSAL CONTRACT PASS; JEI COMPATIBILITY PARTIAL')


if __name__ == '__main__':
    main()
