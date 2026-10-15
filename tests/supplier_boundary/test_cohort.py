import sys, unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'helper_registry'))
from cohort import validate

class SupplierCohort(unittest.TestCase):
 def stages(self):
  contract={'status':'PASS','compatSHA256':'a'*64,'fixtureSHA256':'b'*64}
  control=dict(contract,observerSHA256='c'*64,installedJARs={'compat.jar':'a'*64,'fixture.jar':'b'*64,'observer.jar':'c'*64},fixtureEnvironment={'DF4C1_REGISTER':'false'})
  treatment=dict(control,fixtureEnvironment={'DF4C1_REGISTER':'true'})
  return contract,control,treatment
 def test_exact_new_condition(self):
  self.assertEqual(validate(*self.stages(),register_environment='DF4C1_REGISTER')['status'],'PASS')
 def test_wrong_condition_or_cohort_fails(self):
  for mutate in ['compat','fixture','installed','extra-condition','legacy-condition']:
   a,b,c=self.stages()
   if mutate=='compat':c['compatSHA256']='d'*64
   if mutate=='fixture':c['fixtureSHA256']='d'*64
   if mutate=='installed':c['installedJARs']={}
   if mutate=='extra-condition':c['fixtureEnvironment']={'DF4C1_REGISTER':'true','EXTRA':'true'}
   if mutate=='legacy-condition':c['fixtureEnvironment']={'DF4B_REGISTER':'true'}
   with self.subTest(mutate=mutate),self.assertRaises(ValueError):validate(a,b,c,register_environment='DF4C1_REGISTER')
if __name__=='__main__':unittest.main()
