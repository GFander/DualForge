import json
from pathlib import Path
import sys
import tempfile
import unittest
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'tools/api-atlas'))
from analyze import usage, srg_names
from inputs import save
from model import write_records


class UsageTests(unittest.TestCase):
    def test_official_names_join_overloads_by_full_descriptor(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            with zipfile.ZipFile(root / 'mcp.zip', 'w') as z:
                z.writestr('config/joined.tsrg', 'tsrg2 obf srg id\na net/minecraft/src/C_1 1\n\ta ()I m_1_ 1\n\ta (I)I m_2_ 2\n')
            (root / 'moj.tsrg').write_text('net/minecraft/One a\n\tfirst ()I a\n\tsecond (I)I a\n')
            names = srg_names(root / 'mcp.zip', root / 'moj.tsrg')
            self.assertEqual(names[('net/minecraft/One', 'm_1_')], ['first'])
            self.assertEqual(names[('net/minecraft/One', 'm_2_')], ['second'])

    def test_unresolved_is_retained_and_not_counted_as_compatible(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp); atlas = root / 'atlas'; atlas.mkdir()
            for host in ['forge', 'neoforge', 'vanilla-minecraft']:
                write_records(atlas / (host + '-abi.jsonl.gz'), [])
            write_records(atlas / 'api-diff.jsonl.gz', [])
            ref = {'class': 'probe/Entry', 'method': 'run()V', 'instructionIndex': 7,
                   'owner': 'net/minecraftforge/unknown/Absent', 'name': 'go',
                   'descriptor': '()V', 'kind': 'METHOD', 'opcode': 184,
                   'artifact': 'fixture', 'context': 'instruction'}
            node = {'name': 'probe/Entry', 'artifact': 'fixture', 'superclass': 'java/lang/Object',
                    'methods': [], 'references': [ref]}
            write_records(root / 'usage.gz', [node])
            result = usage(root / 'usage.gz', atlas, root / 'output', {})
            self.assertEqual(result['usePointsByClassification'], {'UNRESOLVED': 1})
            self.assertEqual(result['newRuntimeVerifiedMappings'], 0)
            output = json.loads((root / 'output/references.json').read_text())[0]
            self.assertEqual(output['instructionIndex'], 7)
            self.assertEqual(output['descriptor'], '()V')

    def test_strings_are_blind_spot_evidence_not_static_class_uses(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp); atlas = root / 'atlas'; atlas.mkdir()
            for host in ['forge', 'neoforge', 'vanilla-minecraft']:
                write_records(atlas / (host + '-abi.jsonl.gz'), [])
            write_records(atlas / 'api-diff.jsonl.gz', [])
            node = {'name': 'probe/Entry', 'artifact': 'fixture', 'superclass': 'java/lang/Object',
                    'methods': [], 'references': [{'class': 'probe/Entry', 'method': 'run()V',
                    'instructionIndex': 0, 'owner': 'net.minecraftforge.Hidden', 'name': None,
                    'descriptor': None, 'kind': 'STRING', 'opcode': 18, 'artifact': 'fixture', 'context': 'ldc'}]}
            write_records(root / 'usage.gz', [node])
            result = usage(root / 'usage.gz', atlas, root / 'output', {})
            self.assertEqual(result['totalStaticExternalUsePoints'], 0)
            self.assertEqual(len(json.loads((root / 'output/dynamic-sites-and-strings.json').read_text())), 1)


if __name__ == '__main__':
    unittest.main()
