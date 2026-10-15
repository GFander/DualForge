#!/usr/bin/env python3
"""Audit existing controlled rules against original official and inherited compat ABI."""
import argparse
import json
from pathlib import Path
from inputs import save, sha
from model import records, resolve

F = 'net/minecraftforge/'
N = 'net/neoforged/'
DR = F + 'registries/DeferredRegister'
ND = N + 'neoforge/registries/DeferredRegister'
RO = F + 'registries/RegistryObject'
DH = N + 'neoforge/registries/DeferredHolder'
BUS = '(L' + F + 'eventbus/api/IEventBus;)V'
NBUS = '(L' + N + 'bus/api/IEventBus;)V'
CTX = F + 'fml/javafmlmod/FMLJavaModLoadingContext'
BRIDGE = 'io/github/gfander/dualforge/ForgeConstructorContext'


def ref(owner, name=None, descriptor=None):
    return {'owner': owner, 'name': name, 'descriptor': descriptor}


def inventory(root, atlas, production, output):
    source = {n['name']: n for n in records(atlas / 'forge-abi.jsonl.gz')}
    target = {n['name']: n for n in records(atlas / 'neoforge-abi.jsonl.gz')}
    compat = list(records(production)); target.update({n['name']: n for n in compat})
    rules = []

    def rule(identity, kind, a, b, profile, contracts, scope='CONTROLLED_ENTRY_ONLY', limit='strict owned profile only'):
        def exists(index, r):
            return r['owner'] in index if kind == 'CLASS' else bool(resolve(index, kind, r['owner'], r['name'], r['descriptor']))
        source_exists, target_exists = exists(source, a), exists(target, b)
        assert source_exists and target_exists, (identity, source_exists, target_exists)
        rules.append({'id': identity, 'kind': kind, 'source': a, 'target': b, 'profile': profile,
            'scope': scope, 'contracts': contracts, 'limit': limit,
            'officialSourceSymbolExists': source_exists, 'targetSymbolExists': target_exists,
            'runtimeEvidence': 'INHERITED_007C3_FINAL; not rerun in 4A',
            'generalHelperEnabled': False})

    for old, new in [('ITEMS', 'ITEM'), ('BLOCKS', 'BLOCK')]:
        rule('controlled.registry.' + old, 'FIELD', ref(F+'registries/ForgeRegistries', old, 'L'+F+'registries/IForgeRegistry;'),
             ref('net/minecraft/core/registries/BuiltInRegistries', new, 'Lnet/minecraft/core/DefaultedRegistry;'),
             'Item/Block', ['005', '006A', '006B', '007B'])
    rule('controlled.registry.create', 'METHOD', ref(DR, 'create', '(L'+F+'registries/IForgeRegistry;Ljava/lang/String;)L'+DR+';'),
         ref(ND, 'create', '(Lnet/minecraft/core/Registry;Ljava/lang/String;)L'+ND+';'), 'Item/Block', ['005', '006A', '006B', '007B'])
    rule('controlled.registry.supplier', 'METHOD', ref(DR, 'register', '(Ljava/lang/String;Ljava/util/function/Supplier;)L'+RO+';'),
         ref(ND, 'register', '(Ljava/lang/String;Ljava/util/function/Supplier;)L'+DH+';'), 'Item/Block', ['005', '006A', '006B', '007B'])
    rule('controlled.registry.attach', 'METHOD', ref(DR, 'register', BUS), ref(ND, 'register', NBUS),
         'Item/Block', ['005', '006A', '006B', '007B'], limit='native injected constructor bus only; not Context BusView')
    rule('controlled.holder.get', 'METHOD', ref(RO, 'get', '()Ljava/lang/Object;'), ref(DH, 'get', '()Ljava/lang/Object;'),
         'Item/Block', ['005', '006A', '006B', '007B'])
    for a, b, identity in [(DR, ND, 'register-field-type'), (RO, DH, 'holder-field-type'),
                          (F+'eventbus/api/SubscribeEvent', N+'bus/api/SubscribeEvent', 'listener-annotation'),
                          (F+'eventbus/api/Event', N+'bus/api/Event', 'event-superclass')]:
        rule('controlled.'+identity, 'CLASS', ref(a), ref(b), 'Item/Block' if 'field-type' in identity else 'Event',
             ['005', '006A', '006B', '007B'] if 'field-type' in identity else ['003b', '004'],
             scope='CONTROLLED_ENTRY_ONLY' if 'field-type' in identity else 'CONTROLLED_HELPER',
             limit='typed generics/field uses are rewritten together' if 'field-type' in identity else 'same-file scan-owned subscriber/direct Event class only')
    rule('controlled.event.constructor', 'METHOD', ref(F+'eventbus/api/Event', '<init>', '()V'),
         ref(N+'bus/api/Event', '<init>', '()V'), 'Event', ['003b', '004'], scope='CONTROLLED_HELPER')
    rule('controlled.global.bus', 'FIELD', ref(F+'common/MinecraftForge', 'EVENT_BUS', 'L'+F+'eventbus/api/IEventBus;'),
         ref(N+'neoforge/common/NeoForge', 'EVENT_BUS', 'L'+N+'bus/api/IEventBus;'), 'Event', ['Probe', '003b', '004'])
    rule('controlled.event.register', 'METHOD', ref(F+'eventbus/api/IEventBus', 'register', '(Ljava/lang/Object;)V'),
         ref(N+'bus/api/IEventBus', 'register', '(Ljava/lang/Object;)V'), 'Event', ['004'], limit='verified nonempty listener only')
    rule('controlled.event.empty', 'METHOD', ref(F+'eventbus/api/IEventBus', 'register', '(Ljava/lang/Object;)V'),
         ref('io/github/gfander/dualforge/ForgeEmptyRegistration', 'register', '(L'+N+'bus/api/IEventBus;Ljava/lang/Object;)V'),
         'Event', ['Probe', '003b'], limit='verified empty listener only; interface call becomes static receiver argument')
    rule('controlled.context.get', 'METHOD', ref(CTX, 'get', '()L'+CTX+';'), ref(BRIDGE, 'get', '()L'+BRIDGE+';'),
         'Context', ['007C-3'], limit='owned unique noarg constructor only')
    rule('controlled.context.bus', 'METHOD', ref(CTX, 'getModEventBus', '()L'+F+'eventbus/api/IEventBus;'),
         ref(BRIDGE, 'getModEventBus', '()L'+F+'eventbus/api/IEventBus;'), 'Context', ['007C-3'],
         limit='source-ABI view; neither native bus identity nor general Forge EventBus operations')
    source_files = [root/'dualforge/src/main/java/io/github/gfander/dualforge'/name for name in [
        'DualForgeEntryTransformer.java', 'ControlledForgeItemProfile.java', 'ControlledForgeBlockProfile.java',
        'ControlledForgeContextProfile.java', 'ForgeConstructorContext.java', 'ForgeEntryProfile.java', 'ForgeEmptyRegistration.java']]
    save(output, {'schemaVersion': 1, 'productionCodeReadOnly': True, 'rules': rules,
        'sourceFingerprints': [{'path': p.relative_to(root).as_posix(), 'sha256': sha(p)} for p in source_files],
        'compiledCompatArtifact': compat[0]['artifact'],
        'constructorABIRule': {'source': BUS, 'target': NBUS, 'scope': 'CONTROLLED_ENTRY_ONLY',
                               'contracts': ['Probe', '003b', '004', '005', '006A', '006B', '007B']},
        'compiledAPIStringTokens': sorted({r['owner'] for n in compat for r in n['references']
            if r['kind']=='STRING' and ('net/minecraftforge/' in r['owner'] or 'net/neoforged/' in r['owner'])}),
        'generalizationDecision': 'candidate only; no existing entry-only rule grants FnF helpers a transformation scope'})
    return {'status': 'PASS', 'rules': len(rules), 'sourceFiles': len(source_files)}


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--root', type=Path, required=True)
    p.add_argument('--atlas', type=Path, required=True)
    p.add_argument('--production', type=Path, required=True)
    p.add_argument('--output', type=Path, required=True)
    a = p.parse_args()
    print(json.dumps(inventory(a.root, a.atlas, a.production, a.output)))
