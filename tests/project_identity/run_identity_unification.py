"""Verify a newly built DualForge JAR using unchanged historical Forge fixtures."""
import argparse
import importlib.util
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import zipfile

REPO = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('registry', REPO / 'tests/registry/run_registry.py')
r = importlib.util.module_from_spec(spec)
spec.loader.exec_module(r)
w = r.w
ROOT = REPO / 'out/identity-unification'


def static(args, run, binary):
    with zipfile.ZipFile(args.jar) as archive:
        names = archive.namelist()
        manifest = archive.read('META-INF/MANIFEST.MF')
        providers = {name: archive.read(name).decode().strip() for name in names if name.startswith('META-INF/services/') and not name.endswith('/')}
        checks = {'manifest': b'Automatic-Module-Name: dualforge' in manifest and b'FMLModType: LIBRARY' in manifest,
                  'four original SPI interfaces': set(providers) == {'META-INF/services/' + p.name for p in (REPO / 'dualforge/src/main/resources/META-INF/services').iterdir()},
                  'all providers packaged': all(v.replace('.', '/') + '.class' in names for v in providers.values()),
                  'provider package': all(v.startswith('io.github.gfander.dualforge.') for v in providers.values()),
                  'old identity absent': all(b'forgecompat' not in archive.read(n).lower() for n in names if not n.endswith('/')),
                  'new classes only': all(n.startswith(('io/github/gfander/dualforge/', 'net/minecraftforge/eventbus/api/')) for n in names if n.endswith('.class'))}
        transformer = archive.read('io/github/gfander/dualforge/DualForgeEntryTransformer.class')
        checks['all six property names'] = all(('dualforge.' + key).encode() in transformer for key in ('2a.rewrite', '0g.apiRewrite', '0j.emptyRegistration', '0l.subscriberRewrite', 'dump.path', 'dump.profileDir'))
    w.native(run, 'describe-module', [binary / 'jar', '--describe-module', '--file', args.jar])
    w.native(run, 'jar-tf', [binary / 'jar', 'tf', args.jar])
    for name in ('DualForgeTransformationService', 'DualForgeLanguageLoader', 'DualForgeEntryTransformer'):
        w.native(run, 'javap-' + name, [binary / 'javap', '-v', '-p', '-c', '-classpath', args.jar, 'io.github.gfander.dualforge.' + name])
    checks['JPMS automatic dualforge'] = 'dualforge automatic' in (run / 'describe-module.stdout.log').read_text()
    text = (run / 'javap-DualForgeLanguageLoader.stdout.log').read_text()
    checks['javafml and 9999 unchanged'] = 'javafml' in text and '9999' in text
    for p in (REPO / 'dualforge/src/main/java').rglob('*.java'):
        package = re.search(r'^package (.*);', p.read_text(), re.M).group(1)
        checks['package path ' + p.name] = p.relative_to(REPO / 'dualforge/src/main/java').parent.as_posix() == package.replace('.', '/')
    w.save(run / 'SPI.json', providers)
    w.save(run / 'assertions.json', checks)
    assert all(checks.values()), 'Static identity gate failed'
    return {'status': 'PASS', 'compat': r.fingerprint(args.jar), 'assertions': checks}


def loading(args, run, binary):
    source = Path(__file__).with_name('ModuleLayersAgent.java')
    classes = run / 'agent-classes'
    classes.mkdir()
    w.native(run, 'agent-javac', [binary / 'javac', '--release', '21', '-d', classes, source])
    manifest = run / 'agent-manifest.mf'
    manifest.write_text('Manifest-Version: 1.0\nPremain-Class: io.github.gfander.dualforge.test.ModuleLayersAgent\n\n')
    agent = run / 'layer-observer.jar'
    w.native(run, 'agent-jar', [binary / 'jar', '--create', '--file', agent, '--manifest', manifest, '-C', classes, '.'])
    w.server_preflight(args, args.jar)
    meta, logs, server = w.harness(args, run, binary, args.probe.resolve(), args.jar.resolve(),
                                   extra_jvm_args=(f'-javaagent:{agent}={run / "module-layers.json"}',))
    checks = w.assertions(run, meta, logs, server, args.probe.resolve(), args.jar.resolve())
    layers = json.loads((run / 'module-layers.json').read_text())
    checks['actual official layers observed'] = layers['status'] == 'PASS' and set(layers['layers']) == {'BOOT', 'SERVICE', 'PLUGIN', 'GAME'}
    checks['new module in SERVICE'] = layers['layers']['SERVICE'].count('dualforge') == 1
    checks['all loaded providers are dualforge'] = set(layers['loadedProviders'].values()) == {'dualforge'}
    stdout = (logs / 'spike002-0l-treatment.stdout.log').read_text()
    checks['new service initialize and call'] = '[DUALFORGE-XFORM] initialize() called, name=dualforge-transform' in stdout and '[DUALFORGE-XFORM] transformers() called' in stdout
    checks['LanguageLoader enumerated and called'] = '[DUALFORGE-LANG] INSTANTIATED DualForgeLanguageLoader' in stdout and '[DUALFORGE-LANG] loadMod called for forgeprobe' in stdout
    w.save(run / 'assertions.json', checks)
    assert all(checks.values()), 'Actual module/loading gate failed'
    return {'status': 'PASS', 'compat': r.fingerprint(args.jar), 'meta': meta, 'assertions': checks, 'moduleLayers': layers}


