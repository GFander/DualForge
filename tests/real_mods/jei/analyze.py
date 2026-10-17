"""Read-only JEI release/host ABI evidence; candidates never authorize rewriting."""
import argparse
from collections import defaultdict
import gzip
import hashlib
import json
from pathlib import Path
import re
import zipfile


def read_rows(path):
    with gzip.open(path, 'rt') as stream:
        return [json.loads(line) for line in stream]


def save(path, value):
    with path.open('x') as stream:
        json.dump(value, stream, ensure_ascii=False, indent=2)
        stream.write('\n')


def candidate_name(name):
    # Research candidates only, not a production namespace mapper.
    pairs = [('net/minecraftforge/common/MinecraftForge', 'net/neoforged/neoforge/common/NeoForge'),
             ('net/minecraftforge/common/ForgeConfigSpec', 'net/neoforged/neoforge/common/ModConfigSpec'),
             ('net/minecraftforge/eventbus/', 'net/neoforged/bus/'),
             ('net/minecraftforge/forgespi/', 'net/neoforged/neoforgespi/'),
             ('net/minecraftforge/fml/', 'net/neoforged/fml/'),
             ('net/minecraftforge/api/', 'net/neoforged/api/'),
             ('net/minecraftforge/', 'net/neoforged/neoforge/')]
    for source, target in pairs:
        if name.startswith(source):
            return target + name[len(source):]
    return name


def descriptor_candidate(desc):
    return re.sub(r'L([^;]+);', lambda match: 'L' + candidate_name(match[1]) + ';', desc) if desc else desc


def indexes(rows, artifacts):
    providers = defaultdict(list)
    for row in rows:
        if row['java21Eligible']:
            providers[(artifacts[row['artifact']], row['name'])].append(row)
    index, conflicts = {}, []
    for (side, name), group in providers.items():
        if name == 'module-info':
            continue  # Each artifact owns its descriptor; it is not a global class provider.
        if len({row['classSHA256'] for row in group}) == 1:
            index[(side, name)] = group[0]
        else:
            conflicts.append({'side': side, 'class': name,
                              'providers': [{'artifact': r['artifact'], 'sha256': r['classSHA256']} for r in group]})
    return index, conflicts


def member(index, side, kind, owner, name, desc, visited=None):
    visited = set(visited or ())
    if owner in visited or (side, owner) not in index:
        return []
    visited.add(owner)
    node = index[(side, owner)]
    group = 'fields' if kind == 'FIELD' else 'methods'
    found = [m for m in node[group] if m['name'] == name and m['descriptor'] == desc]
    if found or name in ('<init>', '<clinit>'):
        return found
    inherited = []
    for parent in [node['superclass'], *node['interfaces']]:
        if parent:
            inherited.extend(m for m in member(index, side, kind, parent, name, desc, visited) if not m['access'] & 2)
    return list({(m['owner'], m['name'], m['descriptor']): m for m in inherited}.values())


