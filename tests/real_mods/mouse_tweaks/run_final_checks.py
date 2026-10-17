"""Serial current-artifact Mouse Tweaks contract/native API/link cohort."""
import argparse
from pathlib import Path
import sys
from support import ENV, fresh, run, save, record


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ['jar', 'run-root', 'fixture', 'output']:
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    output = fresh(args.output)
    tools = Path(__file__).parent
    mouse = ENV / 'inputs/MouseTweaks-forge-mc1.21-2.26.jar'
    contract = output / 'contract'
    run(output, 'contract-driver', [sys.executable, '-B', tools / 'run_contract.py', '--jar', args.jar,
        '--mouse', mouse, '--fixture', args.fixture, '--output', contract])
    for host in ['forge', 'neo']:
        command = [sys.executable, '-B', tools / 'run_api.py', '--host', host, '--jar', args.jar,
            '--fixture', args.fixture, '--client-libraries', args.run_root / ('client-abi/' + host + '/libraries'),
            '--output', output / ('api-' + host)]
        if host == 'neo':
            command += ['--transformed', contract / 'transformed/gestureinput']
        run(output, 'api-' + host + '-driver', command)
    run(output, 'links-driver', [sys.executable, '-B', tools / 'run_links.py', '--jar', args.jar,
        '--mouse', mouse, '--transformed', contract / 'transformed/mousetweaks',
        '--client-libraries', args.run_root / 'client-abi/neo/libraries', '--output', output / 'links'])
    save(output / 'result.json', {'status': 'PASS', 'compat': record(args.jar),
        'scope': 'Current-artifact contracts, FML planning model, native API isolation and static/no-init JVM links; four GUI gates NOT_RUN'})
    print('FINAL MOUSE AUTOMATED COHORT PASS', flush=True)


if __name__ == '__main__':
    main()
