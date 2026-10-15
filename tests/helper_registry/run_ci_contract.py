"""Compile the production stack verifier with three SHA-pinned ASM jars; no game installation."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import urllib.request
ROOT=Path(__file__).resolve().parents[2]

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--java-home',type=Path,required=True);p.add_argument('--libraries',type=Path);p.add_argument('--output',type=Path,default=ROOT/'out/helper-registry-ci');a=p.parse_args()
    a.output.mkdir(parents=True,exist_ok=False);deps=a.output/'deps';deps.mkdir();jars=[]
    for row in json.loads(Path(__file__).with_name('ci-inputs.json').read_text()):
        target=deps/row['name']
        if a.libraries:shutil.copyfile(next(a.libraries.rglob(row['name'])),target)
        else:
            with urllib.request.urlopen(row['url'],timeout=60) as source:target.write_bytes(source.read())
        assert hashlib.sha256(target.read_bytes()).hexdigest()==row['sha256'];jars.append(target)
    cp=os.pathsep.join(map(str,jars));classes=a.output/'classes';classes.mkdir();sources=[ROOT/'dualforge/src/main/java/io/github/gfander/dualforge/RegistryBytecodeVerifier.java',Path(__file__).with_name('BytecodeVerifierContract.java'),Path(__file__).with_name('FnFAdmissionExpectation.java'),Path(__file__).with_name('FnFAdmissionExpectationContract.java')]
    for name,args in [('javac',[a.java_home/'bin/javac','--release','21','-cp',cp,'-d',classes,*sources]),('java',[a.java_home/'bin/java','-Xverify:all','-cp',str(classes)+os.pathsep+cp,'io.github.gfander.dualforge.BytecodeVerifierContract'])]:
        cmd=list(map(str,args));(a.output/(name+'.command.json')).write_text(json.dumps({'argv':cmd,'cwd':str(ROOT)})+'\n')
        with (a.output/(name+'.stdout.log')).open('w') as out,(a.output/(name+'.stderr.log')).open('w') as err:r=subprocess.run(cmd,cwd=ROOT,stdout=out,stderr=err)
        (a.output/(name+'.exit.json')).write_text(json.dumps({'exitCode':r.returncode})+'\n');assert r.returncode==0,name+' failed'
    cmd=[str(a.java_home/'bin/java'),'-Xverify:all','-cp',str(classes),'io.github.gfander.dualforge.FnFAdmissionExpectationContract']
    (a.output/'fnf-expectation.command.json').write_text(json.dumps({'argv':cmd,'cwd':str(ROOT)})+'\n')
    r=subprocess.run(cmd,cwd=ROOT,capture_output=True,text=True)
    (a.output/'fnf-expectation.stdout.log').write_text(r.stdout);(a.output/'fnf-expectation.stderr.log').write_text(r.stderr)
    (a.output/'fnf-expectation.exit.json').write_text(json.dumps({'exitCode':r.returncode})+'\n')
    assert r.returncode==0 and r.stdout.strip()=='FNF EXPECTATION CONTRACT PASS positive=3 negatives=12'
    text=(a.output/'java.stdout.log').read_text();assert 'BYTECODE VERIFIER CONTRACT PASS' in text;print(text)
if __name__=='__main__':main()
