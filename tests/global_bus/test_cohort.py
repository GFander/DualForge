import copy,unittest
from global_cohort import validate
class GlobalCohort(unittest.TestCase):
 def receipts(self):
  a,b,c='a'*64,'b'*64,'c'*64
  contract={'status':'PASS','compatSHA256':a,'fixtureSHA256':b};base={**contract,'observerSHA256':c,'installedJARs':{'compat':a,'fixture':b,'observer':c}}
  return contract,{**base,'fixtureEnvironment':{'DF4C3_VARIANT':'combined','DF4C3_REGISTER':'false'}},{**base,'fixtureEnvironment':{'DF4C3_VARIANT':'combined','DF4C3_REGISTER':'true'}}
 def test_fixed_variant_cohort(self):self.assertEqual(validate(*self.receipts())['status'],'PASS')
 def test_variant_or_extra_environment_rejected(self):
  for env in [{'DF4C3_VARIANT':'signal','DF4C3_REGISTER':'true'},{'DF4C3_VARIANT':'combined','DF4C3_REGISTER':'true','EXTRA':'value'}]:
   contract,c,t=self.receipts();t['fixtureEnvironment']=env
   with self.assertRaises(ValueError):validate(contract,c,t)
 def test_different_build_rejected(self):
  contract,c,t=self.receipts();t['compatSHA256']='d'*64
  with self.assertRaises(ValueError):validate(contract,c,t)
if __name__=='__main__':unittest.main()
