package io.github.gfander.dualforge;

import java.lang.reflect.*;
import java.util.concurrent.CompletableFuture;
import static io.github.gfander.dualforge.ForgeDualBusPlan.require;

/** Source Context semantics for the proved sender -> handled(true) -> enqueue(Runnable) sequence. */
public final class ForgeSourceContextBridge {
    private static final StackWalker WALKER=StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    static final class Scope {
        final ForgeBusIdentity identity;final Object nativeContext,network,connection,loop;final String method;final Thread thread;
        int stage;boolean handled,closed;final boolean permission;Object permissionPlayer;
        Scope(ForgeBusIdentity identity,Object context,Object network,String method,Object connection,Object loop,boolean permission){this.identity=identity;nativeContext=context;this.network=network;this.method=method;this.connection=connection;this.loop=loop;this.permission=permission;thread=Thread.currentThread();}
    }
    private ForgeSourceContextBridge() { }
    static void verifyHost(java.util.function.Function<String,byte[]> host){
        {String name="net/neoforged/neoforge/network/handling/ServerPayloadContext";byte[] b=host.apply(name);require(b!=null && ForgeDualBusPlan.digest(b).equals("324a53f7eb51c9ae097f2421fdee9614b27b10d2fcaa88d70d7daf54faaf7ed8"),"UNVERIFIED_SOURCE_CONTEXT_HOST_ABI "+name);}
        {String name="net/neoforged/neoforge/network/handling/ClientPayloadContext";byte[] b=host.apply(name);require(b!=null && ForgeDualBusPlan.digest(b).equals("7f6c5bfdb89bc1f139fdc1088c6c83eab21f7fa5407903473aa8528152de24b9"),"UNVERIFIED_SOURCE_CONTEXT_HOST_ABI "+name);}
    }

