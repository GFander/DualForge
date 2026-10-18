"""Link original Mouse Tweaks members/Mixin selectors against a complete official client SDK."""
import argparse
from pathlib import Path
import re
import zipfile
from support import JDK, fresh, record, run, save


def priority(path):
    suffix = path.name
    rank = 0 if suffix.endswith('-client.jar') else 1 if suffix.endswith('-universal.jar') else 2 if suffix.endswith('-srg.jar') else 3 if suffix.endswith('-slim.jar') else 9 if suffix.endswith('-server.jar') else 4
    return rank, str(path)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for key in ['jar', 'mouse', 'transformed', 'client-libraries', 'output']:
        parser.add_argument('--' + key, type=Path, required=True)
    args = parser.parse_args()
    output = fresh(args.output)
    classes = output / 'classes'
    classes.mkdir()
    deps = sorted(args.client_libraries.rglob('*.jar'), key=priority)
    providers = {}
    for path in [args.jar, *deps]:
        with zipfile.ZipFile(path) as jar:
            for entry in jar.namelist():
                if entry.endswith('.class') and not entry.startswith('META-INF/'):
                    providers.setdefault(entry[:-6], (str(path.resolve()), entry))
    oracle = output / 'host-providers.tsv'
    oracle.write_text(''.join(name + '\t' + '\t'.join(value) + '\n' for name, value in sorted(providers.items())))
    sources = [Path(__file__).with_name(name) for name in ['ClientLinkContract.java', 'OriginalInitContract.java']]
    save(output / 'inputs.json', [record(p) for p in [args.jar, args.mouse, *sources, Path(__file__), oracle, *deps, *args.transformed.rglob('*.class')]])
    cp = ':'.join(map(str, [args.jar, *deps]))
    run(output, 'javac', [JDK / 'bin/javac', '--release', '21', '-proc:none', '-cp', cp, '-d', classes, *sources])
    text = run(output, 'links', [JDK / 'bin/java', '-Xverify:all', '-cp', str(classes) + ':' + cp,
        'io.github.gfander.dualforge.ClientLinkContract', args.mouse, args.transformed, oracle])
    match = re.search(r'CLIENT LINK CONTRACT PASS members=(\d+) mixinMembers=(\d+) jvmVerifiedNoInit=(\d+)', text)
    assert match
    init = run(output, 'original-init-model', [JDK / 'bin/java', '-Xverify:all', '-cp', str(classes) + ':' + cp,
        'io.github.gfander.dualforge.OriginalInitContract', args.mouse, args.transformed, output / 'isolated-game'])
    init_count = re.search(r'ORIGINAL INIT MODEL PASS assertions=(\d+)', init)
    assert init_count
    result = {'status': 'PASS', 'members': int(match[1]), 'mixinMembers': int(match[2]),
              'verifiedWithoutInitialization': int(match[3]), 'originalInitModelAssertions': int(init_count[1]), 'compat': record(args.jar),
              'scope': 'STATIC_LINKS/JVM_NO_INIT plus explicitly injected constructor-free Minecraft initialization model; actual client/GUI/Mixin application NOT_RUN'}
    save(output / 'result.json', result)
    print(result)


if __name__ == '__main__':
    main()
