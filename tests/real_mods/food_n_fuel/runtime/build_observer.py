"""Build a separate native NeoForge read-only observer, never compile/modify FnF."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import zipfile


def main():
    p = argparse.ArgumentParser(description=__doc__)
    for name in ['install', 'jdk-home', 'output']:
        p.add_argument('--' + name, type=Path, required=True)
    a = p.parse_args()
    out = a.output.resolve()
    out.mkdir(parents=True, exist_ok=False)
    classes = out / 'classes'
    classes.mkdir()
    src = Path(__file__).with_name('NeoRegistryObserver.java').resolve()
    libs = sorted((a.install.resolve() / 'libraries').rglob('*.jar'))
    digest = lambda f: hashlib.sha256(f.read_bytes()).hexdigest()
    inputs = [{'path': str(f), 'bytes': f.stat().st_size, 'sha256': digest(f)} for f in [src, Path(__file__).resolve(), *libs]]
    (out / 'inputs.json').write_text(json.dumps(inputs, indent=2) + '\n')
    command = [str(a.jdk_home.resolve() / 'bin/javac'), '--release', '21', '-proc:none',
               '-cp', os.pathsep.join(map(str, libs)), '-d', str(classes), str(src)]
    (out / 'command.json').write_text(json.dumps(command, indent=2) + '\n')
    r = subprocess.run(command, capture_output=True)
    (out / 'stdout.log').write_bytes(r.stdout)
    (out / 'stderr.log').write_bytes(r.stderr)
    (out / 'exit.json').write_text(json.dumps({'exitCode': r.returncode}) + '\n')
    r.check_returncode()
    jar = out / 'fnf-runtime-observer.jar'
    with zipfile.ZipFile(jar, 'x', zipfile.ZIP_DEFLATED) as z:
        z.writestr('META-INF/neoforge.mods.toml', 'modLoader="javafml"\nloaderVersion="[4,)"\nlicense="test fixture"\n[[mods]]\nmodId="fnfruntimeobserver"\nversion="1"\n')
        for f in sorted(classes.rglob('*.class')):
            z.write(f, f.relative_to(classes).as_posix())
    assert all(digest(Path(v['path'])) == v['sha256'] for v in inputs)
    (out / 'result.json').write_text(json.dumps({'jar': str(jar), 'sha256': digest(jar),
        'bytes': jar.stat().st_size, 'sourceCommit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], text=True).strip()}, indent=2) + '\n')
    print(jar)


if __name__ == '__main__':
    main()
