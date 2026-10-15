import sys
from pathlib import Path
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'tools/api-atlas'))
from model import build_index, records, write_records


def row(artifact, release=0, digest='same'):
    return {'artifact': artifact, 'name': 'p/A', 'member': 'p/A.class', 'release': release,
            'java21Eligible': True, 'classSHA256': digest}


class ScanTests(unittest.TestCase):
    def test_duplicate_conflict_never_overwrites(self):
        artifacts = {a: {'role': 'RUNTIME_DEPENDENCY'} for a in ['a', 'b']}
        index, duplicates, _ = build_index([row('a', digest='a'), row('b', digest='b')], artifacts)
        self.assertNotIn('p/A', index)
        self.assertEqual(duplicates[0]['resolution'], 'AMBIGUOUS_PROVIDER')

    def test_explicit_server_overlay(self):
        artifacts = {'patch': {'role': 'PATCHED_MINECRAFT'}, 'base': {'role': 'MINECRAFT_INTERMEDIATE', 'platform': 'neoforge', 'filename': 'base-srg.jar'}}
        index, duplicates, _ = build_index([row('patch', digest='p'), row('base', digest='b')], artifacts)
        self.assertEqual(index['p/A']['artifact'], 'patch')
        self.assertEqual(duplicates[0]['resolution'], 'SERVER_PATCH_OVERLAY')

    def test_byte_identical_alias_explicit(self):
        artifacts = {a: {'role': 'RUNTIME_DEPENDENCY'} for a in ['a', 'b']}
        index, duplicates, _ = build_index([row('a'), row('b')], artifacts)
        self.assertEqual(duplicates[0]['resolution'], 'BYTE_IDENTICAL_ALIAS')
        self.assertEqual(len(index), 1)

    def test_java21_multi_release_selection(self):
        index, _, _ = build_index([row('a', 0), row('a', 21)], {'a': {'role': 'RUNTIME_DEPENDENCY'}})
        self.assertEqual(index['p/A']['release'], 21)

    def test_inactive_artifact_excluded_with_count(self):
        index, _, excluded = build_index([row('a')], {'a': {'activeServerResource': False}})
        self.assertEqual(index, {})
        self.assertEqual(excluded, {'a': 1})

    def test_deterministic_compressed_json(self):
        with tempfile.TemporaryDirectory() as temp:
            a, b = Path(temp) / 'a.gz', Path(temp) / 'b.gz'
            values = [{'z': 2, 'a': ['é', 1]}]
            write_records(a, values); write_records(b, values)
            self.assertEqual(a.read_bytes(), b.read_bytes())
            self.assertEqual(list(records(a)), values)


if __name__ == '__main__':
    unittest.main()