    static Scope open(ForgeBusIdentity identity,Object context,Object network,String method,String direction,boolean permission){
        identity.sameOwner(network.getClass());String name="net.neoforged.neoforge.network.handling."+(direction.equals("SERVERBOUND")?"ServerPayloadContext":"ClientPayloadContext");
        require(context!=null && context.getClass()==ForgeBusIdentity.host(name,"neoforge"),"CONTEXT_SENDER_UNVERIFIED");
        require(ForgeNetworkBridge.invoke(context,"flow").toString().equals(direction) && ForgeNetworkBridge.invoke(context,"protocol").toString().equals("PLAY"),"WRONG_PAYLOAD_DIRECTION");
        Object connection=ForgeNetworkBridge.invoke(context,"connection"),listener=ForgeNetworkBridge.invoke(context,"listener");
        require(connection!=null && ForgeBusIdentity.host("net.minecraft.network.Connection","minecraft").isInstance(connection) && ForgeNetworkBridge.invoke(connection,"getPacketListener")==listener && ForgeNetworkBridge.invoke(connection,"getReceiving").toString().equals(direction),"CONTEXT_SENDER_UNVERIFIED");
        Object loop=ForgeNetworkBridge.invoke(listener,"getMainThreadEventLoop");require(ForgeBusIdentity.host("net.minecraft.util.thread.BlockableEventLoop","minecraft").isInstance(loop),"CONTEXT_THREAD_UNVERIFIED");
        require(!permission || direction.equals("SERVERBOUND"),"UNVERIFIED_SERVER_SIDE");return new Scope(identity,context,network,method,connection,loop,permission);
    }
    private static Scope scope(Object token,int stage){
        require(token instanceof Scope,"UNKNOWN_CONTEXT_METHOD");Scope s=(Scope)token;
        var caller=WALKER.walk(frames->frames.skip(2).findFirst().orElseThrow());s.identity.sameOwner(caller.getDeclaringClass());
        require(!s.closed && s.thread==Thread.currentThread() && caller.getDeclaringClass()==s.network.getClass() && caller.getMethodName().equals(s.method),"CONTEXT_THREAD_UNVERIFIED");
        require(s.stage==stage,"CONTEXT_ENQUEUE_UNVERIFIED");return s;
    }
    public static Object getSender(Object token){Scope s=scope(token,0);Object player=sender(s.connection);if(s.permission){permissionIdentity(s,player,false);s.permissionPlayer=player;}s.stage=1;return player;}
    public static void setPacketHandled(Object token,boolean value){Scope s=scope(token,1);require(value,"CONTEXT_HANDLED_STATE_UNVERIFIED");s.handled=value;s.stage=2;}
    public static CompletableFuture<Void> enqueueWork(Object token,Runnable work){Scope s=scope(token,2);require(work!=null && work.getClass().isHidden() && work.getClass().getModule()==s.identity.module && work.getClass().getClassLoader()==s.identity.loader,"CONTEXT_ENQUEUE_UNVERIFIED");s.stage=3;return enqueue(s.loop,s.permission?()->{permissionIdentity(s,s.permissionPlayer,true);work.run();}:work);}
    static void close(Scope s,boolean normal){s.closed=true;if(normal)require(s.stage==3 && s.handled,"CONTEXT_HANDLED_STATE_UNVERIFIED");}
    private static Object sender(Object connection){
        Object listener=ForgeNetworkBridge.invoke(connection,"getPacketListener");Class<?> game=ForgeBusIdentity.host("net.minecraft.server.network.ServerGamePacketListenerImpl","minecraft");
        if(!game.isInstance(listener))return null;
        try{return game.getField("player").get(listener);}catch(ReflectiveOperationException e){throw new IllegalStateException("CONTEXT_SENDER_UNVERIFIED",e);}
    }
    // Only the permission profile uses this guard; the original predicate still computes the decision.
    private static void permissionIdentity(Scope s,Object player,boolean queued){
        s.identity.sameOwner(s.network.getClass());require(s.loop!=null && s.loop.getClass()==ForgeBusIdentity.host("net.minecraft.server.dedicated.DedicatedServer","minecraft"),"WRONG_SERVER_AUTHORITY");
        require(!queued || (boolean)ForgeNetworkBridge.invoke(s.loop,"isSameThread"),"WRONG_PERMISSION_THREAD");
        require(player!=null && player.getClass()==ForgeBusIdentity.host("net.minecraft.server.level.ServerPlayer","minecraft"),"WRONG_PLAYER_SOURCE");
        Object listener=ForgeNetworkBridge.invoke(s.connection,"getPacketListener");require(listener!=null && listener.getClass()==ForgeBusIdentity.host("net.minecraft.server.network.ServerGamePacketListenerImpl","minecraft"),"WRONG_PLAYER_SOURCE");
        try{require(listener.getClass().getField("player").get(listener)==player && player.getClass().getField("connection").get(player)==listener,"WRONG_PLAYER_SOURCE");}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Dual bus rejected: WRONG_PLAYER_SOURCE",failure);}
        require(ForgeNetworkBridge.invoke(listener,"getConnection")==s.connection && (boolean)ForgeNetworkBridge.invoke(s.connection,"isConnected") && (boolean)ForgeNetworkBridge.invoke(listener,"isAcceptingMessages"),"WRONG_PLAYER_SOURCE closed connection");
        require(ForgeNetworkBridge.invoke(player,"getServer")==s.loop && ForgeNetworkBridge.invoke(listener,"getMainThreadEventLoop")==s.loop,"WRONG_SERVER_AUTHORITY");
    }
    @SuppressWarnings("unchecked")
    private static CompletableFuture<Void> enqueue(Object loop,Runnable work){
        // Reconstruct the fixed Forge supplyAsync algorithm. Native Context.guard swallows failures,
        // and the native loop submitAsync adds logging absent from the source implementation.
        if((boolean)ForgeNetworkBridge.invoke(loop,"isSameThread")){work.run();return CompletableFuture.completedFuture(null);}
        require(loop instanceof java.util.concurrent.Executor,"CONTEXT_THREAD_UNVERIFIED");
        return CompletableFuture.supplyAsync(()->{work.run();return null;},(java.util.concurrent.Executor)loop);
    }
}
