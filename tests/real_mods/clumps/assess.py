"""Fail on any missing or mismatched current-run evidence; never promote lifecycle to functionality."""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
SPEC = importlib.util.spec_from_file_location('snbt', ROOT / 'tests/real_mods/food_n_fuel/behavior/snbt.py')
SNBT = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(SNBT)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run-root', type=Path, required=True)
    parser.add_argument('--jar', type=Path, required=True)
    parser.add_argument('--final-cohort', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        parser.error('Never overwrite previous assessments')
    sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
    read = lambda path: json.loads(path.read_text())
    checks = []
    def phase(name):
        if args.final_cohort and (args.final_cohort / name).exists():
            return args.final_cohort / name
        return args.run_root / name

    def check(value, label):
        if not value:
            raise AssertionError(label)
        checks.append(label)

    results = {}
    directories = sorted(args.run_root.iterdir())
    if args.final_cohort:
        directories += sorted(args.final_cohort.iterdir())
    for directory in directories:
        if not (directory / 'installed-inputs.json').exists():
            continue
        results[directory.name] = read(directory / 'result.json')
        results[directory.name]['sourcePath'] = str(directory)
        check(results[directory.name]['checks']['inputsUnchanged'], directory.name + ' installed inputs unchanged')
        command = read(directory / 'command.json')
        host = 'forge-52.1.0' if '/minecraftforge/' in command['expectedJavaArguments'][1] else 'neoforge-21.1.256'
        installation = Path.home() / 'Env/DualForge' / host
        installed = read(directory / 'installed-inputs.json')
        if args.final_cohort and directory.parent == args.final_cohort:
            check(any(row['path'] == 'mods/dualforge.jar' and row['sha256'] == sha(args.jar)
                      for row in installed), directory.name + ' installed final compat SHA')
        for row in installed:
            if row['path'].startswith('mods/'):
                continue
            check(sha(installation / row['path']) == row['sha256'], directory.name + ' source/copy match ' + row['path'])
    expected_failures = {'neo-before', 'registry-global-runtime'}
    check(expected_failures.issubset(results), 'retained actual failures present')
    for name, result in results.items():
        check(result['lifecycle'] == ('FAIL' if name in expected_failures else 'PASS'), name + ' exact lifecycle expectation')
    for name in ['forge-empty', 'forge-clumps', 'forge-observed', 'neo-clumps-control', 'neo-treatment-1', 'neo-observed']:
        snapshot = read(phase(name) / 'orb-snapshots.json')
        check(snapshot['before']['entityCount'] == 3 and snapshot['before']['totalXP'] == 21, name + ' actual initial orbs')
        check(snapshot['after']['gameTime'] - snapshot['before']['gameTime'] == 400, name + ' exact world ticks')
        clumps = name not in ['forge-empty', 'neo-clumps-control']
        check(snapshot['after']['entityCount'] == (1 if clumps else 3) and snapshot['after']['totalXP'] == 21,
              name + ' actual count/XP')
        if clumps:
            check(snapshot['after']['entities'][0]['clumpedMap'] == {'3': 1, '7': 1, '11': 1}, name + ' actual clumpedMap')
    # Neo Control/Treatment differ only by the original Clumps release.
    control = {row['path']: row['sha256'] for row in read(phase('neo-clumps-control') / 'installed-inputs.json')}
    treatment = {row['path']: row['sha256'] for row in read(phase('neo-treatment-1') / 'installed-inputs.json')}
    added = set(treatment) - set(control)
    check(len(added) == 1 and next(iter(added)).endswith('/Clumps-forge-1.21.1-19.0.0.1.jar')
          and all(treatment.get(path) == digest for path, digest in control.items()), 'single-factor Neo cohort')
    check((phase('neo-clumps-control') / 'initial-server.properties').read_bytes()
          == (phase('neo-treatment-1') / 'initial-server.properties').read_bytes(), 'Clumps C/T identical initial properties')
    check(read(phase('neo-clumps-control') / 'actual-java-command.json')
          == read(phase('neo-treatment-1') / 'actual-java-command.json'), 'Clumps C/T identical actual Java command')
    for name in ['forge-reload', 'neo-reload']:
        recovered = read(phase(name) / 'orb-reload.json')
        check(recovered['sameUUIDValueCountMap'] and recovered['totalPreserved'], name + ' actual persisted identity/value/map')
        check(results[name]['checks']['reloadSourceWorldUnchanged'], name + ' source world preserved')
    for name in ['forge-observed', 'neo-observed', 'forge-independent', 'neo-independent']:
        observed = read(phase(name) / 'server/event-observer.json')
        check(observed['status'] == 'PASS' and observed['assertions'] == 15, name + ' native event API observations')
    native = read(phase('fnf-native') / 'server/fnf-behavior.json')
    compat = read(phase('fnf-neo-observed') / 'server/fnf-behavior.json')
    for gate in ['registry', 'food', 'tab']:
        check(native[gate]['status'] == compat[gate]['status'] == 'PASS', 'FnF ' + gate + ' queries')
    check(native['registry']['ids'] == compat['registry']['ids'] and len(native['registry']['ids']) == 67
          and native['registry']['holders'] == compat['registry']['holders'], 'FnF 67 actual IDs/Holder identities')
    check(native['food'] == compat['food'] and len(native['food']['foods']) == 58, 'FnF all 58 Food records')
    check(native['tab'] == compat['tab'] and len(native['tab']['display']) == 66, 'FnF full Tab/icon/display/search')
    furnaces = []
    for name in ['fnf-native', 'fnf-neo-standard', 'fnf-neo-observed']:
        snapshots = read(phase(name) / 'furnace-snapshots.json')['snapshots']
        check(len(snapshots) == 72, name + ' 72 actual furnace states')
        furnaces.append([(x['case'], x['elapsed'], SNBT.furnace(x['response']), x['lit']) for x in snapshots])
    check(furnaces[0] == furnaces[1] == furnaces[2], 'FnF native/standard/observed actual fuel states identical')
    for name, marker in [('original-probe', 'register returned target=com.example.forgeprobe.ForgeProbe'),
                         ('registry-global-observed', '[DF4C3] PASS variant=combined handlers=1')]:
        text = (phase(name) / 'stdout.log').read_text()
        check(marker in text and '[DF007-NATIVE] SERVER_STARTED' in text, name + ' actual legacy/native runtime receipt')
    for name in ['existing-contracts', 'profile-regression', 'contract-final']:
        check(read(phase(name) / 'result.json')['status'] == 'PASS', name + ' current contract status')
        check(read(phase(name) / 'result.json')['compatSHA256'] == sha(args.jar), name + ' final compat identity')
    for name in ['forge', 'neo']:
        for row in read(args.run_root / ('audit/' + name + '-libraries.json')):
            check(sha(Path(row['path'])) == row['sha256'], 'original host JAR preserved ' + row['path'])
    # Bind all tested production bytes, even when Git HEAD still denotes the clean base.
    build_inputs = read(args.jar.parent / 'build-inputs.json')
    production = []
    for row in build_inputs:
        path = Path(row['Path'])
        if path.is_relative_to(ROOT / 'dualforge/src'):
            check(sha(path) == row['SHA256'], 'final source matches tested build ' + str(path.relative_to(ROOT)))
            production.append({'path': str(path.relative_to(ROOT)), 'bytes': path.stat().st_size, 'sha256': row['SHA256']})
    production.sort(key=lambda row: row['path'])
    status = {name: 'PASS' for name in ['CLUMPS_NATIVE_FORGE_BOOT', 'CLUMPS_NEOFORGE_DISCOVERY',
              'CLUMPS_ENTRY_PROFILE', 'CLUMPS_MOD_CONSTRUCTION', 'CLUMPS_EVENT_BRIDGE', 'CLUMPS_SERVICELOADER',
              'CLUMPS_MIXIN_APPLICATION', 'CLUMPS_NEOFORGE_SERVER_BOOT', 'CLUMPS_XP_ORB_MERGE',
              'CLUMPS_PERSISTENCE', 'DUALFORGE_REGRESSION', 'FNF_REGRESSION']}
    status.update({name: 'NOT_RUN' for name in ['CLUMPS_PLAYER_XP_PICKUP', 'CLUMPS_MENDING',
                                              'CLUMPS_CLIENT_BOOT', 'CLUMPS_CLIENT_MANUAL']})
    result = {'status': 'PARTIAL', 'measuredServerScope': 'PASS', 'finalStates': status,
              'compat': {'path': str(args.jar), 'bytes': args.jar.stat().st_size, 'sha256': sha(args.jar)},
              'production': production, 'productionFingerprintAlgorithm': 'SHA256 canonical sorted JSON of production records',
              'productionFingerprint': hashlib.sha256(json.dumps(production, sort_keys=True, separators=(',', ':')).encode()).hexdigest(),
              'successfulChecks': len(checks), 'checks': checks, 'runtimeResults': results,
              'limits': ['Event API tests use synthetic null inputs; no real player XP/Mending claim.',
                         'Persistence PASS covers this round\'s real orbs only, not player data.',
                         'Relevant original contracts/runtimes rerun; full historical game matrix NOT_RUN.',
                         'New client JAR never inherits historical FnF/Clumps client acceptance.']}
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps({'status': result['status'], 'successfulChecks': len(checks), 'finalStates': status}, indent=2))


if __name__ == '__main__':
    main()
