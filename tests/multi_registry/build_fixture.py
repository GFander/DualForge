"""Clean native Forge compilation and actual bytecode evidence; every JVM is awaited serially."""
import argparse,importlib.util,json,os,shutil,zipfile,hashlib
from pathlib import Path
REPO=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('registry',REPO/'tests/registry/run_registry.py');r=importlib.util.module_from_spec(spec);spec.loader.exec_module(r)
w=r.w
SOURCE=REPO/'tests/multi_registry'

def build(args):
    run=w.run_directory(REPO/'out/convergence-006a/fixture')
    binary=w.tools(args,run)
    forge=[args.forge_dependencies/n for n,_ in r.identity.FORGE]
    for p,(_,sha) in zip(forge,r.identity.FORGE):assert w.digest(p)==sha
    libs=sorted((args.server_input/'libraries').rglob('*.jar'))
    vanilla=args.server_input/'libraries/net/minecraft/server/1.21.1-20240808.144430/server-1.21.1-20240808.144430-srg.jar'
    with zipfile.ZipFile(vanilla) as jar:
        assert all(b'net/neoforged/' not in jar.read(n) and b'net/minecraftforge/' not in jar.read(n) for n in jar.namelist() if n.endswith('.class'))
    support=[p for p in libs if p.relative_to(args.server_input/'libraries').as_posix().startswith(('com/google/','com/mojang/','it/unimi/','org/slf4j/','org/apache/logging/log4j/','org/jetbrains/','org/joml/'))]
    forgecp=[*forge,vanilla,*support]
    neo=args.server_input/'libraries/net/neoforged/neoforge/21.1.256/neoforge-21.1.256-server.jar'
    sourcefiles=[p for p in SOURCE.rglob('*') if p.is_file()]
    inputs=w.fingerprints(sourcefiles+forgecp+libs);w.save(run/'inputs.json',inputs)
    w.save(run/'forge-compile-inputs.json',w.fingerprints(forgecp))
    shutil.copytree(SOURCE,run/'source')
    jars={}
    for name,folder,cp in [('multi-registry-probe',SOURCE/'src',forgecp),('multi-registry-driver',SOURCE/'driver',[neo,*libs]),('registry-matrix',SOURCE/'matrix',forgecp)]:
        classes=run/(name+'-classes');classes.mkdir()
        w.native(run,name+'-javac',[binary/'javac','--release','21','-encoding','UTF-8','-classpath',os.pathsep.join(map(str,cp)),'-d',classes,*sorted(folder.rglob('*.java'))])
        root=run/(name+'-root');shutil.copytree(classes,root);shutil.copytree(folder/'main/resources',root,dirs_exist_ok=True)
        jar=run/(name+'.jar')
        w.native(run,name+'-jar',[binary/'jar','--create','--file',jar,'--manifest',root/'META-INF/MANIFEST.MF','-C',root,'.'])
        jars[name]=r.fingerprint(jar);w.save(run/(name+'-contents.json'),w.jar_contents(jar))
        if name!='multi-registry-driver':
            with zipfile.ZipFile(jar) as z:
                for entry in z.namelist():
                    if entry.endswith('.class'):
                        value=z.read(entry);assert b'net/neoforged/' not in value and b'io/github/gfander/dualforge/' not in value
    probe=run/'multi-registry-probe.jar'
    with zipfile.ZipFile(probe) as z,zipfile.ZipFile(run/'native-control.jar','w') as n:
        for name in z.namelist():n.writestr(name,z.read(name))
        n.writestr('META-INF/neoforge.mods.toml',z.read('META-INF/mods.toml').replace(b'[52,)',b'[4,)'))
    jars['native-control']=r.fingerprint(run/'native-control.jar')
    # Preserve actual javap, including private factory and BootstrapMethods.
    for name,jar,cls in [('multi',probe,'com.example.multiregistrycheck.MultiRegistryProbe'),('matrix',run/'registry-matrix.jar','com.example.registrymatrix.LocalRegistryProbe'),('forms',run/'registry-matrix.jar','com.example.registrymatrix.SupplierForms'),('driver',run/'multi-registry-driver.jar','com.example.multiregistrydriver.MultiRegistryDriver'),('driver-observer',run/'multi-registry-driver.jar','com.example.multiregistrydriver.MultiRegistryDriver$1'),('original005',args.original_fixture,'com.example.registryprobe.RegistryProbe')]:
        w.native(run,'javap-'+name,[binary/'javap','-p','-v','-c','-classpath',jar,cls])
    for name,cp,cls in [('forge-dr',forgecp,'net.minecraftforge.registries.DeferredRegister'),('forge-holder',forgecp,'net.minecraftforge.registries.RegistryObject'),('forge-registry',forgecp,'net.minecraftforge.registries.ForgeRegistries'),('neo-dr',[neo,*libs],'net.neoforged.neoforge.registries.DeferredRegister'),('neo-holder',[neo,*libs],'net.neoforged.neoforge.registries.DeferredHolder')]:
        w.native(run,'javap-'+name,[binary/'javap','-p','-s','-classpath',os.pathsep.join(map(str,cp)),cls])
    classes=run/'asm-evidence-classes';classes.mkdir();asmcp=os.pathsep.join(map(str,libs))
    w.native(run,'asm-javac',[binary/'javac','--release','21','-classpath',asmcp,'-d',classes,SOURCE/'contract/AsmEvidence.java'])
    w.native(run,'asm-nodes',[binary/'java','-classpath',str(classes)+os.pathsep+asmcp,'io.github.gfander.dualforge.test.AsmEvidence',args.original_fixture,'com/example/registryprobe/RegistryProbe.class',probe,'com/example/multiregistrycheck/MultiRegistryProbe.class',run/'registry-matrix.jar','com/example/registrymatrix/LocalRegistryProbe.class',run/'registry-matrix.jar','com/example/registrymatrix/SupplierForms.class'])
    driver=(run/'javap-driver.stdout.log').read_text()
    assert 'Registry.register:' not in driver and 'RegisterEvent.register:' not in driver and 'setAccessible' not in driver and 'Unsafe' not in driver
    checks={'nativeForgeOnly':True,'twoDeclarationSites':(run/'javap-multi.stdout.log').read_text().count('Method net/minecraftforge/registries/DeferredRegister.register:(Ljava/lang/String;Ljava/util/function/Supplier;)')==2,'lambdaAndSameEntryMethodReference':all(v in (run/'javap-multi.stdout.log').read_text() for v in ('lambda$static$0','createSecondItem','LambdaMetafactory.metafactory')),'actualCapturedAndAlternateBootstrap':all(v in (run/'javap-forms.stdout.log').read_text() for v in ('altMetafactory','(Ljava/lang/String;)Ljava/util/function/Supplier;'))}
    assert all(checks.values()),checks
    w.unchanged(inputs)
    result={'status':'PASS','jars':jars,'source':w.fingerprints(sourcefiles),'nativeCompilation':checks,'directory':str(run)}
    w.save(run/'result.json',result);w.save(REPO/'out/convergence-006a/fixture.json',result)
    print('RESULT PASS',run)
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    for name in ('server-input','forge-dependencies','jdk-home','original-fixture'):p.add_argument('--'+name,type=Path,required=True)
    build(p.parse_args())
