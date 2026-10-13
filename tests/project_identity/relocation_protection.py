"""Exact authorized path relocation; the original index remains the byte authority."""
import hashlib
from pathlib import PurePosixPath

PREFIXES = (
    ('spike000/probe/', 'history/early-spikes/forge-probe-000/'),
    ('spike001a/compat/', 'history/early-spikes/discovery-001a/'),
    ('spike001b5/probe/', 'history/early-spikes/modinfo-001b5/'),
)
CONFIGS = {'fml.toml', 'neoforge-common.toml', 'neoforge-server.toml'}


def destination(name):
    for old, new in PREFIXES:
        if name.startswith(old):
            return new + name[len(old):]
    prefix = 'spike000/neoforge-server/config/'
    if name.startswith(prefix) and name[len(prefix):] in CONFIGS:
        return 'runtime/neoforge-server/config/' + name[len(prefix):]
    return None


def protect(root, items, relocations, tracked):
    protected = {item['path']: item['sha256'] for item in items}
    assert len(protected) == len(items), 'Duplicate protected item'
    mapping = {}
    targets = set()
    for item in relocations:
        assert set(item) == {'oldPath', 'newPath', 'sha256'}, 'Unknown relocation metadata'
        old, new = item['oldPath'], item['newPath']
        for name in (old, new):
            path = PurePosixPath(name)
            assert not path.is_absolute() and '..' not in path.parts and str(path) == name, 'Unsafe relocation path'
        assert old in protected and old not in mapping, 'Unknown or duplicate relocation source'
        assert new not in targets, 'Duplicate relocation target'
        assert new == destination(old), 'Unauthorized relocation target'
        assert new not in protected, 'Relocation collides with protected path'
        assert item['sha256'] == protected[old], 'Relocation hash differs from original index'
        mapping[old] = new
        targets.add(new)
    if relocations:
        expected = {name for name in protected if destination(name) is not None}
        assert set(mapping) == expected, 'Relocation protection item omitted'
    current = {}
    for old, digest in protected.items():
        name = mapping.get(old, old)
        if old in mapping:
            assert old not in tracked, 'Old and new paths both tracked: ' + old
        assert name in tracked, 'Missing protected tracked file: ' + name
        path = root / name
        assert path.is_file() and not path.is_symlink(), 'Missing protected file: ' + name
        assert hashlib.sha256(path.read_bytes()).hexdigest() == digest, 'Protected bytes changed: ' + name
        current[name] = old
    return current
