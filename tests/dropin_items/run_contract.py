"""Run exact bytecode/scope/atomicity checks against a specified production JAR."""
import argparse
import importlib.util
import os
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('workflow', REPO / 'tools/lib/unix_workflow.py')
w = importlib.util.module_from_spec(spec)
spec.loader.exec_module(w)

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for key in ('jar', 'fixture', 'native-control', 'server-input', 'jdk-home'):
        parser.add_argument('--' + key, type=Path, required=True)
    args = parser.parse_args()
    run = w.run_directory(REPO / 'out/convergence-007b/contract')
    records = w.fingerprints([args.jar, args.fixture, args.native_control, Path(__file__).with_name('NormalItemBytecodeTest.java')])
    w.save(run / 'inputs.json', records)
    binary = w.tools(args, run)
    libraries = sorted((args.server_input / 'libraries').rglob('*.jar'))
    cp = os.pathsep.join(map(str, [args.jar, *libraries]))
    classes = run / 'classes'
    classes.mkdir()
    result = {'status': 'FAIL', 'compatSHA256': w.digest(args.jar)}
    try:
        w.native(run, 'javac', [binary / 'javac', '--release', '21', '-cp', cp, '-d', classes,
                 Path(__file__).with_name('NormalItemBytecodeTest.java')])
        w.native(run, 'bytecode', [binary / 'java', '--add-opens', 'java.base/java.lang.invoke=ALL-UNNAMED',
                 '--add-exports', 'java.base/sun.security.util=ALL-UNNAMED', '-cp', str(classes) + os.pathsep + cp,
                 'io.github.gfander.dualforge.NormalItemBytecodeTest', args.fixture, args.native_control, run / 'bytes'])
        assert 'NORMAL ITEM CONTRACT PASS' in (run / 'bytecode.stdout.log').read_text()
        w.unchanged(records)
        result.update(status='PASS', negativeCases=13, fixtureSHA256=w.digest(args.fixture))
    finally:
        w.save(run / 'result.json', result)
    print('RESULT', result['status'], run)
