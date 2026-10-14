"""Controlled Block registry stages; physical copies and serial JVMs use the existing harness."""
import argparse,importlib.util,json,re,traceback,os
from pathlib import Path
REPO=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('registry',REPO/'tests/registry/run_registry.py');r=importlib.util.module_from_spec(spec);spec.loader.exec_module(r)
w=r.w
ROOT=REPO/'out/convergence-006b'
SOURCE=REPO/'tests/block_registry'
ENTRY='com.example.blockregistrycheck.BlockRegistryProbe'

def baseline(args,run,binary):
    w.server_preflight(args,args.jar)
    artifacts=[args.jar,args.fixture,args.driver];w.save(run/'artifacts.json',w.fingerprints(artifacts))
    meta,logs,server=w.harness(args,run,binary,args.fixture,args.jar,installed_mod_name=args.fixture.name,extra_mods=(args.driver,),extra_jvm_args=('-DDF006B_REGISTER=true',))
    return {'status':'OBSERVED','compat':r.fingerprint(args.jar),'fixture':r.fingerprint(args.fixture),'driver':r.fingerprint(args.driver),'meta':meta,'logs':str(logs),'note':'Actual pre-change observation; root-cause assertions are recorded only after reading raw logs.'}

def contract(args,run,binary,runtime=None):
    libs=sorted((args.server_input/'libraries').rglob('*.jar'))
    cp=os.pathsep.join(map(str,[args.jar,*libs]))
    records=w.fingerprints([args.jar,args.fixture,args.native_control,args.matrix,SOURCE/'BlockRegistryBytecodeTest.java',*libs])
    w.save(run/'inputs.json',records)
    classes=run/'classes';classes.mkdir()
    w.native(run,'javac-contract',[binary/'javac','--release','21','-classpath',cp,'-d',classes,SOURCE/'BlockRegistryBytecodeTest.java'])
    w.native(run,'bytecode',[binary/'java','--add-opens','java.base/java.lang.invoke=ALL-UNNAMED','--add-exports','java.base/sun.security.util=ALL-UNNAMED',*w.FLAGS,f'-Ddualforge.dump.profileDir={run / "dumps"}','-classpath',str(classes)+os.pathsep+cp,'io.github.gfander.dualforge.BlockRegistryBytecodeTest',args.fixture,args.native_control,args.matrix,run/'bytes',runtime if runtime else '-'])
    assert 'PASS controlled Block pair scope and atomicity' in (run/'bytecode.stdout.log').read_text()
    assert (run/'bytecode.stderr.log').stat().st_size==0
    for path in (run/'bytes').glob('*.class'):
        w.native(run,'javap-'+path.stem,[binary/'javap','-p','-v','-c',path])
    w.save(run/'bytes.json',w.fingerprints(list((run/'bytes').glob('*.class'))))
    w.unchanged(records)
    return {'status':'PASS','compat':r.fingerprint(args.jar),'fixture':r.fingerprint(args.fixture),'matrix':r.fingerprint(args.matrix),'bytecode':'actual negative/positive FML scan, ASM assertions; injected dump failure is an expected negative'}


