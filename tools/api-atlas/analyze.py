#!/usr/bin/env python3
"""Deterministic candidate/usage reports; no candidate is enabled in production."""
import argparse
from collections import Counter, defaultdict
import json
from pathlib import Path
import re
import zipfile
from inputs import save, sha
from model import generate, records, resolve, symbol, write_records, remap_descriptor


def srg_names(mcp_zip, mojmap_tsrg):
    """Join the two official naming datasets by obfuscated class/member identity."""
    def parse(text):
        result, owner = {}, None
        for line in text.splitlines():
            if not line or line.startswith('tsrg2 '):
                continue
            tokens = line.split()
            if not line.startswith('\t'):
                owner = tokens[0]
                result[owner] = {'name': tokens[1], 'fields': {}, 'methods': {}}
            elif not line.startswith('\t\t') and owner is not None:
                if tokens[1].startswith('('):
                    result[owner]['methods'][(tokens[0], tokens[1])] = tokens[2]
                else:
                    result[owner]['fields'][tokens[0]] = tokens[1]
        return result
    with zipfile.ZipFile(mcp_zip) as z:
        srg = parse(z.read('config/joined.tsrg').decode())
    moj = parse(Path(mojmap_tsrg).read_text())
    # Forge server mapping is Mojang -> obf; invert to the same obf identities.
    inverted = {}
    obfuscated_classes = {name: data['name'] for name, data in moj.items()}
    for mojang, data in moj.items():
        inverted[data['name']] = {'owner': mojang,
            'fields': {obf: name for name, obf in data['fields'].items()},
            'methods': {(obf, remap_descriptor(desc, obfuscated_classes)): name
                        for (name, desc), obf in data['methods'].items()}}
    result = defaultdict(set)
    for obf_owner, data in srg.items():
        official = inverted.get(obf_owner)
        if not official:
            continue
        for kind in ['fields', 'methods']:
            for obf, name in data[kind].items():
                target = official[kind].get(obf)
                if target:
                    result[(official['owner'], name)].add(target)
    return {key: sorted(value) for key, value in result.items()}


def domain(owner):
    if '/registries/' in owner:
        return 'REGISTRY'
    if 'eventbus/' in owner or '/bus/' in owner or owner.endswith('/MinecraftForge'):
        return 'EVENT_BUS'
    if '/fml/' in owner:
        return 'FML_CONTEXT'
    if 'CreativeModeTab' in owner:
        return 'CREATIVE_TAB'
    if '/food/' in owner or '/item/' in owner:
        return 'ITEM_FOOD_FUEL'
    if owner.startswith('net/minecraft/'):
        return 'MINECRAFT'
    if owner.startswith(('java/', 'javax/', 'jdk/')):
        return 'JAVA_STANDARD'
    return 'OTHER'


def invocation_agrees(reference, member, owner):
    opcode = reference['opcode']
    if ':handle' in reference['context']:
        opcode = {1: 180, 2: 178, 3: 181, 4: 179, 5: 182, 6: 184,
                  7: 183, 8: 183, 9: 185}.get(opcode, -2)
    if opcode == -1:  # Metadata/override relationship, not an instruction.
        return True
    static = bool(member['access'] & 8)
    if reference['kind'] == 'FIELD':
        return static == (opcode in (178, 179))
    if static != (opcode == 184):
        return False
    if opcode == 185 and not owner['access'] & 512:
        return False
    if opcode == 182 and owner['access'] & 512:
        return False
    return True


