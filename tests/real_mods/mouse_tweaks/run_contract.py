"""Validate the actual release, independent Forge fixture and refusal contracts."""
import argparse
from pathlib import Path
import re
from support import ENV, JDK, fresh, libraries, record, run, save


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for key in ['jar', 'mouse', 'fixture', 'output']:
        parser.add_argument('--' + key, type=Path, required=True)
    args = parser.parse_args()
    output = fresh(args.output)
    classes = output / 'classes'
    classes.mkdir()
    sources = [Path(__file__).with_name(name) for name in ['ClientProfileContract.java', 'ClientOwnershipContract.java']]
    deps = libraries('neo')
    save(output / 'inputs.json', [record(p) for p in [args.jar, args.mouse, args.fixture, *sources, Path(__file__), *deps]])
    cp = ':'.join(map(str, [args.jar, *deps]))
    run(output, 'javac', [JDK / 'bin/javac', '--release', '21', '-proc:none', '-cp', cp, '-d', classes, *sources])
    host = ENV / 'neoforge-21.1.256/libraries/net/neoforged/neoforge/21.1.256/neoforge-21.1.256-universal.jar'
    text = run(output, 'contract', [JDK / 'bin/java', '-Xverify:all', '-cp', str(classes) + ':' + cp,
        'io.github.gfander.dualforge.ClientProfileContract', args.mouse, args.fixture, host, output / 'transformed'])
    match = re.search(r'CLIENT PROFILE CONTRACT PASS assertions=(\d+) negatives=(\d+)', text)
    assert match
    ownership = {}
    for mode in ['client', 'server']:
        text = run(output, 'ownership-' + mode, [JDK / 'bin/java', '-Xverify:all',
            '--add-opens', 'java.base/java.lang.invoke=ALL-UNNAMED', '--add-exports', 'java.base/sun.security.util=ALL-UNNAMED',
            '-cp', str(classes) + ':' + cp, 'io.github.gfander.dualforge.ClientOwnershipContract',
            args.mouse, args.fixture, host, output / ('ownership-' + mode), mode])
        count = re.search(r'CLIENT OWNERSHIP CONTRACT PASS mode=' + mode + r' assertions=(\d+)', text)
        assert count
        ownership[mode] = int(count[1])
    result = {'status': 'PASS', 'scope': 'BYTECODE_AND_DIAGNOSTIC_TRANSFORM_CONTRACT; no client launch',
              'assertions': int(match[1]), 'negatives': int(match[2]), 'ownershipAssertions': ownership, 'compat': record(args.jar)}
    save(output / 'result.json', result)
    print(result)


if __name__ == '__main__':
    main()
