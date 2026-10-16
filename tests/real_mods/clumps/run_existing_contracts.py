"""Run unchanged metadata, platform, Registry/Global and FnF admission contracts on SSD.

Frozen fixture JARs must come from the SHA-verified public 4C-3 archive, never from
rebuilding its historical source. These are offline contracts, not game runs.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[3]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ['jar', 'archive-directory', 'neo-install', 'jdk-home', 'fnf', 'output']:
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=False)
    sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
    frozen = args.archive_directory / 'DualForge-007C4C3-Evidence'
    project = frozen / 'inputs/project-jars'
    fixtures = {}
    for name, prefix in [('forgeprobe.jar', 'a774d2f737b17f089046014ab55a53767dbad008cd44dcce52c727b87e559ae8'),
                         ('native-bootstrap.jar', '270c30aca9ddd42b625efe6e1eeefafa7865b5b4c4d5ae709a0623109cf5251c'),
                         ('helper-probe.jar', '0fd2e66dd33623ff58d8eca931ec9ae24b76aabbb9fa1eee5af191e1c1c0d610')]:
        original = project / (prefix + '-' + name)
        assert sha(original) == prefix
        target = args.output / name
        shutil.copyfile(original, target)
        fixtures[name] = target
    for name, directory in [('catalogbus.jar', 'combined'), ('quietbus.jar', 'quiet'), ('signalbus.jar', 'signal')]:
        original = frozen / ('raw/out/convergence-007c4c3/fixture/' + directory + '/' + name)
        target = args.output / name
        shutil.copyfile(original, target)
        fixtures[name] = target
    libraries = sorted((args.neo_install / 'libraries').rglob('*.jar'),
                       key=lambda p: (not p.name.endswith('-server.jar'), str(p)))
    providers = {}
    for jar in libraries:
        with zipfile.ZipFile(jar) as archive:
            for name in archive.namelist():
                if name.endswith('.class') and not name.startswith('META-INF/'):
                    providers.setdefault(name[:-6], (str(jar.resolve()), name))
    oracle = args.output / 'host-providers.tsv'
    oracle.write_text(''.join(name + '\t' + '\t'.join(provider) + '\n'
                             for name, provider in sorted(providers.items())))
    source_names = ['tests/dependencies/DependencyContractTest.java',
                    'tests/real_mods/food_n_fuel/dependency/PlatformDependencyContract.java',
                    'tests/helper_registry/HelperRegistryContract.java',
                    'tests/global_bus/GlobalBusContract.java',
                    'tests/global_bus/FnFGlobalAdmission.java',
                    'tests/supplier_boundary/PinnedHostOracle.java']
    sources = [ROOT / name for name in source_names]
    inputs = [Path(__file__), args.jar, args.fnf, oracle, *sources, *libraries, *fixtures.values()]
    records = [{'path': str(p.resolve()), 'bytes': p.stat().st_size, 'sha256': sha(p)} for p in inputs]
    (args.output / 'inputs.json').write_text(json.dumps(records, indent=2) + '\n')
    classes = args.output / 'classes'
    classes.mkdir()
    cp = os.pathsep.join(map(str, [args.jar, *libraries]))
    prefix = [str(args.jdk_home / 'bin/java'), '--add-opens', 'java.base/java.lang.invoke=ALL-UNNAMED',
              '--add-exports', 'java.base/sun.security.util=ALL-UNNAMED', '-cp', str(classes) + os.pathsep + cp]
    commands = [('javac', [str(args.jdk_home / 'bin/javac'), '--release', '21', '-proc:none',
                          '-cp', cp, '-d', str(classes), *map(str, sources)])]
    for name, main, params in [
        ('metadata', 'DependencyContractTest', ['after', ROOT / 'tests/dependencies', fixtures['forgeprobe.jar']]),
        ('platform', 'PlatformDependencyContract', [fixtures['forgeprobe.jar'], fixtures['native-bootstrap.jar'], args.fnf]),
        ('helper', 'HelperRegistryContract', [fixtures['helper-probe.jar'], args.output / 'helper-bytes']),
        ('global', 'GlobalBusContract', [fixtures['catalogbus.jar'], oracle, args.output / 'global-bytes', fixtures['quietbus.jar'], fixtures['signalbus.jar']]),
        ('fnf', 'FnFGlobalAdmission', [args.fnf, oracle, args.output / 'fnf-bytes']),
    ]:
        commands.append((name, [*prefix, 'io.github.gfander.dualforge.' + main, *map(str, params)]))
    result = {'status': 'FAIL', 'compatSHA256': sha(args.jar), 'phases': {}}
    try:
        for name, command in commands:
            (args.output / (name + '.command.json')).write_text(json.dumps(command, indent=2) + '\n')
            run = subprocess.run(command, cwd=ROOT, capture_output=True)
            (args.output / (name + '.stdout.log')).write_bytes(run.stdout)
            (args.output / (name + '.stderr.log')).write_bytes(run.stderr)
            result['phases'][name] = {'exitCode': run.returncode}
            assert run.returncode == 0, name + ' failed; see retained stderr'
        assert all(sha(Path(row['path'])) == row['sha256'] for row in records)
        result['status'] = 'PASS'
    finally:
        (args.output / 'result.json').write_text(json.dumps(result, indent=2) + '\n')
        print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
