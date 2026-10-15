"""Read-only closure analysis of the fixed FnF release; never approves a production rule."""
import argparse
from collections import Counter
import json
from pathlib import Path
import zipfile

from inputs import save, sha
from model import records, resolve, remap_descriptor
from analyze import usage

F = 'net/minecraftforge/'
DR = F + 'registries/DeferredRegister'
RO = F + 'registries/RegistryObject'
ITEM = 'net/minecraft/world/item/Item'
CATEGORIES = ['STATIC_ABI_PRESENT', 'EXISTING_PRODUCTION_SUPPORTED',
              'LOCAL_MATCH_GROUP_REJECTED', 'NEW_ASM_RULE_REQUIRED',
              'RUNTIME_BRIDGE_REQUIRED', 'UNRESOLVED']
FNF_SHA = 'bd415b04529c4ffc31e14757488f71dab6917e4560c61e236c3c4d0de6557958'


def implementation(site):
    args = site.get('arguments', [])
    return args[1].get('handle') if len(args) > 1 and isinstance(args[1], dict) else None


def known_bootstrap(site):
    b = site.get('bootstrap', {}).get('handle', {})
    return (b.get('owner'), b.get('name'), b.get('tag'), b.get('descriptor')) == (
        'java/lang/invoke/LambdaMetafactory', 'metafactory', 6,
        '(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;'
        'Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;')


def supplier_kind(code, owned):
    """Descriptive inventory, deliberately NOT a safety/production admission validator."""
    instructions = code['instructions']
    allocated = [i['type'] for i in instructions if i['opcode'] == 187]
    custom = [n for n in allocated if n in owned and owned[n]['superclass'] == ITEM]
    if custom:
        return 'CUSTOM_ITEM_SUBCLASS', custom
    if ITEM not in allocated:
        return 'UNRESOLVED', []
    if any(i.get('owner') == ITEM + '$Properties' and i.get('name') == 'food' for i in instructions):
        return 'ITEM_WITH_FOOD_PROPERTIES', []
    return 'ORDINARY_ITEM', []


def cfg(code):
    """Potential instruction edges (including exception edges), not a runtime trace."""
    result = []
    ins = code['instructions']
    for i, row in enumerate(ins):
        op = row['opcode']
        targets = row.get('targets', []) + ([row['target']] if 'target' in row else [])
        if 'default' in row:
            targets.append(row['default'])
        for target in sorted(set(targets)):
            result.append([i, target, 'BRANCH'])
        if i + 1 < len(ins) and op not in (167, 169, 170, 171, 172, 173, 174, 175, 176, 177, 191):
            result.append([i, i + 1, 'FALLTHROUGH'])
    for exception in code['exceptionTable']:
        for i in range(exception['start'], exception['end']):
            result.append([i, exception['handler'], 'EXCEPTION'])
    return result


def disposition(ref):
    if not ref['owner'].startswith(F):
        return 'STATIC_ABI_PRESENT' if ref['currentCompatibilityStatus'] == 'ALREADY_COMPATIBLE' else 'UNRESOLVED'
    if ref['currentCompatibilityStatus'] == 'ALREADY_SUPPORTED_BY_DUALFORGE' or ref['owner'] == F + 'fml/common/Mod':
        return 'EXISTING_PRODUCTION_SUPPORTED'
    if ref['owner'] == F + 'common/MinecraftForge' or ref['name'] == 'register' and ref['descriptor'] == '(Ljava/lang/Object;)V':
        return 'RUNTIME_BRIDGE_REQUIRED'
    if '/registries/' in ref['owner']:
        # Item boundary primitives exist in 4B, but the entire FnF plan is rejected.
        if ref['owner'] == DR and ref['name'] == 'create' and ref['descriptor'].startswith('(Lnet/minecraft/resources/ResourceKey;'):
            return 'NEW_ASM_RULE_REQUIRED'
        return 'LOCAL_MATCH_GROUP_REJECTED'
    return 'RUNTIME_BRIDGE_REQUIRED'


