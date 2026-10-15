"""Typed Registry full-plan ABI, callback closure and immutable refusal contract."""
import argparse,importlib.util,os,re,subprocess,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('w',ROOT/'tools/lib/unix_workflow.py');w=importlib.util.module_from_spec(spec);spec.loader.exec_module(w)
def main():
 p=argparse.ArgumentParser(description=__doc__)
 for n in ['jar','fixture','server-input','jdk-home','host-oracle','output']:p.add_argument('--'+n,type=Path,required=True)
 a=p.parse_args();a.output.mkdir(parents=True,exist_ok=False);libs=sorted((a.server_input/'libraries').rglob('*.jar'));source=Path(__file__).with_name('TypedRegistryContract.java');oracle=ROOT/'tests/supplier_boundary/PinnedHostOracle.java';inputs=w.fingerprints([a.jar,a.fixture,a.host_oracle,source,oracle,Path(__file__),*libs]);w.save(a.output/'inputs.json',inputs);cp=os.pathsep.join(map(str,[a.jar,*libs]));c=a.output/'classes';c.mkdir();result={'status':'FAIL','compatSHA256':w.digest(a.jar),'fixtureSHA256':w.digest(a.fixture),'sourceCommit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()}
 try:
  w.native(a.output,'javac',[a.jdk_home/'bin/javac','--release','21','-proc:none','-cp',cp,'-d',c,source,oracle]);w.native(a.output,'contract',[a.jdk_home/'bin/java','--add-opens','java.base/java.lang.invoke=ALL-UNNAMED','--add-exports','java.base/sun.security.util=ALL-UNNAMED','-cp',str(c)+os.pathsep+cp,'io.github.gfander.dualforge.TypedRegistryContract',a.fixture,a.host_oracle,a.output/'bytes']);match=re.search(r'TYPED REGISTRY CONTRACT PASS assertions=(\d+) negatives=(\d+)',(a.output/'contract.stdout.log').read_text());assert match;w.unchanged(inputs);result.update(status='PASS',assertions=int(match[1]),negatives=int(match[2]))
 finally:w.save(a.output/'result.json',result);print(json.dumps(result,indent=2))
if __name__=='__main__':main()
