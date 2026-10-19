"""Bind selected final evidence to exact production inputs and artifact; no inherited PASS."""
import argparse
import json
from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'mouse_tweaks'))
from support import ENV,ROOT,record,save,sha

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--run',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();r=a.run.resolve();assert r.is_relative_to(ENV/'runs') and not a.output.exists()
    c=r/'cohort-final';jar=r/'build-3/dualforge.jar';digest=sha(jar);checks=[]
    def check(ok,label):
        assert ok,label
        checks.append(label)
    def read(path):return json.loads(path.read_text())
    check(read(c/'result.json')['status']=='PASS','serial final cohort complete')
    check(read(r/'regressions-current/result.json')['status']=='PASS','18 current regressions complete')
    for directory,key in [(r/'regressions-current','compat'),*( (c/n,'jar') for n in ['network-contract','network-boundary','network-live','constructor-contract','constructor-boundary','dual-contract','constructor-live','original-config-live','dual-live']), (c/'mouse','compat')]:
        result=read(directory/'result.json');check(result['status']=='PASS' and result[key]['sha256']==digest,str(directory.relative_to(r))+' final artifact binding')
    for file,text in [('network-contract/plan.stdout.log','NETWORK_PLAN_PASS assertions=136'),('network-boundary/boundary.stdout.log','NETWORK BOUNDARY CONTRACT PASS assertions=34'),('constructor-contract/plan.stdout.log','CONSTRUCTOR_PLAN_PASS assertions=212'),('constructor-boundary/boundary.stdout.log','CONSTRUCTOR BOUNDARY CONTRACT PASS assertions=34'),('dual-contract/plan.stdout.log','DUAL_BUS_PLAN_PASS assertions=128')]:check(text in (c/file).read_text(),text)
    for side in ['forge','neo']:check('NATIVE_BUS_API_PASS side='+side in (c/'dual-contract'/(side+'-native-api.stdout.log')).read_text(),side+' native Bus API')
    runtime=[]
    for label,item in read(c/'network-live/result.json')['results'].items():
        directory=c/'network-live'/label;runtime.append(directory);text=(directory/'stdout.log').read_text(errors='replace')
        check(text.count('NETWORK_OBSERVER_PASS')==1 and 'NETWORK_OBSERVER_FAIL' not in text,label+' actual network registration/codec observer')
        for id in ['netalpha','netbeta']:
            installed=directory/'server/mods'/(id+'.jar');check(sha(installed)==sha(r/'fixture-build-2'/id/(id+'.jar')),label+' same original Forge '+id)
        if label.startswith('neo'):
            for token in ['NETWORK_BRIDGE_HOST_FAULT_PASS','REGISTRATION_TOO_EARLY','WRONG_REGISTRATION_PHASE','DUPLICATE_REGISTRATION','WRONG_MODULE','WRONG_CLASSLOADER','WRONG_CHANNEL_OWNER','REGISTRATION_TOO_LATE','NETWORK_NATIVE_PARTIAL','NETWORK_NEGOTIATION_PASS']:
                check(token in text,label+' '+token)
    for name in ['constructor-live','original-config-live','dual-live']:
        result=read(c/name/'result.json');check(len(result['results'])==6,name+' six real fresh game runs')
        for label in result['results']:runtime.append(c/name/label)
    for directory in (r/'regressions-current').rglob('result.json'):
        if directory.parent.name=='regressions-current':continue
        result=read(directory)
        if result.get('lifecycle')=='PASS':runtime.append(directory.parent)
    runtime.append(c/'dual-owner');runtime=sorted(set(runtime))
    for directory in runtime:
        check(read(directory/'result.json').get('lifecycle')=='PASS',str(directory.relative_to(r))+' lifecycle')
        for row in read(directory/'installed-inputs.json'):
            if not row['path'].startswith('mods/'):continue
            check(sha(directory/'server'/row['path'])==row['sha256'],str(directory.relative_to(r))+' installed input unchanged '+row['path'])
            if row['path'].endswith('/dualforge.jar'):check(row['sha256']==digest,str(directory.relative_to(r))+' installed final JAR')
    owner=(c/'dual-owner/stdout.log').read_text(errors='replace');check('CONFIG_OWNER_MISMATCH_B_TO_A' in owner and 'DUAL_HOST_FAULT_MODEL_PASS' in owner,'old owner/config/partial-error negatives retained')
    helper=(c/'helper-ci-driver.stdout.log').read_text(errors='replace');check('BYTECODE VERIFIER CONTRACT PASS assertions=10' in helper,'CI helper and FnF strict closure contract')
    local=read(c/'local/result.json');check(local['pythonTests']==106 and local['protectedFiles']==374 and local['zshScripts']==25,'106 Python + 374 protected + 25 zsh')
    jei=read(c/'jei/result.json');check(jei['status']=='PASS_EXPECTED_REFUSAL' and jei['jar']['sha256']==digest and 'UNKNOWN_NETWORK_SIDE_EFFECT' in jei['firstFailure'] and 'Internal.setServerConnection' in jei['firstFailure'],'actual original JEI precise network preplan refusal')
    source=read(r/'source-current.json')['source'];build=read(r/'build-3/build-inputs.json');built={row['Path']:row['SHA256'] for row in build}
    check(len(source)==39,'all 34 Java + 5 resource production inputs')
    for name,row in source.items():check(sha(ROOT/name)==row['sha256']==built[str(ROOT/name)],'production bytes match tested build '+name)
    for row in read(r/'historical-preservation.json'):check(sha(Path(row['path']))==row['sha256'],'historical evidence unchanged '+Path(row['path']).name)
    receipt=read(r/'intake/git-baseline.json')['userUntrackedFile'];check(sha(Path(receipt['path']))==receipt['sha256'],'user untracked manual report preserved')
    fields={'NETWORK_ABI_INVENTORY':'PASS','NETWORK_LIFECYCLE_CONTRACT':'PASS_SCOPED_PLAY_ADD_MAIN','PAYLOAD_TYPE_AND_CODEC':'PASS_SCOPED_INT_CODEC','HANDLER_IDENTITY':'PASS_REGISTRATION_IDENTITY_ONLY','PROTOCOL_VERSION_CONTRACT':'PARTIAL','PAYLOAD_DIRECTION_CONTRACT':'PASS_REGISTRATION_ONLY','OPTIONAL_CONNECTION_SEMANTICS':'PARTIAL','INDEPENDENT_FORGE_RUNTIME':'PASS','INDEPENDENT_NEO_RUNTIME':'PASS','REAL_CONNECTION':'NOT_RUN','REAL_PACKET_DELIVERY':'NOT_RUN','CLIENT_GUI':'NOT_RUN','JEI_NETWORK_CLOSURE':'BLOCKED','JEI_NEO_CURRENT_FIRST_FAILURE':jei['firstFailure'],'JEI_COMPATIBILITY':'PARTIAL','FNF_REGRESSION':'PASS','CLUMPS_REGRESSION':'PASS','MOUSE_TWEAKS_REGRESSION':'PASS_AUTOMATED_GUI_NOT_RUN','DUAL_BUS_REGRESSION':'PASS','CONSTRUCTOR_REGRESSION':'PASS','FINAL_DUALFORGE_SHA256':digest,'CLIENT_ZIP_STATUS':'010D_JEI_CLIENT_ZIP=NOT_PRODUCED'}
    save(a.output,{'SCOPED_AUTOMATED':'PASS','overall':'PARTIAL','jar':record(jar),'checks':checks,'checkCount':len(checks),'selectedSuccessfulGameRuns':len(runtime),'runtimeDirectories':list(map(str,runtime)),'fields':fields,'nonPassingScopes':['original JEI eager global state/add/Context/send/query/client/Dist closure','full connection protocol equivalence','real connection and actual packet delivery','GUI/Windows/real player behavior']})
    print('010D_ASSESSMENT_PASS checks='+str(len(checks))+' games='+str(len(runtime))+' overall=PARTIAL')
if __name__=='__main__':main()
