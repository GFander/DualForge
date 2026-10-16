import unittest
from global_receipt import VERSION,ADMISSION,RUNTIME,validate
class CompletePlanReceipts(unittest.TestCase):
 def receipt(self):return '\n'.join(['FNF_TARGET_VERIFIED='+n+' originalSHA256='+'a'*64+' outputSHA256='+'b'*64 for n in ['Entry','Items','Tabs']]+[VERSION,ADMISSION,RUNTIME])
 def test_exact_receipt(self):self.assertEqual(validate(self.receipt())['runtime'],'NOT RUN')
 def test_wrong_admission(self):
  for value in ['REJECTED','ACCEPTED targets=2 candidate=COMPOSED_CONTEXT globalListeners=0','ACCEPTED targets=3 candidate=COMPOSED_CONTEXT globalListeners=1']:
   with self.subTest(value=value),self.assertRaises(AssertionError):validate(self.receipt().replace(ADMISSION,'FNF_PLAN_ADMISSION='+value))
 def test_conflicting_version(self):
  with self.assertRaises(AssertionError):validate(self.receipt()+'\nFNF_CONTRACT_VERSION=typed-v1')
 def test_conflicting_admission(self):
  with self.assertRaises(AssertionError):validate(self.receipt()+'\n'+ADMISSION)
 def test_runtime_not_claimed(self):
  with self.assertRaises(AssertionError):validate(self.receipt().replace('FNF_RUNTIME=NOT RUN','FNF_RUNTIME=PASS'))
 def test_missing_target(self):
  with self.assertRaises(AssertionError):validate('\n'.join(self.receipt().splitlines()[1:]))
 def test_duplicate_target(self):
  with self.assertRaises(AssertionError):validate(self.receipt().replace('FNF_TARGET_VERIFIED=Tabs','FNF_TARGET_VERIFIED=Items'))
 def test_invalid_digest(self):
  with self.assertRaises(AssertionError):validate(self.receipt().replace('a'*64,'bad'))
 def test_exception_text_is_not_plan_success(self):
  with self.assertRaises(AssertionError):validate('java.lang.IllegalStateException: expected')
if __name__=='__main__':unittest.main()
