"""Read-only Supplier/link contracts and full-plan preservation on a supplied final JAR."""
import argparse
import importlib.util
import json
import os
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('w', ROOT/'tools/lib/unix_workflow.py')
w = importlib.util.module_from_spec(spec)
spec.loader.exec_module(w)


def main():
    p = argparse.ArgumentParser(description=__doc__)
    for name in ('jar', 'fnf', 'server-input', 'jdk-home', 'host-oracle', 'output'):
        p.add_argument('--'+name, type=Path, required=True)
    p.add_argument('--fixture-index', type=Path, action='append', required=True)
    a = p.parse_args()
    out = a.output.resolve()
    out.mkdir(parents=True, exist_ok=False)
    sources = sorted(Path(__file__).parent.glob('*.java'))
    fixtures = [json.loads(i.read_text()) for i in a.fixture_index]
    jars = [Path(i['artifacts']['fixture']['Path']) for i in fixtures]
    for f, i in zip(jars, fixtures):
        assert w.digest(f) == i['artifacts']['fixture']['SHA256']
    libs = sorted((a.server_input/'libraries').rglob('*.jar'))
    inputs = w.fingerprints([a.jar, a.fnf, a.host_oracle, *a.fixture_index, *jars, *sources, Path(__file__), *libs])
    w.save(out/'inputs.json', inputs)
    cp = os.pathsep.join(map(str, [a.jar.resolve(), *libs]))
    classes = out/'classes'
    classes.mkdir()
    result = {'status':'FAIL', 'compatSHA256':w.digest(a.jar), 'sourceCommit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()}
    try:
        w.native(out, 'javac', [a.jdk_home/'bin/javac','--release','21','-proc:none','-cp',cp,'-d',classes,*sources])
        base = [a.jdk_home/'bin/java','--add-opens','java.base/java.lang.invoke=ALL-UNNAMED','--add-exports','java.base/sun.security.util=ALL-UNNAMED','-cp',str(classes)+os.pathsep+cp]
        w.native(out, 'supplier', [*base,'io.github.gfander.dualforge.SupplierContract',a.fnf,a.host_oracle])
        text = (out/'supplier.stdout.log').read_text()
        match = re.search(r'SUPPLIER CONTRACT PASS assertions=(\d+) negatives=(\d+)', text)
        assert match and 'SUPPLIER_INDIVIDUAL_PASS count=67' in text
        result.update(individualSuppliers=67, assertions=int(match[1]), negativeCases=int(match[2]), plans=[])
        for f, i in zip(jars, fixtures):
            case=i['case']; directory=out/case; directory.mkdir()
            w.native(directory, 'plan', [*base,'io.github.gfander.dualforge.PlanContract',f,a.host_oracle,directory/'invalid-later-supplier.jar'])
            assert 'negativePlans=1' in (directory/'plan.stdout.log').read_text()
            record={'status':'PASS','case':case,'compatSHA256':w.digest(a.jar),'fixtureSHA256':w.digest(f),'negativePlans':1}
            w.save(directory/'result.json',record);result['plans'].append(record)
        w.unchanged(inputs)
        result['status']='PASS'
    finally:
        w.save(out/'result.json',result)
        print(json.dumps(result,indent=2))

if __name__ == '__main__':
    main()
