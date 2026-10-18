"""Verify final-artifact receipts, native runs and refusal separately from JEI compatibility."""
import argparse
import json
from pathlib import Path
import re
import sys
import zipfile
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'mouse_tweaks'))
from support import ENV,ROOT,record,save,sha

def read(path):return json.loads(Path(path).read_text())

def main():
    p=argparse.ArgumentParser(description=__doc__)
    for name in ['run','output']:p.add_argument('--'+name,type=Path,required=True)
    a=p.parse_args();r=a.run.resolve();assert r.is_relative_to(ENV/'runs') and not a.output.exists()
    jar=r/'build-current/dualforge.jar';digest=sha(jar);checks=[]
    def check(condition,label):
        assert condition,label
        checks.append(label)
    for name,key in [('contracts-current','jar'),('dual-contract-current','jar'),('live-verified','jar'),('dual-live-current','jar'),('original-config-live','jar'),('boundary-atomic','jar'),('regressions-current','compat'),('mouse-current','compat')]:
        value=read(r/name/'result.json');check(value['status']=='PASS' and value[key]['sha256']==digest,name+' final artifact')
    check('CONSTRUCTOR_PLAN_PASS assertions=212' in (r/'contracts-current/plan.stdout.log').read_text(),'212 new bytecode assertions')
    check('DUAL_BUS_PLAN_PASS assertions=128' in (r/'dual-contract-current/plan.stdout.log').read_text(),'128 unchanged old bytecode assertions')
    check('CONSTRUCTOR BOUNDARY CONTRACT PASS assertions=34' in (r/'boundary-atomic/boundary.stdout.log').read_text(),'two dist reader/planner atomic refusal after complete earlier plan')
    for side in ['forge','neo']:
        check('NATIVE_BUS_API_PASS side='+side in (r/'dual-contract-current'/(side+'-native-api.stdout.log')).read_text(),side+' native API')
    runtimes=[]
    for matrix,ids in [('live-verified',['localpha','locbeta']),('dual-live-current',['busalpha','busbeta'])]:
        value=read(r/matrix/'result.json')
        for side in ['forge','neo']:
            for stage in ['default','modified','persisted']:
                name=side+'-'+stage;folder=r/matrix/name;runtimes.append(folder)
                text=(folder/'stdout.log').read_text(errors='replace')
                check(text.count('DUAL_OBSERVER_PASS')==1,matrix+'/'+name+' actual observer')
                expected='alpha=true,true beta=true,true' if stage=='default' else 'alpha=false,true beta=true,false'
                check('DUAL_CONFIG_OBSERVED '+expected in text,matrix+'/'+name+' actual config values')
                if matrix=='live-verified':
                    for marker in ['LOCALE_FIXTURE_PASS actualCounts=1,1','ORIGINAL_CONFIG_COMPONENT_PASS','ORIGINAL_LOCALE_COMPONENT_PASS']:
                        check(text.count(marker)==1,matrix+'/'+name+' '+marker)
                if stage!='default':check(read(folder/'config-world-copy.json')['sourceUnchangedAfterRestart'],matrix+'/'+name+' source copy preserved')
                configs=value['results'][name]['actualConfigs'];serverfile=ids[1]+'-server.toml'
                check(Path(configs[serverfile]['relativePath']).parent==Path(configs['dualobserver-server.toml']['relativePath']).parent,matrix+'/'+name+' native SERVER policy')
        for identity in ids:check(sha(r/matrix/'forge-default/server/mods'/(identity+'.jar'))==sha(r/matrix/'neo-default/server/mods'/(identity+'.jar')),matrix+'/'+identity+' same original Forge input')
    integration=read(r/'original-config-live/result.json')
    for side in ['forge','neo']:
        for stage in ['default','modified','persisted']:
            folder=r/'original-config-live'/(side+'-'+stage);runtimes.append(folder);text=(folder/'stdout.log').read_text(errors='replace')
            expected='true,true,false' if stage=='default' else 'false,true,false'
            check(text.count('ORIGINAL_CONFIG_INTEGRATION_PASS')==1 and 'actualFactoryRegistration=true values='+expected in text,side+'/'+stage+' original actual factory and interface values')
            check('DUAL_STARTED originalcfg count=1 setups=1 values='+expected in text,side+'/'+stage+' original config used by actual unchanged callback')
            if stage!='default':check(read(folder/'config-world-copy.json')['sourceUnchangedAfterRestart'],side+'/'+stage+' original component persistence source preserved')
            configs=integration['results'][side+'-'+stage]['actualConfigs']
            check(Path(configs['originalcfg-server.toml']['relativePath']).parent==Path(configs['configobserver-server.toml']['relativePath']).parent,side+'/'+stage+' original factory native SERVER policy')
    componentJar=r/'original-config-fixture/originalcfg.jar'
    check(sha(componentJar)==sha(r/'original-config-live/forge-default/server/mods/originalcfg.jar')==sha(r/'original-config-live/neo-default/server/mods/originalcfg.jar'),'same original component Forge input used on both hosts')
    original=ENV/'runs/convergence-010a-jei/inputs/jei-1.21.1-forge-19.27.0.340.jar'
    with zipfile.ZipFile(original) as release,zipfile.ZipFile(componentJar) as fixture:
        for name in ['mezz/jei/forge/config/ServerConfig.class','mezz/jei/common/config/IServerConfig.class']:
            check(release.read(name)==fixture.read(name),'unchanged original class bytes '+name)
    check(read(r/'regressions-current/result.json')['runtimeGroups']==18,'18 actual regression groups')
    runtimes.extend(r/'regressions-current'/item['name'] for item in read(r/'regressions-current/runtime-invocations.json'))
    runtimes.append(r/'owner-negative-current')
    for folder in runtimes:
        result=read(folder/'result.json');check(result['lifecycle']=='PASS' and all(result['checks'].values()),str(folder.relative_to(r))+' stock lifecycle')
        command=read(folder/'command.json');check(command['argv']==['sh','run.sh','nogui'] and command['injectedJvmOptions']==[],str(folder.relative_to(r))+' stock command')
        check(read(folder/'actual-java-command.json')[1:]==command['expectedJavaArguments'],str(folder.relative_to(r))+' actual JVM command')
        mods=[item for item in read(folder/'installed-inputs.json') if item['path'].startswith('mods/')]
        check(all(sha(folder/'server'/item['path'])==item['sha256'] for item in mods),str(folder.relative_to(r))+' installed inputs preserved')
        compat=[item for item in mods if item['path']=='mods/dualforge.jar']
        if compat:check(compat[0]['sha256']==digest,str(folder.relative_to(r))+' final installed JAR')
    owner=(r/'owner-negative-current/stdout.log').read_text(errors='replace')
    check('CONFIG_OWNER_MISMATCH_B_TO_A' in owner and 'DUAL_HOST_FAULT_MODEL_PASS' in owner,'old live-owner cross-config/fault negatives')
    native=(r/'jei-native-locale-2/stdout.log').read_text(errors='replace')
    check(read(r/'jei-native-locale-2/result.json')['lifecycle']=='PASS' and 'LOCALE_NATIVE_PASS' in native and 'supplierGet=java.lang.RuntimeException:Attempted to load class net/minecraft/client/Minecraft' in native,'actual native JEI constructor versus dedicated supplier failure')
    jei=read(r/'jei-current/result.json');check(jei['status']=='PASS_EXPECTED_REFUSAL' and jei['jar']['sha256']==digest,'actual final JEI first failure')
    check('UNVERIFIED_NETWORK_INITIALIZATION' in jei['firstFailure'] and 'nextType=mezz/jei/forge/network/NetworkHandler' in jei['firstFailure'],'actual first unverified segment')
    check(jei['original']['sha256']=='f5e61a001d57dd044d6f2ba255daf955c50d90e4173f8b4933ab0eb8cd386ed5','untouched original JEI')
    check(read(r/'abi-current/result.json')['classes']=={'forge-jei':923,'neo-jei':923},'all original class SHA revalidated')
    check(read(r/'locale-api-current/result.json')['status']=='PASS','both SDK exact locale members')
    local=read(r/'local-current/result.json');check(local['pythonTests']==106 and local['zshScripts']==25 and local['protectedFiles']==374,'required repository checks')
    check('BYTECODE VERIFIER CONTRACT PASS' in (r/'helper-current/java.stdout.log').read_text() and 'FNF EXPECTATION CONTRACT PASS positive=3 negatives=12' in (r/'helper-current/fnf-expectation.stdout.log').read_text(),'CI bytecode contract')
    snapshot=read(r/'source-current.json');check(snapshot['jar']['sha256']==digest,'source snapshot artifact')
    check(all(sha(ROOT/path)==entry['sha256'] for path,entry in snapshot['source'].items()),'all 36 final production files match build snapshot')
    frozen=read(r/'intake/frozen.json');check(all(sha(entry['path'])==entry['sha256'] for entry in frozen['files']+[frozen['userFile']]),'historical reports and user file preserved')
    save(a.output,{'overall':'PARTIAL','SCOPED_AUTOMATED':'PASS','successfulChecks':len(checks),'checks':checks,'jar':record(jar),
      'newPlanAssertions':212,'oldPlanAssertions':128,'atomicPlannerAssertions':34,'newRuntimeGroups':6,'originalConfigFactoryRuntimeGroups':6,'oldDualBusRuntimeGroups':6,'regressionRuntimeGroups':18,
      'JEI_COMPATIBILITY':'PARTIAL','JEI_NEO_CURRENT_FIRST_FAILURE':jei['firstFailure'],'JEI_STAGES':jei['stages'],
      'CLIENT_ZIP_STATUS':'010C_JEI_CLIENT_ZIP=NOT_PRODUCED','client':'NOT_RUN','runtimeWeakConsumer':'BLOCKED',
      'actualOriginalConfigFactoryOnNeo':'REAL_RUNTIME_PASS_COMPONENT_IN_NEW_OWNER; full JEI entry remains blocked',
      'source':record(r/'source-current.json'),'rawRuntimeResults':[record(folder/'result.json') for folder in runtimes]})
    print('010C SCOPED_AUTOMATED=PASS overall=PARTIAL checks='+str(len(checks)))
if __name__=='__main__':main()
