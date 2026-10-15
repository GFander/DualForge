import sys
from pathlib import Path
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'tools/api-atlas'))
from model import compare, remap_descriptor, resolve, public_class


def node(owner, fields=(), methods=(), parent=None, interfaces=()):
    return {'name': owner, 'artifact': owner + '.jar', 'access': 1, 'superclass': parent,
            'interfaces': list(interfaces), 'signature': None, 'innerClasses': [],
            'fields': list(fields), 'methods': list(methods)}


def member(owner, name, descriptor, access=1):
    return {'owner': owner, 'name': name, 'descriptor': descriptor, 'access': access, 'signature': None}


class MappingTests(unittest.TestCase):
    def test_complete_descriptor_identity(self):
        d = '([[Lold/X;Ljava/lang/String;)Lold/X;'
        self.assertEqual(remap_descriptor(d, {'old/X': 'new/Y'}), '([[Lnew/Y;Ljava/lang/String;)Lnew/Y;')

    def test_super_method_resolution(self):
        method = member('Base', 'f', '()V')
        index = {'Base': node('Base', methods=[method]), 'Child': node('Child', parent='Base')}
        self.assertEqual(resolve(index, 'METHOD', 'Child', 'f', '()V'), [method])
        self.assertEqual(resolve(index, 'METHOD', 'Child', 'f', '(I)V'), [])

    def test_constructor_not_inherited(self):
        index = {'Base': node('Base', methods=[member('Base', '<init>', '()V')]), 'Child': node('Child', parent='Base')}
        self.assertEqual(resolve(index, 'METHOD', 'Child', '<init>', '()V'), [])

    def test_private_super_method_not_inherited(self):
        index = {'Base': node('Base', methods=[member('Base', 'f', '()V', 2)]), 'Child': node('Child', parent='Base')}
        self.assertEqual(resolve(index, 'METHOD', 'Child', 'f', '()V'), [])

    def test_interface_conflict_retained(self):
        index = {'A': node('A', methods=[member('A', 'f', '()V')]),
                 'B': node('B', methods=[member('B', 'f', '()V')]), 'C': node('C', interfaces=['A', 'B'])}
        self.assertEqual(len(resolve(index, 'METHOD', 'C', 'f', '()V')), 2)

    def test_public_inner_in_inaccessible_outer_not_api(self):
        outer, inner = node('Outer'), node('Outer$Inner')
        outer['access'] = 0
        inner['innerClasses'] = [{'name': 'Outer$Inner', 'access': 1, 'outerName': 'Outer'}]
        self.assertFalse(public_class(inner, {'Outer': outer, 'Outer$Inner': inner}))

    def test_ambiguity_does_not_choose_target(self):
        source_owner = 'net/minecraftforge/Example'
        target_owner = 'net/neoforged/neoforge/Example'
        src = {source_owner: node(source_owner, methods=[member(source_owner, 'old', '()V')])}
        dst = {target_owner: node(target_owner, methods=[member(target_owner, 'a', '()V'), member(target_owner, 'b', '()V')])}
        result = next(r for r in compare(src, dst) if r['kind'] == 'METHOD')
        self.assertEqual(result['status'], 'AMBIGUOUS_MAPPING')
        self.assertEqual(len(result['targets']), 2)
        self.assertFalse(result['productionEnabled'])
        self.assertFalse(result['runtimeVerified'])

    def test_invocation_kind_change_is_visible(self):
        a, b = 'net/minecraftforge/Example', 'net/neoforged/neoforge/Example'
        src, dst = {a: node(a, methods=[member(a, 'f', '()V', 9)])}, {b: node(b, methods=[member(b, 'f', '()V', 1)])}
        rule = next(r for r in compare(src, dst) if r['kind'] == 'METHOD')
        self.assertEqual(rule['sourceInvocationKind'], 184)
        self.assertEqual(rule['targetInvocationKinds'], [182])
        self.assertNotEqual(rule['strategy'], 'EXACT_ABI_CANDIDATE')


if __name__ == '__main__':
    unittest.main()
