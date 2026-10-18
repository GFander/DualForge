"""Separate native observers; original Forge business JARs are never rebuilt for Neo."""
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
            text=text.replace('@ENTRY@','return owner.getClass().getMethod("getMod").invoke(owner);').replace('@CHANNEL@','check(((Map<?,?>)get(channel,"payloads")).size()==6,"six actual source payloads");')
            text=text.replace('@REGISTRATION@','''Object reg=((Map<?,?>)get(channel,"payloads")).get(type.id());check(reg!=null && call(reg,"type")==type && call(reg,"codec")==codec && call(reg,"consumer")==original,"source add retains exact type/codec/consumer");check(call(reg,"direction")== (leaf.startsWith("Up")?PacketFlow.SERVERBOUND:PacketFlow.CLIENTBOUND),"source direction");''')
        else:
            text=text.replace('@ENTRY@','Object session=((java.lang.ref.Reference<?>)((Map<?,?>)get(Class.forName("io.github.gfander.dualforge.ForgeDualBusBridge"),"STATES")).get(owner)).get();return ((java.lang.ref.Reference<?>)get(session,"entry")).get();')
            text=text.replace('@CHANNEL@','check(get(channel,"phase").toString().equals("REGISTERED") && ((List<?>)get(channel,"definitions")).size()==6,"official native registration complete");')
            text=text.replace('@REGISTRATION@','''var map=(Map<?,?>)((Map<?,?>)get(net.neoforged.neoforge.network.registration.NetworkRegistry.class,"PAYLOAD_REGISTRATIONS")).get(ConnectionProtocol.PLAY);var reg=(net.neoforged.neoforge.network.registration.PayloadRegistration<?>)map.get(type.id());check(reg!=null && reg.type()==type && reg.codec()==codec && reg.version().equals("3") && reg.optional(),"exact native source type/codec + attributes");check(reg.matchesFlow(leaf.startsWith("Up")?PacketFlow.SERVERBOUND:PacketFlow.CLIENTBOUND) && !reg.matchesFlow(leaf.startsWith("Up")?PacketFlow.CLIENTBOUND:PacketFlow.SERVERBOUND),"native exact direction");check(!(reg.handler() instanceof net.neoforged.neoforge.network.handling.MainThreadPayloadHandler) && Proxy.isProxyClass(reg.handler().getClass()),"source add begins on native NETWORK thread");var defs=(List<?>)get(channel,"definitions");Object def=defs.get(List.of("UpTuple","UpWords","UpUnit","DownTuple","DownWords","DownUnit").indexOf(leaf));check(call(def,"consumer")==original,"exact original source handler identity");''')
        models=(src/'ContextModels.java.in').read_text()
        game='net.minecraftforge' if side=='forge' else 'net.neoforged.neoforge'
        models=models.replace('@TICK@',game+('.event.TickEvent.ServerTickEvent.Post' if side=='forge' else '.event.tick.ServerTickEvent.Post')).replace('@STOP@',game+'.event.server.ServerStoppingEvent')
        if side=='forge':
            models=models.replace('@MAKE_CONTEXT@','return new net.minecraftforge.event.network.CustomPayloadEvent.Context(new net.minecraft.network.Connection(PacketFlow.SERVERBOUND));')
            models=models.replace('@QUEUE@','return ((net.minecraftforge.event.network.CustomPayloadEvent.Context)context).enqueueWork(work);')
            models=models.replace('@HANDLE@','((java.util.function.BiConsumer)get(network,leaf.toLowerCase(Locale.ROOT)+"Handler")).accept(payload,context);')
            models=models.replace('@SENDER_HANDLED@','var raw=(net.minecraftforge.event.network.CustomPayloadEvent.Context)context;check(raw.getSender()==null && !raw.getPacketHandled(),"actual disconnected source context default state");raw.setPacketHandled(true);check(raw.getPacketHandled(),"actual source handled state write");')
            models=models.replace('@NATIVE_GUARD@','').replace('@CHECK_NATIVE_GUARD@','')
        else:
            models=models.replace('@MAKE_CONTEXT@','''net.minecraft.network.Connection connection=new net.minecraft.network.Connection(PacketFlow.SERVERBOUND);
        Object listener=Proxy.newProxyInstance(net.minecraft.network.protocol.common.ServerCommonPacketListener.class.getClassLoader(),new Class<?>[]{net.minecraft.network.protocol.common.ServerCommonPacketListener.class},(self,m,args)->switch(m.getName()){
            case "getConnection"->connection;case "getMainThreadEventLoop"->server;case "protocol"->ConnectionProtocol.PLAY;case "flow"->PacketFlow.SERVERBOUND;case "toString"->"010E disconnected listener component";case "hashCode"->System.identityHashCode(self);case "equals"->self==args[0];default->throw new UnsupportedOperationException("unverified diagnostic listener method "+m.getName());});
        Field field=net.minecraft.network.Connection.class.getDeclaredField("packetListener");field.setAccessible(true);field.set(connection,listener);
        return new net.neoforged.neoforge.network.handling.ServerPayloadContext((net.minecraft.network.protocol.common.ServerCommonPacketListener)listener,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("closureobserver","context-model"));''')
            models=models.replace('@QUEUE@','''Method method=Class.forName("io.github.gfander.dualforge.ForgeSourceContextBridge").getDeclaredMethod("enqueue",Object.class,Runnable.class);method.setAccessible(true);
        try{return (java.util.concurrent.CompletableFuture<Void>)method.invoke(null,((net.neoforged.neoforge.network.handling.IPayloadContext)context).listener().getMainThreadEventLoop(),work);}catch(InvocationTargetException e){if(e.getCause() instanceof RuntimeException failure)throw failure;throw e;}''')
            models=models.replace('@HANDLE@','''var type=(CustomPacketPayload.Type)packet.getField("TYPE").get(null);var map=(Map<?,?>)((Map<?,?>)get(net.neoforged.neoforge.network.registration.NetworkRegistry.class,"PAYLOAD_REGISTRATIONS")).get(ConnectionProtocol.PLAY);
        var registered=(net.neoforged.neoforge.network.registration.PayloadRegistration)map.get(type.id());registered.handler().handle((CustomPacketPayload)payload,(net.neoforged.neoforge.network.handling.IPayloadContext)context);''')
            models=models.replace('@SENDER_HANDLED@','''Method sender=Class.forName("io.github.gfander.dualforge.ForgeSourceContextBridge").getDeclaredMethod("sender",Object.class);sender.setAccessible(true);check(sender.invoke(null,((net.neoforged.neoforge.network.handling.IPayloadContext)context).connection())==null,"actual disconnected connection has no server game sender");''')
            models=models.replace('@NATIVE_GUARD@','''System.out.println("NATIVE_GUARD_EXPECTED_ERROR scope=ISOLATED_NATIVE_API");nativeGuard=((net.neoforged.neoforge.network.handling.IPayloadContext)modelContext(server)).enqueueWork(()->{throw new RuntimeException("010E_NATIVE_GUARD_EXPECTED_FAILURE");});''')
            models=models.replace('@CHECK_NATIVE_GUARD@','check(nativeGuard!=null && nativeGuard.isDone() && !nativeGuard.isCompletedExceptionally() && nativeGuard.join()==null,"actual native guard logs and changes exceptional future; adapter deliberately preserves source future");')
        models+='\n'+(src/'OriginalCodecModels.java.in').read_text()
        if side=='neo':models+='\n'+(src/'OwnerModels.java.in').read_text()
        text=text.replace('@MODELS@','originalCodecs(event.getServer());contextModels(event.getServer(),networks);').replace('@EXTRA@',models).replace('@MODEL_LISTENERS@',game+'.common.'+('MinecraftForge' if side=='forge' else 'NeoForge')+'.EVENT_BUS.addListener(this::contextTick);'+game+'.common.'+('MinecraftForge' if side=='forge' else 'NeoForge')+'.EVENT_BUS.addListener(this::contextStop);')
        if side=='neo':
            text=text.replace('originalCodecs(event.getServer());','ownerModels(networks);originalCodecs(event.getServer());').replace('public Observer(){','public Observer(){net.neoforged.fml.ModLoadingContext.get().getActiveContainer().getEventBus().addListener(net.neoforged.bus.api.EventPriority.LOWEST,false,net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent.class,this::hostFault);')
        source=group/'Observer.java';source.write_text(text)
        save(group/'inputs.json',[record(p) for p in [Path(__file__),src/'Observer.java.in',src/'ContextModels.java.in',src/'OriginalCodecModels.java.in',src/'OwnerModels.java.in',*deps]])
        run(group,'javac',[JDK/'bin/javac','--release','21','-proc:none','-cp',':'.join(map(str,deps)),'-d',classes,source])
        meta=classes/'META-INF';meta.mkdir();metadata='modLoader="javafml"\nloaderVersion="[1,)"\nlicense="test-only"\n[[mods]]\nmodId="closureobserver"\nversion="1"\n'
        for owner in ['flowalpha','flowbeta']:
            metadata+='[[dependencies.closureobserver]]\nmodId="'+owner+'"\n'+('mandatory=true\n' if side=='forge' else 'type="required"\n')+'versionRange="[1,)"\nordering="AFTER"\nside="BOTH"\n'
        (meta/('mods.toml' if side=='forge' else 'neoforge.mods.toml')).write_text(metadata)
        run(group,'jar',[JDK/'bin/jar','--create','--file',group/'closureobserver.jar','-C',classes,'.'])
    save(out/'result.json',{'status':'PASS','scope':'NATIVE_DIAGNOSTIC_BUILD_ONLY'})
if __name__=='__main__':main()
