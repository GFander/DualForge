package io.github.gfander.dualforge;

import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import net.neoforged.bus.api.*;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import static io.github.gfander.dualforge.ForgeDualBusPlan.require;

/** Constructor definitions are captured once; official host registration occurs only in its FML event. */
public final class ForgeNetworkBridge {
    record Owner(ForgeBusIdentity identity,ForgeNetworkPlan plan,WeakReference<Object> entry,BooleanSupplier complete) { }
    private static final StackWalker WALKER=StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private static final Map<ModContainer,WeakReference<Session>> STATES=new WeakHashMap<>();
    private enum Phase { CONSTRUCTOR_STARTED, GLOBAL_SETTER_ENTERED, GLOBAL_REFERENCE_ASSIGNED, NAMED, VERSION, OPTIONAL, PAYLOAD, PLAY, SERVERBOUND, SERVER_ADDED, CLIENTBOUND, CLIENT_ADDED, PLANNED, REGISTERING, REGISTERED, UNCERTAIN }
    private record Definition(Object type,Object codec,BiConsumer<Object,Object> consumer,ForgeNetworkPlan.Payload source) { }
    private static final class Session {
        final Owner owner;final Object network;List<Definition> definitions=new ArrayList<>();Phase phase=Phase.NAMED;
        Session(Owner owner,Object network){this.owner=owner;this.network=network;}
    }
    private ForgeNetworkBridge() { }
    private static StackWalker.StackFrame caller(){return WALKER.walk(s->s.skip(2).findFirst().orElseThrow());}
    public static Object begin(Object network){
        var caller=caller();require(network!=null && network.getClass()==caller.getDeclaringClass() && caller.getMethodName().equals("<init>"),"WRONG_NETWORK_HANDLER_IDENTITY");Owner owner=ForgeDualBusBridge.networkOwner(caller.getDeclaringClass());require(owner.plan.closure!=null,"WRONG_NETWORK_HANDLER_IDENTITY");
        synchronized(STATES){ModContainer container=owner.identity.owner.get();require(!STATES.containsKey(container),"DUPLICATE_GLOBAL_CONNECTION_WRITE");Session s=new Session(owner,network);s.phase=Phase.CONSTRUCTOR_STARTED;STATES.put(container,new WeakReference<>(s));return s;}
    }
    private static Session active(){synchronized(STATES){var ref=STATES.get(ModLoadingContext.get().getActiveContainer());require(ref!=null && ref.get()!=null,"WRONG_MOD_CONTAINER");return ref.get();}}
    private static Class<?> source(Session s,String name){Class<?> c=Class.forName(s.owner.identity.module,name.replace('/','.'));require(c!=null && c.getClassLoader()==s.owner.identity.loader,"WRONG_CLASSLOADER");return c;}
    public static void globalWrite(Object connection){
        var caller=caller();Session s=active();var p=s.owner.plan.closure;require(p!=null,"GLOBAL_CONNECTION_OWNER_MISMATCH");s.owner.identity.sameOwner(caller.getDeclaringClass());
        require(caller.getDeclaringClass()==source(s,p.global.owner()) && caller.getMethodName().equals(p.global.setter()),"GLOBAL_CONNECTION_OWNER_MISMATCH");require(s.phase==Phase.CONSTRUCTOR_STARTED,"DUPLICATE_GLOBAL_CONNECTION_WRITE");
        require(connection!=null && connection.getClass()==source(s,p.server.owner()) && field(connection.getClass(),connection,p.server.parent())==s.network && field(s.network.getClass(),s.network,p.serverField)==connection,"WRONG_NETWORK_HANDLER_IDENTITY");
        require(field(source(s,p.global.owner()),null,p.global.value())==null,"DUPLICATE_GLOBAL_CONNECTION_WRITE");
        require(field(s.network.getClass(),s.network,ForgeNetworkClosurePlan.ANCHOR)==s,"WRONG_CHANNEL_OWNER");
        require(WALKER.walk(frames->frames.anyMatch(f->f.getDeclaringClass()==s.network.getClass() && f.getMethodName().equals("<init>"))),"GLOBAL_CONNECTION_WRONG_ORDER");s.phase=Phase.GLOBAL_SETTER_ENTERED;
    }
    public static void globalAssigned(Object network){
        var caller=caller();Session s=active();constructor(s,caller);require(network==s.network && s.phase==Phase.GLOBAL_SETTER_ENTERED,"GLOBAL_CONNECTION_WRONG_ORDER");var p=s.owner.plan.closure;
        require(field(source(s,p.global.owner()),null,p.global.value())==field(network.getClass(),network,p.serverField),"GLOBAL_CONNECTION_OWNER_MISMATCH");
        if(p.global.counter()!=null)require(field(source(s,p.global.owner()),null,p.global.counter()).equals(1),"DUPLICATE_GLOBAL_CONNECTION_WRITE");s.phase=Phase.GLOBAL_REFERENCE_ASSIGNED;
    }
    public static void constructorFailure(Object network,Throwable failure){var caller=caller();Session s=active();constructor(s,caller);require(s.network==network && failure!=null,"WRONG_NETWORK_HANDLER_IDENTITY");s.phase=Phase.UNCERTAIN;}
    public static Object named(Object id,Object network) {
        var caller=caller();require(network!=null && network.getClass()==caller.getDeclaringClass() && caller.getMethodName().equals("<init>"),"WRONG_CHANNEL_OWNER");
        Owner owner=ForgeDualBusBridge.networkOwner(caller.getDeclaringClass());
        require(id!=null && id.getClass()==host(ForgeNetworkPlan.RL,"minecraft") && id.toString().equals(owner.plan.channelId),"WRONG_CHANNEL_OWNER");
        synchronized(STATES){ModContainer container=owner.identity.owner.get();
            if(owner.plan.closure!=null){Session s=active();var p=owner.plan.closure;require(s.network==network && s.phase==Phase.GLOBAL_REFERENCE_ASSIGNED,"GLOBAL_CONNECTION_WRONG_ORDER");
                Object entry=owner.entry.get();require(entry!=null && field(network.getClass(),network,p.configField)==field(entry.getClass(),entry,p.entryConfig),"WRONG_NETWORK_HANDLER_IDENTITY");Object client=field(network.getClass(),network,p.clientField);require(client!=null && client.getClass()==source(s,p.client.owner()) && field(client.getClass(),client,p.client.parent())==network,"WRONG_NETWORK_HANDLER_IDENTITY");s.phase=Phase.NAMED;return s;}
            require(!STATES.containsKey(container),"DUPLICATE_REGISTRATION");Session s=new Session(owner,network);STATES.put(container,new WeakReference<>(s));return s;}
    }
    private static Session constructor(Object token,StackWalker.StackFrame caller) {
        require(token instanceof Session,"WRONG_CHANNEL_OWNER");Session s=(Session)token;s.owner.identity.sameOwner(caller.getDeclaringClass());
        require(caller.getDeclaringClass()==s.network.getClass() && caller.getMethodName().equals("<init>") && !s.owner.complete.getAsBoolean(),"WRONG_REGISTRATION_PHASE");
        require(ModLoadingContext.get().getActiveContainer()==s.owner.identity.owner.get(),"WRONG_MOD_CONTAINER");
        synchronized(STATES){var ref=STATES.get(s.owner.identity.owner.get());require(ref!=null && ref.get()==s,"WRONG_CHANNEL_OWNER");}return s;
    }
    private static Session transition(Object token,StackWalker.StackFrame caller,Phase before,Phase after) {
        Session s=constructor(token,caller);require(s.phase==before,"WRONG_REGISTRATION_PHASE expected="+before+" actual="+s.phase);s.phase=after;return s;
    }
    public static Object version(Object token,int version){Session s=constructor(token,caller());require(s.phase==Phase.NAMED,"WRONG_REGISTRATION_PHASE");require(version==s.owner.plan.version,"WRONG_PROTOCOL_VERSION");s.phase=Phase.VERSION;return s;}
    public static Object optional(Object token){return transition(token,caller(),Phase.VERSION,Phase.OPTIONAL);}
    public static Object payloadChannel(Object token){return transition(token,caller(),Phase.OPTIONAL,Phase.PAYLOAD);}
    public static Object play(Object token){return transition(token,caller(),Phase.PAYLOAD,Phase.PLAY);}
    public static Object serverbound(Object token){return transition(token,caller(),Phase.PLAY,Phase.SERVERBOUND);}
    public static Object clientbound(Object token){return transition(token,caller(),Phase.SERVER_ADDED,Phase.CLIENTBOUND);}
    public static Object addMain(Object token,Object type,Object codec,BiConsumer<?,?> consumer){return capture(token,type,codec,consumer,caller(),false);}
    public static Object add(Object token,Object type,Object codec,BiConsumer<?,?> consumer){return capture(token,type,codec,consumer,caller(),true);}
    @SuppressWarnings("unchecked")
    private static Object capture(Object token,Object type,Object codec,BiConsumer<?,?> consumer,StackWalker.StackFrame caller,boolean contextful) {
        Session s=constructor(token,caller);int index=s.definitions.size();require(index<s.owner.plan.payloads.size(),"DUPLICATE_PAYLOAD_ID");
        require((s.owner.plan.closure!=null)==contextful,"CONTEXT_ENQUEUE_UNVERIFIED");var p=s.owner.plan.payloads.get(index);
        boolean server=s.phase==Phase.SERVERBOUND || contextful && s.phase==Phase.SERVER_ADDED;boolean client=s.phase==Phase.CLIENTBOUND || contextful && s.phase==Phase.CLIENT_ADDED;
        require((server && p.direction().equals("SERVERBOUND")) || (client && p.direction().equals("CLIENTBOUND")),"WRONG_PAYLOAD_DIRECTION");
        Class<?> packet=source(s,p.owner());require(type==field(packet,null,p.typeField()) && codec==field(packet,null,p.codecField()),"WRONG_CODEC_OWNER");
        require(type.getClass()==host(ForgeNetworkPlan.PT,"minecraft") && invoke(type,"id").toString().equals(p.id()),"CROSS_MODFILE_PAYLOAD");
        require(consumer!=null && consumer.getClass().isHidden() && consumer.getClass().getModule()==s.owner.identity.module && consumer.getClass().getClassLoader()==s.owner.identity.loader && consumer==field(s.network.getClass(),s.network,p.handlerField()),"UNVERIFIED_HANDLER_CAPTURE");
        s.definitions.add(new Definition(type,codec,(BiConsumer<Object,Object>)consumer,p));s.phase=server?Phase.SERVER_ADDED:Phase.CLIENT_ADDED;return s;
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    public static Object build(Object token) {
        Session s=constructor(token,caller());Phase expected=s.owner.plan.closure!=null && s.owner.plan.closure.permission?Phase.SERVER_ADDED:Phase.CLIENT_ADDED;require(s.phase==expected,"WRONG_REGISTRATION_PHASE expected="+expected+" actual="+s.phase);s.phase=Phase.PLANNED;require(s.definitions.size()==s.owner.plan.payloads.size(),"PARTIAL_NETWORK_PLAN");
        s.definitions=List.copyOf(s.definitions);
        Class<?> event=host("net/neoforged/neoforge/network/event/RegisterPayloadHandlersEvent","neoforge");
        try {s.owner.identity.mod.addListener(EventPriority.NORMAL,false,(Class)event,(Consumer)(e->register(s,e)));}
        catch(RuntimeException|Error failure){s.phase=Phase.UNCERTAIN;throw failure;}return s;
    }
    private static void register(Session s,Object event) {
        require(s.owner.complete.getAsBoolean(),"REGISTRATION_TOO_EARLY");require(s.phase==Phase.PLANNED,"DUPLICATE_REGISTRATION actual="+s.phase);
        require(event!=null && event.getClass()==host("net/neoforged/neoforge/network/event/RegisterPayloadHandlersEvent","neoforge"),"WRONG_REGISTRATION_PHASE");
        Class<?> registry=host("net/neoforged/neoforge/network/registration/NetworkRegistry","neoforge");
        require(WALKER.walk(frames->frames.anyMatch(f->f.getDeclaringClass()==registry && f.getMethodName().equals("setup"))),"WRONG_REGISTRATION_PHASE");
        s.owner.identity.sameOwner(s.network.getClass());Object entry=s.owner.entry.get();require(entry!=null && field(entry.getClass(),entry,s.owner.plan.entryField)==s.network,"WRONG_CHANNEL_OWNER");
        require(field(s.network.getClass(),s.network,s.owner.plan.channelField)==s,"WRONG_CHANNEL_OWNER");
        s.phase=Phase.REGISTERING;
        try {
            Object registrar=invoke(event,"registrar",Integer.toString(s.owner.plan.version));
            Class<?> thread=host("net/neoforged/neoforge/network/registration/HandlerThread","neoforge");Object main=enumValue(thread,s.owner.plan.closure==null?"MAIN":"NETWORK");
            registrar=invoke(registrar,"executesOn",main);registrar=invoke(registrar,"optional");
            Class<?> handler=host("net/neoforged/neoforge/network/handling/IPayloadHandler","neoforge");
            for(Definition d:s.definitions) {
                Object proxy=Proxy.newProxyInstance(handler.getClassLoader(),new Class<?>[]{handler},(self,method,args)->{
                    if(method.getDeclaringClass()==Object.class)return switch(method.getName()){case "toString"->"DualForge verified payload handler";case "hashCode"->System.identityHashCode(self);case "equals"->self==args[0];default->throw new IllegalStateException("UNVERIFIED_HANDLER");};
                    require(method.getName().equals("handle") && args!=null && args.length==2 && method.getReturnType()==void.class,"UNVERIFIED_HANDLER");
                    require(s.phase==Phase.REGISTERED,"WRONG_REGISTRATION_PHASE");
                    Object payload=args[0],context=args[1];Class<?> packet=Class.forName(s.owner.identity.module,d.source.owner().replace('/','.'));
                    require(payload!=null && payload.getClass()==packet && invoke(payload,"type")==d.type,"WRONG_PAYLOAD_DIRECTION");
                    require(context!=null && context.getClass().getModule()==registry.getModule() && context.getClass().getClassLoader()==registry.getClassLoader(),"UNVERIFIED_HANDLER");
                    require(invoke(context,"flow").toString().equals(d.source.direction()),"WRONG_PAYLOAD_DIRECTION");
                    // Supported source callbacks never inspect the Forge Context; an opaque private token preserves that finite ABI.
                    if(s.owner.plan.closure==null)d.consumer.accept(payload,new Object());
                    else {var scope=ForgeSourceContextBridge.open(s.owner.identity,context,s.network,d.source.handlerMethod(),d.source.direction(),s.owner.plan.closure.permission);boolean normal=false;try{d.consumer.accept(payload,scope);normal=true;}finally{ForgeSourceContextBridge.close(scope,normal);}}return null;
                });
                invoke(registrar,d.source.direction().equals("SERVERBOUND")?"playToServer":"playToClient",d.type,d.codec,proxy);
            }
            s.phase=Phase.REGISTERED;
        } catch(RuntimeException|Error failure){s.phase=Phase.UNCERTAIN;throw failure;}
    }
    private static Class<?> host(String internal,String module){return ForgeBusIdentity.host(internal.replace('/','.'),module);}
    @SuppressWarnings({"unchecked","rawtypes"}) private static Object enumValue(Class<?> type,String name){return Enum.valueOf((Class)type,name);}
    private static Object field(Class<?> type,Object value,String name){try{Field f=type.getDeclaredField(name);require(f.trySetAccessible(),"WRONG_MODULE");return f.get(value);}catch(ReflectiveOperationException e){throw new IllegalStateException("WRONG_CHANNEL_OWNER "+name,e);}}
    static Object invoke(Object target,String name,Object...args) {
        Method selected=null;
        for(Method method:target.getClass().getMethods())if(!method.isBridge() && method.getName().equals(name) && method.getParameterCount()==args.length) {
            boolean compatible=true;Class<?>[] types=method.getParameterTypes();for(int i=0;i<args.length;i++)compatible&=args[i]!=null && types[i].isInstance(args[i]);
            if(compatible){require(selected==null,"UNVERIFIED_NETWORK_RESULT ambiguous "+name);selected=method;}
        }
        require(selected!=null,"UNVERIFIED_NETWORK_RESULT missing "+name);
        try {return selected.invoke(target,args);}catch(InvocationTargetException e){if(e.getCause() instanceof RuntimeException x)throw x;if(e.getCause() instanceof Error x)throw x;throw new IllegalStateException(e.getCause());}
        catch(ReflectiveOperationException e){throw new IllegalStateException("HOST_NETWORK_API "+name,e);}
    }
}