def invoke(args, run, name, command):
    w.native(run, name, command)
    match = re.search(r'RESULT PASS(?: exit=0)? (.+)', (run / (name + '.stdout.log')).read_text())
    assert match, 'No observed PASS for ' + name
    path = Path(match.group(1)) / 'result.json'
    result = json.loads(path.read_text())
    assert result['status'] == 'PASS'
    if 'compat' in result:
        assert result['compat']['SHA256'] == w.digest(args.jar)
    return str(path)


def main():
    p = argparse.ArgumentParser(description=__doc__)
    for key in ('jar', 'fixture', 'driver', 'native-control', 'identity-fixture', 'event-fixture', 'event-driver', 'event-rejected', 'forge-dependencies', 'server-input', 'probe', 'jdk-home', 'java-network-args'):
        p.add_argument('--' + key, type=Path, required=key != 'java-network-args')
    p.add_argument('--public-key-relay', action='store_true')
    p.add_argument('--allow-server-start', action='store_true')
    p.add_argument('--max-wait-seconds', type=int, default=120)
    p.add_argument('--post-done-seconds', type=int, default=10)
    args = p.parse_args()
    assert args.jar.name == 'dualforge.jar'
    assert w.digest(args.fixture) == '312a4f697c1dc617ed48822c10179384b6559f4e6399db9210c15541c774132c'
    run = w.run_directory(ROOT / 'verification')
    inputs = w.fingerprints([p for p in vars(args).values() if isinstance(p, Path) and p.is_file()])
    w.save(run / 'inputs.json', inputs)
    w.save(run / 'invocation.json', {k: str(v) if isinstance(v, Path) else v for k,v in vars(args).items()})
    report = {'status': 'FAIL', 'testedCommit': subprocess.check_output(['git','rev-parse','HEAD'],cwd=REPO,text=True).strip(),
              'compat': r.fingerprint(args.jar), 'fixture': r.fingerprint(args.fixture), 'results': {}, 'clientAcceptance': 'NOT RUN'}
    def record(name, result):
        path = run / (name + '-result.json')
        w.save(path, result)
        report['results'][name] = str(path)
        w.save(ROOT / 'progress.json', {'finished': list(report['results']), 'status': 'RUNNING', 'run': str(run)})
        print('PASS', name, flush=True)
    try:
        binary = w.tools(args, run)
        record('static', static(args, run, binary))
        load = run / 'loading'; load.mkdir()
        record('loading', loading(args, load, binary))
        base = ['zsh', REPO / 'tests/registry/run-registry.zsh']
        common = ['--server-input', args.server_input, '--jar', args.jar, '--jdk-home', args.jdk_home,
                  '--fixture', args.fixture, '--driver', args.driver, '--native-control', args.native_control]
        network = (['--java-network-args', args.java_network_args] if args.java_network_args else []) + (['--public-key-relay'] if args.public_key_relay else [])
        for stage in ('contract', 'control', 'treatment', 'assets', 'regression'):
            extra = []
            if stage in ('control', 'treatment', 'regression'):
                extra = ['--allow-server-start', *network]
            if stage == 'regression':
                extra += ['--identity-fixture', args.identity_fixture, '--event-fixture', args.event_fixture,
                          '--event-driver', args.event_driver, '--event-rejected', args.event_rejected,
                          '--forge-dependencies', args.forge_dependencies, '--probe', args.probe]
            assert stage not in ('control','treatment','regression') or args.allow_server_start
            name = 'registry-' + stage if stage in ('contract','control','treatment') else stage
            report['results'][name] = invoke(args, run, name, [*base, '--stage', stage, *common, *extra])
            w.save(ROOT / 'progress.json', {'finished': list(report['results']), 'status':'RUNNING','run':str(run)})
            print('PASS', name, flush=True)
        # Preserve all existing registry cohort assertions, using the explicitly retained inputs.
        results = {s: json.loads(Path(report['results']['registry-' + s if s in ('contract','control','treatment') else s]).read_text()) for s in ('contract','control','treatment','regression','assets')}
        results['fixture'] = {'jars': {'registry-probe': r.fingerprint(args.fixture), 'registry-driver': r.fingerprint(args.driver)}}
        assert r.cohort_consistent(results), 'New JAR C/T and regression cohort mismatch'
        w.unchanged(inputs)
        report.update(status='PASS', sameArtifactCohort=True)
    finally:
        w.save(run / 'result.json', report)
        w.save(ROOT / 'validation.json', report)
        w.save(ROOT / 'progress.json', {'finished': list(report['results']), 'status':report['status'],'run':str(run)})
    print('RESULT', report['status'], run, flush=True)


if __name__ == '__main__':
    main()
