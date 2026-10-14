"""Add client assets to the frozen native Forge fixture; package without recompiling Java."""
import argparse, hashlib, json, shutil, struct, subprocess, zipfile
from pathlib import Path

SOURCE = Path(__file__).resolve().parent
REPO = SOURCE.parents[1]
TEXTURE_SHA = 'cae07e312af545d8a98b5347032d51353583b22edc9fa39cea5353c689e56596'

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def check_assets(resources):
    base = resources / 'assets/blockregistryprobe'
    load = lambda name: json.loads((base / name).read_text())
    assert load('blockstates/sample_block.json') == {'variants': {'': {'model': 'blockregistryprobe:block/sample_block'}}}
    assert load('models/block/sample_block.json') == {'parent': 'minecraft:block/cube_all', 'textures': {'all': 'blockregistryprobe:block/ice_ether_block'}}
    assert load('models/item/sample_block.json') == {'parent': 'blockregistryprobe:block/sample_block'}
    for language, name in [('en_us', 'Ice Ether Block'), ('zh_cn', '冰之以太块')]:
        assert load('lang/' + language + '.json') == {'block.blockregistryprobe.sample_block': name}
    texture = base / 'textures/block/ice_ether_block.png'
    data = texture.read_bytes()
    assert data[:8] == bytes.fromhex('89504e470d0a1a0a') and struct.unpack('>II', data[16:24]) == (16, 16)
    assert sha(texture) == TEXTURE_SHA, 'User texture bytes changed'
    return sorted(p for p in resources.rglob('*') if p.is_file())

def check_translations(resources):
    assert json.loads((resources / 'pack.mcmeta').read_text())['pack']['pack_format'] == 34
    keys = {'registryprobe': {'item.registryprobe.ice_ether': ('Ice Ether', '冰之以太')},
            'multiregistryprobe': {'item.multiregistryprobe.alpha_item': ('Ice Ether', '冰之以太'), 'item.multiregistryprobe.beta_item': ('Raw Ice Ether', '冰之以太粗矿')},
            'blockregistryprobe': {'block.blockregistryprobe.sample_block': ('Ice Ether Block', '冰之以太块')}}
    for namespace, names in keys.items():
        for index, language in enumerate(('en_us', 'zh_cn')):
            data = json.loads((resources / 'assets' / namespace / 'lang' / (language + '.json')).read_text())
            assert data == {key: value[index] for key, value in names.items()}
    return sorted(p for p in resources.rglob('*') if p.is_file())

