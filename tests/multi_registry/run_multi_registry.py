"""Convergence-006A stages reuse the shared physical-copy server and strict 005 regressions."""
import argparse,importlib.util,json,os,re,traceback
from pathlib import Path
REPO=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('registry',REPO/'tests/registry/run_registry.py');r=importlib.util.module_from_spec(spec);spec.loader.exec_module(r)
w=r.w
ROOT=REPO/'out/convergence-006a'
SOURCE=REPO/'tests/multi_registry'
ENTRY='com.example.multiregistrycheck.MultiRegistryProbe'


def contract(args,run,binary,runtime=None):
    libs=sorted((args.server_input/'libraries').rglob('*.jar'))
    cp=os.pathsep.join(map(str,[args.jar,*libs]))
    records=w.fingerprints([args.jar,args.fixture,args.native_control,args.matrix,SOURCE/'MultiRegistryBytecodeTest.java',*libs])
    w.save(run/'inputs.json',records)
    classes=run/'classes';classes.mkdir()
    w.native(run,'javac-contract',[binary/'javac','--release','21','-classpath',cp,'-d',classes,SOURCE/'MultiRegistryBytecodeTest.java'])
    w.native(run,'bytecode',[binary/'java','--add-opens','java.base/java.lang.invoke=ALL-UNNAMED','--add-exports','java.base/sun.security.util=ALL-UNNAMED',*w.FLAGS,f'-Ddualforge.dump.profileDir={run / "dumps"}','-classpath',str(classes)+os.pathsep+cp,'io.github.gfander.dualforge.MultiRegistryBytecodeTest',args.fixture,args.native_control,args.matrix,run/'bytes',runtime if runtime else '-'])
    assert 'PASS controlled multi Item scope and atomicity' in (run/'bytecode.stdout.log').read_text()
    assert (run/'bytecode.stderr.log').stat().st_size==0
    for path in (run/'bytes').glob('*.class'):
        w.native(run,'javap-'+path.stem,[binary/'javap','-p','-v','-c',path])
    w.save(run/'bytes.json',w.fingerprints(list((run/'bytes').glob('*.class'))))
    w.unchanged(records)
    return {'status':'PASS','compat':r.fingerprint(args.jar),'fixture':r.fingerprint(args.fixture),'matrix':r.fingerprint(args.matrix),'bytecode':'actual negative/positive FML scan, ASM assertions; injected dump failure is an expected negative'}


