"""Bind final evidence to actual bytes; successful refusal never becomes JEI runtime PASS."""
import argparse
import json
from pathlib import Path
import re
import sys
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'mouse_tweaks'))
from support import ENV,ROOT,record,sha

def read(path):
    return json.loads(Path(path).read_text())

def main():
    p=argparse.ArgumentParser(description=__doc__)
    for key in ['jar','contracts','live','regression','mouse','boundary','source-snapshot','output']:
        p.add_argument('--'+key,type=Path,required=True)
    a=p.parse_args(); digest=sha(a.jar); evidence=[]
    assert a.output.resolve().is_relative_to(ENV/'runs') and not a.output.exists()
    for folder,key in [(a.contracts,'jar'),(a.live,'jar'),(a.regression,'compat'),(a.mouse,'compat')]:
        value=read(folder/'result.json');assert value['status']=='PASS',folder
        assert value[key]['sha256']==digest,folder
        evidence.append(record(folder/'result.json'))
    assert read(a.regression/'result.json')['runtimeGroups']==18
    # Every actual runtime must retain the installed immutable treatment bytes.
    for folder in [*(a.live/x for x in ['neo-default','neo-modified','neo-persisted']),
                   *(a.regression/read(a.regression/'runtime-invocations.json')[i]['name'] for i in range(18))]:
        result=read(folder/'result.json');assert result['lifecycle']=='PASS',folder
        inputs=read(folder/'installed-inputs.json')
        matches=[r for r in inputs if r['path']=='mods/dualforge.jar']
        if matches:assert matches[0]['sha256']==digest and sha(folder/'server/mods/dualforge.jar')==digest
        evidence.append(record(folder/'result.json'))
    for side in ['forge','neo']:
        for stage in ['default','modified','persisted']:
            folder=a.live/(side+'-'+stage)
            text=(folder/'stdout.log').read_text(errors='replace')
            assert text.count('DUAL_OBSERVER_PASS')==1
            expected='alpha=true,true beta=true,true' if stage=='default' else 'alpha=false,true beta=true,false'
            assert 'DUAL_CONFIG_OBSERVED '+expected in text
            if stage!='default':assert read(folder/'config-world-copy.json')['sourceUnchangedAfterRestart']
        for identity in ['busalpha','busbeta']:
            native=a.live/'forge-default/server/mods'/ (identity+'.jar')
            compat=a.live/'neo-default/server/mods'/ (identity+'.jar')
            assert sha(native)==sha(compat)
            evidence.extend([record(native),record(compat)])
    text=(a.contracts/'plan.stdout.log').read_text();match=re.search(r'DUAL_BUS_PLAN_PASS assertions=(\d+)',text);assert match
    for side in ['forge','neo']:assert 'NATIVE_BUS_API_PASS side='+side in (a.contracts/(side+'-native-api.stdout.log')).read_text()
    native=(a.live/'neo-default/stdout.log').read_text();assert 'DUAL_HOST_FAULT_MODEL_PASS scope=ISOLATED_HOST_OPERATION callerPath=NOT_EXERCISED' in native
    boundary=read(a.boundary);assert boundary['compat']['sha256']==digest and boundary['JEI_NEO_RUNTIME']=='FAIL_AT_PREPLAN' and boundary['noDone']
    assert 'UNKNOWN_CONSTRUCTOR_EFFECT' in boundary['firstFailure'] and 'MinecraftLocaleSupplier' in boundary['firstFailure']
    snapshot=read(a.source_snapshot);assert snapshot['jar']['sha256']==digest
    current={str(p.resolve()):sha(p) for p in (ROOT/'dualforge/src/main').rglob('*') if p.is_file()}
    assert current=={r['path']:r['sha256'] for r in snapshot['production']},'production source changed after build'
    value={'overall':'PARTIAL','SCOPED_AUTOMATED':'PASS','jar':record(a.jar),'planAssertions':int(match[1]),
        'dualOwnerRealRuntimeGroups':6,'existingRealRuntimeGroups':18,'sourceSnapshot':record(a.source_snapshot),
        'JEI_NEO_CURRENT_FIRST_FAILURE':boundary['firstFailure'],'JEI_COMPATIBILITY':'PARTIAL; full original entry remains rejected',
        'CLIENT_ZIP_STATUS':'010B_JEI_CLIENT_ZIP=NOT_PRODUCED','GUI':'NOT_RUN','CLIENT_CONFIG_LIFECYCLE':'NOT_RUN',
        'LIVING_DEATH_GAMEPLAY':'NOT_RUN; positive source callback/cancellation tests are diagnostic',
        'CUSTOM_BUS_SUPPORT':'NOT_IMPLEMENTED','rawEvidence':evidence}
    a.output.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n')
    print('010B SCOPED AUTOMATION PASS; OVERALL PARTIAL; ORIGINAL JEI STILL REJECTED')
if __name__=='__main__':main()
