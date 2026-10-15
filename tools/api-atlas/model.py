"""Conservative ABI indexes and discovery candidates, separate from rewrite approval."""
from collections import Counter, defaultdict
import gzip
import json
import re
from pathlib import Path
from inputs import save


def records(path):
    with gzip.open(path, 'rt') as stream:
        for line in stream:
            yield json.loads(line)


def write_records(path, rows):
    # mtime and original filename are excluded from deterministic gzip headers.
    with open(path, 'wb') as raw, gzip.GzipFile(fileobj=raw, mode='wb', mtime=0, filename='') as zipped:
        for row in rows:
            zipped.write((json.dumps(row, ensure_ascii=False, sort_keys=True,
                                     separators=(',', ':')) + '\n').encode())


def active_artifact(artifact):
    if 'activeServerResource' in artifact:
        return artifact['activeServerResource']
    role = artifact['role']
    if role == 'DEVELOPMENT_TOOLCHAIN':
        return False
    if role == 'MINECRAFT_INTERMEDIATE':
        return (artifact['platform'] == 'neoforge'
                and artifact['filename'].endswith(('-srg.jar', '-extra.jar')))
    return True


def build_index(rows, artifacts):
    """Retain every origin. Ambiguous active providers are never silently overwritten."""
    versions = defaultdict(list)
    excluded = Counter()
    for row in rows:
        artifact = artifacts[row['artifact']]
        if not active_artifact(artifact) or not row['java21Eligible']:
            excluded[row['artifact']] += 1
            continue
        versions[(row['artifact'], row['name'])].append(row)
    owners = defaultdict(list)
    duplicates = []
    for (artifact, name), variants in sorted(versions.items()):
        release = max(v['release'] for v in variants)
        selected = [v for v in variants if v['release'] == release]
        if len(selected) != 1:
            raise ValueError(f'duplicate class at one release: {artifact} {name}')
        owners[name].append(selected[0])
        if len(variants) > 1:
            duplicates.append({'class': name, 'status': 'MULTI_RELEASE_JAVA21',
                               'origins': [v['artifact'] + '!' + v['member'] for v in variants],
                               'provider': artifact + '!' + selected[0]['member']})
    index = {}
    for name, candidates in sorted(owners.items()):
        if name == 'module-info':
            # Module descriptors belong to their artifact, not one global class provider.
            duplicates.append({'class': name, 'status': 'PER_MODULE_DESCRIPTOR',
                               'origins': [r['artifact'] + '!' + r['member'] for r in candidates]})
            continue
        if len(candidates) == 1:
            index[name] = candidates[0]
            continue
        patched = [r for r in candidates if artifacts[r['artifact']]['role'] == 'PATCHED_MINECRAFT']
        # This explicit server patch overlay follows the installer game resource order.
        if len(patched) == 1 and all(artifacts[r['artifact']]['role'] in {
                'PATCHED_MINECRAFT', 'MINECRAFT_INTERMEDIATE'} for r in candidates):
            chosen, status = patched[0], 'SERVER_PATCH_OVERLAY'
        elif len({r.get('classSHA256') for r in candidates}) == 1 and all(r.get('classSHA256') for r in candidates):
            chosen, status = min(candidates, key=lambda r: r['artifact']), 'BYTE_IDENTICAL_ALIAS'
        else:
            chosen, status = None, 'AMBIGUOUS_PROVIDER'
        duplicates.append({'class': name, 'status': 'DUPLICATE_CLASS', 'resolution': status,
                           'origins': [r['artifact'] + '!' + r['member'] for r in candidates],
                           'provider': None if chosen is None else chosen['artifact'] + '!' + chosen['member']})
        if chosen is not None:
            index[name] = chosen
    return index, duplicates, dict(excluded)


def resolve(index, kind, owner, name, descriptor, seen=None):
    """Conservative declared/super/interface lookup; unknown or conflicting paths stay unresolved."""
    seen = set() if seen is None else set(seen)
    if owner in seen or owner not in index:
        return []
    seen.add(owner)
    node = index[owner]
    group = 'fields' if kind == 'FIELD' else 'methods'
    direct = [m for m in node[group] if m['name'] == name and m['descriptor'] == descriptor]
    if direct:
        return direct
    if name in ('<init>', '<clinit>'):
        return []
    # Field resolution visits interfaces before the superclass (JVMS 5.4.3.2).
    if kind == 'METHOD' and node['superclass']:
        inherited = resolve(index, kind, node['superclass'], name, descriptor, seen)
        if inherited:
            return [m for m in inherited if not m['access'] & 2]
    matches = []
    for parent in node['interfaces']:
        matches.extend(resolve(index, kind, parent, name, descriptor, seen))
    if matches:
        unique = {(m['owner'], m['name'], m['descriptor']): m for m in matches}
        return list(unique.values())  # Multiple unrelated declarations remain ambiguous.
    if kind == 'FIELD' and node['superclass']:
        return resolve(index, kind, node['superclass'], name, descriptor, seen)
    return []