def dynamic(args,run,binary):
    enabled=args.stage=='treatment';value=str(enabled).lower()
    artifacts=[args.jar,args.fixture,args.driver];records=w.fingerprints(artifacts)
    w.server_preflight(args,args.jar);w.save(run/'artifacts.json',records)
    meta,logs,server=w.harness(args,run,binary,args.fixture,args.jar,installed_mod_name=args.fixture.name,extra_mods=(args.driver,),extra_jvm_args=('-DDF006_REGISTER='+value,))
    label='spike002-0l-treatment'
    stdout=(logs/(label+'.stdout.log')).read_text(errors='replace');debug=(logs/(label+'.debug.log')).read_text(errors='replace')
    injected=re.findall(r'\[DF006\] INJECTED MODBUS=(\d+)',stdout)
    container=re.findall(r'\[DF006\] CONTAINER MODBUS=(\d+) GLOBALBUS=(\d+)',stdout)
    checks={'exactThreeArtifacts':sorted(p.name for p in (server/'mods').iterdir())==sorted(p.name for p in artifacts) and all(w.digest(server/'mods'/p.name)==w.digest(p) for p in artifacts),
        'scanAndMetadataOnce':debug.count('Found mod file "multi-registry-probe.jar" [locator: dualforge locator')==1 and 'Found valid mod file multi-registry-probe.jar with {multiregistryprobe} mods' in debug,
        'ownedEntryAndOnlyEntryTransformed':'loadMod called for multiregistryprobe entrypoints=['+ENTRY+']' in stdout and stdout.count('offered: com/example/multiregistrycheck/MultiRegistryProbe')==1 and 'offered: com/example/multiregistrydriver/' not in stdout,
        'actualFMLModBus':len(injected)==len(container)==1 and injected[0]==container[0][0] and container[0][0]!=container[0][1],
        'constructorOnceAndSwitchArrived':stdout.count('[DF006] CONSTRUCTOR REACHED')==1 and stdout.count('[DF006] ATTACH RETURNED')==int(enabled) and stdout.count('[DF006] ATTACH SKIPPED')==int(not enabled),
        'nativeDriverAndNeoForgeDelegated':'delegate -> builtin javafml for multiregistrydriver' in stdout and 'delegate -> builtin javafml for neoforge' in stdout,
        'DoneStopExit0':meta['status']=='DONE_STOPPED_GRACEFULLY' and meta['exitCode']==0 and 'Stopping server' in stdout,
        'noErrorFatalDumpFail':not any(re.search(r'\b(ERROR|FATAL)\b|dump FAIL',text) for text in (stdout,debug,(logs/(label+'.log')).read_text(errors='replace'))),
        'stderrEmptyNoCrash':(logs/(label+'.stderr.log')).stat().st_size==0 and not any(p.is_file() for p in (logs/'crash-reports').rglob('*')),
        'twoDeclarationsActuallyValidated':'createHits=1 supplierHits=2 modBusAttachHits=1 holderGetHits=2' in stdout,
        'queryOnce':stdout.count('[DF006] QUERY PASS')==1,'distinctItemsAndHolders':stdout.count('[DF006] DISTINCT items=true holders=true')==int(enabled)}
    for name,marker in [('alpha_item','ALPHA'),('beta_item','BETA')]:
        checks[name+' actual registry query']=stdout.count('[DF006] QUERY phase=ServerStartedEvent id=multiregistryprobe:'+name+' contains='+value+' optional='+value+' suppliers='+str(int(enabled)))==1
        checks[name+' supplier once or zero']=stdout.count('[DF006] '+marker+' SUPPLIER REACHED')==int(enabled)
        checks[name+' actual holder object and key']=stdout.count('[DF006] VERIFIED Item key=multiregistryprobe:'+name+' holder=net.neoforged.neoforge.registries.DeferredHolder sameObject=true air=false')==int(enabled)
        checks[name+' official supplier stack']=(all('[DF006] FRAME item='+name+' '+frame in stdout for frame in ('net.neoforged.neoforge.registries.RegisterEvent.register','net.neoforged.neoforge.registries.DeferredRegister.addEntries','net.neoforged.fml.ModLoader.postEventWrapContainerInModOrder')) if enabled else '[DF006] FRAME item='+name not in stdout)
    w.save(run/'assertions.json',checks);w.save(run/'meta.json',meta)
    host=[record for record in json.loads((run/'harness-inputs.json').read_text()) if Path(record['Path']).is_relative_to(args.server_input)]
    environment={'hostInputs':host,'java':r.fingerprint(binary/'java'),'actualJavaVersionSHA256':w.digest(run/'java-version.stderr.log'),'serverProperties':sorted(line for line in (server/'server.properties').read_text().splitlines() if line and not line.startswith('#')),'commonJvmFlags':['-Xmx3G','-Xms1G',*w.FLAGS],'publicKeyRelay':args.public_key_relay,'javaNetworkArgs':r.fingerprint(args.java_network_args) if args.java_network_args else None}
    w.save(run/'ct-environment.json',environment)
    assert all(checks.values()),'Inspect exact actual C/T assertions; no presumed registration success'
    verify=run/'contract';verify.mkdir();runtime=contract(args,verify,binary,run/'dumps/profile')
    w.unchanged(records)
    return {'status':'PASS','compat':r.fingerprint(args.jar),'fixture':r.fingerprint(args.fixture),'driver':r.fingerprint(args.driver),'environment':environment,'meta':meta,'assertions':checks,'runtimeBytecode':runtime,'registered':enabled}


