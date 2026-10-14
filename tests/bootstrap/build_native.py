"""Build a separate native NeoForge coexistence mod against the actual host runtime."""
import argparse
import importlib.util
import os
from pathlib import Path
import shutil

REPO = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('workflow', REPO / 'tools/lib/unix_workflow.py')
w = importlib.util.module_from_spec(spec)
spec.loader.exec_module(w)

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--server-input', type=Path, required=True)
    parser.add_argument('--jdk-home', type=Path, required=True)
    args = parser.parse_args()
    run = w.run_directory(REPO / 'out/convergence-007/native-fixture')
    binary = w.tools(args, run)
    source = Path(__file__).with_name('native')
    libraries = sorted((args.server_input / 'libraries').rglob('*.jar'))
    records = w.fingerprints([p for p in source.rglob('*') if p.is_file()] + libraries)
    w.save(run / 'inputs.json', records)
    shutil.copytree(source, run / 'source')
    classes = run / 'classes'
    classes.mkdir()
    w.native(run, 'javac', [binary / 'javac', '--release', '21', '-cp', os.pathsep.join(map(str, libraries)),
             '-d', classes, *sorted(source.rglob('*.java'))])
    root = run / 'root'
    shutil.copytree(classes, root)
    shutil.copytree(source / 'main/resources', root, dirs_exist_ok=True)
    jar = run / 'native-bootstrap.jar'
    w.native(run, 'jar', [binary / 'jar', '--create', '--file', jar, '--manifest',
             root / 'META-INF/MANIFEST.MF', '-C', root, '.'])
    w.unchanged(records)
    w.save(REPO / 'out/convergence-007/native-fixture.json', {'status': 'PASS', 'jar': w.fingerprints([jar])[0]})
    print('RESULT PASS', run)
