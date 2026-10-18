"""Build separate native diagnostics; no platform-specific business implementation."""
import argparse
from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'mouse_tweaks'))
from support import ENV,JDK,fresh,record,run,save

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',type=Path,required=True);a=p.parse_args();out=fresh(a.output);src=Path(__file__).parent
    for side in ['forge','neo']:
        group=out/side;group.mkdir();classes=group/'classes';classes.mkdir();deps=sorted((ENV/('runs/convergence-009c-20261017/client-abi/'+side+'/libraries')).rglob('*.jar'),key=lambda p:(not p.name.endswith('-server.jar'),str(p)))
        text=(src/'Observer.java.in').read_text().replace('@FML@','net.minecraftforge.fml' if side=='forge' else 'net.neoforged.fml').replace('@BUS@','net.minecraftforge.eventbus.api' if side=='forge' else 'net.neoforged.bus.api').replace('@GAME@','net.minecraftforge' if side=='forge' else 'net.neoforged.neoforge').replace('@GLOBAL@','net.minecraftforge.common.MinecraftForge' if side=='forge' else 'net.neoforged.neoforge.common.NeoForge')
        if side=='forge':
            text=text.replace('@ENTRY@','return owner.getClass().getMethod("getMod").invoke(owner);').replace('@EARLY@','').replace('@EXTRA@','').replace('@FINAL@','')
            text=text.replace('@CHANNEL@','''Object instanceField=get(channel,"instance");check((int)get(instanceField,"networkProtocolVersion")==3,"source protocol integer");
                for(String sideName:List.of("clientAcceptedVersions","serverAcceptedVersions")){
                    var test=(net.minecraftforge.network.Channel.VersionTest)get(instanceField,sideName);
                    check(test.accepts(net.minecraftforge.network.Channel.VersionTest.Status.PRESENT,3) && !test.accepts(net.minecraftforge.network.Channel.VersionTest.Status.PRESENT,4) && test.accepts(net.minecraftforge.network.Channel.VersionTest.Status.MISSING,0) && test.accepts(net.minecraftforge.network.Channel.VersionTest.Status.VANILLA,0),"source optional truth table "+sideName);
                }''')
            text=text.replace('@REGISTRATION@','''Object reg=((Map<?,?>)get(channel,"payloads")).get(type.id());check(reg!=null,"actual source channel registration");
                    check(call(reg,"type")==type && call(reg,"codec")==codec,"source exact type + codec");
                    check(call(reg,"protocol")==net.minecraftforge.network.NetworkProtocol.PLAY && call(reg,"direction")== (path.equals("up")?PacketFlow.SERVERBOUND:PacketFlow.CLIENTBOUND),"source play direction");
                    Object wrapped=call(reg,"consumer");boolean captured=false;for(Field f:wrapped.getClass().getDeclaredFields()){f.setAccessible(true);captured|=f.get(wrapped)==original;}check(captured,"source addMain retains exact business consumer");''')
        else:
            text=text.replace('@ENTRY@','Class<?> bridge=Class.forName("io.github.gfander.dualforge.ForgeDualBusBridge");Object session=((java.lang.ref.Reference<?>)((Map<?,?>)get(bridge,"STATES")).get(owner)).get();return ((java.lang.ref.Reference<?>)get(session,"entry")).get();').replace('@EARLY@','early();').replace('@EXTRA@',(src/'NeoChecks.java.in').read_text())
            text=text.replace('@CHANNEL@','check(get(channel,"phase").toString().equals("REGISTERED"),"bridge registered only after official event");')
            text=text.replace('@REGISTRATION@','''var map=(Map<?,?>)((Map<?,?>)get(net.neoforged.neoforge.network.registration.NetworkRegistry.class,"PAYLOAD_REGISTRATIONS")).get(ConnectionProtocol.PLAY);
                    var reg=(net.neoforged.neoforge.network.registration.PayloadRegistration<?>)map.get(type.id());check(reg!=null,"actual native registration");
                    check(reg.type()==type && reg.codec()==codec && reg.version().equals("3") && reg.optional(),"native exact source type/codec and protocol attributes");
                    check(reg.protocols().equals(List.of(ConnectionProtocol.PLAY)) && reg.matchesFlow(path.equals("up")?PacketFlow.SERVERBOUND:PacketFlow.CLIENTBOUND) && !reg.matchesFlow(path.equals("up")?PacketFlow.CLIENTBOUND:PacketFlow.SERVERBOUND),"native wrong direction excluded");
                    check(reg.handler() instanceof net.neoforged.neoforge.network.handling.MainThreadPayloadHandler,"official native main-thread wrapper");
                    Object proxy=((net.neoforged.neoforge.network.handling.MainThreadPayloadHandler<?>)reg.handler()).handler();check(Proxy.isProxyClass(proxy.getClass()),"private adapter proxy");
                    var defs=(List<?>)get(channel,"definitions");Object definition=defs.get(path.equals("up")?0:1);check(call(definition,"consumer")==original,"captured original consumer identity");
                    boolean captured=false;var adapter=Proxy.getInvocationHandler(proxy);for(Field f:adapter.getClass().getDeclaredFields()){f.setAccessible(true);captured|=f.get(adapter)==definition;}check(captured,"native handler retains exact immutable definition");
                    try{((List)defs).add(definition);throw new AssertionError("definitions mutable");}catch(UnsupportedOperationException expected){check(true,"immutable constructor receipt");}''')
            text=text.replace('@FINAL@','''check(officialEvents==1 && (boolean)get(net.neoforged.neoforge.network.registration.NetworkRegistry.class,"setup"),"actual native lock after event");negotiation();guardModels(networks[0],networks[1]);
            var reg=(net.neoforged.neoforge.network.registration.PayloadRegistration)((Map<?,?>)((Map<?,?>)get(net.neoforged.neoforge.network.registration.NetworkRegistry.class,"PAYLOAD_REGISTRATIONS")).get(ConnectionProtocol.PLAY)).get(ResourceLocation.fromNamespaceAndPath("netalpha","up"));
            try{net.neoforged.neoforge.network.registration.NetworkRegistry.register(reg.type(),reg.codec(),reg.handler(),reg.protocols(),reg.flow(),reg.version(),reg.optional());throw new AssertionError("late registration accepted");}catch(UnsupportedOperationException expected){check(true,"native late registration rejects");System.out.println("NETWORK_NATIVE_NEGATIVE REGISTRATION_TOO_LATE "+expected);}''')
        assert '@' not in text.replace('@Mod','').replace('@SuppressWarnings',''),text
        source=group/'Observer.java';source.write_text(text);save(group/'inputs.json',[record(p) for p in [src/'Observer.java.in',src/'NeoChecks.java.in',Path(__file__),*deps]])
        run(group,'javac',[JDK/'bin/javac','--release','21','-proc:none','-cp',':'.join(map(str,deps)),'-d',classes,source])
        meta=classes/'META-INF';meta.mkdir();(meta/('mods.toml' if side=='forge' else 'neoforge.mods.toml')).write_text('modLoader="javafml"\nloaderVersion="[1,)"\nlicense="test-only"\n[[mods]]\nmodId="networkobserver"\nversion="1"\n')
        run(group,'jar',[JDK/'bin/jar','--create','--file',group/'networkobserver.jar','-C',classes,'.'])
    save(out/'result.json',{'status':'PASS','scope':'NATIVE_DIAGNOSTIC_BUILD_ONLY'})
if __name__=='__main__':main()
