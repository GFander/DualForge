#!/usr/bin/env python3
"""Serial offline ASM/JVM checks using external, frozen official dependencies."""
import argparse
import json
import os
from pathlib import Path
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools/api-atlas'))
from atlas import compile_tools, execute
from inputs import save, sha
from model import records


def package(path, directory, forge=True):
    with zipfile.ZipFile(path, 'w', zipfile.ZIP_DEFLATED) as jar:
        for file in sorted(directory.rglob('*.class')):
            info = zipfile.ZipInfo(file.relative_to(directory).as_posix(), (2020, 1, 1, 0, 0, 0))
            jar.writestr(info, file.read_bytes())
        if forge:
            info = zipfile.ZipInfo('META-INF/mods.toml', (2020, 1, 1, 0, 0, 0))
            jar.writestr(info, 'modLoader="javafml"\nloaderVersion="[52,)"\nlicense="test fixture"\n[[mods]]\nmodId="atlasprobe"\nversion="1"\n')


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--java-home', required=True, type=Path)
    p.add_argument('--libraries', required=True, type=Path)
    p.add_argument('--inputs-directory', required=True, type=Path)
    p.add_argument('--atlas-directory', required=True, type=Path)
    p.add_argument('--output', required=True, type=Path)
    p.add_argument('--fnf', required=True, type=Path)
    a = p.parse_args()
    a.output.mkdir(parents=True, exist_ok=False)
    cp = compile_tools(a.java_home, a.libraries, a.output / 'tool')
    java, javac = a.java_home / 'bin/java', a.java_home / 'bin/javac'
    locals_ = {r['id']: r['path'] for r in json.loads((a.inputs_directory / 'local-paths.json').read_text())}
    manifest = json.loads((a.inputs_directory / 'manifest.json').read_text())
    assert (a.atlas_directory / 'summary.json').is_file(), 'fixed ABI atlas is required'
    assert manifest['versions'] == {'minecraft': '1.21.1', 'forge': '52.1.0', 'neoforge': '21.1.256', 'fml': '4.0.45'}
    forge = [Path(locals_[r['id']]) for r in manifest['artifacts']
             if r['platform'] == 'forge' and r['activeServerResource']]
    assert not any('neoforged' in str(j) for j in forge)
    sources = [ROOT / 'tests/api_atlas/fixtures' / n for n in ['AtlasProbe.java', 'Peer.java', 'Untouched.java', 'VerifyFixture.java']]
    classes = a.output / 'fixture-classes'; classes.mkdir()
    execute([javac, '--release', '21', '-g', '-proc:none', '-cp', os.pathsep.join(map(str, forge)), '-d', classes, *sources], a.output / 'fixture-compile')
    fixture = a.output / 'forge-atlas-probe.jar'; package(fixture, classes)
    identity = {'source': {p.relative_to(ROOT).as_posix(): sha(p) for p in sources},
                'classes': {p.relative_to(classes).as_posix(): sha(p) for p in sorted(classes.rglob('*.class'))},
                'jar': {'sha256': sha(fixture), 'size': fixture.stat().st_size},
                'compileDependencies': [{'path': str(p), 'sha256': sha(p)} for p in forge],
                'purity': 'official Forge52.1.0 installation only; no NeoForge or DualForge compile inputs'}
    save(a.output / 'fixture-identity.json', identity)
    seeds = json.loads((ROOT / 'tools/api-atlas/mappings/seeds.json').read_text())['rules']
    config = {'schemaVersion': 1, 'approval': 'OFFLINE_STATIC_EXPERIMENT',
        'productionEnabled': False, 'runtimeVerified': False,
        'classes': [
            {'source': 'net/minecraftforge/registries/DeferredRegister', 'target': 'net/neoforged/neoforge/registries/DeferredRegister'},
            {'source': 'net/minecraftforge/registries/RegistryObject', 'target': 'net/neoforged/neoforge/registries/DeferredHolder'},
            {'source': 'net/minecraftforge/registries/IForgeRegistry', 'target': 'net/minecraft/core/Registry'},
            {'source': 'net/minecraftforge/fml/common/Mod', 'target': 'net/neoforged/fml/common/Mod'}],
        'signatures': [{'source': 'Lnet/minecraftforge/registries/RegistryObject<Lnet/minecraft/world/item/Item;>;',
                        'target': 'Lnet/neoforged/neoforge/registries/DeferredHolder<Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/Item;>;'}],
        'rules': [r for r in seeds if r['kind'] != 'CLASS' and r['strategy'] != 'RUNTIME_SHIM_REQUIRED']}
    rules = a.output / 'offline-rules.json'; save(rules, config)
    oracles = [a.inputs_directory / (h + '-oracle.tsv') for h in ['forge', 'neoforge']]
    execute([java, '-Xverify:all', '-cp', cp, 'OfflinePrototype', fixture, rules, *oracles,
             a.output / 'transformed-classes', a.output / 'verification.json'], a.output / 'prototype')
    verification = json.loads((a.output / 'verification.json').read_text())
    assert verification['status'] == 'OFFLINE_TRANSFORMATION_PASS', verification
    assert (a.output / 'transformed-classes/research/fixture/Untouched.class').read_bytes() == (classes / 'research/fixture/Untouched.class').read_bytes()
    contract_classes = a.output / 'contract-classes'; contract_classes.mkdir()
    execute([javac, '--release', '21', '-proc:none', '-cp', cp, '-d', contract_classes,
             ROOT / 'tests/api_atlas/PrototypeContract.java', ROOT / 'tests/api_atlas/ScannerContract.java'], a.output / 'contract-compile')
    execute([java, '-Xverify:all', '-cp', str(contract_classes) + os.pathsep + cp, 'PrototypeContract', fixture, rules, *oracles], a.output / 'contract')
    contract_text = (a.output / 'contract/stdout.txt').read_text()
    assert 'PROTOTYPE_CONTRACT_PASS' in contract_text
    execute([java, '-Xverify:all', '-cp', str(contract_classes) + os.pathsep + cp, 'ScannerContract'], a.output / 'scanner-contract')
    neo_paths = []
    for row in (a.inputs_directory / 'neoforge-oracle.tsv').read_text().splitlines():
        name, path, member = row.split('\t')
        if path.endswith('.jar') and path not in neo_paths:
            neo_paths.append(path)
    # Patched Minecraft precedes the unchanged base layer in this standalone verifier classpath.
    neo_paths.sort(key=lambda p: (not p.endswith('21.1.256-server.jar'), p))
    execute([java, '-Xverify:all', '-cp', str(a.output / 'transformed-classes') + os.pathsep + os.pathsep.join(neo_paths),
             'research.fixture.VerifyFixture'], a.output / 'jvm-verifier')
    assert 'OFFLINE_JVM_VERIFIER_PASS' in (a.output / 'jvm-verifier/stdout.txt').read_text()
    # Original release: inventory its blocking boundary, never define or execute it.
    assert sha(a.fnf) == 'bd415b04529c4ffc31e14757488f71dab6917e4560c61e236c3c4d0de6557958'
    execute([java, '-cp', cp, 'OfflinePrototype', a.fnf, rules, *oracles,
             a.output / 'fnf-output', a.output / 'fnf-rejected.json'], a.output / 'fnf-prototype')
    rejection = json.loads((a.output / 'fnf-rejected.json').read_text())
    assert rejection['status'] == 'REJECTED' and not (a.output / 'fnf-output').exists(), rejection
    # ABI and usage scanners: same input has byte-identical deterministic output.
    shapes = a.output / 'scanner-classes'; shapes.mkdir()
    execute([javac, '--release', '21', '-g', '-proc:none', '-d', shapes,
             ROOT / 'tests/api_atlas/fixtures/Shapes.java'], a.output / 'scanner-compile')
    shape_jar = a.output / 'scanner.jar'; package(shape_jar, shapes, False)
    tsv = a.output / 'scanner.tsv'; tsv.write_text('scanner/fixture\t' + str(shape_jar.resolve()) + '\n')
    outputs = []
    for number in [1, 2]:
        output = a.output / f'scan-{number}.jsonl.gz'; outputs.append(output)
        execute([java, '-cp', cp, 'Atlas', 'scan', tsv, output], a.output / f'scan-{number}')
    assert sha(outputs[0]) == sha(outputs[1])
    nodes = list(records(outputs[0])); shape = next(n for n in nodes if n['name'] == 'research/scan/Shapes')
    assert shape['permittedSubclasses'] and shape['nestMembers'] and shape['signature']
    assert any(f['value'] == {'floatBits': 'ff800000'} for f in shape['fields'])
    pair = next(n for n in nodes if n['name'].endswith('$Pair')); assert len(pair['recordComponents']) == 2
    execute([java, '-cp', cp, 'Atlas', 'usage', tsv, a.output / 'scanner-usage.jsonl.gz'], a.output / 'scanner-usage')
    usages = [r for n in records(a.output / 'scanner-usage.jsonl.gz') for r in n['references']]
    assert any(r['context'] == 'genericSignature' and r['owner'] == 'java/util/List' for r in usages)
    assert any(r['context'].endswith(':handle') for r in usages)
    assert any(r['kind'] == 'INVOKEDYNAMIC' for r in usages)
    assert identity['jar']['sha256'] == sha(fixture) and sha(a.fnf) == rejection['inputSHA256']
    result = {'status': 'PASS', 'fixture': identity['jar'], 'prototype': verification,
        'contract': contract_text.splitlines()[-1], 'deterministicScannerSHA256': sha(outputs[0]),
        'scannerContract': (a.output / 'scanner-contract/stdout.txt').read_text().strip(),
        'standaloneJVMVerification': 'PASS; fixture class loading/null receipt only, no game/registry lifecycle',
        'fnfOffline': rejection, 'registryChain': 'PARTIAL; Context/ModBus runtime bridge and full FnF Supplier/CreativeTab closure unimplemented',
        'fnfRuntime': 'FAIL inherited, not rerun', 'productionEnabled': False}
    save(a.output / 'result.json', result)
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
