"""Freeze installed official artifacts without copying or changing runtime inputs."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import zipfile


def sha(path):
    with Path(path).open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def save(path, value):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, sort_keys=True,
                               separators=(',', ':')) + '\n')


def role(relative):
    if 'net/minecraftforge/forge/' in relative or 'net/neoforged/neoforge/' in relative:
        return 'PATCHED_MINECRAFT' if relative.endswith('-server.jar') else 'PLATFORM'
    if any(x in relative for x in ('/fmlcore/', '/fmlloader/', '/javafmllanguage/',
                                   '/fancymodloader/', '/eventbus/', '/bus/')):
        return 'PLATFORM'
    if '/net/minecraft/' in '/' + relative:
        return 'MINECRAFT_INTERMEDIATE'
    if any(x in relative.lower() for x in ('installertools', 'binarypatcher',
                                          'autorenamingtool', 'jarsplitter',
                                          'mcp_config', 'neoform', 'specialsource')):
        return 'DEVELOPMENT_TOOLCHAIN'
    return 'RUNTIME_DEPENDENCY'


def coordinate(relative):
    parts = relative.split('/')
    if 'libraries' not in parts:
        return 'installer-output:' + parts[-1]
    p = parts[parts.index('libraries') + 1:]
    return '.'.join(p[:-3]) + ':' + p[-3] + ':' + p[-2] + ':' + p[-1]


def inventory(path):
    with zipfile.ZipFile(path) as jar:
        names = jar.namelist()
        if len(names) != len(set(names)):
            raise ValueError('duplicate ZIP member: ' + str(path))
        members = [{'name': x.filename, 'size': x.file_size, 'crc32': x.CRC}
                   for x in sorted(jar.infolist(), key=lambda x: x.filename)]
        blob = json.dumps(members, sort_keys=True, separators=(',', ':')).encode()
        return {'memberInventorySHA256': hashlib.sha256(blob).hexdigest(),
                'inventoryDefinition': 'sorted ZIP name/uncompressed size/CRC32; whole JAR SHA binds member bytes',
                'memberCount': len(names), 'classCount': sum(n.endswith('.class') for n in names)}


def freeze(forge, neo, output, java_home=None):
    artifacts, local = [], []
    for platform, root in [('forge', Path(forge).resolve()), ('neoforge', Path(neo).resolve())]:
        if platform == 'forge':
            with zipfile.ZipFile(root / 'forge-1.21.1-52.1.0-shim.jar') as jar:
                selection_text = jar.read('bootstrap-shim.list').decode()
            selected = {'libraries/' + line.split('\t')[2]
                        for line in selection_text.splitlines() if line}
            selection_basis = 'official bootstrap-shim.list (exact path and SHA-256)'
        else:
            argument_file = root / 'libraries/net/neoforged/neoforge/21.1.256/unix_args.txt'
            selection_text = argument_file.read_text()
            selected = set(re.findall(r'libraries/[^\s:]+\.jar', selection_text))
            selected.update(p.relative_to(root).as_posix() for p in root.rglob('*.jar')
                            if ('/net/neoforged/neoforge/' in str(p)
                                or p.name.endswith(('-srg.jar', '-extra.jar'))))
            selection_basis = 'official unix_args module/legacy paths plus fixed game universal/patch/srg/extra resources'
        for path in sorted(root.rglob('*.jar')):
            # The two root/libraries Forge bootstrap shims are retained as distinct origins.
            relative = path.relative_to(root).as_posix()
            identity = platform + '/' + relative
            record = {'id': identity, 'platform': platform, 'filename': path.name,
                      'coordinates': coordinate(relative), 'relativePath': relative,
                      'role': role(relative), 'size': path.stat().st_size,
                      'activeServerResource': relative in selected,
                      'serverResourceSelectionBasis': selection_basis,
                      'sha256': sha(path), **inventory(path),
                      'provenance': 'existing official version installer output; generated outputs are not Maven downloads',
                      'sourceURL': None, 'javaTargetVersions': 'filled by ASM scan'}
            if '/libraries/' in '/' + relative:
                tail = relative.split('libraries/', 1)[1]
                origin = ('https://maven.minecraftforge.net/' if platform == 'forge'
                          else 'https://maven.neoforged.net/releases/')
                if record['role'] == 'PLATFORM' and not path.name.endswith('-server.jar'):
                    record['sourceURL'] = origin + tail
            artifacts.append(record)
            local.append({'id': identity, 'path': str(path)})
    if java_home is not None:
        path = Path(java_home).resolve() / 'jmods/java.base.jmod'
        identity = 'java/java.base.jmod'
        artifacts.append({'id': identity, 'platform': 'java', 'filename': path.name,
            'coordinates': 'Microsoft OpenJDK:java.base:21.0.10+7-LTS', 'relativePath': 'jmods/java.base.jmod',
            'role': 'JAVA_STANDARD', 'activeServerResource': True, 'size': path.stat().st_size,
            'sha256': sha(path), **inventory(path), 'sourceURL': None,
            'provenance': 'same actual Java21 toolchain used by both fixed installations',
            'javaTargetVersions': 'filled by ASM scan'})
        local.append({'id': identity, 'path': str(path)})
    output = Path(output)
    save(output / 'manifest.json', {'schemaVersion': 1, 'versions': {
        'minecraft': '1.21.1', 'forge': '52.1.0', 'neoforge': '21.1.256', 'fml': '4.0.45'},
        'scope': 'all JARs present in the two fixed official server installations, including unused intermediates/tools',
        'clientPatchedABI': 'PARTIAL / INPUT_MISSING: no complete patched client binaries',
        'artifacts': artifacts})
    save(output / 'local-paths.json', local)
    (output / 'scan.tsv').write_text(''.join(f"{x['id']}\t{x['path']}\n" for x in local))
    return artifacts


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--forge-install', required=True, type=Path)
    parser.add_argument('--neo-install', required=True, type=Path)
    parser.add_argument('--output', required=True, type=Path)
    parser.add_argument('--java-home', type=Path)
    args = parser.parse_args()
    items = freeze(args.forge_install, args.neo_install, args.output, args.java_home)
    print(json.dumps({'artifacts': len(items), 'bytes': sum(x['size'] for x in items)}))
