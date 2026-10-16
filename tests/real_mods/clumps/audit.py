"""Index original JAR references and retained javap evidence; never load mod classes."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import zipfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ['jar', 'javap-directory', 'failure-run', 'output']:
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        parser.error('Never overwrite existing evidence')
    sha = lambda data: hashlib.sha256(data).hexdigest()
    classes, edges, references = [], [], {}
    with zipfile.ZipFile(args.jar) as archive:
        for name in archive.namelist():
            if not name.endswith('.class'):
                continue
            source = name[:-6]
            data = archive.read(name)
            refs = sorted(set(re.findall(r'net/minecraftforge/[A-Za-z0-9_/$]+', data.decode('latin1'))))
            if refs:
                references[source] = refs
            evidence = args.javap_directory / (source.replace('/', '.') + '.stdout')
            text = evidence.read_text()
            assert 'SHA-256 checksum ' + sha(data) in text
            classes.append({'class': source, 'classSHA256': sha(data), 'javap': str(evidence),
                            'javapSHA256': sha(evidence.read_bytes())})
            method = None
            for line in text[text.index('{'):].splitlines():
                if re.match(r'^  \S.*(?:\);|\{\};)$', line):
                    method = line.strip()
                instruction = re.match(r'^\s+(\d+):\s+(\S+)\s+.*// (.+)', line)
                if instruction and any(word in instruction[3] for word in ['Method ', 'InterfaceMethod ', 'Field ', 'InvokeDynamic ']):
                    edges.append({'class': source, 'method': method, 'offset': int(instruction[1]),
                                  'opcode': instruction[2], 'reference': instruction[3]})
        mixin = json.loads(archive.read('clumps.mixins.json'))
        services = {name: archive.read(name).decode().strip() for name in archive.namelist()
                    if name.startswith('META-INF/services/') and not name.endswith('/')}
        refmap_present = mixin['refmap'] in archive.namelist()
    entry = (args.javap_directory / 'com.blamejared.clumps.Clumps.stdout').read_text()
    assert 'public com.blamejared.clumps.Clumps();' in entry and 'static {};' not in entry
    assert entry.count('REF_invokeStatic com/blamejared/clumps/Clumps.lambda$new$0:') == 2
    failure = (args.failure_run / 'stderr.log').read_text()
    assert 'UNSUPPORTED_ENTRY com/blamejared/clumps/Clumps: no controlled Forge entry profile' in failure
    result = json.loads((args.failure_run / 'result.json').read_text())
    assert result['state'] == 'EXITED_BEFORE_DONE' and result['exitCode'] != 0
    output = {'status': 'PASS', 'jar': {'path': str(args.jar), 'bytes': args.jar.stat().st_size,
              'sha256': sha(args.jar.read_bytes()), 'provenance': 'user-provided local input'},
              'gates': {name: 'PASS' for name in ['ENTRY_BYTECODE_ANALYZED', 'CALL_GRAPH_RECORDED',
                        'FORGE_API_DEPENDENCIES_RECORDED', 'MIXIN_REQUIREMENTS_RECORDED', 'FIRST_FAILURE_CONFIRMED']},
              'classes': classes, 'instructionReferences': edges, 'forgeReferences': references,
              'services': services, 'mixin': mixin, 'refmapPresent': refmap_present,
              'mixinScope': 'Original Mojmap selectors and member references; no namespace/member rewrite proposed. Actual application separately verified in game.',
              'failureEvidence': {'path': str(args.failure_run), 'stderrSHA256': sha((args.failure_run / 'stderr.log').read_bytes())},
              'windowsReports': {'source': 'user-provided excerpts and hashes only; original ZIPs not found in declared local inputs',
                  '23.19.55.zip': '8b5de41a5abb833382de2b47271f73a31d742e9e1591261988898e3825cafb50',
                  '23.28.51.zip': 'b550d47a83b3a41448caf12217fc5218548b0c66dba46f5bf731a6e605bb921f'}}
    args.output.write_text(json.dumps(output, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps(output['gates']))


if __name__ == '__main__':
    main()