def target(ref, host):
    remaps = {DR: 'net/neoforged/neoforge/registries/DeferredRegister',
              RO: 'net/neoforged/neoforge/registries/DeferredHolder',
              F + 'registries/IForgeRegistry': 'net/minecraft/core/Registry',
              F + 'eventbus/api/IEventBus': 'net/neoforged/bus/api/IEventBus',
              F + 'common/MinecraftForge': 'net/neoforged/neoforge/common/NeoForge'}
    owner = remaps.get(ref['owner'], ref['owner'])
    name, desc = ref['name'], ref['descriptor']
    if owner == 'net/neoforged/neoforge/common/NeoForge' and name == 'EVENT_BUS':
        desc = 'Lnet/neoforged/bus/api/IEventBus;'
    elif ref['owner'] == F + 'registries/ForgeRegistries' and name == 'ITEMS':
        owner, name, desc = 'net/minecraft/core/registries/BuiltInRegistries', 'ITEM', 'Lnet/minecraft/core/DefaultedRegistry;'
    elif desc:
        desc = remap_descriptor(desc, remaps)
    hits = [host[owner]] if ref['kind'] == 'CLASS' and owner in host else (
        resolve(host, ref['kind'], owner, name, desc) if ref['kind'] != 'CLASS' else [])
    return {'owner': owner, 'name': name, 'descriptor': desc,
            'binaryDeclarations': [{k: x[k] for k in ('owner', 'name', 'descriptor', 'signature', 'access', 'artifact', 'classSHA256') if k in x} for x in hits],
            'staticResolution': 'UNIQUE' if len(hits) == 1 else 'UNRESOLVED' if not hits else 'AMBIGUOUS',
            'semanticEquivalence': 'NOT VERIFIED'}


