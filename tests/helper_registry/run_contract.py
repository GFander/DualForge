"""Run helper plan contracts with real FML metadata on one supplied compat JAR."""
import argparse
import importlib.util
import os
import re
import subprocess
from fnf_receipt import validate_fnf_admission
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('w',ROOT/'tools/lib/unix_workflow.py');w=importlib.util.module_from_spec(spec);spec.loader.exec_module(w)

def main():
    p=argparse.ArgumentParser(description=__doc__)
    for n in ('jar','fixture','server-input','jdk-home'):p.add_argument('--'+n,type=Path,required=True)
    p.add_argument('--fnf',type=Path)
    p.add_argument('--host-oracle',type=Path)
    p.add_argument('--fnf-contract-version',choices=['legacy-4b','supplier-v1'],default='legacy-4b')
    a=p.parse_args();
    if a.fnf and a.fnf_contract_version=='supplier-v1' and not a.host_oracle:p.error('supplier-v1 requires --host-oracle')
    out=w.run_directory(ROOT/'out/convergence-007c4b/contract');binary=w.tools(a,out)
    libs=sorted((a.server_input/'libraries').rglob('*.jar'));source=Path(__file__).with_name('HelperRegistryContract.java');inputs=w.fingerprints([a.jar,a.fixture,source,source.with_name('FnFRegistryAdmission.java'),source.with_name('FnFAdmissionExpectation.java'),source.with_name('FnFAdmissionExpectationContract.java'),source.with_name('fnf_receipt.py'),ROOT/'tests/supplier_boundary/PinnedHostOracle.java',*([a.host_oracle] if a.host_oracle else []),Path(__file__),*libs]);w.save(out/'inputs.json',inputs)
    cp=os.pathsep.join(map(str,[a.jar,*libs]));classes=out/'classes';classes.mkdir()
    result={'status':'FAIL','compatSHA256':w.digest(a.jar),'fixtureSHA256':w.digest(a.fixture),'sourceCommit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()}
    try:
        w.native(out,'javac',[binary/'javac','--release','21','-proc:none','-cp',cp,'-d',classes,source,source.with_name('FnFRegistryAdmission.java'),source.with_name('FnFAdmissionExpectation.java'),source.with_name('FnFAdmissionExpectationContract.java'),ROOT/'tests/supplier_boundary/PinnedHostOracle.java'])
        w.native(out,'fnf-expectation-contract',[binary/'java','-cp',str(classes),'io.github.gfander.dualforge.FnFAdmissionExpectationContract'])
        assert (out/'fnf-expectation-contract.stdout.log').read_text().strip()=='FNF EXPECTATION CONTRACT PASS positive=2 negatives=9'
        w.native(out,'contract',[binary/'java','--add-opens','java.base/java.lang.invoke=ALL-UNNAMED','--add-exports','java.base/sun.security.util=ALL-UNNAMED','-cp',str(classes)+os.pathsep+cp,'io.github.gfander.dualforge.HelperRegistryContract',a.fixture,out/'bytes'])
        summary=re.search(r'HELPER REGISTRY CONTRACT PASS assertions=(\d+) negatives=(\d+)',(out/'contract.stdout.log').read_text());assert summary;result.update(assertions=int(summary[1]),negativeCases=int(summary[2]));w.unchanged(inputs)
        if a.fnf:
            fnf_inputs=w.fingerprints([a.fnf]);w.save(out/'fnf-input.json',fnf_inputs)
            w.native(out,'fnf-admission',[binary/'java','--add-opens','java.base/java.lang.invoke=ALL-UNNAMED','--add-exports','java.base/sun.security.util=ALL-UNNAMED','-cp',str(classes)+os.pathsep+cp,'io.github.gfander.dualforge.FnFRegistryAdmission',a.fnf,*([a.host_oracle] if a.fnf_contract_version=='supplier-v1' and a.host_oracle else [])])
            result['fnfRejectionReason']=validate_fnf_admission((out/'fnf-admission.stdout.log').read_text(),a.fnf_contract_version);w.unchanged(fnf_inputs);result['fnfPlanAdmission']='REJECTED';result['fnfRuntime']='NOT RUN';result['fnfContractVersion']=a.fnf_contract_version
        result['status']='PASS'
    finally:w.save(out/'result.json',result);print(result['status'],out,flush=True)
if __name__=='__main__':main()