def public_class(node, index, visited=None):
    visited = set() if visited is None else set(visited)
    if node['name'] in visited:
        return False
    visited.add(node['name'])
    inner = next((r for r in node['innerClasses'] if r['name'] == node['name']), None)
    flags = node['access'] if inner is None else inner['access']
    if not flags & (1 | 4):
        return False
    outer = None if inner is None else inner['outerName']
    return outer is None or (outer in index and public_class(index[outer], index, visited))


def platform_class(name):
    return name.startswith(('net/minecraftforge/', 'net/neoforged/'))


def symbol(kind, owner, name=None, descriptor=None):
    return owner if kind == 'CLASS' else owner + '.' + name + (':' if kind == 'FIELD' else '') + descriptor


def remap_descriptor(descriptor, classes):
    return re.sub(r'L([^;]+);', lambda m: 'L' + classes.get(m[1], m[1]) + ';', descriptor)


def class_candidates(name, target):
    candidates = []
    if name in target:
        candidates.append(name)
    replacements = [('net/minecraftforge/', 'net/neoforged/neoforge/'),
                    ('net/minecraftforge/fml/', 'net/neoforged/fml/'),
                    ('net/minecraftforge/eventbus/api/', 'net/neoforged/bus/api/'),
                    ('net/minecraftforge/forgespi/', 'net/neoforged/neoforgespi/')]
    for old, new in replacements:
        if name.startswith(old) and new + name[len(old):] in target:
            candidates.append(new + name[len(old):])
    if name == 'net/minecraftforge/registries/RegistryObject':
        candidate = 'net/neoforged/neoforge/registries/DeferredHolder'
        if candidate in target:
            candidates.append(candidate)
    return sorted(set(candidates))


