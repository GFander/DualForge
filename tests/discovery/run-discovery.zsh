#!/usr/bin/env zsh
# Metadata/discovery only: no Java server or candidate constructor is started.
emulate -LR zsh
setopt errexit nounset pipefail
if (( $# != 3 )); then
  print -u2 'Usage: zsh tests/discovery/run-discovery.zsh <new-compat-jar> <server-input> <fixed-probe>'
  exit 2
fi
python3 -B - "${0:A:h:h:h}" "$@" <<'PY'
from pathlib import Path
import hashlib, importlib.util, json, os, shutil, sys, zipfile
repo, compat, server, probe = (Path(value).resolve() for value in sys.argv[1:])
spec = importlib.util.spec_from_file_location('workflow', repo/'tools/unix_workflow.py')
workflow = importlib.util.module_from_spec(spec)
spec.loader.exec_module(workflow)
assert hashlib.sha256(probe.read_bytes()).hexdigest() == workflow.PROBE_SHA, 'Fixed Probe mismatch'
run = workflow.run_directory(repo/'out/convergence-003a/discovery')
root = run/'fixtures'
names = ['A','B','C','D','E','F','native-dual','missing-loader','wrong-loader','multiple','ambiguous']
for name in names:
    (root/name/'mods').mkdir(parents=True)
native = server/'libraries/net/neoforged/neoforge/21.1.256/neoforge-21.1.256-universal.jar'
with zipfile.ZipFile(probe) as archive:
    toml = archive.read('META-INF/mods.toml').decode()
shutil.copy2(probe, root/'A/mods/forgeprobe.jar')
shutil.copy2(probe, root/'B/mods/arbitrary-renamed-input.jar')
shutil.copy2(native, root/'D/mods/native.jar')

def jar(path, metadata=None):
    with zipfile.ZipFile(path, 'w') as archive:
        archive.writestr('META-INF/MANIFEST.MF',
                         'Manifest-Version: 1.0\nAutomatic-Module-Name: controlledcandidate\n\n')
        if metadata is not None:
            archive.writestr('META-INF/mods.toml', metadata)

controlled = toml.replace('forgeprobe', 'controlled_candidate')
jar(root/'C/mods/second.jar', controlled)
jar(root/'E/mods/library.jar')
(root/'F/mods/invalid.jar').write_bytes(b'this is not a ZIP or JAR')
with zipfile.ZipFile(native) as source, zipfile.ZipFile(root/'native-dual/mods/native-with-legacy-metadata.jar','w') as target:
    for entry in source.infolist():
        target.writestr(entry, source.read(entry.filename))
    target.writestr('META-INF/mods.toml', toml)
jar(root/'missing-loader/mods/missing.jar', controlled.replace('loaderVersion="[52,)"\n',''))
jar(root/'wrong-loader/mods/wrong.jar', controlled.replace('javafml', 'not_a_supported_loader'))
shutil.copy2(probe, root/'multiple/mods/z-probe.jar')
shutil.copy2(root/'C/mods/second.jar', root/'multiple/mods/a-candidate.jar')
jar(root/'ambiguous/mods/ambiguous.jar', controlled.replace('[52,)', '[1,)'))
workflow.save(run/'fixtures.json', workflow.fingerprints(list(root.rglob('*.jar'))))
libraries = sorted((server/'libraries').rglob('*.jar'))
jdk = Path(os.environ.get('JAVA_HOME', '/workspace/dualforge-cloud/tools/msjdk/usr/lib/jvm/msopenjdk-21-amd64'))/'bin'
inputs = workflow.fingerprints([compat, probe, native, repo/'tests/discovery/DiscoveryContractTest.java',
                               repo/'tests/discovery/run-discovery.zsh', *libraries])
workflow.save(run/'inputs.json', inputs)
cp = os.pathsep.join(map(str, [compat, *libraries]))
classes = run/'classes'
classes.mkdir()
result = {'status': 'FAIL', 'directory': str(run)}
try:
    workflow.native(run, 'javac', [jdk/'javac','--release','21','-encoding','UTF-8','-classpath',cp,
                    '-d',classes,repo/'tests/discovery/DiscoveryContractTest.java'], cwd=run)
    workflow.native(run, 'discovery', [jdk/'java','--add-opens','java.base/java.lang.invoke=ALL-UNNAMED',
                    '--add-exports','java.base/sun.security.util=ALL-UNNAMED','-classpath',str(classes)+os.pathsep+cp,
                    'com.example.forgecompat.DiscoveryContractTest',root], cwd=run)
    output = (run/'discovery.stdout.log').read_text()
    assert 'PASS A-F + native-dual' in output, 'Discovery cases did not complete'
    assert (root/'A/mods/forgeprobe.jar').read_bytes() == (root/'B/mods/arbitrary-renamed-input.jar').read_bytes()
    workflow.unchanged(inputs)
    result.update(status='PASS', assertions={
        'A-F actual FML pipeline completed': True,
        'A/B byte-identical with different filenames': True,
        'native-dual/missing/wrong-loader/stable-multiple/ambiguity-stop': True,
        'inputs unchanged': True})
finally:
    workflow.save(run/'result.json', result)
    print('RESULT', result['status'], run)
PY
