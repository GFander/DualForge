"""Serial final-JAR runtime cohort, reusing this round's unchanged native baselines."""
import argparse
import json
import os
from pathlib import Path
import subprocess
import sys


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run-root', type=Path, required=True)
    parser.add_argument('--jar', type=Path, required=True)
    args = parser.parse_args()
    environment = Path.home() / 'Env/DualForge'
    final = args.run_root / 'final'
    fixed = final / 'existing-contracts'
    native, probe = fixed / 'native-bootstrap.jar', fixed / 'forgeprobe.jar'
    clumps = environment / 'inputs/Clumps-forge-1.21.1-19.0.0.1.jar'
    fnf = environment / 'inputs/fnf-forge-1.3-1.21.x.jar'
    original = args.run_root / 'historical-inputs/DualForge-007C4C3-Evidence/raw/out/convergence-007c4c3/fixture-verified/neoforge/neoforge-global-observer.jar'
    fixture = args.run_root / 'fixture-build/independent-publisher.jar'
    observer = args.run_root / 'neo-observer-build/neoforge-event-observer.jar'
    fixture_observer = args.run_root / 'neo-fixture-observer-build/neoforge-event-observer.jar'
    fnf_observer = args.run_root / 'fnf-neo-observer-build/neoforge-behavior-observer.jar'
    stages = [
        ('neo-clumps-control', [args.jar], ['--orbs']),
        ('neo-treatment-1', [args.jar, clumps], ['--orbs']),
        ('neo-observed', [args.jar, clumps, observer], ['--orbs']),
        ('neo-independent', [args.jar, fixture, fixture_observer], []),
        ('neo-reload', [args.jar, clumps], ['--reload-from', str(final / 'neo-treatment-1')]),
        ('neo-control', [args.jar, native], ['--orbs']),
        ('fnf-neo-standard', [args.jar, native, fnf], ['--fnf-furnace']),
        ('fnf-neo-observed', [args.jar, native, fnf, fnf_observer], ['--fnf-furnace']),
        ('original-probe', [args.jar, probe, native], []),
        ('registry-global-observed', [args.jar, fixed / 'catalogbus.jar', native, original], []),
    ]
    invocations = []
    for name, mods, options in stages:
        cmd = [sys.executable, '-B', str(Path(__file__).with_name('run_server.py')), '--host', 'neoforge',
               '--install', str(environment / 'neoforge-21.1.256'),
               '--jdk-home', str(environment / 'jdks/msjdk-21.0.10'), '--output', str(final / name)]
        for mod in mods:
            cmd += ['--mod', str(mod)]
        cmd += options
        env = os.environ.copy()
        fixture_env = {'DF4C3_VARIANT': 'combined'} if name == 'registry-global-observed' else {}
        env.update(fixture_env)
        invocations.append({'phase': name, 'argv': cmd, 'fixtureEnvironment': fixture_env})
        (final / 'runtime-invocations.json').write_text(json.dumps(invocations, indent=2) + '\n')
        result = subprocess.run(cmd, env=env)
        if result.returncode:
            raise SystemExit(result.returncode)
        print('FINAL RUNTIME PASS ' + name, flush=True)


if __name__ == '__main__':
    main()
