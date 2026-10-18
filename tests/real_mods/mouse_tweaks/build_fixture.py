"""Compile a separate identity against original Forge-only client ABI."""
import argparse
from pathlib import Path
import zipfile
from support import JDK, ROOT, fresh, libraries, record, run, save


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--client-libraries', type=Path, required=True)
    args = parser.parse_args()
    output = fresh(args.output)
    classes = output / 'classes'
    classes.mkdir()
    source = Path(__file__).with_name('fixture')
    inputs = sorted(source.rglob('*.java'))
    deps = sorted(args.client_libraries.rglob('*.jar'))
    if not deps:
        parser.error('A complete original Forge client library directory is required')
    save(output / 'inputs.json', [record(p) for p in [Path(__file__), *inputs, *deps]])
    run(output, 'javac', [JDK / 'bin/javac', '--release', '21', '-proc:none', '-cp', ':'.join(map(str, deps)), '-d', classes, *inputs])
    jar = output / 'gesture-input.jar'
    with zipfile.ZipFile(jar, 'w', zipfile.ZIP_DEFLATED) as archive:
        for file in sorted(classes.rglob('*.class')):
            archive.write(file, str(file.relative_to(classes)))
        archive.writestr('META-INF/mods.toml', 'modLoader="javafml"\nloaderVersion="[51,)"\nlicense="test-only"\nclientSideOnly=true\n[[mods]]\nmodId="gestureinput"\nversion="1"\ndisplayName="Independent Gesture Input"\n')
    save(output / 'result.json', {'status': 'PASS', 'jar': record(jar), 'scope': 'New Forge-only fixture; no third-party classes rebuilt'})
    print(jar)


if __name__ == '__main__':
    main()
