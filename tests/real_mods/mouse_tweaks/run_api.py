"""Exercise native event APIs without starting Minecraft or initializing a window."""
import argparse
from pathlib import Path
import re
from support import ENV, JDK, fresh, record, run, save


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--host', choices=['forge', 'neo'], required=True)
    for key in ['jar', 'fixture', 'client-libraries', 'output']:
        parser.add_argument('--' + key, type=Path, required=True)
    parser.add_argument('--transformed', type=Path)
    args = parser.parse_args()
    if args.host == 'neo' and not args.transformed:
        parser.error('Neo requires entry output from the current final-JAR contract')
    output = fresh(args.output)
    classes = output / 'classes'
    classes.mkdir()
    deps = sorted(args.client_libraries.rglob('*.jar'))
    sources = [Path(__file__).with_name('NativeApiHarness.java')]
    if args.host == 'neo':
        sources.append(Path(__file__).with_name('NeoBridgeContract.java'))
    inputs = [args.jar, args.fixture, Path(__file__), *sources, *deps]
    if args.transformed:
        inputs += sorted(args.transformed.rglob('*.class'))
    save(output / 'inputs.json', [record(p) for p in inputs])
    # Forge API baseline must not load DualForge's marker IEventBus shell.
    cp = ':'.join(map(str, ([args.jar] if args.host == 'neo' else []) + deps))
    run(output, 'javac', [JDK / 'bin/javac', '--release', '21', '-proc:none', '-cp', cp, '-d', classes, *sources])
    main_class = 'NeoBridgeContract' if args.host == 'neo' else 'NativeApiHarness'
    command = [JDK / 'bin/java', '-Xverify:all', '--add-opens', 'java.base/java.lang.invoke=ALL-UNNAMED',
               '--add-exports', 'java.base/sun.security.util=ALL-UNNAMED', '-cp', str(classes) + ':' + cp,
               'io.github.gfander.dualforge.' + main_class, args.fixture]
    if args.host == 'neo':
        command += [args.transformed, ENV / 'neoforge-21.1.256/libraries/net/neoforged/neoforge/21.1.256/neoforge-21.1.256-universal.jar']
    text = run(output, 'api', command)
    match = re.search(r'(?:SCREEN API|API CORE) PASS assertions=(\d+)', text)
    assert match
    result = {'status': 'PASS', 'host': args.host, 'assertions': int(match[1]), 'compat': record(args.jar),
              'scope': 'REAL_NATIVE_BUS_AND_EVENT_API_ISOLATION; constructor-free Screen and fixture; no GUI/client/FML construction PASS'}
    save(output / 'result.json', result)
    print(result)


if __name__ == '__main__':
    main()