def usage(raw, atlas, output, names):
    output.mkdir(parents=True, exist_ok=True)
    nodes = list(records(raw)); owned = {n['name'] for n in nodes}
    source = {n['name']: n for n in records(atlas / 'forge-abi.jsonl.gz')}
    target = {n['name']: n for n in records(atlas / 'neoforge-abi.jsonl.gz')}
    vanilla = {n['name']: n for n in records(atlas / 'vanilla-minecraft-abi.jsonl.gz')}
    candidates = {r['id']: r for r in records(atlas / 'api-diff.jsonl.gz')}
    refs, internal, dynamic, edges, call_graph = [], [], [], Counter(), []
    for node in nodes:
        references = list(node['references'])
        if node['superclass'] not in owned:
            for method in node['methods']:
                if method['name'].startswith('<') or method['access'] & (2 | 8):
                    continue
                inherited = resolve(source, 'METHOD', node['superclass'], method['name'], method['descriptor'])
                if inherited:
                    references.append({'artifact': node['artifact'], 'class': node['name'],
                        'method': method['name'] + method['descriptor'], 'instructionIndex': -1,
                        'kind': 'METHOD', 'owner': node['superclass'], 'name': method['name'],
                        'descriptor': method['descriptor'], 'opcode': -1, 'context': 'externalOverrideContract'})
        for original in references:
            r = dict(original)
            if r['kind'] == 'METHOD':
                call_graph.append({**r, 'edgeType': 'POTENTIAL_BOOTSTRAP_TARGET' if ':handle' in r['context'] else
                                   'OVERRIDE_CONTRACT' if r['context'] == 'externalOverrideContract' else 'DIRECT_INSTRUCTION'})
            if r['kind'] == 'INVOKEDYNAMIC' or r['kind'] == 'STRING':
                dynamic.append(r)
                continue
            if r['owner'] in owned:
                internal.append(r)
                if r['kind'] == 'METHOD':
                    edges[(r['class'], r['owner'], r['name'], r['descriptor'])] += 1
                continue
            key = symbol(r['kind'], r['owner'], r['name'], r['descriptor'])
            r['symbol'] = key
            c = candidates.get(key)
            r['mappingCandidate'] = None if c is None else c['id']
            r['domain'] = domain(r['owner'])
            r['apiOrigin'] = ('FORGE_PLATFORM' if r['owner'].startswith('net/minecraftforge/')
                else 'MINECRAFT' if r['owner'].startswith('net/minecraft/') else
                'JAVA_STANDARD' if r['owner'].startswith(('java/', 'javax/', 'jdk/')) else
                'COMPILE_ONLY_ANNOTATION_OR_MISSING_INPUT' if r['owner'].startswith('org/jetbrains/annotations/')
                else 'SHARED_THIRD_PARTY_OR_UNKNOWN')
            origin = source.get(r['owner'])
            r['sourceArtifact'] = None if origin is None else origin['artifact']
            r['targetSymbolResolution'] = []
            r['namingLayer'] = None
            r['minecraftPatchRelation'] = None
            if r['apiOrigin'] == 'MINECRAFT':
                if r['kind'] == 'CLASS':
                    r['minecraftPatchRelation'] = 'VANILLA_CLASS' if r['owner'] in vanilla else 'PLATFORM_ADDED_CLASS'
                else:
                    base = resolve(vanilla, r['kind'], r['owner'], r['name'], r['descriptor'])
                    patched = resolve(source, r['kind'], r['owner'], r['name'], r['descriptor'])
                    r['minecraftPatchRelation'] = 'VANILLA_MEMBER' if base else 'PLATFORM_ADDED_OR_INHERITED_MEMBER' if patched else 'UNRESOLVED_SOURCE_MEMBER'
            r['compatibilityBasis'] = 'static inventory only; no runtime compatibility claim'
            if r['owner'].startswith('net/minecraftforge/'):
                context_call = (r['kind'] == 'METHOD' and r['owner'] == 'net/minecraftforge/fml/javafmlmod/FMLJavaModLoadingContext'
                    and r['method'] == '<init>()V' and r['class'] in owned and r['context'] == 'instruction'
                    and ((r['name'] == 'get' and r['descriptor'] == '()Lnet/minecraftforge/fml/javafmlmod/FMLJavaModLoadingContext;')
                         or (r['name'] == 'getModEventBus' and r['descriptor'] == '()Lnet/minecraftforge/eventbus/api/IEventBus;')))
                if context_call:
                    r['currentCompatibilityStatus'] = 'ALREADY_SUPPORTED_BY_DUALFORGE'
                    r['compatibilityBasis'] = '007C-3 verified owned constructor pair only; BusView is not native IEventBus'
                elif r['domain'] in ('EVENT_BUS', 'FML_CONTEXT') or r['owner'].endswith('/IForgeRegistry') or (c and c['strategy'] == 'RUNTIME_SHIM_REQUIRED'):
                    r['currentCompatibilityStatus'] = 'SHIM_REQUIRED'
                else:
                    r['currentCompatibilityStatus'] = 'MAPPING_CANDIDATE' if c and c['targets'] else 'UNRESOLVED'
            elif r['kind'] == 'CLASS' and r['owner'] in target:
                r['currentCompatibilityStatus'] = 'ALREADY_COMPATIBLE'
                r['compatibilityBasis'] = 'same named class is present; member/lifecycle semantics not asserted'
                r['targetSymbolResolution'] = [r['owner']]
            elif r['kind'] != 'CLASS':
                resolved = resolve(target, r['kind'], r['owner'], r['name'], r['descriptor'])
                if len(resolved) == 1 and bool(resolved[0]['access'] & (1 | 4)) and invocation_agrees(r, resolved[0], target[r['owner']]):
                    r['currentCompatibilityStatus'] = 'ALREADY_COMPATIBLE'
                    r['targetSymbolResolution'] = [symbol(r['kind'], x['owner'], x['name'], x['descriptor']) for x in resolved]
                else:
                    aliases = names.get((r['owner'], r['name']), [])
                    found = []
                    for alias in aliases:
                        found.extend(resolve(target, r['kind'], r['owner'], alias, r['descriptor']))
                    r['namingLayer'] = {'source': r['name'], 'candidateNames': aliases,
                                        'binaryResolved': [symbol(r['kind'], x['owner'], x['name'], x['descriptor']) for x in found]}
                    r['currentCompatibilityStatus'] = 'MAPPING_CANDIDATE' if len(found) == 1 else 'UNRESOLVED'
            else:
                r['currentCompatibilityStatus'] = 'UNRESOLVED'
            refs.append(r)
            edges[(r['class'], r['owner'], r['name'] or '', r['descriptor'] or '')] += 1
    save(output / 'references.json', refs)
    save(output / 'internal-references.json', internal)
    save(output / 'dynamic-sites-and-strings.json', dynamic)
    save(output / 'call-graph.json', call_graph)
    save(output / 'usage-graph.json', {'nodes': sorted(owned), 'edges': [
        {'from': a, 'to': b, 'member': c, 'descriptor': d, 'uses': count}
        for (a, b, c, d), count in sorted(edges.items())]})
    symbols = {r['symbol']: r for r in refs}
    coverage = {'classes': len(nodes), 'totalStaticExternalUsePoints': len(refs), 'uniqueExternalSymbols': len(symbols),
        'usePointsByKind': dict(Counter(r['kind'] for r in refs)),
        'uniqueSymbolsByKind': dict(Counter(r['kind'] for r in symbols.values())),
        'usePointsByOrigin': dict(Counter(r['apiOrigin'] for r in refs)),
        'usePointsByDomain': dict(Counter(r['domain'] for r in refs)),
        'usePointsByClassification': dict(Counter(r['currentCompatibilityStatus'] for r in refs)),
        'uniqueSymbolsByClassification': dict(Counter(r['currentCompatibilityStatus'] for r in symbols.values())),
        'staticVerifiedRewriteRules': 0, 'newRuntimeVerifiedMappings': 0,
        'alreadySupportedUsePoints': sum(r['currentCompatibilityStatus'] == 'ALREADY_SUPPORTED_BY_DUALFORGE' for r in refs),
        'minecraftPatchReferenceKinds': dict(Counter(r['minecraftPatchRelation'] for r in refs if r['minecraftPatchRelation'])),
        'externalOverrideContracts': sum(r['context'] == 'externalOverrideContract' for r in refs),
        'srgNamedMemberUsePoints': sum((r['name'] or '').startswith(('m_', 'f_')) for r in refs),
        'dynamicSites': sum(r['kind'] == 'INVOKEDYNAMIC' for r in dynamic),
        'reflectionLikeCalls': [r for r in call_graph if (r['owner'], r['name']) in {
            ('java/lang/Class', 'forName'), ('java/lang/ClassLoader', 'loadClass'),
            ('java/util/ServiceLoader', 'load'), ('java/lang/invoke/MethodHandles$Lookup', 'findVirtual'),
            ('java/lang/invoke/MethodHandles$Lookup', 'findStatic')}],
        'staticAnalysisBlindSpots': ['reflection computed names, service loading, JNI, runtime generation/mixins',
                                    'same symbol presence proves no lifecycle or semantic equivalence',
                                    'absence from server view does not prove absence on client',
                                    'SRG aliases are naming candidates, not production remapping'],
        'inputClasses': sorted(owned)}
    save(output / 'coverage.json', coverage)
    return coverage


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('mode', choices=['compare', 'usage'])
    p.add_argument('--scan', type=Path, required=True)
    p.add_argument('--manifest', type=Path)
    p.add_argument('--atlas', type=Path)
    p.add_argument('--mcp', type=Path)
    p.add_argument('--mojmap', type=Path)
    p.add_argument('--output', type=Path, required=True)
    args = p.parse_args()
    if args.mode == 'compare':
        if args.manifest is None:
            p.error('--manifest required for compare')
        _, _, result = generate(args.scan, args.manifest, args.output)
    else:
        if any(v is None for v in [args.atlas, args.mcp, args.mojmap]):
            p.error('--atlas, --mcp, --mojmap required for usage')
        result = usage(args.scan, args.atlas, args.output, srg_names(args.mcp, args.mojmap))
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
