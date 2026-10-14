"""Package only the tested DualForge JAR and untouched native Forge fixture for manual zero-parameter acceptance."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import zipfile


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for key in ('jar', 'fixture', 'output'):
        parser.add_argument('--' + key, type=Path, required=True)
    parser.add_argument('--tested-commit', required=True)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=False)
    root = args.output / 'DualForge-007-Mac'
    (root / 'mods').mkdir(parents=True)
    for source, name in ((args.jar, 'DualForge.jar'), (args.fixture, 'dropin-items.jar')):
        shutil.copyfile(source, root / 'mods' / name)
        assert digest(source) == digest(root / 'mods' / name)
    with zipfile.ZipFile(args.fixture) as jar:
        for name in jar.namelist():
            if name.endswith('.class'):
                data = jar.read(name)
                assert b'net/neoforged/' not in data and b'io/github/gfander/dualforge/' not in data
                assert all(key not in data for key in (b'DF005_REGISTER', b'DF006_REGISTER', b'DF006B_REGISTER', b'getBoolean'))
    identity = {'testedCommit': args.tested_commit, 'compatSHA256': digest(args.jar),
                'nativeForgeSHA256': digest(args.fixture), 'MC': '1.21.1', 'NeoForge': '21.1.256',
                'Java': '21', 'clientStatus': 'NOT RUN', 'businessProperties': [],
                'requiredDualForgeJvmArguments': [], 'queryDriverIncluded': False,
                'preconvertedForgeInput': False}
    (root / 'BUILD-IDENTITY.json').write_text(json.dumps(identity, indent=2) + '\n')
    for name in ('README.md', 'CHECKLIST.md'):
        shutil.copyfile(Path(__file__).with_name('mac') / name, root / name)
    files = sorted(p for p in root.rglob('*') if p.is_file())
    (root / 'SHA256SUMS').write_text(''.join(digest(p) + '  ' + str(p.relative_to(root)) + '\n' for p in files))
    archive = args.output / 'dualforge-007-zero-config-mac-kit.zip'
    with zipfile.ZipFile(archive, 'w', compression=zipfile.ZIP_DEFLATED) as jar:
        for path in sorted(root.rglob('*')):
            if path.is_file():
                jar.write(path, str(path.relative_to(args.output)))
    with zipfile.ZipFile(archive) as jar:
        assert jar.testzip() is None
        for record in (root / 'SHA256SUMS').read_text().splitlines():
            expected, name = record.split('  ', 1)
            assert hashlib.sha256(jar.read('DualForge-007-Mac/' + name)).hexdigest() == expected
    (args.output / 'result.json').write_text(json.dumps({'status': 'PASS', 'clientStatus': 'NOT RUN',
         'archive': str(archive.resolve()), 'sha256': digest(archive), 'bytes': archive.stat().st_size,
         'identity': identity}, indent=2) + '\n')
    print(archive.resolve(), digest(archive))