def build(args):
    report = json.loads(args.validation.read_text())
    assert report['006B-S'] == 'PASS' and report['sameArtifactCohort']
    for name, path in [('compat', args.compat), ('fixture', args.fixture)]:
        assert report[name]['Path'] == str(path.resolve()) and report[name]['SHA256'] == sha(path)
    for stage, path in report['stages'].items():
        result = json.loads(Path(path).read_text())
        assert result['status'] == 'PASS' and result['compat']['SHA256'] == sha(args.compat), stage
        if stage in ('contract', 'control', 'treatment'):
            assert result['fixture']['SHA256'] == sha(args.fixture)
    resources = SOURCE / 'client_resources'
    assets = check_assets(resources)
    translations = SOURCE / 'client_translations'
    translation_files = check_translations(translations)
    build_inputs = [SOURCE / name for name in ('build_mac_kit.py', 'build-mac-kit.zsh', 'MAC-SETUP.md', 'JVM-ARGS.txt', 'CHECKLIST.md')]
    original_hashes = {str(p.resolve()): sha(p) for p in [args.compat, args.fixture, args.validation, *assets, *translation_files, *build_inputs]}
    out = args.output_dir.resolve()
    assert out.is_relative_to(REPO / 'out')
    out.mkdir(parents=True, exist_ok=False)
    root = out / 'dualforge-006b-mac-test-kit'
    (root / 'mods').mkdir(parents=True)
    shutil.copyfile(args.compat, root / 'mods/dualforge.jar')
    fixture = root / 'mods/block-registry-probe.jar'
    with zipfile.ZipFile(args.fixture) as original, zipfile.ZipFile(fixture, 'w', zipfile.ZIP_DEFLATED) as client:
        names = original.namelist()
        assert len(names) == len(set(names)) and original.testzip() is None
        entry = original.read('com/example/blockregistrycheck/BlockRegistryProbe.class')
        assert b'net/neoforged/' not in entry and b'net/minecraftforge/registries/DeferredRegister' in entry
        for name in names:
            client.writestr(name, original.read(name))
        for path in assets:
            name = path.relative_to(resources).as_posix()
            assert name not in names
            client.writestr(name, path.read_bytes())
    with zipfile.ZipFile(args.fixture) as original, zipfile.ZipFile(fixture) as client:
        assert client.testzip() is None
        assert set(client.namelist()) == set(original.namelist()) | {p.relative_to(resources).as_posix() for p in assets}
        assert all(client.read(name) == original.read(name) for name in original.namelist())
        assert all(client.read(p.relative_to(resources).as_posix()) == p.read_bytes() for p in assets)
        classes = {name: hashlib.sha256(original.read(name)).hexdigest() for name in names if name.endswith('.class')}
    for name in ('MAC-SETUP.md', 'JVM-ARGS.txt', 'CHECKLIST.md'):
        shutil.copyfile(SOURCE / name, root / name)
    (root / 'resourcepacks').mkdir()
    language_pack = root / 'resourcepacks/dualforge-ice-ether-names.zip'
    with zipfile.ZipFile(language_pack, 'w', zipfile.ZIP_DEFLATED) as archive:
        for path in translation_files:
            archive.writestr(path.relative_to(translations).as_posix(), path.read_bytes())
    with zipfile.ZipFile(language_pack) as archive:
        assert archive.testzip() is None
        assert all(archive.read(p.relative_to(translations).as_posix()) == p.read_bytes() for p in translation_files)
    manifest = {'task': 'Convergence-006B-C preparation', 'clientStatus': 'NOT RUN', 'serverStatus': 'PASS for original server cohort', 'minecraft': '1.21.1', 'neoforge': '21.1.256', 'javaMajor': 21, 'sourceCommit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=REPO, text=True).strip(), 'compat': report['compat'], 'originalServerFixture': report['fixture'], 'clientFixture': {'SHA256': sha(fixture), 'Bytes': fixture.stat().st_size}, 'registryID': 'blockregistryprobe:sample_block', 'textureSHA256': TEXTURE_SHA, 'originalMembersUnchanged': True, 'unchangedClassSHA256': classes, 'assets': {p.relative_to(resources).as_posix(): sha(p) for p in assets}, 'testDriverIncluded': False, 'compiledJava': False, 'productionJavaChanged': False, 'uploadUrl': None}
    manifest['files'] = {p.relative_to(root).as_posix(): sha(p) for p in sorted(root.rglob('*')) if p.is_file()}
    manifest['buildInputs'] = {p.relative_to(REPO).as_posix(): sha(p) for p in [*assets, *translation_files, *build_inputs]}
    manifest['languagePack'] = {'path': language_pack.relative_to(root).as_posix(), 'SHA256': sha(language_pack), 'minecraftPackFormat': 34, 'modIDsUnchanged': True, 'visualAcceptance': 'NOT RUN'}
    (root / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n')
    (root / 'SHA256SUMS').write_text(''.join(f'{sha(p)}  {p.relative_to(root)}\n' for p in sorted(root.rglob('*')) if p.is_file()))
    target = out / 'dualforge-006b-mac-test-kit.zip'
    with zipfile.ZipFile(target, 'w', zipfile.ZIP_DEFLATED) as archive:
        for path in sorted(root.rglob('*')):
            if path.is_file():
                archive.write(path, path.relative_to(out))
    with zipfile.ZipFile(target) as archive:
        assert archive.testzip() is None
        for line in archive.read(root.name + '/SHA256SUMS').decode().splitlines():
            digest, name = line.split('  ', 1)
            assert hashlib.sha256(archive.read(root.name + '/' + name)).hexdigest() == digest
        file_count = len(archive.namelist())
    assert all(sha(Path(path)) == digest for path, digest in original_hashes.items())
    result = {'status': 'PASS', 'scope': 'assets, original-member preservation and packaging only; no client or server execution', 'sourceCommit': manifest['sourceCommit'], 'zip': str(target), 'SHA256': sha(target), 'bytes': target.stat().st_size, 'contents': file_count, 'clientFixtureSHA256': sha(fixture), 'compatSHA256': sha(args.compat), 'textureSHA256': TEXTURE_SHA, 'clientStatus': 'NOT RUN', 'uploaded': False}
    (out / 'result.json').write_text(json.dumps(result, indent=2) + '\n')
    (out / (target.name + '.sha256')).write_text(sha(target) + '  ' + target.name + '\n')
    print(json.dumps(result, indent=2))

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('compat', 'fixture', 'validation', 'output-dir'):
        parser.add_argument('--' + name, type=Path, required=True)
    build(parser.parse_args())
