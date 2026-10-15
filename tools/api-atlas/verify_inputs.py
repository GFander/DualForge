#!/usr/bin/env python3
"""Verify original installer checksums and immutable local artifact identities."""
import argparse
import hashlib
import json
from pathlib import Path
import zipfile
from inputs import inventory, save, sha


def verify(directory, forge_installer, neo_installer, output):
    manifest = json.loads((directory / 'manifest.json').read_text())
    local = {r['id']: Path(r['path']) for r in json.loads((directory / 'local-paths.json').read_text())}
    checks = []
    for artifact in manifest['artifacts']:
        p = local[artifact['id']]
        assert p.stat().st_size == artifact['size'] and sha(p) == artifact['sha256'], artifact['id']
        assert inventory(p)['memberInventorySHA256'] == artifact['memberInventorySHA256'], artifact['id']
    for host, installer in [('forge', forge_installer), ('neoforge', neo_installer)]:
        lookup = {r['relativePath']: local[r['id']] for r in manifest['artifacts'] if r['platform'] == host}
        with zipfile.ZipFile(installer) as jar:
            for member in ['install_profile.json', 'version.json']:
                profile = json.loads(jar.read(member))
                for lib in profile.get('libraries', []):
                    entry = lib.get('downloads', {}).get('artifact', {})
                    path = lookup.get('libraries/' + entry.get('path', ''))
                    if path is not None:
                        actual = hashlib.sha1(path.read_bytes()).hexdigest()
                        assert actual == entry['sha1'], lib['name']
                        checks.append({'platform': host, 'coordinate': lib['name'],
                            'installerSHA256': sha(installer), 'member': member,
                            'expectedSHA1': entry['sha1'], 'actualSHA1': actual,
                            'sourceURL': entry.get('url'), 'status': 'PASS'})
    # Forge's generated patched-server JAR is bound by the official bootstrap list too.
    forge_root = next(p for identity, p in local.items() if identity == 'forge/forge-1.21.1-52.1.0-shim.jar')
    with zipfile.ZipFile(forge_root) as jar:
        boot = jar.read('bootstrap-shim.list').decode()
    for line in boot.splitlines():
        expected, coordinate, relative = line.split('\t')
        identity = 'forge/libraries/' + relative
        assert identity in local and sha(local[identity]) == expected, coordinate
        checks.append({'platform': 'forge', 'coordinate': coordinate, 'member': 'bootstrap-shim.list',
                       'expectedSHA256': expected, 'actualSHA256': sha(local[identity]), 'status': 'PASS'})
    result = {'status': 'PASS', 'artifacts': len(manifest['artifacts']), 'installerAndBootstrapChecks': len(checks),
              'checks': checks, 'minecraftPatchedClient': 'PARTIAL / INPUT_MISSING',
              'remoteForgeChecksumRequests': '403 retained separately; not claimed as successful'}
    save(output, result)
    return {'status': 'PASS', 'artifacts': len(manifest['artifacts']), 'checks': len(checks)}


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--inputs-directory', type=Path, required=True)
    p.add_argument('--forge-installer', type=Path, required=True)
    p.add_argument('--neo-installer', type=Path, required=True)
    p.add_argument('--output', type=Path, required=True)
    a = p.parse_args()
    print(json.dumps(verify(a.inputs_directory, a.forge_installer, a.neo_installer, a.output)))