def dynamic(args,run,binary):
    enabled=args.stage=='treatment';value=str(enabled).lower();artifacts=[args.jar,args.fixture,args.driver];records=w.fingerprints(artifacts)
    w.server_preflight(args,args.jar);w.save(run/'artifacts.json',records)
    meta,logs,server=w.harness(args,run,binary,args.fixture,args.jar,installed_mod_name=args.fixture.name,extra_mods=(args.driver,),extra_jvm_args=('-DDF006B_REGISTER='+value,))
    label='spike002-0l-treatment';stdout=(logs/(label+'.stdout.log')).read_text(errors='replace');debug=(logs/(label+'.debug.log')).read_text(errors='replace')
    injected=re.findall(r'\[DF006B\] INJECTED MODBUS=(\d+)',stdout);container=re.findall(r'\[DF006B\] CONTAINER MODBUS=(\d+) GLOBALBUS=(\d+)',stdout)
    checks={'exactThreeArtifacts':sorted(p.name for p in (server/'mods').iterdir())==sorted(p.name for p in artifacts) and all(w.digest(server/'mods'/p.name)==w.digest(p) for p in artifacts),
        'discoveryReaderAndMetadataOnce':debug.count('Found mod file "block-registry-probe.jar" [locator: dualforge locator')==1 and 'Found valid mod file block-registry-probe.jar with {blockregistryprobe} mods' in debug,
        'ownedEntryOnly':'loadMod called for blockregistryprobe entrypoints=['+ENTRY+']' in stdout and stdout.count('offered: '+ENTRY.replace('.','/'))==1 and 'offered: com/example/blockregistrydriver/' not in stdout and 'offered: com/example/blockregistrycheck/SupplierForms' not in stdout,
        'actualFMLModBus':len(injected)==len(container)==1 and injected[0]==container[0][0] and container[0][0]!=container[0][1],
        'constructorAndTwoAttachments':stdout.count('[DF006B] CONSTRUCTOR REACHED')==1 and stdout.count('[DF006B] ATTACH RETURNED')==int(enabled) and stdout.count('[DF006B] ATTACH SKIPPED')==int(not enabled),
        'nativePathsDelegated':'delegate -> builtin javafml for blockregistrydriver' in stdout and 'delegate -> builtin javafml for neoforge' in stdout,
        'allMappedSitesObserved':'createHits=2 supplierHits=2 modBusAttachHits=2 holderGetHits=3 supplierImplementationHits=2' in stdout,
        'DoneStopExit0':meta['status']=='DONE_STOPPED_GRACEFULLY' and meta['exitCode']==0 and 'Stopping server' in stdout,
        'noErrorFatalDumpFail':not any(re.search(r'\b(ERROR|FATAL)\b|dump FAIL',t) for t in (stdout,debug,(logs/(label+'.log')).read_text(errors='replace'))),
        'stderrEmptyNoCrash':(logs/(label+'.stderr.log')).stat().st_size==0 and not any(p.is_file() for p in (logs/'crash-reports').rglob('*')),
        'actualTwoRegistriesAndCounts':stdout.count('[DF006B] QUERY block='+value+' item='+value+' blockSuppliers='+str(int(enabled))+' itemSuppliers='+str(int(enabled)))==1,
        'blockSupplierExactlyOnceOrZero':stdout.count('[DF006B] BLOCK SUPPLIER REACHED')==int(enabled),
        'itemSupplierExactlyOnceOrZero':stdout.count('[DF006B] BLOCKITEM SUPPLIER REACHED')==int(enabled),
        'holderAvailableDuringItemSupplier':stdout.count('[DF006B] ITEM_PHASE blockRegistered=true holderBound=true sequence=2')==int(enabled),
        'objectHolderRegistryAndOrder':stdout.count('[DF006B] VERIFIED blockKey=blockregistryprobe:sample_block itemKey=blockregistryprobe:sample_block sameBlock=true holdersGet=true holderRegistries=true blockBeforeItem=true')==int(enabled),
        'queryOnce':stdout.count('[DF006B] QUERY PASS')==1}
    for role in (0,1):
        checks['officialLifecycle'+str(role)]=(all('[DF006B] FRAME role='+str(role)+' '+f in stdout for f in ('net.neoforged.neoforge.registries.RegisterEvent.register','net.neoforged.neoforge.registries.DeferredRegister.addEntries','net.neoforged.fml.ModLoader.postEventWrapContainerInModOrder')) if enabled else '[DF006B] FRAME role='+str(role) not in stdout)
    w.save(run/'assertions.json',checks);w.save(run/'meta.json',meta)
    host=[x for x in json.loads((run/'harness-inputs.json').read_text()) if Path(x['Path']).is_relative_to(args.server_input)]
    environment={'hostInputs':host,'java':r.fingerprint(binary/'java'),'actualJavaVersionSHA256':w.digest(run/'java-version.stderr.log'),'serverProperties':sorted((server/'server.properties').read_text().splitlines()),'commonJvmFlags':['-Xmx3G','-Xms1G',*w.FLAGS],'publicKeyRelay':args.public_key_relay,'javaNetworkArgs':r.fingerprint(args.java_network_args) if args.java_network_args else None}
    w.save(run/'ct-environment.json',environment);assert all(checks.values()),'See actual Block registry assertions'
    verify=run/'contract';verify.mkdir();runtime=contract(args,verify,binary,run/'dumps/profile');w.unchanged(records)
    return {'status':'PASS','compat':r.fingerprint(args.jar),'fixture':r.fingerprint(args.fixture),'driver':r.fingerprint(args.driver),'environment':environment,'meta':meta,'assertions':checks,'runtimeBytecode':runtime,'registered':enabled}

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--stage',choices=('baseline','contract','control','treatment'),required=True)
    for key in ('jar','fixture','driver','server-input','jdk-home','java-network-args'):p.add_argument('--'+key,type=Path,required=key!='java-network-args')
    for key in ('native-control','matrix'):p.add_argument('--'+key,type=Path)
    p.add_argument('--allow-server-start',action='store_true');p.add_argument('--public-key-relay',action='store_true')
    p.add_argument('--max-wait-seconds',type=int,default=120);p.add_argument('--post-done-seconds',type=int,default=10)
    args=p.parse_args()
    for key,value in vars(args).items():
        if isinstance(value,Path):setattr(args,key,value.resolve())
    run=w.run_directory(ROOT/args.stage);w.save(run/'invocation.json',{k:str(v) if isinstance(v,Path) else v for k,v in vars(args).items()})
    result={'status':'FAIL','stage':args.stage,'directory':str(run)}
    try:
        frozen=json.loads((ROOT/'fixture.json').read_text())['jars']
        for key,name in [('fixture','block-registry-probe'),('driver','block-registry-driver')]:assert w.digest(getattr(args,key))==frozen[name]['SHA256']
        binary=w.tools(args,run);result.update(baseline(args,run,binary) if args.stage=='baseline' else contract(args,run,binary) if args.stage=='contract' else dynamic(args,run,binary))
    except Exception as error:
        result.update(error=str(error));(run/'failure.txt').write_text(traceback.format_exc());raise
    finally:
        w.save(run/'result.json',result)
        index=ROOT/'stage-results.json';stages=json.loads(index.read_text()) if index.exists() else {};stages[args.stage]=str(run/'result.json');w.save(index,stages)
        print('RESULT',result['status'],run)
if __name__=='__main__':main()
