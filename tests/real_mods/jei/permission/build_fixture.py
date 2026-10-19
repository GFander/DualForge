"""Compile two independent Forge-only identities; same JARs are used on both hosts."""
import argparse
import json
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'mouse_tweaks'))
from support import ENV, JDK, fresh, record, run, save


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--output', type=Path, required=True)
    a = p.parse_args()
    output = fresh(a.output)
    sdk = ENV / 'runs/convergence-009c-20261017/client-abi/forge/libraries'
    libraries = sorted(sdk.rglob('*.jar'))
    templates = sorted(Path(__file__).with_name('fixture').glob('*.java.in'))
    save(output / 'inputs.json', [record(path) for path in [*libraries, *templates, Path(__file__)]])
    artifacts = []
    for identity, package, config in [('gatealpha', 'org.example.gatealpha', 'SERVER'),
                                       ('gatebeta', 'org.example.gatebeta', 'SERVER')]:
        group = output / identity
        source, classes = group / 'src', group / 'classes'
        source.mkdir(parents=True)
        classes.mkdir()
        files = []
        for template in templates:
            target = source / template.name.removesuffix('.in')
            target.write_text(template.read_text().replace('@PACKAGE@', package)
                              .replace('@ID@', identity).replace('@CONFIGTYPE@', config)
                              .replace('@DEATH_REGISTER@', 'registerCanceled' if identity == 'gatebeta' else 'register'))
            files.append(target)
        run(group, 'javac', [JDK / 'bin/javac', '--release', '21', '-proc:none',
            '-cp', ':'.join(map(str, libraries)), '-d', classes, *files])
        metadata = classes / 'META-INF'
        metadata.mkdir()
        (metadata / 'mods.toml').write_text('modLoader="javafml"\nloaderVersion="[52,)"\n'
            'license="test-only"\n[[mods]]\nmodId="' + identity + '"\nversion="1"\n')
        jar = group / (identity + '.jar')
        run(group, 'jar', [JDK / 'bin/jar', '--create', '--file', jar, '-C', classes, '.'])
        artifacts.append({'identity': identity, 'configType': config, 'jar': record(jar)})
    save(output / 'result.json', {'status': 'PASS', 'scope': 'FORGE_ONLY_COMPILE', 'artifacts': artifacts})
    print(json.dumps(artifacts, indent=2))


if __name__ == '__main__':
    main()
