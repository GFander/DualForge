"""Reject missing/mismatched current evidence; GUI states never derive from API/server PASS."""
import argparse
import importlib.util
import json
from pathlib import Path
from support import ENV, ROOT, record, save, sha

SPEC = importlib.util.spec_from_file_location('fnf_snbt', ROOT / 'tests/real_mods/food_n_fuel/behavior/snbt.py')
SNBT = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(SNBT)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for key in ['jar', 'regression', 'mouse-checks', 'original-model', 'output']:
        parser.add_argument('--' + key, type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        parser.error('Never overwrite previous assessments')
    read = lambda p: json.loads(p.read_text())
    checks = []

    def check(value, label):
        if not value:
            raise AssertionError(label)
        checks.append(label)

    runtime = {}
    phases = read(args.regression / 'runtime-invocations.json')
    check(len(phases) == 18, '18 fresh runtime phases listed')
    for invocation in phases:
        name = invocation['name']
        directory = args.regression / name
        result = read(directory / 'result.json')
        runtime[name] = result
        check(result['lifecycle'] == 'PASS' and result['exitCode'] == 0 and result['state'] == 'DONE_STOPPED', name + ' lifecycle')
        check(all(result['checks'].values()) and not result['severityLines'], name + ' actual process/input/log checks')
        launch = read(directory / 'invocation.json')
        check(not invocation['reusedResults'], name + ' no inherited result')
        source = ENV / ('forge-52.1.0' if launch['host'] == 'forge' else 'neoforge-21.1.256')
        installed = read(directory / 'installed-inputs.json')
        for row in installed:
            if not row['path'].startswith('mods/'):
                check(sha(source / row['path']) == row['sha256'], name + ' original/copy ' + row['path'])
        mods = {row['path']: row['sha256'] for row in installed if row['path'].startswith('mods/')}
        check((mods.get('mods/dualforge.jar') == sha(args.jar)) if launch['host'] != 'forge'
              else 'mods/dualforge.jar' not in mods, name + ' exact compat scope')
        check(not any('MouseTweaks' in p for p in mods), name + ' client-only Mouse not a server input')
        command = read(directory / 'command.json')
        check(command['argv'] == ['sh', 'run.sh', 'nogui'] and not command['injectedJvmOptions'], name + ' stock launch')
        check(read(directory / 'actual-java-command.json')[1:] == command['expectedJavaArguments'], name + ' actual JVM args')
    for name in ['forge-empty', 'forge-clumps', 'forge-observed', 'neo-clumps-control', 'neo-treatment-1',
                 'neo-observed', 'mixed-forge', 'mixed-neo']:
        orb = read(args.regression / name / 'orb-snapshots.json')
        expected = 3 if name in ['forge-empty', 'neo-clumps-control'] else 1
        check(orb['before']['entityCount'] == 3 and orb['before']['totalXP'] == 21, name + ' initial ordinary orbs')
        check(orb['after']['gameTime'] - orb['before']['gameTime'] == 400, name + ' 400 actual world ticks')
        check(orb['after']['entityCount'] == expected and orb['after']['totalXP'] == 21, name + ' count/XP')
        if expected == 1:
            check(orb['after']['entities'][0]['clumpedMap'] == {'3': 1, '7': 1, '11': 1}, name + ' merge map')
    control = read(args.regression / 'neo-clumps-control/installed-inputs.json')
    treatment = read(args.regression / 'neo-treatment-1/installed-inputs.json')
    left, right = ({row['path']: row['sha256'] for row in rows} for rows in [control, treatment])
    check(set(right) - set(left) == {'mods/Clumps-forge-1.21.1-19.0.0.1.jar'}
          and all(right.get(k) == v for k, v in left.items()), 'single-factor Clumps Neo cohort')
    check((args.regression / 'neo-clumps-control/initial-server.properties').read_bytes()
          == (args.regression / 'neo-treatment-1/initial-server.properties').read_bytes(), 'C/T identical initial properties')
    for name in ['forge-reload', 'neo-reload']:
        restored = read(args.regression / name / 'orb-reload.json')
        check(restored['sameUUIDValueCountMap'] and restored['totalPreserved'], name + ' real persisted orbs')
        check(runtime[name]['checks']['reloadSourceWorldUnchanged'], name + ' source world preserved')
    for name in ['forge-observed', 'forge-independent', 'neo-observed', 'neo-independent', 'mixed-forge', 'mixed-neo']:
        events = read(args.regression / name / 'server/event-observer.json')
        check(events['status'] == 'PASS' and events['assertions'] == 15, name + ' Clumps real event API')
    native = read(args.regression / 'fnf-native/server/fnf-behavior.json')
    check(native['registry']['globalBus'] != native['registry']['modBus'], 'native distinct global/mod bus observations')
    for name in ['fnf-neo-observed', 'mixed-forge', 'mixed-neo']:
        value = read(args.regression / name / 'server/fnf-behavior.json')
        for gate in ['registry', 'food', 'tab']:
            if gate == 'registry':
                check(value[gate]['globalBus'] != value[gate]['modBus'], name + ' distinct global/mod bus observations')
                # identityHashCode is meaningful only inside its own JVM. Preserve
                # every recorded identity, compare the complete remaining schema/data.
                observed = {k: v for k, v in value[gate].items() if k not in ['globalBus', 'modBus']}
                baseline = {k: v for k, v in native[gate].items() if k not in ['globalBus', 'modBus']}
                check(observed == baseline and value[gate]['status'] == 'PASS', name + ' FnF full registry semantics')
            else:
                check(value[gate] == native[gate] and value[gate]['status'] == 'PASS', name + ' FnF full ' + gate)
    check(len(native['registry']['ids']) == 67 and len(native['food']['foods']) == 58
          and len(native['tab']['display']) == 66, 'FnF 67 IDs/58 Food/66 Tab')
    furnaces = []
    for name in ['fnf-native', 'fnf-neo-standard', 'fnf-neo-observed', 'mixed-forge', 'mixed-neo']:
        snapshots = read(args.regression / name / 'furnace-snapshots.json')['snapshots']
        check(len(snapshots) == 72, name + ' 72 real furnace snapshots')
        furnaces.append([(row['case'], row['elapsed'], SNBT.furnace(row['response']), row['lit']) for row in snapshots])
    check(all(value == furnaces[0] for value in furnaces), 'native/Neo/observed/mixed full furnace equality')
    for name, marker in [('original-probe', 'register returned target=com.example.forgeprobe.ForgeProbe'),
                         ('registry-global-observed', '[DF4C3] PASS variant=combined handlers=1')]:
        text = (args.regression / name / 'stdout.log').read_text()
        check(marker in text and '[DF007-NATIVE] SERVER_STARTED' in text, name + ' actual fixed/native lifecycle')
    for directory in [args.regression / n for n in ['existing-contracts', 'profiles', 'clumps-contract']]:
        value = read(directory / 'result.json')
        check(value['status'] == 'PASS' and value['compatSHA256'] == sha(args.jar), directory.name + ' current compat contract')
    for directory in [args.mouse_checks / n for n in ['contract', 'api-forge', 'api-neo', 'links']]:
        value = read(directory / 'result.json')
        check(value['status'] == 'PASS' and value['compat']['sha256'] == sha(args.jar), directory.name + ' final Mouse scope')
    value = read(args.original_model / 'result.json')
    check(value['status'] == 'PASS' and value['compat']['sha256'] == sha(args.jar)
          and value['originalInitModelAssertions'] == 8, 'actual original init/config Screen model bound to final compat')
    final_states = {name: 'PASS' for name in ['MOUSE_RELEASE_IDENTITY', 'MOUSE_METADATA_ADMISSION', 'MOUSE_ENTRY_PROFILE',
                    'MOUSE_EVENT_API_ISOLATION', 'MOUSE_CONFIG_FACTORY_API', 'MOUSE_INITIALIZATION_MODEL',
                    'MOUSE_MEMBER_LINKS', 'MOUSE_MIXIN_TARGETS_STATIC', 'DUALFORGE_RELEVANT_REGRESSION',
                    'FNF_SERVER_REGRESSION', 'CLUMPS_SERVER_REGRESSION', 'FNF_CLUMPS_MIXED_SERVER']}
    final_states.update({name: 'NOT_RUN' for name in ['MOUSE_FML_CLIENT_CONSTRUCTION', 'MOUSE_MIXIN_APPLICATION',
                        'MOUSE_CLIENT_BOOT', 'MOUSE_GUI_BEHAVIOR', 'MOUSE_CONFIG_GUI', 'MOUSE_CLIENT_MANUAL',
                        'NEW_JAR_FNF_CLUMPS_CLIENT_REGRESSION', 'NEW_JAR_REAL_PLAYER_XP_MENDING_PERSISTENCE']})
    save(args.output, {'status': 'PARTIAL', 'measuredAutomatedScope': 'PASS', 'compat': record(args.jar),
         'successfulChecks': len(checks), 'checkCountMeaning': 'Includes individual identity/copy checks, not independent behaviors',
         'checks': checks, 'finalStates': final_states, 'runtimeResults': runtime,
         'limits': ['Mouse API tests use constructor-isolated fixture/Screen objects and diagnostic factory accessors.',
                    'Original initialization model injects a constructor-free Minecraft singleton, not live FML construction.',
                    'Mixin selectors and no-init JVM verification do not prove actual application or GUI behavior.',
                    'Clumps persistence covers fresh XP orbs only; real-player/new client gates are NOT_RUN.',
                    'Previous 009A/009B user PASS is historical and not inherited by this new JAR.']})
    print('ASSESSMENT PARTIAL; measured automated scope PASS; checks=' + str(len(checks)))


if __name__ == '__main__':
    main()
