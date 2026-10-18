"""Native observers for unchanged original config factory/owner/lifecycle integration."""
import argparse
from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'mouse_tweaks'))
from support import ENV,JDK,fresh,record,run,save

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',type=Path,required=True);a=p.parse_args();out=fresh(a.output);template=Path(__file__).with_name('OriginalConfigObserver.java.in')
    for side in ['forge','neo']:
        group=out/side;group.mkdir();classes=group/'classes';classes.mkdir();deps=sorted((ENV/('runs/convergence-009c-20261017/client-abi/'+side+'/libraries')).rglob('*.jar'))
        fml='net.minecraftforge.fml' if side=='forge' else 'net.neoforged.fml';bus='net.minecraftforge.eventbus.api' if side=='forge' else 'net.neoforged.bus.api';game='net.minecraftforge' if side=='forge' else 'net.neoforged.neoforge';spec='net.minecraftforge.common.ForgeConfigSpec' if side=='forge' else 'net.neoforged.neoforge.common.ModConfigSpec'
        context='net.minecraftforge.fml.ModLoadingContext.get()' if side=='forge' else 'net.neoforged.fml.ModLoadingContext.get().getActiveContainer()'
        entry='entry=owner.getClass().getMethod("getMod").invoke(owner);' if side=='forge' else 'Class<?> bridge=Class.forName("io.github.gfander.dualforge.ForgeDualBusBridge");Field states=bridge.getDeclaredField("STATES");states.setAccessible(true);Object session=((java.lang.ref.Reference<?>)((Map<?,?>)states.get(null)).get(owner)).get();entry=((java.lang.ref.Reference<?>)get(session,"entry")).get();'
        text=template.read_text()
        for key,value in {'@FML@':fml,'@BUS@':bus,'@GAME@':game,'@SPEC@':spec,'@CONTEXT@':context,'@GLOBAL@':'net.minecraftforge.common.MinecraftForge' if side=='forge' else 'net.neoforged.neoforge.common.NeoForge','@ENTRY@':entry}.items():text=text.replace(key,value)
        src=group/'OriginalConfigObserver.java';src.write_text(text);save(group/'inputs.json',[record(p) for p in [template,Path(__file__),*deps]])
        run(group,'javac',[JDK/'bin/javac','--release','21','-proc:none','-cp',':'.join(map(str,deps)),'-d',classes,src])
        meta=classes/'META-INF';meta.mkdir();(meta/('mods.toml' if side=='forge' else 'neoforge.mods.toml')).write_text('modLoader="javafml"\nloaderVersion="[1,)"\nlicense="test-only"\n[[mods]]\nmodId="configobserver"\nversion="1"\n')
        run(group,'jar',[JDK/'bin/jar','--create','--file',group/'configobserver.jar','-C',classes,'.'])
    save(out/'result.json',{'status':'PASS','scope':'NATIVE_CONFIG_COMPONENT_OBSERVERS'})
if __name__=='__main__':main()
