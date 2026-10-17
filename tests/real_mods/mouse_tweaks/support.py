"""SSD-only evidence helpers; every compiler/JVM is called serially by the runner."""
import hashlib
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[3]
ENV = Path('/home/albert/Env/DualForge')
JDK = ENV / 'jdks/msjdk-21.0.10'


def sha(path):
    with Path(path).open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def record(path):
    path = Path(path).resolve()
    return {'path': str(path), 'bytes': path.stat().st_size, 'sha256': sha(path)}


def save(path, value):
    Path(path).write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')


def fresh(path):
    path = Path(path).resolve()
    if not path.is_relative_to(ENV / 'runs'):
        raise ValueError('New outputs must stay below the SSD runs directory')
    path.mkdir(parents=True, exist_ok=False)
    return path


def libraries(host):
    root = ENV / ('forge-52.1.0' if host == 'forge' else 'neoforge-21.1.256') / 'libraries'
    # Prefer current versions where the stock install also has installer tools.
    return sorted(root.rglob('*.jar'))


def run(output, name, argv):
    command = [str(arg) for arg in argv]
    save(output / (name + '.command.json'), {'argv': command, 'cwd': str(output)})
    result = subprocess.run(command, cwd=output, capture_output=True)
    (output / (name + '.stdout.log')).write_bytes(result.stdout)
    (output / (name + '.stderr.log')).write_bytes(result.stderr)
    save(output / (name + '.exit.json'), {'exitCode': result.returncode})
    if result.returncode:
        raise RuntimeError(name + ' failed; see ' + str(output / (name + '.stderr.log')))
    return result.stdout.decode()
