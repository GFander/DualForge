"""Build separate native observers; never bundled with production or original Forge inputs."""
import argparse
from pathlib import Path
import sys
import zipfile
import hashlib
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'mouse_tweaks'))
from support import ENV,JDK,fresh,record,run,save

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--output',type=Path,required=True)
    a=p.parse_args(); out=fresh(a.output)
    template=Path(__file__).parent.parent/'dual_bus/Observer.java.in'
    for side in ['forge','neo']:
        group=out/side;group.mkdir();classes=group/'classes';classes.mkdir()
        sdk=ENV/('runs/convergence-009c-20261017/client-abi/'+side+'/libraries');deps=sorted(sdk.rglob('*.jar'))
        fml='net.minecraftforge.fml' if side=='forge' else 'net.neoforged.fml'
        text=template.read_text().replace('@FML@',fml).replace('@BUS@','net.minecraftforge.eventbus.api' if side=='forge' else 'net.neoforged.bus.api')
        text=text.replace('@GAME@','net.minecraftforge' if side=='forge' else 'net.neoforged.neoforge').replace('@GLOBAL@','net.minecraftforge.common.MinecraftForge' if side=='forge' else 'net.neoforged.neoforge.common.NeoForge')
        if side=='forge':
            text=text.replace('@OWNER_MODELS@','').replace('@RUN_OWNER_MODELS@','')
            text=text.replace('@ENTRY@','return owner.getClass().getMethod("getMod").invoke(owner);')
        else:
            text=text.replace('@RUN_OWNER_MODELS@','ownerModels(ca,a,b,ma,mb);')
            text=text.replace('@OWNER_MODELS@', (Path(__file__).parent.parent/'dual_bus/OwnerModels.java.in').read_text())
            text=text.replace('@ENTRY@' , 'Class<?> bridge=Class.forName("io.github.gfander.dualforge.ForgeDualBusBridge");Field states=bridge.getDeclaredField("STATES");states.setAccessible(true);Object session=((java.lang.ref.Reference<?>)((Map<?,?>)states.get(null)).get(owner)).get();return ((java.lang.ref.Reference<?>)get(session,"entry")).get();')
        text=text.replace('@ACCEPT_ORIGINAL@','((Consumer<LivingDeathEvent>)get(a,"deathListener")).accept(business);')
        specname='net.minecraftforge.common.ForgeConfigSpec' if side=='forge' else 'net.neoforged.neoforge.common.ModConfigSpec'
        context='net.minecraftforge.fml.ModLoadingContext.get()' if side=='forge' else 'net.neoforged.fml.ModLoadingContext.get().getActiveContainer()'
        text=text.replace('@CONFIG_CONTROL@',specname+'.Builder cfg=new '+specname+'.Builder();cfg.define("nativeControl",true);'+context+'.registerConfig('+fml+'.config.ModConfig.Type.SERVER,cfg.build());')
        for letter in ['A','B']:
            owner='c'+letter.lower()
            setup='new FMLCommonSetupEvent('+owner+', '+(fml+'.ModLoadingStage.COMMON_SETUP' if side=='forge' else 'new net.neoforged.fml.DeferredWorkQueue("diagnostic-010b")')+')'
            text=text.replace('@SETUP_'+letter+'@',setup)
        text=text.replace('busalpha','localpha').replace('busbeta','locbeta')
        extras=Path(__file__).with_name('LocaleChecks.java.in').read_text()
        models=Path(__file__).with_name('OriginalModels.java.in').read_text()
        models=models.replace('@BUILDER_CLASS@',specname+'$Builder').replace('@SPEC_CLASS@',specname)
        if side=='forge':
            models=models.replace('@ORIGINAL_CONFIG_MAP@','').replace('@ORIGINAL_CONFIG_LOAD@','spec.getClass().getMethod("setConfig",com.electronwill.nightconfig.core.CommentedConfig.class).invoke(spec,data);')
        else:
            mapping='''Class<?> planClass=Class.forName("io.github.gfander.dualforge.ForgeDualBusPlan");
        org.objectweb.asm.tree.ClassNode cn=new org.objectweb.asm.tree.ClassNode(),an=new org.objectweb.asm.tree.ClassNode();
        new org.objectweb.asm.ClassReader(configBytes).accept(cn,0);new org.objectweb.asm.ClassReader(apiBytes).accept(an,0);
        callPrivateStatic(planClass,"validateConfigInterface",new Class<?>[]{org.objectweb.asm.tree.ClassNode.class,org.objectweb.asm.tree.ClassNode.class},cn,an);
        callPrivateStatic(planClass,"validateConfig",new Class<?>[]{org.objectweb.asm.tree.ClassNode.class,String.class,String.class},cn,"register",an.name);
        Class<?> bridge=Class.forName("io.github.gfander.dualforge.ForgeDualBusBridge");Field states=bridge.getDeclaredField("STATES");states.setAccessible(true);
        Object owner=ModList.get().getModContainerById("localpha").orElseThrow();Object session=((java.lang.ref.Reference<?>)((Map<?,?>)states.get(null)).get(owner)).get();Object plan=get(session,"plan");
        Method rewrite=planClass.getDeclaredMethod("rewrite",org.objectweb.asm.tree.ClassNode.class);rewrite.setAccessible(true);rewrite.invoke(plan,cn);
        org.objectweb.asm.ClassWriter writer=new org.objectweb.asm.ClassWriter(0);cn.accept(writer);configBytes=writer.toByteArray();'''
            load='''// Actual permitted host record; no proxy or source ILoadedConfig implementation.
        // In-memory component only: never register/save this record or claim owner persistence.
        spec.getClass().getMethod("correct",com.electronwill.nightconfig.core.CommentedConfig.class).invoke(spec,data);
        check((boolean)spec.getClass().getMethod("isCorrect",com.electronwill.nightconfig.core.UnmodifiableCommentedConfig.class).invoke(spec,data),"correct isolated native config");
        Class<?> loaded=Class.forName("net.neoforged.fml.config.IConfigSpec$ILoadedConfig"),impl=Class.forName("net.neoforged.fml.config.LoadedConfig");
        Constructor<?> loadedCtor=impl.getDeclaredConstructor(com.electronwill.nightconfig.core.CommentedConfig.class,java.nio.file.Path.class,Class.forName("net.neoforged.fml.config.ModConfig"));loadedCtor.setAccessible(true);
        Object loadedConfig=loadedCtor.newInstance(data,null,null);
        spec.getClass().getMethod("acceptConfig",loaded).invoke(spec,loadedConfig);'''
            models=models.replace('@ORIGINAL_CONFIG_MAP@',mapping).replace('@ORIGINAL_CONFIG_LOAD@',load)
        extras+='\n'+models
        text=text.replace('    @OWNER_MODELS@', '    @OWNER_MODELS@')
        text=text.replace('    private void started(ServerStartedEvent event) {',extras+'\n    private void started(ServerStartedEvent event) {')
        text=text.replace('Object a=entry(ca), b=entry(cb);','Object a=entry(ca), b=entry(cb);localeChecks(a,b);originalModels(a);')
        text=text.replace('public Observer(){','public Observer(){localeBefore();')
        src=group/'Observer.java';src.write_text(text)
        save(group/'inputs.json',[record(p) for p in [template,Path(__file__).parent.parent/'dual_bus/OwnerModels.java.in',Path(__file__).with_name('LocaleChecks.java.in'),Path(__file__).with_name('OriginalModels.java.in'),Path(__file__),*deps]])
        run(group,'javac',[JDK/'bin/javac','--release','21','-proc:none','-cp',':'.join(map(str,deps)),'-d',classes,src])
        original=ENV/'runs/convergence-010a-jei/inputs/jei-1.21.1-forge-19.27.0.340.jar'
        assert hashlib.sha256(original.read_bytes()).hexdigest()=='f5e61a001d57dd044d6f2ba255daf955c50d90e4173f8b4933ab0eb8cd386ed5'
        with zipfile.ZipFile(original) as archive:
            for name in ['mezz/jei/forge/config/ServerConfig','mezz/jei/common/config/IServerConfig','mezz/jei/common/util/MinecraftLocaleSupplier','mezz/jei/common/util/Translator']:
                target=classes/('META-INF/constructor-model/'+name+'.bin');target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(archive.read(name+'.class'))
        save(group/'original-components.json',{'original':record(original),'components':[record(p) for p in sorted((classes/'META-INF/constructor-model').rglob('*.bin'))],'scope':'ISOLATED_COMPONENT_MODEL; not a JEI container'})
        meta=classes/'META-INF';meta.mkdir(exist_ok=True)
        (meta/('mods.toml' if side=='forge' else 'neoforge.mods.toml')).write_text('modLoader="javafml"\nloaderVersion="[1,)"\nlicense="test-only"\n[[mods]]\nmodId="dualobserver"\nversion="1"\n')
        run(group,'jar',[JDK/'bin/jar','--create','--file',group/'dualobserver.jar','-C',classes,'.'])
    save(out/'result.json',{'status':'PASS','scope':'SEPARATE_NATIVE_DIAGNOSTIC_OBSERVERS'})
if __name__=='__main__':main()
