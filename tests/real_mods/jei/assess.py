"""Assess measured JEI failures separately from successful evidence/regression checks."""
import argparse
import json
from pathlib import Path
import sys
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / 'mouse_tweaks'))
from support import ENV, ROOT, record, save, sha


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ['run', 'output']:
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    if not args.output.resolve().is_relative_to(ENV / 'runs'):
        parser.error('Assessment output must stay on SSD')
    if args.output.exists():
        parser.error('Never overwrite an assessment')
    read = lambda path: json.loads((args.run / path).read_text())
    checks = []

    def check(value, label):
        if not value:
            raise AssertionError(label)
        checks.append(label)

    original = args.run / 'inputs/jei-1.21.1-forge-19.27.0.340.jar'
    reference = args.run / 'inputs/jei-1.21.1-neoforge-19.27.0.340.jar'
    jar = args.run / 'build-baseline/dualforge.jar'
    check(sha(original) == 'f5e61a001d57dd044d6f2ba255daf955c50d90e4173f8b4933ab0eb8cd386ed5', 'frozen original release')
    check(sha(reference) == '8aaf547432f1b4958239b036356b910692fe40f858c3073d996f56bbf7c99826', 'frozen reference only')
    for name, count in [('native-forge', 1), ('native-observed', 2), ('neo-baseline', 2)]:
        command = read(name + '/command.json')
        check(command['argv'] == ['sh', 'run.sh', 'nogui'] and not command['injectedJvmOptions'], name + ' stock launch')
        check(read(name + '/actual-java-command.json')[1:] == command['expectedJavaArguments'], name + ' actual JVM command')
        mods = [m for m in read(name + '/installed-inputs.json') if m['path'].startswith('mods/')]
        check(len(mods) == count and all('neoforge-19.27.0.340' not in m['path'] for m in mods), name + ' inputs isolated')
        for item in mods:
            check(sha(args.run / name / 'server' / item['path']) == item['sha256'], name + ' untouched ' + item['path'])
        check(any(m['sha256'] == sha(original) for m in mods), name + ' same original Forge JEI')
        if name == 'neo-baseline':
            check(any(m['sha256'] == sha(jar) for m in mods), 'Neo exact baseline artifact')
            state = read(name + '/result.json')
            check(state['state'] == 'EXITED_BEFORE_DONE' and state['exitCode'] == 1, 'actual early Neo failure')
            error = (args.run / name / 'stderr.log').read_text()
            check(error.startswith('Exception in thread "main" java.lang.IllegalStateException: Global bus rejected: LISTENER_CONTRACT\n')
                  and 'ForgeRegistryTransformationPlan.prepare' in error and 'DualForgeEntryTransformer.targets' in error,
                  'precise original fatal chain, not an inferred missing-class cause')
        else:
            state = read(name + '/result.json')
            check(state['lifecycle'] == 'PASS' and state['exitCode'] == 0 and all(state['checks'].values())
                  and not state['severityLines'], name + ' normal lifecycle/no error')
    observation = read('boundary/result.json')
    check(observation['plannerRefusalAssertions'] == 28 and observation['status'] == 'PASS', 'two-dist planner refusal')
    check('JEI_NATIVE_OBSERVER_PASS entry=mezz.jei.forge.JustEnoughItems module=jei' in observation['nativeObservation']
          and 'hostRecipes=1290' in observation['nativeObservation'], 'real native instance/provider/scan observation')
    check('JEI BOUNDARY CONTRACT PASS assertions=28' in (args.run / 'boundary-committed.stdout.log').read_text(),
          'committed-source refusal rerun')
    analysis = read('analysis-committed/summary.json')
    check(analysis['coverage']['forge-jei']['classes'] == analysis['coverage']['neo-jei']['classes'] == 923,
          'complete class inventory')
    check(all(c['classSHAExactlyMatchesOriginal'] for c in analysis['coverage'].values()), 'all class source hashes')
    check(analysis['forgeUniqueAPISymbols'] == 211 and analysis['forgeAPIUseSites'] == 674, 'complete Forge API use inventory')
    links = read('sdk-links/result.json')
    check(links['forge']['linked'] == 923 and links['forge']['unresolved'] == 0, 'native SDK links only')
    check(links['neo']['linked'] == 904 and links['neo']['unresolved'] == 19, 'Neo SDK unresolved classes retained')
    binding = read('production-binding.json')
    check(len(binding['productionFiles']) == 31 and all(p['gitBlobMatches']
          and sha(ROOT / p['path']) == p['sha256'] for p in binding['productionFiles']), 'unchanged production source binding')
    with zipfile.ZipFile(jar) as current, zipfile.ZipFile(binding['contentIdenticalTo009CJar']) as previous:
        check(set(current.namelist()) == set(previous.namelist())
              and all(current.read(n) == previous.read(n) for n in current.namelist()), 'all baseline JAR entries unchanged')
    regression = read('assessment.json')
    check(regression['measuredAutomatedScope'] == 'PASS' and regression['compat']['sha256'] == sha(jar),
          'all current-artifact server/contract comparisons')
    local = read('local-checks/result.json')
    check(local['status'] == 'PASS' and local['pythonTests'] == 106 and local['zshScripts'] == 25, 'local required checks')
    check(not (args.run / 'DualForge-010A-client-kit.zip').exists(), 'known-failing client kit not produced')
    states = {key: 'NOT_RUN' for key in ['JEI_NEO_CLASS_TRANSFORMATION', 'JEI_NEO_CLASS_DEFINITION',
        'JEI_NEO_MOD_CONSTRUCTION', 'JEI_NEO_LIFECYCLE', 'JEI_CLIENT_PLUGIN_REGISTRATION', 'JEI_RUNTIME_INITIALIZATION',
        'JEI_AT_APPLICATION', 'JEI_CLIENT_BOOT', 'JEI_GUI_BEHAVIOR', 'JEI_CLIENT_MANUAL', 'JEI_MIXED_CLIENT',
        'CURRENT_REBUILD_WINDOWS_REGRESSION']}
    states.update({'JEI_RELEASE_IDENTITY': 'PASS', 'JEI_STATIC_ABI_INVENTORY': 'PASS',
        'JEI_NATIVE_SERVER_BASELINE': 'PASS', 'JEI_NATIVE_DIAGNOSTIC_OBSERVATION': 'PASS',
        'JEI_NEO_PREPLAN': 'FAIL', 'JEI_REFUSAL_MODEL': 'PASS', 'JEI_CLIENT_KIT': 'BLOCKED',
        'NEW_JEI_PRODUCTION_COMPATIBILITY': 'NOT_IMPLEMENTED', 'CURRENT_ARTIFACT_RELEVANT_REGRESSION': 'PASS'})
    save(args.output, {'status': 'PARTIAL', 'evidenceAndRegressionChecks': 'PASS',
        'successfulEvidenceChecks': len(checks), 'checks': checks, 'finalStates': states,
        'compat': record(jar), 'original': record(original), 'clientZIP': None,
        'note': 'Expected refusal tests passing never mean JEI Neo runtime is compatible.'})
    print('JEI ASSESSMENT PARTIAL; evidence/regression checks PASS count=' + str(len(checks)))


if __name__ == '__main__':
    main()