def baseline(args,run,binary):
    artifacts=[args.jar,args.fixture,args.driver]
    w.server_preflight(args,args.jar);w.save(run/'artifacts.json',w.fingerprints(artifacts))
    meta,logs,server=w.harness(args,run,binary,args.fixture,args.jar,installed_mod_name=args.fixture.name,
                              extra_mods=(args.driver,),extra_jvm_args=('-DDF006_REGISTER=true',))
    stdout=(logs/'spike002-0l-treatment.stdout.log').read_text(errors='replace')
    stderr=(logs/'spike002-0l-treatment.stderr.log').read_text(errors='replace')
    debug=(logs/'spike002-0l-treatment.debug.log').read_text(errors='replace')
    checks={'exactInstalledCohort':all(w.digest(server/'mods'/p.name)==w.digest(p) for p in artifacts),
            'actualForgeDiscoveryOnce':debug.count('Found mod file "multi-registry-probe.jar" [locator: dualforge locator')==1,
            'realReaderAndFMLMetadata':'ForgeModReader: found META-INF/mods.toml' in stdout and 'Found valid mod file multi-registry-probe.jar with {multiregistryprobe} mods' in debug,
            'firstFailureRawEvidence':'005 unsupported Item profile: class structure' in stderr and 'ControlledForgeItemProfile.validate' in stderr and 'DualForgeEntryTransformer.targets' in stderr,
            'beforeConstruction':'[DF006] CONSTRUCTOR REACHED' not in stdout,
            'JVMExitedBeforeDone':meta['status']=='EXITED_EARLY' and meta['doneLine'] is None}
    w.save(run/'assertions.json',checks);w.save(run/'meta.json',meta)
    assert all(checks.values()),'Read actual baseline failure; do not presume a root cause'
    return {'status':'PASS','observedBehavior':'Existing profile rejects at targets/validate; reproduction only, not functional PASS',
            'compat':r.fingerprint(args.jar),'fixture':r.fingerprint(args.fixture),'assertions':checks,'meta':meta}


def consistent(results):
    required=('contract','control','treatment','regression')
    if any(results.get(s,{}).get('status')!='PASS' for s in required):return False
    sha=[results[s].get('compat',{}).get('SHA256') for s in required]
    if not all(isinstance(h,str) and re.fullmatch('[a-f0-9]{64}',h) for h in sha) or len(set(sha))!=1:return False
    c,t=results['control'],results['treatment']
    if any(c.get(k)!=t.get(k) for k in ('compat','fixture','driver','environment')) or not c.get('environment'):return False
    if results['contract'].get('fixture')!=t.get('fixture'):return False
    for value in (c,t):
        nested=value.get('runtimeBytecode',{})
        if nested.get('status')!='PASS' or any(nested.get(k)!=value.get(k) for k in ('compat','fixture')):return False
    return True


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--stage',choices=('baseline','contract','control','treatment'),required=True)
    for key in ('jar','fixture','driver','native-control','matrix','server-input','jdk-home','java-network-args'):
        p.add_argument('--'+key,type=Path,required=key!='java-network-args')
    p.add_argument('--allow-server-start',action='store_true');p.add_argument('--public-key-relay',action='store_true')
    p.add_argument('--max-wait-seconds',type=int,default=120);p.add_argument('--post-done-seconds',type=int,default=10)
    args=p.parse_args()
    for key,value in vars(args).items():
        if isinstance(value,Path):setattr(args,key,value.resolve())
    run=w.run_directory(ROOT/args.stage);w.save(run/'invocation.json',{k:str(v) if isinstance(v,Path) else v for k,v in vars(args).items()})
    result={'status':'FAIL','stage':args.stage,'directory':str(run)}
    try:
        frozen=json.loads((ROOT/'fixture.json').read_text())['jars']
        for key,role in [('fixture','multi-registry-probe'),('driver','multi-registry-driver'),('native_control','native-control'),('matrix','registry-matrix')]:assert w.digest(getattr(args,key))==frozen[role]['SHA256'],'Frozen fixture cohort mismatch'
        binary=w.tools(args,run)
        result.update(baseline(args,run,binary) if args.stage=='baseline' else contract(args,run,binary) if args.stage=='contract' else dynamic(args,run,binary))
    except Exception as error:
        result.update(status='NOT RUN' if isinstance(error,w.NotRunError) else 'FAIL',error=str(error));(run/'failure.txt').write_text(traceback.format_exc());raise
    finally:
        w.save(run/'result.json',result)
        index=ROOT/'stage-results.json';stages=json.loads(index.read_text()) if index.exists() else {};stages[args.stage]=str(run/'result.json');w.save(index,stages)
        print('RESULT',result['status'],run)
if __name__=='__main__':main()
