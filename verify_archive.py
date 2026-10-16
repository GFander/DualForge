"""Verify an evidence ZIP against its external digest, manifest and checksum list."""
import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import stat
import zipfile


def verify(path, expected):
    assert hashlib.sha256(path.read_bytes()).hexdigest() == expected, 'ZIP SHA mismatch'
    prefix = 'DualForge-007C4C4-Evidence/'
    with zipfile.ZipFile(path) as z:
        names = z.namelist()
        assert len(names) == len(set(names)), 'Duplicate ZIP member'
        for i in z.infolist():
            parts = PurePosixPath(i.filename).parts
            assert i.filename.startswith(prefix) and not PurePosixPath(i.filename).is_absolute() and '..' not in parts
            assert not stat.S_ISLNK(i.external_attr >> 16), 'Symlink'
        assert z.testzip() is None, 'CRC failure'
        manifest = json.loads(z.read(prefix+'MANIFEST.json'))
        entries = manifest['files']
        assert len({r['archivePath'] for r in entries}) == len(entries)
        expected_names = {prefix+r['archivePath'] for r in entries} | {prefix+'MANIFEST.json', prefix+'SHA256SUMS'}
        assert set(names) == expected_names, 'Manifest member set mismatch'
        for r in entries:
            b = z.read(prefix+r['archivePath'])
            assert len(b) == r['bytes'] and hashlib.sha256(b).hexdigest() == r['archiveSHA256'], r['archivePath']
        assert len(entries) == manifest['fileCount'] and sum(r['bytes'] for r in entries) == manifest['totalBytes']
        lines = z.read(prefix+'SHA256SUMS').decode().splitlines()
        assert len(lines) == len(entries)+1
        checks = dict(line.split('  ', 1)[::-1] for line in lines)
        assert set(checks) == {r['archivePath'] for r in entries} | {'MANIFEST.json'}
        for name, digest in checks.items():
            assert hashlib.sha256(z.read(prefix+name)).hexdigest() == digest, name
        return {'status':'PASS', 'zipSHA256':expected, 'files':len(entries), 'bytes':manifest['totalBytes']}

if __name__ == '__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('zip',type=Path)
    p.add_argument('--sha256',required=True)
    a=p.parse_args()
    print(json.dumps(verify(a.zip,a.sha256),indent=2))
