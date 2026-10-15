import sys,unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'helper_registry'))
from fnf_receipt import validate_fnf_admission,PASS_LINE
from cohort import validate
class TypedReceipts(unittest.TestCase):
 def test_exact_typed_refusal(self):
  text='FNF_CONTRACT_VERSION=typed-v1\nFNF_PLAN_ADMISSION=REJECTED reason=Registry helper rejected: GLOBAL_EVENT_BUS_UNSUPPORTED\n'+PASS_LINE
  self.assertIn('GLOBAL_EVENT_BUS_UNSUPPORTED',validate_fnf_admission(text,'typed-v1'))
  for changed in [text.replace('typed-v1','supplier-v1'),text.replace('GLOBAL_EVENT_BUS_UNSUPPORTED','ITEM_FIELD_DESCRIPTOR'),text+'\nFNF_CONTRACT_VERSION=typed-v1',text.replace('REJECTED','ADMITTED')]:
   with self.subTest(changed=changed),self.assertRaises(AssertionError):validate_fnf_admission(changed,'typed-v1')
 def test_legacy_rejects_conflicting_version(self):
  text='FNF_CONTRACT_VERSION=typed-v1\nFNF_PLAN_ADMISSION=REJECTED reason=Registry helper rejected: SUPPLIER_BUSINESS_UNSUPPORTED\n'+PASS_LINE
  with self.assertRaises(AssertionError):validate_fnf_admission(text,'legacy-4b')
 def test_typed_cohort(self):
  c={'status':'PASS','compatSHA256':'a'*64,'fixtureSHA256':'b'*64};a=dict(c,observerSHA256='c'*64,installedJARs={'compat.jar':'a'*64,'fixture.jar':'b'*64,'observer.jar':'c'*64},fixtureEnvironment={'DF4C2_REGISTER':'false'});b=dict(a,fixtureEnvironment={'DF4C2_REGISTER':'true'})
  self.assertEqual(validate(c,a,b,register_environment='DF4C2_REGISTER')['status'],'PASS');b['compatSHA256']='d'*64
  with self.assertRaises(ValueError):validate(c,a,b,register_environment='DF4C2_REGISTER')
 def test_wrong_condition(self):
  c={'status':'PASS','compatSHA256':'a'*64,'fixtureSHA256':'b'*64};a=dict(c,observerSHA256='c'*64,installedJARs={'compat.jar':'a'*64,'fixture.jar':'b'*64,'observer.jar':'c'*64},fixtureEnvironment={'DF4C1_REGISTER':'false'});b=dict(a,fixtureEnvironment={'DF4C1_REGISTER':'true'})
  with self.assertRaises(ValueError):validate(c,a,b,register_environment='DF4C2_REGISTER')
if __name__=='__main__':unittest.main()
