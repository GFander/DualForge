"""Build separate native observers; never bundled with production or original Forge inputs."""
import argparse
from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'mouse_tweaks'))
from support import ENV,JDK,fresh,record,run,save

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--output',type=Path,required=True)
    a=p.parse_args(); out=fresh(a.output)
    template=Path(__file__).with_name('Observer.java.in')
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
            text=text.replace('@OWNER_MODELS@', Path(__file__).with_name('OwnerModels.java.in').read_text())
            text=text.replace('@ENTRY@' , 'Class<?> bridge=Class.forName("io.github.gfander.dualforge.ForgeDualBusBridge");Field states=bridge.getDeclaredField("STATES");states.setAccessible(true);Object session=((java.lang.ref.Reference<?>)((Map<?,?>)states.get(null)).get(owner)).get();return ((java.lang.ref.Reference<?>)get(session,"entry")).get();')
        text=text.replace('@ACCEPT_ORIGINAL@','((Consumer<LivingDeathEvent>)get(a,"deathListener")).accept(business);')
        specname='net.minecraftforge.common.ForgeConfigSpec' if side=='forge' else 'net.neoforged.neoforge.common.ModConfigSpec'
        context='net.minecraftforge.fml.ModLoadingContext.get()' if side=='forge' else 'net.neoforged.fml.ModLoadingContext.get().getActiveContainer()'
        text=text.replace('@CONFIG_CONTROL@',specname+'.Builder cfg=new '+specname+'.Builder();cfg.define("nativeControl",true);'+context+'.registerConfig('+fml+'.config.ModConfig.Type.SERVER,cfg.build());')
        for letter in ['A','B']:
            owner='c'+letter.lower()
            setup='new FMLCommonSetupEvent('+owner+', '+(fml+'.ModLoadingStage.COMMON_SETUP' if side=='forge' else 'new net.neoforged.fml.DeferredWorkQueue("diagnostic-010b")')+')'
            text=text.replace('@SETUP_'+letter+'@',setup)
        src=group/'Observer.java';src.write_text(text)
        save(group/'inputs.json',[record(p) for p in [template,Path(__file__).with_name('OwnerModels.java.in'),Path(__file__),*deps]])
        run(group,'javac',[JDK/'bin/javac','--release','21','-proc:none','-cp',':'.join(map(str,deps)),'-d',classes,src])
        meta=classes/'META-INF';meta.mkdir()
        (meta/('mods.toml' if side=='forge' else 'neoforge.mods.toml')).write_text('modLoader="javafml"\nloaderVersion="[1,)"\nlicense="test-only"\n[[mods]]\nmodId="dualobserver"\nversion="1"\n')
        run(group,'jar',[JDK/'bin/jar','--create','--file',group/'dualobserver.jar','-C',classes,'.'])
    save(out/'result.json',{'status':'PASS','scope':'SEPARATE_NATIVE_DIAGNOSTIC_OBSERVERS'})
if __name__=='__main__':main()
