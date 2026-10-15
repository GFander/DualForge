import copy
from pathlib import Path
import sys
import unittest
sys.path.insert(0,str(Path(__file__).parent))
from cohort import validate

class CohortTest(unittest.TestCase):
    def setUp(self):
        self.contract={'status':'PASS','compatSHA256':'a'*64,'fixtureSHA256':'b'*64}
        self.control={**self.contract,'installedJARs':{'compat':'a'*64,'fixture':'b'*64},'observerSHA256':'c'*64,'fixtureEnvironment':{'DF4B_REGISTER':'false'}}
        self.treatment=copy.deepcopy(self.control);self.treatment['fixtureEnvironment']={'DF4B_REGISTER':'true'}
    def test_matching(self):self.assertEqual(validate(self.contract,self.control,self.treatment)['status'],'PASS')
    def test_different_build(self):
        self.contract['compatSHA256']='d'*64
        with self.assertRaisesRegex(ValueError,'compatSHA256'):validate(self.contract,self.control,self.treatment)
    def test_different_fixture(self):
        self.treatment['fixtureSHA256']='d'*64
        with self.assertRaisesRegex(ValueError,'fixtureSHA256'):validate(self.contract,self.control,self.treatment)
    def test_different_observer(self):
        self.treatment['observerSHA256']='d'*64
        with self.assertRaisesRegex(ValueError,'Observer'):validate(self.contract,self.control,self.treatment)
    def test_incomplete(self):
        self.control['status']='NOT RUN'
        with self.assertRaisesRegex(ValueError,'not PASS'):validate(self.contract,self.control,self.treatment)
    def test_different_installed_jar(self):
        self.control['installedJARs']['extra']='d'*64
        with self.assertRaisesRegex(ValueError,'Installed'):validate(self.contract,self.control,self.treatment)
    def test_extra_condition(self):
        self.control['fixtureEnvironment']['OTHER']='true'
        with self.assertRaisesRegex(ValueError,'condition'):validate(self.contract,self.control,self.treatment)
