"""Original FnF read-only whole-plan validation; no source class is defined."""
import argparse,importlib.util,json,os,subprocess
from pathlib import Path
from global_receipt import validate
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('w',ROOT/'tools/lib/unix_workflow.py');w=importlib.util.module_from_spec(spec);spec.loader.exec_module(w)
def main():
 p=argparse.ArgumentParser(description=__doc__)
 for n in ['jar','fnf','host-oracle','server-input','jdk-home','output']:p.add_argument('--'+n,type=Path,required=True)
 a=p.parse_args();assert w.digest(a.fnf)=='bd415b04529c4ffc31e14757488f71dab6917e4560c61e236c3c4d0de6557958';a.output.mkdir(parents=True,exist_ok=False);libs=sorted((a.server_input/'libraries').rglob('*.jar'));source=Path(__file__).with_name('FnFGlobalAdmission.java');oracle=ROOT/'tests/supplier_boundary/PinnedHostOracle.java';inputs=w.fingerprints([a.jar,a.fnf,a.host_oracle,source,oracle,Path(__file__),Path(__file__).with_name('global_receipt.py'),*libs]);w.save(a.output/'inputs.json',inputs);cp=os.pathsep.join(map(str,[a.jar,*libs]));c=a.output/'classes';c.mkdir();result={'status':'FAIL','compatSHA256':w.digest(a.jar),'fnfSHA256':w.digest(a.fnf),'fnfRuntime':'NOT RUN','sourceCommit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()}
 try:
  w.native(a.output,'javac',[a.jdk_home/'bin/javac','--release','21','-proc:none','-cp',cp,'-d',c,source,oracle]);w.native(a.output,'fnf-plan',[a.jdk_home/'bin/java','--add-opens','java.base/java.lang.invoke=ALL-UNNAMED','--add-exports','java.base/sun.security.util=ALL-UNNAMED','-cp',str(c)+os.pathsep+cp,'io.github.gfander.dualforge.FnFGlobalAdmission',a.fnf,a.host_oracle,a.output/'bytes']);result.update(validate((a.output/'fnf-plan.stdout.log').read_text()));w.unchanged(inputs);result['status']='PASS'
 finally:w.save(a.output/'result.json',result);print(json.dumps(result,indent=2))
if __name__=='__main__':main()