def categories(owner, name):
    tags = {'G'}  # No JEI lifecycle/GUI contract is approved by this analysis.
    if owner == 'net/minecraftforge/fml/common/Mod':
        tags.add('A')
    if owner.startswith(('net/minecraftforge/fml/javafmlmod/', 'net/minecraftforge/fml/ModLoadingContext')):
        tags.update(['A', 'C', 'E', 'F'])  # Existing shim only within its admitted profile.
    if '/event' in owner or '/DistExecutor' in owner:
        tags.update(['C', 'E', 'F'])
    if '/network/' in owner or 'ConfigSpec' in owner or '/config/' in owner or '/fluids/' in owner:
        tags.update(['C', 'E', 'F'])
    if '/fml/' in owner or '/forgespi/' in owner:
        tags.add('D')
    return sorted(tags)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for key in ['run', 'output']:
        parser.add_argument('--' + key, type=Path, required=True)
    args = parser.parse_args()
    out = args.output.resolve()
    if not out.is_relative_to((Path.home() / 'Env/DualForge/runs').resolve()):
        parser.error('Evidence output must stay on SSD')
    out.mkdir(parents=True, exist_ok=False)
    rows = read_rows(args.run / 'static/usage.jsonl.gz')
    artifacts = json.loads((args.run / 'static/host-api-inputs.json').read_text())
    index, conflicts = indexes(read_rows(args.run / 'static/host-api.jsonl.gz'),
                               {a['artifact']: a['side'] for a in artifacts})
    coverage, inventories, all_refs = {}, {}, []
    for identity, platform in [('forge-jei', 'forge'), ('neo-jei', 'neoforge')]:
        selected = [row for row in rows if row['artifact'] == identity]
        with zipfile.ZipFile(args.run / f'inputs/jei-1.21.1-{platform}-19.27.0.340.jar') as jar:
            originals = {n: hashlib.sha256(jar.read(n)).hexdigest() for n in jar.namelist() if n.endswith('.class')}
        scanned = {r['member']: r['classSHA256'] for r in selected}
        if originals != scanned:
            raise ValueError('Incomplete or changed ClassFile scan: ' + identity)
        coverage[identity] = {'classes': len(selected), 'classSHAExactlyMatchesOriginal': True,
                              'referenceCount': sum(len(r['references']) for r in selected)}
        inventories[identity] = [{'class': r['name'], 'sha256': r['classSHA256'],
                                 'annotations': r['annotations'], 'superclass': r['superclass'],
                                 'interfaces': r['interfaces'], 'methods': r['methods'], 'fields': r['fields']}
                                for r in selected]
        all_refs.extend(ref for row in selected for ref in row['references'])
    sites = defaultdict(list)
    for ref in all_refs:
        if ref['artifact'] == 'forge-jei' and (ref.get('owner') or '').startswith('net/minecraftforge/'):
            sites[(ref['kind'], ref['owner'], ref.get('name'), ref.get('descriptor'))].append(ref)
    api = []
    for (kind, owner, name, desc), uses in sorted(sites.items(), key=lambda row: str(row[0])):
        target = candidate_name(owner)
        target_desc = descriptor_candidate(desc)
        source_decl = member(index, 'forge', kind, owner, name, desc) if kind in ['METHOD', 'FIELD'] else []
        target_decl = member(index, 'neo', kind, target, name, target_desc) if kind in ['METHOD', 'FIELD'] else []
        tags = categories(owner, name)
        if target != owner or target_desc != desc:
            tags = sorted(set(tags) | {'B'})
        api.append({'kind': kind, 'sourceOwner': owner, 'sourceName': name, 'sourceDescriptor': desc,
                    'useSites': uses, 'sourceDeclarations': source_decl,
                    'candidateOwner': target, 'candidateDescriptor': target_desc,
                    'candidateClassPresent': ('neo', target) in index,
                    'candidateDeclarations': target_decl, 'categories': tags,
                    'executionSemantics': 'Original instructions retained in usage.jsonl.gz; no equivalence established',
                    'proposedConversion': 'NONE APPROVED; exact symbol is a research candidate only',
                    'refusalConditions': ['source or target ambiguity', 'unproved owner/module/event lifecycle',
                                          'unknown dataflow/bootstrap', 'unverified access/opcode/AT behavior'],
                    'status': 'UNVERIFIED_NOT_A_PRODUCTION_RULE'})
    forge = [r for r in rows if r['artifact'] == 'forge-jei']
    neo = {r['name']: r for r in rows if r['artifact'] == 'neo-jei'}
    platforms = []
    for row in forge:
        if not row['name'].startswith('mezz/jei/forge/'):
            continue
        counterpart = neo.get(row['name'].replace('mezz/jei/forge/', 'mezz/jei/neoforge/', 1))
        platforms.append({'source': row['name'], 'sourceSHA256': row['classSHA256'],
                          'reference': None if counterpart is None else counterpart['name'],
                          'sourceMethods': [{'name': m['name'], 'descriptor': m['descriptor']} for m in row['methods']],
                          'referenceMethods': [] if counterpart is None else [
                              {'name': m['name'], 'descriptor': m['descriptor']} for m in counterpart['methods']],
                          'status': 'REFERENCE_ONLY_NOT_COPIED'})
    summary = {'status': 'PASS', 'scope': 'STATIC_ANALYSIS_ONLY', 'coverage': coverage,
               'forgeAPIUseSites': sum(map(len, sites.values())), 'forgeUniqueAPISymbols': len(api),
               'forgeAPIOwners': len({key[1] for key in sites}),
               'candidateClassesAbsentOrAmbiguous': sum(not r['candidateClassPresent'] for r in api),
               'hostProviderConflicts': len(conflicts),
               'bootstrapSites': sum(i['opcode'] == 186 for r in forge for m in r['code'] for i in m['instructions']),
               'minecraftSRGMemberSymbols': len({(r['owner'], r['name'], r['descriptor']) for r in all_refs
                   if r['artifact'] == 'forge-jei' and r['kind'] in ['METHOD', 'FIELD']
                   and r['owner'].startswith('net/minecraft/') and re.match(r'[mf]_\d+_', r['name'])}),
               'jeiPluginAnnotations': [r['name'] for r in forge if any(
                   a['annotation']['descriptor'] == 'Lmezz/jei/api/JeiPlugin;' for a in r['annotations'])],
               'runtimeCompatibility': 'NOT_EVALUATED_BY_STATIC_ANALYSIS'}
    for name, data in [('summary', summary), ('forge-api', api), ('class-inventories', inventories),
                       ('host-provider-conflicts', conflicts), ('official-platform-comparison', platforms)]:
        save(out / (name + '.json'), data)
    with gzip.open(out / 'complete-callgraph.jsonl.gz', 'wt') as stream:
        for ref in all_refs:
            stream.write(json.dumps(ref, ensure_ascii=False) + '\n')
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
