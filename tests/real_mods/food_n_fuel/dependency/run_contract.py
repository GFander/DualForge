"""Serial official-FML platform contract plus explicitly diagnostic pre-implementation proof."""
import argparse
import importlib.util
import os
from pathlib import Path
import subprocess

REPO = Path(__file__).resolve().parents[4]
spec = importlib.util.spec_from_file_location('workflow', REPO / 'tools/lib/unix_workflow.py')
w = importlib.util.module_from_spec(spec); spec.loader.exec_module(w)


def main():
    p = argparse.ArgumentParser(description=__doc__)
    for key in ('jar', 'server-input', 'jdk-home', 'probe', 'native', 'fnf'):
        p.add_argument('--' + key, type=Path, required=True)
    a = p.parse_args()
    for proc in Path('/proc').iterdir():
        try:
            if (proc / 'exe').resolve().name in ('java', 'javac'):
                raise RuntimeError('All JVM work must be serial')
        except (FileNotFoundError, PermissionError):
            pass
    run = w.run_directory(REPO / 'out/convergence-007c2/contract')
    sources = list(Path(__file__).parent.glob('*.java'))
    libs = sorted((a.server_input / 'libraries').rglob('*.jar'))
    asm = [p for p in libs if p.name in ('asm-9.10.1.jar', 'asm-tree-9.10.1.jar')]
    records = w.fingerprints([a.jar, a.probe, a.native, a.fnf, Path(__file__), *sources, *libs])
    w.save(run / 'inputs.json', records)
    w.save(run / 'invocation.json', {k: str(v) for k, v in vars(a).items()})
    result = {'status': 'FAIL', 'compatSHA256': w.digest(a.jar), 'testedCommit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=REPO, text=True).strip(), 'memoryContext': 'DIAGNOSTIC_ONLY; no GAME layer or fake Forge mod'}
    try:
        binary = w.tools(a, run)
        cp = os.pathsep.join(map(str, [a.jar, *asm, *libs]))
        classes = run / 'classes'; classes.mkdir()
        w.native(run, 'javac', [binary / 'javac', '--release', '21', '-proc:none', '-cp', cp, '-d', classes, *sources])
        prefix = [binary / 'java', '--add-opens', 'java.base/java.lang.invoke=ALL-UNNAMED', '--add-exports',
                  'java.base/sun.security.util=ALL-UNNAMED', '-cp', str(classes) + os.pathsep + cp]
        w.native(run, 'contract', [*prefix, 'io.github.gfander.dualforge.PlatformDependencyContract', a.probe, a.native, a.fnf])
        assert 'PLATFORM DEPENDENCY CONTRACT PASS' in (run / 'contract.stdout.log').read_text()
        w.native(run, 'feasibility', [*prefix, 'io.github.gfander.dualforge.ProjectionFeasibilityTest', a.fnf, a.probe])
        assert 'DIAGNOSTIC_ONLY PROJECTION FEASIBILITY PASS' in (run / 'feasibility.stdout.log').read_text()
        w.unchanged(records)
        result.update(status='PASS', officialSorter=True, assertions='contract.stdout.log')
    finally:
        w.save(run / 'result.json', result)
        print('RESULT', result['status'], run)


if __name__ == '__main__':
    main()
