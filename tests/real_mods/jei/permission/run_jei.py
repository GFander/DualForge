"""Measure the original JEI's actual first Neo failure with a supplied final artifact."""
import argparse
import json
from pathlib import Path
import subprocess
import sys
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'mouse_tweaks'))
from support import ENV,JDK,ROOT,fresh,record,save,sha

def main():
    p=argparse.ArgumentParser(description=__doc__)
    for name in ['jar','output']:p.add_argument('--'+name,type=Path,required=True)
    a=p.parse_args();out=fresh(a.output)
    original=ENV/'runs/convergence-010a-jei/inputs/jei-1.21.1-forge-19.27.0.340.jar'
    assert sha(original)=='f5e61a001d57dd044d6f2ba255daf955c50d90e4173f8b4933ab0eb8cd386ed5'
    run=out/'runtime'
    command=list(map(str,[sys.executable,'-B',ROOT/'tests/real_mods/clumps/run_server.py','--host','neoforge','--install',ENV/'neoforge-21.1.256','--jdk-home',JDK,'--output',run,'--mod',a.jar,'--mod',original]))
    save(out/'command.json',{'argv':command,'cwd':str(out)});proc=subprocess.run(command,cwd=out,capture_output=True)
    (out/'driver.stdout.log').write_bytes(proc.stdout);(out/'driver.stderr.log').write_bytes(proc.stderr);save(out/'driver.exit.json',{'exitCode':proc.returncode})
    state=json.loads((run/'result.json').read_text());text=(run/'stdout.log').read_text(errors='replace');error=(run/'stderr.log').read_text(errors='replace')
    assert proc.returncode==1 and state['state']=='EXITED_BEFORE_DONE' and state['exitCode']==1,state
    assert 'selected=DUAL_BUS' in text and 'modId=jei' in text
    prefix='Dual bus rejected: UNVERIFIED_NETWORK_INITIALIZATION'
    assert error.startswith('Exception in thread "main" java.lang.IllegalStateException: '+prefix+' '),error
    assert 'UNVERIFIED_PLAYER_INVENTORY' in error and 'permission=STATIC_VERIFIED' in error and 'playerIdentity=STATIC_DATAFLOW_ONLY' in error and 'originalProcess=RETAINED' in error and 'globalConnection=STATIC_VERIFIED' in error and 'originalSetter=RETAINED' in error and 'sourceClinit=STATIC_VERIFIED' in error
    assert 'combined=REJECTED_NO_TARGETS' in error and 'source=mezz/jei/common/network/packets/PacketDeletePlayerItem' in error
    assert 'ForgeDualBusPlan.rejectLocalBoundary' in error and 'DualForgeEntryTransformer.targets' in error
    assert 'DUAL_BUS_TRANSFORM class=mezz/jei/' not in text and 'DUAL_BUS_PREPLAN modId=jei' not in text
    mods=[m for m in json.loads((run/'installed-inputs.json').read_text()) if m['path'].startswith('mods/')]
    assert len(mods)==2 and {m['sha256'] for m in mods}=={sha(a.jar),sha(original)}
    assert all(sha(run/'server'/m['path'])==m['sha256'] for m in mods)
    first=error.splitlines()[0]
    stages={'Discovery':'REAL_RUNTIME_PASS','Metadata':'REAL_RUNTIME_PASS','EntrySelection':'REAL_RUNTIME_PASS_DUAL_BUS',
      'LocaleInitializerPlan':'STATIC_VERIFIED_IN_LIVE_PREPLAN','BusIdentityPlan':'STATIC_VERIFIED_IN_LIVE_PREPLAN',
      'EventSemanticPlan':'BLOCKED_NO_EXPLICIT_SUBSCRIPTIONS','ConfigPlan':'STATIC_VERIFIED_IN_LIVE_PREPLAN',
      'GlobalConnectionPrefix':'STATIC_VERIFIED_IN_LIVE_PREPLAN_ORIGINAL_SETTER_RETAINED',
      'SourceClinit':'STATIC_VERIFIED_IN_LIVE_PREPLAN',
      'SourceContextPlan':'FINITE_ADD_PROFILE_ONLY_ORIGINAL_DERIVED_HANDLER_NOT_APPROVED',
      'DerivedPayloadTypePlan':'STATIC_VERIFIED_FIRST_PACKET_ONLY',
      'CodecPlan':'STATIC_VERIFIED_FIRST_ITEMSTACK_PACKET_ONLY',
      'PermissionPlan':'STATIC_VERIFIED_ORIGINAL_NATIVE_PREDICATE_RETAINED',
      'PlayerIdentity':'STATIC_DATAFLOW_ONLY_REAL_CONNECTED_PLAYER_NOT_RUN',
      'HandlerPlan':'BLOCKED_UNVERIFIED_PLAYER_INVENTORY',
      'PayloadDefinitionPlan':'BLOCKED_INCOMPLETE_BUSINESS_CLOSURE',
      'ChannelOperationPlan':'NOT_IMPLEMENTED',
      'NetworkPlan':'BLOCKED_FULL_ORIGINAL_CLOSURE','DistBoundary':'NOT_REACHED','CombinedConstructorPlan':'REAL_RUNTIME_FAIL','Transformation':'NOT_RUN','ClassDefinition':'NOT_RUN','ModConstruction':'NOT_RUN','Lifecycle':'NOT_RUN'}
    save(out/'result.json',{'status':'PASS_EXPECTED_REFUSAL','JEI_COMPATIBILITY':'PARTIAL','jar':record(a.jar),'original':record(original),
      'firstFailure':first,'stages':stages,'runtime':state,'clientZIP':'010G_JEI_CLIENT_ZIP=NOT_PRODUCED'})
    print(first+'; JEI_COMPATIBILITY=PARTIAL')
if __name__=='__main__':main()
