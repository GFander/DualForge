"""The ASM/JVM contract is explicit; Python CI checks the candidate approval boundary."""
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]


class ApprovalBoundaryTests(unittest.TestCase):
    def test_registry_bus_rule_cannot_be_auto_enabled(self):
        seeds = json.loads((ROOT / 'tools/api-atlas/mappings/seeds.json').read_text())
        self.assertFalse(seeds['productionEnabled'])
        self.assertFalse(seeds['runtimeVerified'])
        rule = next(r for r in seeds['rules'] if r['id'] == 'registry.attach.mod.bus')
        self.assertEqual(rule['strategy'], 'RUNTIME_SHIM_REQUIRED')
        self.assertIn('net/minecraftforge/eventbus/api/IEventBus', rule['source']['descriptor'])
        self.assertIn('net/neoforged/bus/api/IEventBus', rule['target']['descriptor'])

    def test_seed_symbol_identities_are_unique(self):
        seeds = json.loads((ROOT / 'tools/api-atlas/mappings/seeds.json').read_text())['rules']
        identities = [(r['kind'], r['source']['owner'], r['source']['name'], r['source']['descriptor']) for r in seeds]
        self.assertEqual(len(identities), len(set(identities)))
        for rule in seeds:
            self.assertEqual(rule['status'], 'SEMANTIC_REVIEW_REQUIRED')
            self.assertNotIn('<exact', rule['source'].get('descriptor') or '')
            self.assertNotIn('<exact', rule['target'].get('descriptor') or '')


if __name__ == '__main__':
    unittest.main()
