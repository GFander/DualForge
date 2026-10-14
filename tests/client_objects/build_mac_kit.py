"""Package two independently verified native Forge fixtures with canonical runtime IDs."""
import argparse, hashlib, importlib.util, json, shutil, subprocess, zipfile
from pathlib import Path

SOURCE = Path(__file__).resolve().parent
REPO = SOURCE.parents[1]
spec = importlib.util.spec_from_file_location('cohort', REPO / 'tests/multi_registry/run_multi_registry.py')
cohort = importlib.util.module_from_spec(spec); spec.loader.exec_module(cohort)
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
TEXTURES = {'ice_ether': '2c810e0c1cca3c451a536a4cf1d72448a5809f999933acc45ca01240315f1681',
            'raw_ice_ether': '4fce855f8822a589c8a0a1b83d61c422711710fbf69a52947358727b183dcf69',
            'ice_ether_block': 'cae07e312af545d8a98b5347032d51353583b22edc9fa39cea5353c689e56596'}

def assets(role, fixture):
    namespace = 'multiregistryprobe' if role == 'items' else 'blockregistryprobe'
    names = ['ice_ether', 'raw_ice_ether'] if role == 'items' else ['ice_ether_block']
    labels = [('Ice Ether', '冰之以太'), ('Raw Ice Ether', '冰之以太粗矿')] if role == 'items' else [('Ice Ether Block', '冰之以太块')]
    kind = 'item' if role == 'items' else 'block'
    with zipfile.ZipFile(fixture) as z:
        assert z.testzip() is None
        for index, language in enumerate(('en_us', 'zh_cn')):
            actual = json.loads(z.read(f'assets/{namespace}/lang/{language}.json'))
            assert actual == {f'{kind}.{namespace}.{name}': label[index] for name, label in zip(names, labels)}
        for name in names:
            prefix = f'assets/{namespace}/'
            model = json.loads(z.read(prefix + f'models/item/{name}.json'))
            if role == 'items':
                assert model == {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{namespace}:item/{name}'}}
            else:
                assert model == {'parent': f'{namespace}:block/{name}'}
                assert json.loads(z.read(prefix + f'blockstates/{name}.json')) == {'variants': {'': {'model': f'{namespace}:block/{name}'}}}
                assert json.loads(z.read(prefix + f'models/block/{name}.json')) == {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{namespace}:block/{name}'}}
            assert hashlib.sha256(z.read(prefix + f'textures/{kind}/{name}.png')).hexdigest() == TEXTURES[name]
        assert not any(any(old in n for old in ('alpha_item', 'beta_item', 'sample_block')) for n in z.namelist())

def build(args):
    history = json.loads(args.historical.read_text())
    results = {}
    for role in ('items', 'block'):
        paths = json.loads((REPO / 'out/client-objects' / role / 'stage-results.json').read_text())
        stages = {stage: json.loads(Path(paths[stage]).read_text()) for stage in ('contract', 'control', 'treatment')}
        stages['regression'] = history
        assert cohort.consistent(stages), 'Actual cohort differs: ' + role
        assert all(stages[s]['legacyRegistryIDsAbsent'] for s in ('control', 'treatment'))
        result = stages['treatment']; assert result['compat']['SHA256'] == sha(args.compat)
        fixture = Path(result['fixture']['Path']); assert result['fixture']['SHA256'] == sha(fixture)
        assets(role, fixture)
        results[role] = {'stageResults': paths, 'fixture': result['fixture'], 'compat': result['compat'], 'clientStatus': 'NOT RUN', 'actualRenamedServerCT': 'PASS'}
    out = args.output_dir.resolve(); assert out.is_relative_to(REPO / 'out'); out.mkdir(parents=True, exist_ok=False)
    root = out / 'dualforge-ice-ether-client-test-kit'; root.mkdir()
    for role, result in results.items():
        mods = root / (role + '-test/mods'); mods.mkdir(parents=True)
        shutil.copyfile(args.compat, mods / 'dualforge.jar')
        fixture = Path(result['fixture']['Path']); shutil.copyfile(fixture, mods / fixture.name)
        assert sha(mods / fixture.name) == result['fixture']['SHA256']
        switch = 'DF006_REGISTER' if role == 'items' else 'DF006B_REGISTER'
        flags = ['-Ddualforge.2a.rewrite=true', '-Ddualforge.0g.apiRewrite=true', '-Ddualforge.0j.emptyRegistration=true', '-Ddualforge.0l.subscriberRewrite=true', '-D' + switch + '=true', f'-Ddualforge.dump.path=dualforge-{role}-evidence/rewritten.class', f'-Ddualforge.dump.profileDir=dualforge-{role}-evidence/profile']
        (mods.parent / 'JVM-ARGS.txt').write_text('\n'.join(flags) + '\n')
    for name in ('MAC-SETUP.md', 'CHECKLIST.md'): shutil.copyfile(SOURCE / name, root / name)
    source_files = [p for role in ('items', 'block') for p in (SOURCE / role).rglob('*') if p.is_file()]
    source_files += [SOURCE / name for name in ('build_mac_kit.py', 'MAC-SETUP.md', 'CHECKLIST.md')]
    manifest = {'task': 'Ice Ether object identity unification', 'sourceCommit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=REPO, text=True).strip(), 'compatSHA256': sha(args.compat), 'roles': results, 'sourceInputs': {p.relative_to(REPO).as_posix(): sha(p) for p in source_files}, 'actualClientStatus': 'NOT RUN', 'simultaneousForgeModsTested': False, 'productionJavaChanged': False, 'runtimeIDs': ['multiregistryprobe:ice_ether', 'multiregistryprobe:raw_ice_ether', 'blockregistryprobe:ice_ether_block'], 'textureSHA256': TEXTURES, 'uploaded': False}
    manifest['files'] = {p.relative_to(root).as_posix(): sha(p) for p in sorted(root.rglob('*')) if p.is_file()}
    (root / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n')
    (root / 'SHA256SUMS').write_text(''.join(sha(p) + '  ' + p.relative_to(root).as_posix() + '\n' for p in sorted(root.rglob('*')) if p.is_file()))
    target = out / 'dualforge-ice-ether-client-test-kit.zip'
    with zipfile.ZipFile(target, 'w', zipfile.ZIP_DEFLATED) as z:
        for p in sorted(root.rglob('*')):
            if p.is_file(): z.write(p, p.relative_to(out))
    with zipfile.ZipFile(target) as z:
        assert z.testzip() is None
        for line in z.read(root.name + '/SHA256SUMS').decode().splitlines():
            digest, name = line.split('  ', 1); assert hashlib.sha256(z.read(root.name + '/' + name)).hexdigest() == digest
    result = {'status': 'PASS', 'scope': 'canonical compiled fixture assets and packaging', 'sourceCommit': manifest['sourceCommit'], 'zip': str(target), 'SHA256': sha(target), 'bytes': target.stat().st_size, 'clientStatus': 'NOT RUN', 'serverIdentityCT': 'PASS both independent fixtures', 'remoteUrl': None}
    (out / 'result.json').write_text(json.dumps(result, indent=2) + '\n'); print(json.dumps(result, indent=2))

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('compat', 'historical', 'output-dir'): parser.add_argument('--' + name, type=Path, required=True)
    build(parser.parse_args())
