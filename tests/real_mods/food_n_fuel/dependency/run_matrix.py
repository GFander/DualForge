"""Serial stock launches and precise dependency-boundary assertions for one immutable compat cohort."""
import argparse
import importlib.util
import json
from pathlib import Path
import subprocess

REPO = Path(__file__).resolve().parents[4]
spec=importlib.util.spec_from_file_location('workflow',REPO/'tools/lib/unix_workflow.py')
w=importlib.util.module_from_spec(spec);spec.loader.exec_module(w)


def main():
    p=argparse.ArgumentParser(description=__doc__)
    for key in ('jar','fixture-index','fnf','dropin','observer','native','install','jdk-home'):
        p.add_argument('--'+key,type=Path,required=True)
    p.add_argument('--fnf-next-stage',choices=('item-profile','registry-helper'),default='item-profile',
                   help='Explicit expected next boundary; the 007C-2 historical default remains strict')
    p.add_argument('--skip-fnf-runtime',action='store_true',
                   help='007C-4B Gate 8: exclude N2 after read-only closure rejection; preserve all other assertions')
    a=p.parse_args()
    assert a.jar.name=='DualForge.jar'
    fixture=json.loads(a.fixture_index.read_text());assert fixture['status']=='PASS'
    jars={k:Path(v['Path']) for k,v in fixture['jars'].items()}
    for key,path in jars.items():assert w.digest(path)==fixture['jars'][key]['SHA256']
    run=w.run_directory(REPO/'out/convergence-007c2/matrix')
    w.save(run/'invocation.json',{k:str(v) for k,v in vars(a).items()})
    compat=w.fingerprints([a.jar])[0]
    identity=json.loads((REPO/'tests/real_mods/food_n_fuel/input-identity.json').read_text())
    assert w.digest(a.fnf)==identity['sha256'] and a.fnf.stat().st_size==identity['bytes']
    inputs=w.fingerprints([a.jar,a.fixture_index,a.fnf,a.dropin,a.observer,a.native,Path(__file__),*jars.values()])
    w.save(run/'inputs.json',inputs)
    native_id=fixture['nativeId']
    cases=[
        ('N0',[], 'healthy',[]),
        ('N1',[a.jar], 'healthy',[]),
        ('N2',[a.jar,a.fnf], 'fnf-next',[]),
        ('N3',[a.jar,a.dropin], 'healthy',['loadMod called for dropinprobe']),
        ('native',[a.jar,a.native], 'healthy',['[DF007-NATIVE] CONSTRUCTOR','[DF007-NATIVE] SERVER_STARTED']),
        ('combined',[a.jar,a.dropin,a.observer,a.native], 'healthy',['[DF007] QUERY PASS distinctItems=true officialModBus=true','[DF007-NATIVE] SERVER_STARTED']),
        ('platform-a',[a.jar,jars['a-bounded']], 'healthy',['[DF007C2] A CONSTRUCTOR','[DF007C2] A REGISTER RETURNED','sourcePlatformDependency=VERIFIED']),
        ('multi-a-b',[a.jar,jars['a-valid'],jars['b-unsupported']], 'platform-reject',['owner=dependency_b forge range=[53,) excludes reference source ABI 52.1.0','owner=dependency_a']),
        ('other-required-missing',[a.jar,jars['a-missing']], 'required', ['non_existing_mod','[MISSING]']),
        ('other-required-low',[a.jar,jars['a-low-required'],a.native], 'required',[native_id,'1.0.0']),
        ('minecraft-mismatch',[a.jar,jars['a-minecraft']], 'required',['minecraft','1.21.1']),
        ('optional-absent',[a.jar,jars['a-optional-missing']], 'healthy',['[DF007C2] A REGISTER RETURNED']),
        ('optional-low',[a.jar,jars['a-low-optional'],a.native], 'optional',[native_id,'1.0.0']),
        ('client-requirement-inactive',[a.jar,jars['a-client-missing']], 'healthy',['[DF007C2] A REGISTER RETURNED']),
        ('platform-client-inactive',[a.jar,jars['a-client-platform']], 'healthy',['DELEGATE_INACTIVE_SIDE','[DF007C2] A REGISTER RETURNED']),
        ('platform-ordering-reject',[a.jar,jars['a-ordering-platform']], 'platform-reject',['forge ordering=AFTER requires an unsupported real platform ordering node']),
        ('ordinary-ordering',[a.jar,jars['a-after-native'],a.native], 'healthy',['[DF007C2] A REGISTER RETURNED','[DF007-NATIVE] CONSTRUCTOR']),
        ('native-required-forge',[a.jar,jars['native-required-forge']], 'native-required',['forge','[MISSING]']),
        ('native-optional-forge',[a.jar,jars['native-optional-forge']], 'healthy',['[DF007-NATIVE] SERVER_STARTED']),
    ]
    result={'status':'FAIL','compat':compat,'fnfExpectedStage':a.fnf_next_stage,'testedCommit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=REPO,text=True).strip(),'cases':{},'nativeForgeBaseline':'PARTIAL inherited, not rerun','clientRuntime':'NOT RUN'}
    if a.skip_fnf_runtime:
        cases=[case for case in cases if case[0]!='N2']
        result['excludedCases']={'N2':{'status':'NOT RUN','reason':'007C-4B requires complete read-only Helper admission before FnF runtime'}}
        result['full19CaseMatrix']='PARTIAL: N2 explicitly not run; remaining 18 cases retain original assertions'
    try:
        baseline=None
        for name,mods,kind,markers in cases:
            dest=run/name
            cmd=['python3','-B',REPO/'tests/real_mods/food_n_fuel/run_stock.py','--host','neoforge','--install',a.install,'--output',dest,'--jdk-home',a.jdk_home]
            for mod in mods:cmd+=['--mod',mod]
            w.save(run/(name+'.command.json'),{'argv':list(map(str,cmd)),'cwd':str(REPO)})
            with (run/(name+'.stdout.log')).open('wb') as out,(run/(name+'.stderr.log')).open('wb') as err:
                code=subprocess.run(list(map(str,cmd)),cwd=REPO,stdout=out,stderr=err).returncode
            w.save(run/(name+'.exit.json'),{'exitCode':code})
            raw=json.loads((dest/'result.json').read_text())
            stdout=(dest/'stdout.log').read_text();stderr=(dest/'stderr.log').read_text()
            debug=(dest/'logs/debug.log').read_text() if (dest/'logs/debug.log').exists() else ''
            text=stdout+'\n'+stderr+'\n'+debug
            checks={'actualStockCommand':raw['zeroDualForgeParameters'],'inputsUnchanged':raw['assertions']['inputsUnchanged'],
                    'sameCompat':name=='N0' or any(v['path']=='mods/DualForge.jar' and v['sha256']==compat['SHA256'] for v in raw['installedMods'])}
            for marker in markers:checks['marker:'+marker]=marker in text
            if kind=='healthy':
                checks.update(lifecycle=raw['lifecycle']=='PASS',runnerExit=code==0,noDumpFail='dump FAIL' not in text)
                if baseline is None:baseline=raw['severityRecords']
                checks['noNewBaselineErrors']=raw['severityRecords']==baseline
                if name=='ordinary-ordering':checks['officialConstructorOrdering']=stdout.index('[DF007-NATIVE] CONSTRUCTOR')<stdout.index('[DF007C2] A CONSTRUCTOR')
                if name.startswith('native') or name=='native':checks['noNativeProjection']='[DUALFORGE-PLATFORM]' not in stdout
            elif kind=='fnf-next':
                checks.update(projectionAudited='sourcePlatformDependency=VERIFIED' in stdout,
                              noMissingForge="Mod ID: 'forge', Requested by: 'fnf'" not in text,
                              exactOriginalFnf=any(v['sha256']==identity['sha256'] and v['bytes']==identity['bytes'] for v in raw['installedMods']),
                              lifecycleFailurePreserved=raw['lifecycle']=='FAIL')
                if a.fnf_next_stage=='item-profile':
                    checks.update(actualNewException='005 unsupported Item profile: class structure' in stderr,
                                  actualTargetsStack='DualForgeEntryTransformer.targets' in stderr,
                                  actualOwnedProfileStack='ControlledForgeItemProfile.validate' in stderr,
                                  noFnfConstructor='loadMod called for fnf' not in stdout)
                else:
                    checks.update(contextClassified='ENTRY_PROFILE modId=fnf entry=net.natevg.fnf.FoodNFuel selected=CONTEXT' in stdout,
                                  contextMapped='CONTEXT_PROFILE entry=net/natevg/fnf/FoodNFuel contextCalls=2 helpersUnchanged=2' in stdout,
                                  actualContextBound='[DUALFORGE-CONTEXT] BOUND modId=fnf' in stdout,
                                  actualNextRegistryFailure='java.lang.NoClassDefFoundError: net/minecraftforge/registries/ForgeRegistries' in text,
                                  actualHelperInitializer='ItemRegistries.<clinit>' in text,
                                  actualFnfConstructor='FoodNFuel.<init>' in text and 'FMLModContainer.constructMod' in text,
                                  noOldProfileRefusal='unsupported Item profile: class structure' not in text,
                                  noSwallowedContextError='Forge constructor context rejected' not in text)
            elif kind=='platform-reject':
                checks['explicitPlatformRefusal']='Forge platform dependency rejected:' in text
                checks['refusedEntryNotConstructed']=('[DF007C2] B CONSTRUCTOR' if name=='multi-a-b' else '[DF007C2] A CONSTRUCTOR') not in stdout
            else:
                label='Unsupported installed optional dependencies' if kind=='optional' else 'Missing or unsupported mandatory dependencies'
                owner=native_id if kind=='native-required' else 'dependency_a'
                checks.update(officialFMLSpecificRefusal=label in debug and f"Mod ID: '{markers[0]}', Requested by: '{owner}'" in debug and f"Actual version: '{markers[1]}'" in debug,
                              noDone='Done (' not in stdout,
                              noEntryConstruction='[DF007C2] A CONSTRUCTOR' not in stdout)
                if kind=='native-required':
                    checks['noNativeProjection']='[DUALFORGE-PLATFORM]' not in stdout
                    checks['nativeNotConstructed']='[DF007-NATIVE] CONSTRUCTOR' not in stdout
                else:checks['platformAcceptedFirst']='sourcePlatformDependency=VERIFIED' in stdout
            assert all(checks.values()),name+' failed: '+str([k for k,v in checks.items() if not v])
            w.save(dest/'gate-assertions.json',{'status':'PASS','checks':checks,'rawLifecycle':raw['lifecycle'],'rawJvmExit':raw['exitCode'],'compatSHA256':compat['SHA256'],'negativeIsNotFunctionalPASS':kind!='healthy'})
            result['cases'][name]={'status':'PASS','kind':kind,'rawResult':str(dest/'result.json'),'assertions':str(dest/'gate-assertions.json'),'compatSHA256':None if name=='N0' else compat['SHA256']}
            w.save(run/'progress.json',result)
            print('PASS',name,flush=True)
        w.unchanged(inputs);result.update(status='PASS',sameCompatCohort=True)
    finally:
        w.save(run/'result.json',result);print('RESULT',result['status'],run,flush=True)


if __name__=='__main__':main()
