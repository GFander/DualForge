import hashlib
from pathlib import Path
import tempfile
import unittest

from relocation_protection import protect


class RelocationProtectionTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.old = 'spike000/probe/build.gradle'
        self.new = 'history/early-spikes/forge-probe-000/build.gradle'
        self.other = 'history/frozen.txt'
        self.digest = hashlib.sha256(b'frozen').hexdigest()
        self.items = [{'path': self.old, 'sha256': self.digest}, {'path': self.other, 'sha256': self.digest}]
        self.moves = [{'oldPath': self.old, 'newPath': self.new, 'sha256': self.digest}]
        self.tracked = {self.new, self.other}
        for name in self.tracked:
            p = self.root / name
            p.parent.mkdir(parents=True, exist_ok=True)
            p.write_bytes(b'frozen')

    def run_check(self):
        return protect(self.root, self.items, self.moves, self.tracked)

    def test_valid_relocation(self):
        self.assertEqual(self.run_check(), {self.new: self.old, self.other: self.other})

    def test_unmapped_missing(self):
        (self.root / self.other).unlink()
        with self.assertRaisesRegex(AssertionError, 'Missing protected file'):
            self.run_check()

    def test_changed_bytes(self):
        (self.root / self.new).write_bytes(b'changed')
        with self.assertRaisesRegex(AssertionError, 'Protected bytes changed'):
            self.run_check()

    def test_duplicate_target(self):
        self.moves.append({'oldPath': self.other, 'newPath': self.new, 'sha256': self.digest})
        with self.assertRaisesRegex(AssertionError, 'Duplicate relocation target'):
            self.run_check()

    def test_unauthorized_target(self):
        self.moves[0]['newPath'] = 'history/wrong/build.gradle'
        with self.assertRaisesRegex(AssertionError, 'Unauthorized relocation target'):
            self.run_check()

    def test_old_and_new_tracked(self):
        self.tracked.add(self.old)
        with self.assertRaisesRegex(AssertionError, 'both tracked'):
            self.run_check()

    def test_omitted_mapping(self):
        self.items.append({'path': 'spike001b5/probe/Probe.java', 'sha256': self.digest})
        with self.assertRaisesRegex(AssertionError, 'item omitted'):
            self.run_check()

    def test_missing_whole_mapping(self):
        self.moves.clear()
        with self.assertRaisesRegex(AssertionError, 'Missing protected tracked file'):
            self.run_check()

    def test_wrong_recorded_hash(self):
        self.moves[0]['sha256'] = '0' * 64
        with self.assertRaisesRegex(AssertionError, 'hash differs'):
            self.run_check()

    def test_extra_metadata_rejected(self):
        self.moves[0]['note'] = 'unapproved identity text'
        with self.assertRaisesRegex(AssertionError, 'Unknown relocation metadata'):
            self.run_check()

    def test_untracked_target(self):
        self.tracked.remove(self.new)
        with self.assertRaisesRegex(AssertionError, 'Missing protected tracked file'):
            self.run_check()

    def test_path_traversal(self):
        self.moves[0]['newPath'] = '../outside'
        with self.assertRaisesRegex(AssertionError, 'Unsafe relocation path'):
            self.run_check()

    def test_unchanged_original_layout(self):
        p = self.root / self.old
        p.parent.mkdir(parents=True)
        p.write_bytes(b'frozen')
        result = protect(self.root, self.items, [], {self.old, self.other})
        self.assertEqual(result, {self.old: self.old, self.other: self.other})


if __name__ == '__main__':
    unittest.main()
