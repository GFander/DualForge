import importlib.util,json,tempfile,unittest
from pathlib import Path
spec=importlib.util.spec_from_file_location('summary',Path(__file__).with_name('summarize.py'));s=importlib.util.module_from_spec(spec);spec.loader.exec_module(s)
class HistoryBindingTest(unittest.TestCase):
    def test_actual_child_sha_is_checked(self):
        with tempfile.TemporaryDirectory() as temp:
            path=Path(temp)/'child.json';path.write_text(json.dumps({'status':'PASS','compat':{'SHA256':'b'*64}}))
            parent={'status':'PASS','compat':{'SHA256':'a'*64},'regressions':{'smoke':{'status':'PASS','compatSha256':'a'*64,'result':str(path)}}}
            with self.assertRaisesRegex(AssertionError,'Historical result compat mismatch'):s.check_history(parent,'a'*64)
    def test_parent_link_sha_is_checked(self):
        with self.assertRaisesRegex(AssertionError,'Historical child compat mismatch'):
            s.check_history({'status':'PASS','regressions':{'smoke':{'status':'PASS','compatSha256':'b'*64,'result':'unused'}}},'a'*64)
    def test_missing_or_failed_child_never_passes(self):
        with self.assertRaises(AssertionError):s.check_history({'status':'FAIL'},'a'*64)
    def test_same_sha(self):s.check_history({'status':'PASS','compat':{'SHA256':'a'*64}},'a'*64)
if __name__=='__main__':unittest.main()