def compare(source, target, seeds=()):
    """Discovery only: even exact binary candidates do not assert behavioral equivalence."""
    result = []
    source_nodes = [n for k, n in sorted(source.items()) if platform_class(k)]
    classmap = {}
    for node in source_nodes:
        targets = class_candidates(node['name'], target)
        if len(targets) == 1:
            classmap[node['name']] = targets[0]
    seed_index = defaultdict(list)
    for rule in seeds:
        s = rule['source']
        seed_index[symbol(rule['kind'], s['owner'], s['name'], s['descriptor'])].append(rule)
        if rule['kind'] == 'CLASS' and s['owner'] in source and rule['target']['owner'] in target:
            classmap[s['owner']] = rule['target']['owner']
    for node in source_nodes:
        owner = node['name']
        cls = class_candidates(owner, target)
        for kind, members in [('CLASS', [None]), ('FIELD', node['fields']), ('METHOD', node['methods'])]:
            for member in members:
                name = None if member is None else member['name']
                desc = None if member is None else member['descriptor']
                candidates = []
                seed_rules = seed_index.get(symbol(kind, owner, name, desc), [])
                for new_owner in cls:
                    if kind == 'CLASS':
                        t = target[new_owner]
                        candidates.append({'owner': new_owner, 'artifact': t['artifact'], 'name': None,
                                           'descriptor': None, 'access': t['access'],
                                           'signature': t['signature'], 'superclass': t['superclass'], 'interfaces': t['interfaces']})
                    else:
                        new_desc = remap_descriptor(desc, classmap)
                        matches = resolve(target, kind, new_owner, name, new_desc)
                        if not matches:
                            # Renamed or changed descriptors are explicitly review candidates.
                            matches = [m for m in target[new_owner]['fields' if kind == 'FIELD' else 'methods']
                                       if m['name'] == name or (m['descriptor'] == new_desc and name not in ('<init>', '<clinit>'))]
                        for t in matches:
                            candidates.append({'owner': t['owner'], 'name': t['name'], 'descriptor': t['descriptor'],
                                               'access': t['access'], 'signature': t['signature'],
                                               'artifact': target[t['owner']]['artifact']})
                for rule in seed_rules:
                    t = rule['target']
                    if kind == 'CLASS':
                        matches = [target[t['owner']]] if t['owner'] in target else []
                    else:
                        matches = resolve(target, kind, t['owner'], t['name'], t['descriptor'])
                    if len(matches) != 1:
                        raise ValueError('seed target missing/ambiguous: ' + rule['id'])
                    matched = matches[0]
                    candidates.append({'owner': t['owner'], 'name': t['name'], 'descriptor': t['descriptor'],
                                       'access': matched['access'], 'signature': matched['signature'],
                                       'artifact': target[t['owner']]['artifact']})
                candidates = sorted({(c['owner'], c['name'], c['descriptor']): c for c in candidates}.values(),
                                    key=lambda c: (c['owner'], c['name'] or '', c['descriptor'] or ''))
                for candidate in candidates:
                    candidate['platform'] = 'neoforge'
                    candidate['version'] = '21.1.256'
                strategy = 'UNRESOLVED'
                if len(candidates) > 1:
                    status = 'AMBIGUOUS_MAPPING'
                elif not candidates:
                    status = 'UNRESOLVED'
                else:
                    c = candidates[0]
                    flags_equal = ((node if member is None else member)['access'] & (1 | 4 | 8 | 16 | 512 | 1024)) == (c['access'] & (1 | 4 | 8 | 16 | 512 | 1024))
                    if c['owner'] == owner and c['name'] == name and c['descriptor'] == desc and flags_equal:
                        strategy = 'EXACT_ABI_CANDIDATE'
                    elif kind == 'CLASS':
                        strategy = 'CLASS_RENAME_CANDIDATE'
                    elif c['descriptor'] != desc:
                        strategy = 'DESCRIPTOR_REWRITE_CANDIDATE'
                    else:
                        strategy = 'FIELD_REWRITE_CANDIDATE' if kind == 'FIELD' else 'METHOD_REWRITE_CANDIDATE'
                    status = 'SEMANTIC_REVIEW_REQUIRED'
                if any(s in owner for s in ('eventbus/', '/fml/javafmlmod/', 'MinecraftForge', '/registries/IForgeRegistry')):
                    strategy = 'RUNTIME_SHIM_REQUIRED'
                if any(r['strategy'] == 'RUNTIME_SHIM_REQUIRED' for r in seed_rules):
                    strategy = 'RUNTIME_SHIM_REQUIRED'
                result.append({'schemaVersion': 1, 'id': symbol(kind, owner, name, desc), 'kind': kind,
                    'source': {'platform': 'forge', 'version': '52.1.0', 'owner': owner, 'name': name,
                               'descriptor': desc, 'artifact': node['artifact'],
                               'access': (node if member is None else member)['access'],
                               'signature': (node if member is None else member)['signature']},
                    'targets': candidates, 'strategy': strategy, 'status': status,
                    'externallyAccessible': public_class(node, source) and (member is None or bool(member['access'] & (1 | 4))),
                    'evidence': ['fixed official ASM ABI inventory; candidate heuristic, not semantic equivalence'],
                    'invocationKindReview': kind == 'METHOD', 'scope': 'GENERAL_CANDIDATE',
                    'sourceInvocationKind': None if kind != 'METHOD' else (184 if member['access'] & 8 else 183 if name == '<init>' else 185 if node['access'] & 512 else 182),
                    'targetInvocationKinds': [] if kind != 'METHOD' else [184 if c['access'] & 8 else 183 if c['name'] == '<init>' else 185 if target[c['owner']]['access'] & 512 else 182 for c in candidates],
                    'sourceClassShape': {'superclass': node['superclass'], 'interfaces': node['interfaces'], 'access': node['access']} if kind == 'CLASS' else None,
                    'seedEvidence': seed_rules,
                    'runtimeVerified': False, 'productionEnabled': False})
    return result


def counts(index):
    groups = {}
    for label, predicate in [('platform', platform_class), ('minecraft', lambda n: n.startswith('net/minecraft/')),
                             ('dependencies', lambda n: not platform_class(n) and not n.startswith('net/minecraft/'))]:
        nodes = [n for k, n in index.items() if predicate(k)]
        groups[label] = {'classes': len(nodes), 'publicProtectedClasses': sum(public_class(n, index) for n in nodes),
            'fields': sum(len(n['fields']) for n in nodes), 'methods': sum(len(n['methods']) for n in nodes),
            'publicProtectedFields': sum(public_class(n, index) and bool(f['access'] & 5) for n in nodes for f in n['fields']),
            'publicProtectedMethods': sum(public_class(n, index) and bool(m['access'] & 5) for n in nodes for m in n['methods'])}
    return groups


def minecraft_diff(source, target):
    result = []
    for owner in sorted(set(source) | set(target)):
        if not owner.startswith('net/minecraft/'):
            continue
        a, b = source.get(owner), target.get(owner)
        changes = []
        if a is None or b is None:
            changes.append('CLASS_ABSENT_FROM_ONE_SERVER_VIEW')
        else:
            for key in ['access', 'superclass', 'interfaces', 'signature']:
                if a[key] != b[key]:
                    changes.append(key)
            for kind in ['fields', 'methods']:
                def signatures(n):
                    return {(m['name'], m['descriptor'], m['access'], m['signature']) for m in n[kind]}
                if signatures(a) != signatures(b):
                    changes.append(kind)
        if changes:
            result.append({'owner': owner, 'sourceArtifact': None if a is None else a['artifact'],
                           'targetArtifact': None if b is None else b['artifact'], 'changes': changes,
                           'interpretation': 'server declaration ABI difference; not a semantic equivalence claim',
                           'source': a, 'target': b})
    return result