def analyze(nodes, references, internal, raw_path, atlas, jar, output):
    owned = {n['name']: n for n in nodes}
    if len(owned) != len(nodes):
        raise ValueError('duplicate ClassFile identity')
    with zipfile.ZipFile(jar) as z:
        members = [i.filename for i in z.infolist() if i.filename.endswith('.class')]
        if sorted(members) != sorted(n['member'] for n in nodes):
            raise ValueError('scan does not cover all original ClassFiles')
        import hashlib
        for n in nodes:
            if hashlib.sha256(z.read(n['member'])).hexdigest() != n['classSHA256']:
                raise ValueError('ClassFile fingerprint mismatch')
        manifest = z.read('META-INF/MANIFEST.MF').decode()
    source = {n['name']: n for n in records(atlas / 'forge-abi.jsonl.gz')}
    host = {n['name']: n for n in records(atlas / 'neoforge-abi.jsonl.gz')}
    context = {'inputSHA256': sha(jar), 'scanSHA256': sha(raw_path),
               'atlasInputs': {n: sha(atlas / n) for n in ['forge-abi.jsonl.gz', 'neoforge-abi.jsonl.gz']},
               'runtime': 'NOT RUN', 'productionRulesEnabled': 0}
    def emit(name, data):
        save(output / (name + '.json'), {'schemaVersion': 1, 'dataset': name, **context, 'data': data})
    inventory = []
    sites = []
    for n in nodes:
        methods = []
        for m in n['code']:
            methods.append({'name': m['name'], 'descriptor': m['descriptor'], 'instructionNodes': len(m['instructions']),
                            'exceptionTable': m['exceptionTable'], 'controlFlowEdges': cfg(m),
                            'evidence': str(raw_path) + '#' + n['name'] + '/' + m['name'] + m['descriptor']})
            for site in m['instructions']:
                if site['opcode'] != 186:
                    continue
                handle = implementation(site)
                code = next((c for c in owned.get((handle or {}).get('owner'), {}).get('code', [])
                             if c['name'] == handle['name'] and c['descriptor'] == handle['descriptor']), None) if handle else None
                category, custom = supplier_kind(code, owned) if code else ('UNRESOLVED', [])
                if site['descriptor'].endswith('Lnet/minecraft/world/item/CreativeModeTab$DisplayItemsGenerator;'):
                    category = 'TAB_DISPLAY_CALLBACK'
                elif code and code['descriptor'].endswith('Lnet/minecraft/world/item/CreativeModeTab;'):
                    category = 'TAB_REGISTRATION_SUPPLIER'
                elif code and code['descriptor'].endswith('Lnet/minecraft/world/item/ItemStack;'):
                    category = 'TAB_ICON_SUPPLIER'
                if not known_bootstrap(site) or not site['descriptor'].startswith('()'):
                    category = 'UNRESOLVED'
                following = [i for i in m['instructions'][site['index'] + 1:] if i['opcode'] >= 0]
                # Only the immediate register + PUTSTATIC pattern grants a registration association.
                register = following[0] if following and following[0].get('owner') == DR and following[0].get('name') == 'register' else None
                field = following[1] if register and len(following) > 1 and following[1]['opcode'] == 179 else None
                previous = [i for i in m['instructions'][:site['index']] if i['opcode'] >= 0]
                reg_id = previous[-1].get('constant') if previous and previous[-1]['opcode'] == 18 else None
                sites.append({'class': n['name'], 'classSHA256': n['classSHA256'], 'method': m['name'] + m['descriptor'],
                              'index': site['index'], 'callSiteDescriptor': site['descriptor'], 'bootstrap': site['bootstrap'],
                              'bootstrapArguments': site['arguments'], 'implementation': handle, 'classification': category,
                              'zeroCapture': site['descriptor'].startswith('()'), 'customTypes': custom,
                              'registrationID': reg_id if register else None, 'registryCall': register, 'holderField': field,
                              'evidence': str(raw_path) + '#' + n['name'] + '/' + m['name'] + ':' + str(site['index']),
                              'safetyApproval': 'NONE: descriptive classification only'})
        inventory.append({k: n[k] for k in n if k not in ('references', 'code')} | {'methodBodies': methods})
    emit('class-inventory', {'classes': inventory, 'classFileCount': len(nodes),
                            'moduleIdentity': {'descriptor': 'NONE' if 'module-info.class' not in members else 'PRESENT',
                                               'manifest': manifest, 'fmlOwnership': 'See actual 4B read-only FML Contract receipt'},
                            'fields': sum(len(n['fields']) for n in nodes), 'methods': sum(len(n['methods']) for n in nodes)})
    symbols = {}
    for i, ref in enumerate(references):
        key = ref['symbol']
        if key not in symbols:
            hits = [source[ref['owner']]] if ref['kind'] == 'CLASS' and ref['owner'] in source else (
                resolve(source, ref['kind'], ref['owner'], ref['name'], ref['descriptor']) if ref['kind'] != 'CLASS' else [])
            symbols[key] = {'symbol': key, 'kind': ref['kind'], 'owner': ref['owner'], 'name': ref['name'],
                            'descriptor': ref['descriptor'], 'origin': ref['apiOrigin'],
                            'classification': disposition(ref), 'usePoints': 0, 'opcodes': [], 'evidence': [],
                            'sourceResolution': 'UNIQUE' if len(hits) == 1 else 'UNRESOLVED',
                            'sourceDeclarations': [{k: x[k] for k in ('owner', 'name', 'descriptor', 'signature', 'access', 'artifact', 'classSHA256') if k in x} for x in hits],
                            'targetCandidate': target(ref, host)}
        row = symbols[key]
        if row['classification'] != disposition(ref):
            # A class/member may have context-dependent support: preserve per-point partitions.
            row['classification'] = 'LOCAL_MATCH_GROUP_REJECTED'
        row['usePoints'] += 1
        row['opcodes'] = sorted(set(row['opcodes'] + [ref['opcode']]))
        row['evidence'].append({'class': ref['class'], 'method': ref['method'], 'index': ref['instructionIndex'],
                                'context': ref['context'], 'classification': disposition(ref), 'referenceOrdinal': i})
    emit('api-usage-matrix', {'symbols': list(symbols.values()), 'externalUsePoints': len(references),
                             'uniqueSymbols': len(symbols), 'pointClassification': dict(Counter(disposition(r) for r in references)),
                             'forgeUsePoints': sum(r['owner'].startswith(F) for r in references),
                             'forgePointClassification': dict(Counter(disposition(r) for r in references if r['owner'].startswith(F))),
                             'unresolvedUsePoints': sum(disposition(r) == 'UNRESOLVED' for r in references),
                             'semantics': 'STATIC_ABI_PRESENT does not assert game behavior; local primitives grant no FnF plan admission'})
    emit('supplier-classification', {'sites': sites, 'totalInvokeDynamic': len(sites),
                                    'classificationCounts': dict(Counter(s['classification'] for s in sites)),
                                    'registeredItemSuppliers': sum(s['registryCall'] is not None and s['classification'] in ['ORDINARY_ITEM', 'ITEM_WITH_FOOD_PROPERTIES', 'CUSTOM_ITEM_SUBCLASS'] for s in sites),
                                    'unresolvedSites': sum(s['classification'] == 'UNRESOLVED' for s in sites)})
    # Preserve every raw reference as a distinct ordinal: debug metadata may repeat identical symbols.
    edges = []
    for n in nodes:
        for ordinal, ref in enumerate(n['references']):
            if ref['kind'] in ('STRING', 'INVOKEDYNAMIC'):
                continue
            edges.append({'id': n['name'] + ':' + str(ordinal), 'from': n['name'], 'to': ref['owner'],
                          'owned': ref['owner'] in owned, 'kind': ref['kind'], 'method': ref['method'],
                          'name': ref['name'], 'descriptor': ref['descriptor'], 'opcode': ref['opcode'],
                          'index': ref['instructionIndex'], 'context': ref['context']})
    for ref in references:
        if ref['context'] == 'externalOverrideContract':
            edges.append({'id': ref['class'] + ':override:' + ref['name'] + ref['descriptor'],
                          'from': ref['class'], 'to': ref['owner'], 'owned': False,
                          'kind': 'OVERRIDE_CONTRACT', 'method': ref['method'], 'name': ref['name'],
                          'descriptor': ref['descriptor'], 'opcode': -1, 'index': -1, 'context': ref['context']})
    assert len({e['id'] for e in edges}) == len(edges)
    emit('reference-closure', {'ownedClasses': sorted(owned), 'edges': edges,
                              'edgeCount': len(edges), 'internalReferenceCount': len(internal),
                              'limits': ['Potential ClassFile references, not a dynamic execution graph',
                                         'Reflection/computed names/runtime mixins remain outside closed-world proof']})
    return {'classFiles': len(nodes), 'fields': sum(len(n['fields']) for n in nodes),
            'methods': sum(len(n['methods']) for n in nodes), 'externalUsePoints': len(references),
            'forgeUsePoints': sum(r['owner'].startswith(F) for r in references),
            'uniqueSymbols': len(symbols), 'indyClassification': dict(Counter(s['classification'] for s in sites)),
            'forgeClassification': dict(Counter(disposition(r) for r in references if r['owner'].startswith(F))),
            'unresolvedUsePoints': sum(disposition(r) == 'UNRESOLVED' for r in references),
            'envelope': context}


def main():
    p = argparse.ArgumentParser(description=__doc__)
    for key in ['scan', 'atlas', 'fnf', 'output']:
        p.add_argument('--' + key, type=Path, required=True)
    a = p.parse_args()
    if sha(a.fnf) != FNF_SHA:
        p.error('INPUT_MISSING: fixed original FnF release required')
    a.output.mkdir(parents=True, exist_ok=False)
    nodes = list(records(a.scan))
    usage(a.scan, a.atlas, a.output / 'usage', {})
    refs = json.loads((a.output / 'usage/references.json').read_text())
    internal = json.loads((a.output / 'usage/internal-references.json').read_text())
    summary = analyze(nodes, refs, internal, a.scan, a.atlas, a.fnf, a.output)
    design = json.loads(Path(__file__).with_name('fnf-gap-design.json').read_text())
    for dataset, data in design.items():
        save(a.output / (dataset + '.json'), {'schemaVersion': 1, 'dataset': dataset,
             **summary['envelope'], 'data': data})
    assert sha(a.fnf) == FNF_SHA
    save(a.output / 'summary.json', summary)
    print(json.dumps(summary, indent=2))


if __name__ == '__main__':
    main()
