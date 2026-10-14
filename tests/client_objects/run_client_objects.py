"""Run the existing strict contract and physical-copy C/T against canonical object IDs."""
import argparse, difflib, importlib.util, json, traceback
from pathlib import Path

SOURCE = Path(__file__).resolve().parent
REPO = SOURCE.parents[1]
CONFIG = {'items': ('multi_registry', 'multi-registry-probe', 'multi-registry-driver', 'MultiRegistryBytecodeTest.java',
                    {'"alpha_item"': '"ice_ether"', '"beta_item"': '"raw_ice_ether"', '"betaValue"': '"rawIceEtherValue"'}),
          'block': ('block_registry', 'block-registry-probe', 'block-registry-driver', 'BlockRegistryBytecodeTest.java',
                    {'"itemValue"': '"iceEtherBlockItemValue"'})}

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--role', choices=CONFIG, required=True)
    parser.add_argument('--stage', choices=('contract', 'control', 'treatment'), required=True)
    for name in ('fixture-result', 'jar', 'matrix', 'server-input', 'jdk-home', 'java-network-args'):
        parser.add_argument('--' + name, type=Path, required=name != 'java-network-args')
    parser.add_argument('--allow-server-start', action='store_true'); parser.add_argument('--public-key-relay', action='store_true')
    parser.add_argument('--max-wait-seconds', type=int, default=120); parser.add_argument('--post-done-seconds', type=int, default=10)
    args = parser.parse_args()
    for key, value in vars(args).items():
        if isinstance(value, Path): setattr(args, key, value.resolve())
    folder, fixture_name, driver_name, contract_name, names = CONFIG[args.role]
    spec = importlib.util.spec_from_file_location('original_' + folder, REPO / 'tests' / folder / ('run_' + folder + '.py'))
    module = importlib.util.module_from_spec(spec); spec.loader.exec_module(module)
    w = module.w
    run = w.run_directory(REPO / 'out/client-objects' / args.role / args.stage)
    result = {'status': 'FAIL', 'role': args.role, 'stage': args.stage, 'directory': str(run)}
    try:
        build = json.loads(args.fixture_result.read_text()); assert build['status'] == 'PASS' and build['role'] == args.role
        for key, name in [('fixture', fixture_name), ('driver', driver_name), ('native_control', 'native-control')]:
            info = build['jars'][name]; path = Path(info['Path']); assert w.digest(path) == info['SHA256']; setattr(args, key, path)
        args.registry_entries = [('ice_ether', 'ICE_ETHER'), ('raw_ice_ether', 'RAW_ICE_ETHER')]
        args.registry_id = 'blockregistryprobe:ice_ether_block'
        original = REPO / 'tests' / folder / contract_name
        text = original.read_text(); adapted = text
        for old, new in names.items():
            assert old in adapted; adapted = adapted.replace(old, new)
        contract_dir = run / 'contract-source'; contract_dir.mkdir()
        (contract_dir / contract_name).write_text(adapted)
        (run / 'contract-identity.diff').write_text(''.join(difflib.unified_diff(text.splitlines(True), adapted.splitlines(True), fromfile=str(original), tofile=contract_name)))
        w.save(run / 'contract-origin.json', {'original': module.r.fingerprint(original), 'adapted': module.r.fingerprint(contract_dir / contract_name), 'identityOnlyReplacements': names, 'assertionLogicUnchanged': True})
        module.SOURCE = contract_dir
        w.save(run / 'invocation.json', {k: str(v) if isinstance(v, Path) else v for k, v in vars(args).items()})
        binary = w.tools(args, run)
        result.update(module.contract(args, run, binary) if args.stage == 'contract' else module.dynamic(args, run, binary))
        if args.stage != 'contract':
            stdout = (run / 'logs/spike002-0l-treatment/spike002-0l-treatment.stdout.log').read_text()
            marker = '[DF006] LEGACY_IDS_ABSENT alpha_item beta_item' if args.role == 'items' else '[DF006B] LEGACY_IDS_ABSENT sample_block'
            # The query driver rejects old registry IDs, rather than merely changing display text.
            assert stdout.count(marker) == 1
            result['legacyRegistryIDsAbsent'] = True
    except Exception as error:
        result['status'] = 'FAIL'; result['error'] = str(error); (run / 'failure.txt').write_text(traceback.format_exc()); raise
    finally:
        w.save(run / 'result.json', result)
        index = REPO / 'out/client-objects' / args.role / 'stage-results.json'
        stages = json.loads(index.read_text()) if index.exists() else {}
        stages[args.stage] = str(run / 'result.json'); w.save(index, stages)
        print('RESULT', result['status'], run)

if __name__ == '__main__': main()
