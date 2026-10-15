"""Run standard-TOML/identity controls against one supplied compatibility JAR; no servers."""
import argparse
import importlib.util
import os
from pathlib import Path
import subprocess

REPO = Path(__file__).resolve().parents[4]
spec = importlib.util.spec_from_file_location('workflow', REPO / 'tools/lib/unix_workflow.py')
w = importlib.util.module_from_spec(spec)
spec.loader.exec_module(w)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('jar', 'server-input', 'jdk-home', 'fnf', 'dropin', 'multi', 'block', 'clumps', 'probe'):
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    for proc in Path('/proc').iterdir():
        try:
            if (proc / 'exe').resolve().name in ('java', 'javac'):
                raise RuntimeError('All JVM work must be serial')
        except (FileNotFoundError, PermissionError):
            pass
    run = w.run_directory(REPO / 'out/convergence-007c1/metadata-contract')
    w.save(run / 'invocation.json', {k: str(v) for k, v in vars(args).items()})
    libs = sorted((args.server_input / 'libraries').rglob('*.jar'))
    # Other runtime JARs may bundle obsolete ASM; the pinned runtime version wins.
    asm = [p for p in libs if p.name in ('asm-9.10.1.jar', 'asm-tree-9.10.1.jar')]
    native = args.server_input / 'libraries/net/neoforged/neoforge/21.1.256/neoforge-21.1.256-universal.jar'
    source = Path(__file__).with_name('MetadataAdmissionTest.java')
    inputs = w.fingerprints([args.jar, args.fnf, args.dropin, args.multi, args.block, args.clumps, args.probe, native, source, Path(__file__), *libs])
    w.save(run / 'inputs.json', inputs)
    result = {'status': 'FAIL', 'compatSHA256': w.digest(args.jar), 'testedCommit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=REPO, text=True).strip()}
    try:
        binary = w.tools(args, run)
        cp = os.pathsep.join(map(str, [args.jar, *asm, *libs]))
        classes = run / 'classes'; classes.mkdir()
        w.native(run, 'javac', [binary / 'javac', '--release', '21', '-cp', cp, '-d', classes, source])
        w.native(run, 'contract', [binary / 'java', '--add-opens', 'java.base/java.lang.invoke=ALL-UNNAMED',
                 '--add-exports', 'java.base/sun.security.util=ALL-UNNAMED', '-cp', str(classes) + os.pathsep + cp,
                 'io.github.gfander.dualforge.MetadataAdmissionTest', args.fnf, args.dropin, args.multi, args.block,
                 args.clumps, native, args.probe, run / 'controls'])
        assert 'METADATA ADMISSION PASS' in (run / 'contract.stdout.log').read_text()
        assert (run / 'contract.stderr.log').stat().st_size == 0
        w.unchanged(inputs)
        result.update(status='PASS', assertions='contract.stdout.log', bytes=w.fingerprints(list((run / 'controls').rglob('*.*'))))
    finally:
        w.save(run / 'result.json', result)
        print('RESULT', result['status'], run)


if __name__ == '__main__':
    main()
