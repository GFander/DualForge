"""Reject mixed 006A stage artifacts, environments and nested runtime checks."""
import copy,importlib.util,unittest
from pathlib import Path
spec=importlib.util.spec_from_file_location('multi',Path(__file__).with_name('run_multi_registry.py'));m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
def results():
    compat={'Path':'/run/dualforge.jar','SHA256':'a'*64};fixture={'Path':'/run/multi-registry-probe.jar','SHA256':'b'*64};driver={'Path':'/run/multi-registry-driver.jar','SHA256':'c'*64}
    value={s:{'status':'PASS','compat':copy.deepcopy(compat)} for s in ('contract','control','treatment','regression')}
    value['contract']['fixture']=fixture
    for s in ('control','treatment'):value[s].update(fixture=fixture,driver=driver,environment={'java':'pinned','flags':['new']},runtimeBytecode={'status':'PASS','compat':copy.deepcopy(compat),'fixture':fixture})
    return value
class CohortTest(unittest.TestCase):
    def test_consistent(self):self.assertTrue(m.consistent(results()))
    def test_each_stage_mismatch(self):
        for s in ('contract','control','treatment','regression'):
            with self.subTest(stage=s):
                value=results();value[s]['compat']['SHA256']='d'*64;self.assertFalse(m.consistent(value))
    def test_missing_or_unrun_stage(self):
        value=results();value.pop('regression');self.assertFalse(m.consistent(value))
        value=results();value['control']['status']='NOT RUN';self.assertFalse(m.consistent(value))
    def test_driver_fixture_and_environment(self):
        for key in ('driver','fixture','environment'):
            with self.subTest(key=key):
                value=results();value['treatment'][key]={'different':True};self.assertFalse(m.consistent(value))
    def test_nested_runtime(self):
        for key in ('compat','fixture','status'):
            with self.subTest(key=key):
                value=results();value['treatment']['runtimeBytecode'][key]={'different':True} if key!='status' else 'FAIL';self.assertFalse(m.consistent(value))
if __name__=='__main__':unittest.main()
