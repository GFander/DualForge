import copy
import unittest
from cohort import consistent

class BlockCohortTest(unittest.TestCase):
    def setUp(self):
        artifact={'SHA256':'a'*64};fixture={'SHA256':'b'*64};driver={'SHA256':'c'*64}
        value={'status':'PASS','compat':artifact,'fixture':fixture,'driver':driver,'environment':{'jdk':'fixed'},'runtimeBytecode':{'status':'PASS','compat':artifact,'fixture':fixture}}
        self.results={k:copy.deepcopy(value) for k in ('contract','control','treatment','regression')}
    def test_complete_cohort(self):self.assertTrue(consistent(self.results))
    def test_mixed_compat(self):
        self.results['treatment']['compat']={'SHA256':'d'*64};self.assertFalse(consistent(self.results))
    def test_other_fixture(self):
        self.results['control']['fixture']={'SHA256':'d'*64};self.assertFalse(consistent(self.results))
    def test_other_driver(self):
        self.results['treatment']['driver']={'SHA256':'d'*64};self.assertFalse(consistent(self.results))
    def test_other_environment(self):
        self.results['control']['environment']={'jdk':'different'};self.assertFalse(consistent(self.results))
    def test_mixed_runtime_checker(self):
        self.results['control']['runtimeBytecode']['compat']={'SHA256':'d'*64};self.assertFalse(consistent(self.results))
    def test_missing_regression(self):
        del self.results['regression'];self.assertFalse(consistent(self.results))
if __name__=='__main__':unittest.main()
