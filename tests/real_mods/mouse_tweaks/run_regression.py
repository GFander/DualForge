"""Run unchanged relevant contracts and fresh native/Neo game cohorts, always serially.

The earlier 008A directory supplies only SHA-checked immutable measurement JARs
and launch-input descriptions. No result, log or world is inherited as PASS.
"""
import argparse
import os
from pathlib import Path
import sys
from support import ENV, JDK, ROOT, fresh, record, run, save, sha
import json


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jar', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--inputs-008a', type=Path, required=True)
    args = parser.parse_args()
    output = fresh(args.output)
    prior = args.inputs_008a
    archive = prior / 'historical-inputs'
    assert sha(archive / 'DualForge-007C4C3-Evidence.zip') == '6076da52a82b93640c5ab9c657e96b06fc77f1ad5699aba0e8a460589fa4c154'
    fnf = ENV / 'inputs/fnf-forge-1.3-1.21.x.jar'
    clumps = ENV / 'inputs/Clumps-forge-1.21.1-19.0.0.1.jar'
    neo = ENV / 'neoforge-21.1.256'
    forge = ENV / 'forge-52.1.0'
    fixed = output / 'existing-contracts'
    wrapper = Path(__file__).with_name('run_checked_tool.py')
    run(output, 'existing-contracts', [sys.executable, '-B', wrapper, '--script', ROOT / 'tests/real_mods/clumps/run_existing_contracts.py', '--',
        '--jar', args.jar, '--archive-directory', archive, '--neo-install', neo, '--jdk-home', JDK, '--fnf', fnf, '--output', fixed])
    run(output, 'profiles', [sys.executable, '-B', wrapper, '--script', ROOT / 'tests/real_mods/clumps/run_profile_regression.py', '--',
        '--jar', args.jar, '--forge-install', forge, '--neo-install', neo, '--jdk-home', JDK,
        '--fixed-inputs', fixed, '--fnf', fnf, '--output', output / 'profiles'])
    run(output, 'clumps-contract', [sys.executable, '-B', wrapper, '--script', ROOT / 'tests/real_mods/clumps/run_contract.py', '--',
        '--jar', args.jar, '--clumps', clumps, '--fixture', prior / 'fixture-build/independent-publisher.jar',
        '--neo-install', neo, '--jdk-home', JDK, '--output', output / 'clumps-contract'])
    phases = ['forge-empty', 'forge-clumps', 'forge-observed', 'forge-independent', 'forge-reload', 'fnf-native',
              'neo-clumps-control', 'neo-treatment-1', 'neo-observed', 'neo-independent', 'neo-reload',
              'neo-control', 'fnf-neo-standard', 'fnf-neo-observed', 'original-probe', 'registry-global-observed']
    invocations, inputs = [], []
    for name in phases:
        old = prior / 'final' / name
        launch = json.loads((old / 'invocation.json').read_text())
        recorded = {r['path']: r['sha256'] for r in json.loads((old / 'source-inputs.json').read_text())}
        mods = []
        for value in launch['mod']:
            path = Path(value)
            assert sha(path) == recorded[str(path)]
            mods.append(args.jar if path.name == 'dualforge.jar' else path)
        if name in ['original-probe', 'registry-global-observed', 'neo-control', 'fnf-neo-standard', 'fnf-neo-observed']:
            # These exact frozen copies are also checked by this round's contract stage.
            mods = [fixed / p.name if (fixed / p.name).is_file() else p for p in mods]
        cmd = [sys.executable, '-B', ROOT / 'tests/real_mods/clumps/run_server.py', '--host', launch['host'],
               '--install', launch['install'], '--jdk-home', JDK, '--output', output / name]
        for mod in mods:
            cmd += ['--mod', mod]
            inputs.append(record(mod))
        if launch['orbs']:
            cmd.append('--orbs')
        if launch['reload_from']:
            cmd += ['--reload-from', output / Path(launch['reload_from']).name]
        if launch['fnf_furnace']:
            cmd.append('--fnf-furnace')
        environment = os.environ.copy()
        fixture_env = {'DF4C3_VARIANT': 'combined'} if name == 'registry-global-observed' else {}
        environment.update(fixture_env)
        invocations.append({'name': name, 'argv': list(map(str, cmd)), 'fixtureEnvironment': fixture_env,
                            'launchInputDescription': record(old / 'invocation.json'), 'reusedResults': False})
        save(output / 'runtime-invocations.json', invocations)
        print('START ' + name, flush=True)
        # Preserve stock game's stdout/stderr in its own fresh directory; this
        # outer process has a separate, fully captured command and exit receipt.
        from subprocess import run as execute
        result = execute(list(map(str, cmd)), cwd=output, env=environment, capture_output=True)
        (output / (name + '.driver.stdout.log')).write_bytes(result.stdout)
        (output / (name + '.driver.stderr.log')).write_bytes(result.stderr)
        save(output / (name + '.driver.exit.json'), {'exitCode': result.returncode})
        if result.returncode:
            raise RuntimeError(name + ' failed; retained all evidence')
        print('PASS ' + name, flush=True)
    for host in ['forge', 'neoforge']:
        label = 'mixed-' + ('forge' if host == 'forge' else 'neo')
        baseline_name = 'fnf-native' if host == 'forge' else 'fnf-neo-observed'
        original = json.loads((prior / 'final' / baseline_name / 'invocation.json').read_text())
        mods = [args.jar if Path(p).name == 'dualforge.jar' else Path(p) for p in original['mod']]
        observed_name = 'forge-observed' if host == 'forge' else 'neo-observed'
        observed = json.loads((prior / 'final' / observed_name / 'invocation.json').read_text())
        for p in observed['mod']:
            if Path(p).name != 'dualforge.jar' and Path(p) not in mods:
                mods.append(Path(p))
        cmd = [sys.executable, '-B', Path(__file__).with_name('run_mixed.py'), '--host', host,
               '--install', forge if host == 'forge' else neo, '--jdk-home', JDK, '--output', output / label, '--orbs']
        for mod in mods:
            cmd += ['--mod', mod]
            inputs.append(record(mod))
        invocations.append({'name': label, 'argv': list(map(str, cmd)), 'fixtureEnvironment': {}, 'reusedResults': False})
        save(output / 'runtime-invocations.json', invocations)
        print('START ' + label, flush=True)
        run(output, label + '-driver', cmd)
        print('PASS ' + label, flush=True)
    save(output / 'reused-inputs.json', inputs)
    save(output / 'result.json', {'status': 'PASS', 'compat': record(args.jar), 'runtimeGroups': len(invocations),
         'scope': 'Listed current-JAR contracts and 18 fresh game groups; full historical client/game matrix NOT_RUN'})
    print('RELEVANT REGRESSION PASS groups=' + str(len(invocations)), flush=True)


if __name__ == '__main__':
    main()