def generate(scan, manifest, output):
    output.mkdir(parents=True, exist_ok=True)
    artifacts = {v['id']: v for v in json.loads(manifest.read_text())['artifacts']}
    grouped = defaultdict(list)
    targets = defaultdict(set)
    for row in records(scan):
        grouped[artifacts[row['artifact']]['platform']].append(row)
        targets[row['artifact']].add(row['classVersion'] & 0xffff)
    indexes, duplicate_report, excluded = {}, {}, {}
    for host in ['forge', 'neoforge']:
        indexes[host], duplicate_report[host], excluded[host] = build_index(grouped[host], artifacts)
        if grouped.get('java'):
            java_index, java_duplicates, _ = build_index(grouped['java'], artifacts)
            assert not (set(java_index) & set(indexes[host])), 'platform shadows java.base class'
            indexes[host].update(java_index)
            duplicate_report[host].extend(java_duplicates)
        write_records(output / (host + '-abi.jsonl.gz'), [indexes[host][n] for n in sorted(indexes[host])])
    all_origins = defaultdict(list)
    for host, rows in grouped.items():
        for row in rows:
            all_origins[(host, row['name'])].append(row)
    write_records(output / 'all-duplicate-origins.jsonl.gz', [
        {'platform': host, 'class': name, 'status': 'DUPLICATE_CLASS',
         'origins': [{'artifact': r['artifact'], 'member': r['member'], 'release': r['release'],
                      'activeResource': active_artifact(artifacts[r['artifact']]), 'classSHA256': r['classSHA256']} for r in rows],
         'activeProvider': indexes.get(host, {}).get(name, {}).get('artifact')}
        for (host, name), rows in sorted(all_origins.items()) if len(rows) > 1])
    seeds = json.loads((Path(__file__).parent / 'mappings/seeds.json').read_text())['rules']
    candidates = compare(indexes['forge'], indexes['neoforge'], seeds)
    diff = minecraft_diff(indexes['forge'], indexes['neoforge'])
    write_records(output / 'api-diff.jsonl.gz', candidates)
    write_records(output / 'minecraft-abi-diff.jsonl.gz', diff)
    save(output / 'duplicates.json', duplicate_report)
    save(output / 'excluded-intermediates.json', excluded)
    save(output / 'class-targets.json', {k: sorted(v) for k, v in sorted(targets.items())})
    save(output / 'conflicts.json', [r for r in candidates if r['status'] == 'AMBIGUOUS_MAPPING'])
    write_records(output / 'unresolved.jsonl.gz', [r for r in candidates if r['status'] == 'UNRESOLVED'])
    summary = {'schemaVersion': 1, 'scope': 'complete available fixed server-installation ClassFiles; client patched ABI INPUT_MISSING',
        'inventory': {host: counts(indexes[host]) for host in indexes},
        'allScannedClassFileRecords': sum(map(len, grouped.values())),
        'candidateSymbolsByKind': {kind: dict(Counter(r['strategy'] for r in candidates if r['kind'] == kind)) for kind in ['CLASS', 'FIELD', 'METHOD']},
        'statusByKind': {kind: dict(Counter(r['status'] for r in candidates if r['kind'] == kind)) for kind in ['CLASS', 'FIELD', 'METHOD']},
        'minecraftServerABIChangedClasses': len(diff), 'minecraftNamespaces': 'both supplied server views use Mojang names; FnF SRG aliases are a separate naming layer',
        'duplicates': {h: dict(Counter(r.get('resolution', r['status']) for r in rows)) for h, rows in duplicate_report.items()},
        'runtimeVerifiedCandidates': 0, 'productionEnabledCandidates': 0,
        'analysisLimits': ['client patched input missing', 'compiler-emitted declaration API only; runtime mixin/access-transformer additions not applied',
                           'interface resolution with several declarations remains conservative/ambiguous', 'unresolved does not prove API absence']}
    vanilla = {r['name']: r for r in grouped['forge'] if r['artifact'].endswith('server-1.21.1-official.jar')}
    write_records(output / 'vanilla-minecraft-abi.jsonl.gz', [vanilla[n] for n in sorted(vanilla)])
    patch_summary = {}
    for host in indexes:
        patch = minecraft_diff(vanilla, indexes[host])
        write_records(output / (host + '-versus-vanilla.jsonl.gz'), patch)
        patch_summary[host] = {'changedDeclarationClasses': len(patch),
                               'method': 'fixed installer Mojang-named vanilla server versus selected patched server'}
    save(output / 'patch-summary.json', patch_summary)
    save(output / 'summary.json', summary)
    return indexes, candidates, summary
