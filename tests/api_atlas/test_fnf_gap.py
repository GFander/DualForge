import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'tools/api-atlas'))
from fnf_gap import cfg, disposition, implementation, known_bootstrap, supplier_kind


class FnFGapTests(unittest.TestCase):
    def test_supplier_inventory_preserves_unknown(self):
        owned = {'sample/Custom': {'superclass': 'net/minecraft/world/item/Item'}}
        def body(*ins):
            return {'instructions': list(ins)}
        item = {'opcode': 187, 'type': 'net/minecraft/world/item/Item'}
        food = {'opcode': 182, 'owner': 'net/minecraft/world/item/Item$Properties', 'name': 'food'}
        self.assertEqual(supplier_kind(body(item), owned)[0], 'ORDINARY_ITEM')
        self.assertEqual(supplier_kind(body(item, food), owned)[0], 'ITEM_WITH_FOOD_PROPERTIES')
        self.assertEqual(supplier_kind(body({'opcode': 187, 'type': 'sample/Custom'}), owned),
                         ('CUSTOM_ITEM_SUBCLASS', ['sample/Custom']))
        self.assertEqual(supplier_kind(body(), owned)[0], 'UNRESOLVED')

    def test_bootstrap_requires_exact_abi(self):
        descriptor = ('(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;'
                      'Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;')
        handle = {'owner': 'java/lang/invoke/LambdaMetafactory', 'name': 'metafactory', 'tag': 6,
                  'descriptor': descriptor}
        self.assertTrue(known_bootstrap({'bootstrap': {'handle': handle}}))
        for key, wrong in [('owner', 'unknown/Bootstrap'), ('name', 'altMetafactory'),
                           ('tag', 5), ('descriptor', '()V')]:
            with self.subTest(key=key):
                self.assertFalse(known_bootstrap({'bootstrap': {'handle': dict(handle, **{key: wrong})}}))
        self.assertIsNone(implementation({'arguments': []}))

    def test_cfg_branches_exception_and_terminal(self):
        code = {'instructions': [{'opcode': 153, 'target': 2}, {'opcode': 177}, {'opcode': 191}],
                'exceptionTable': [{'start': 0, 'end': 1, 'handler': 2}]}
        self.assertEqual(cfg(code), [[0, 2, 'BRANCH'], [0, 1, 'FALLTHROUGH'], [0, 2, 'EXCEPTION']])

    def test_local_rule_never_grants_whole_fnf(self):
        ref = {'owner': 'net/minecraftforge/registries/RegistryObject', 'name': 'get',
               'descriptor': '()Ljava/lang/Object;', 'currentCompatibilityStatus': 'MAPPING_CANDIDATE'}
        self.assertEqual(disposition(ref), 'LOCAL_MATCH_GROUP_REJECTED')
        ref.update(owner='net/minecraftforge/common/MinecraftForge', name='EVENT_BUS')
        self.assertEqual(disposition(ref), 'RUNTIME_BRIDGE_REQUIRED')
        ref.update(owner='org/jetbrains/annotations/Nullable', currentCompatibilityStatus='UNRESOLVED')
        self.assertEqual(disposition(ref), 'UNRESOLVED')


if __name__ == '__main__':
    unittest.main()
