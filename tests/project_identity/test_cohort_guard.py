"""Prove the shared harness refuses duplicate/Legacy compatibility artifacts before copying."""
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
import zipfile

REPO = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('workflow',REPO / 'tools/lib/unix_workflow.py')
w = importlib.util.module_from_spec(spec)
spec.loader.exec_module(w)
LEGACY = json.loads((REPO / 'Dev Manuals/DualForge 命名统一迁移索引.json').read_text())['legacyModule']


class CohortGuardTest(unittest.TestCase):
    def test_native_input_and_driver_are_accepted(self):
        with tempfile.TemporaryDirectory() as temp:
            paths=[]
            for name in ('probe','driver'):
                path=Path(temp)/(name+'.jar')
                with zipfile.ZipFile(path,'w') as jar: jar.writestr('META-INF/MANIFEST.MF','Automatic-Module-Name: '+name+'\n')
                paths.append(path)
            w.validate_mod_cohort(paths[0],paths[1:])

    def test_legacy_and_current_artifacts_rejected_even_under_alias(self):
        with tempfile.TemporaryDirectory() as temp:
            native=Path(temp)/'native.jar'
            with zipfile.ZipFile(native,'w') as jar: jar.writestr('META-INF/MANIFEST.MF','Automatic-Module-Name: nativeprobe\n')
            for module in (LEGACY,'dualforge'):
                path=Path(temp)/'arbitrary-alias.jar'
                with zipfile.ZipFile(path,'w') as jar: jar.writestr('META-INF/MANIFEST.MF','Automatic-Module-Name: '+module+'\n')
                for mod, extra in ((path,()),(native,(path,))):
                    with self.subTest(module=module, role='input'), self.assertRaisesRegex(w.NotRunError,'Duplicate or Legacy'):
                        w.validate_mod_cohort(mod,extra)


if __name__ == '__main__': unittest.main()
