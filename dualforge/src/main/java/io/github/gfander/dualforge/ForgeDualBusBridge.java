package io.github.gfander.dualforge;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;
import net.neoforged.bus.api.*;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.*;
import static io.github.gfander.dualforge.ForgeDualBusPlan.require;

/** Host operations on official buses/configs; semantic and owner checks precede effects. */
public final class ForgeDualBusBridge {
    private static final StackWalker WALKER=StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    // Both sides weak. Live entry/router fields anchor their session; no static value -> owner cycle.
    private static final Map<ModContainer,WeakReference<Session>> STATES=new WeakHashMap<>();
    private enum Phase { CONSTRUCTING, COMPLETE, UNCERTAIN }
    private record Registration(IEventBus bus,Class<?> event) { }
    private static final class Session {
        final ForgeBusIdentity identity;final ForgeDualBusPlan plan;final WeakReference<Object> entry;
        WeakReference<Object> router=new WeakReference<>(null);final IdentityHashMap<Object,Registration> registered=new IdentityHashMap<>();
        int next;boolean configured;Phase phase=Phase.CONSTRUCTING;
        Session(ModContainer owner,Object entry,ForgeDualBusPlan plan){identity=new ForgeBusIdentity(owner,entry.getClass());this.entry=new WeakReference<>(entry);this.plan=plan;}
    }
    private ForgeDualBusBridge() { }
    private static StackWalker.StackFrame caller(){return WALKER.walk(f->f.skip(2).findFirst().orElseThrow());}
    public static Object begin(Object entry,String fingerprint) {
        var c=caller();require(c.getMethodName().equals("<init>") && entry!=null && entry.getClass()==c.getDeclaringClass(),"UNSUPPORTED_BOOTSTRAP");
        ModContainer owner=ForgeBusIdentity.active(entry.getClass());ForgeDualBusPlan plan=ForgeDualBusPlan.create(owner.getModInfo(),entry.getClass().getName().replace('.','/'));
        require(plan.fingerprint.equals(fingerprint),"INPUT_SHA_CHANGED");
        synchronized(STATES){require(!STATES.containsKey(owner),"DUPLICATE_INITIALIZATION");Session s=new Session(owner,entry,plan);STATES.put(owner,new WeakReference<>(s));return s;}
    }
    private static Session current(StackWalker.StackFrame c) {
        ModContainer owner=ForgeBusIdentity.active(c.getDeclaringClass());
        synchronized(STATES){var ref=STATES.get(owner);require(ref!=null && ref.get()!=null,"UNKNOWN_BUS_ORIGIN");Session s=ref.get();
            require(c.getMethodName().equals("<init>") && c.getClassName().replace('.','/').equals(s.plan.entry),"UNSUPPORTED_BOOTSTRAP");construction(s);return s;}
    }
    public static Object context(){return current(caller());}
    public static IEventBus game(){return current(caller()).identity.game;}
    public static IEventBus mod(Object token){Session s=current(caller());require(s==token,"FOREIGN_MOD_BUS");return s.identity.mod;}
    public static Object bind(Object router,IEventBus game,IEventBus mod) {
        var c=caller();ModContainer owner=ModLoadingContext.get().getActiveContainer();Session s;
        synchronized(STATES){var ref=STATES.get(owner);require(ref!=null && ref.get()!=null,"WRONG_MOD_CONTAINER");s=ref.get();}
        s.identity.sameOwner(c.getDeclaringClass());construction(s);
        require(c.getMethodName().equals("<init>") && router!=null && router.getClass()==c.getDeclaringClass()
            && c.getClassName().replace('.','/').equals(s.plan.router) && s.router.get()==null,"WRONG_LISTENER_OWNER");
        require(game==s.identity.game,"UNKNOWN_BUS_ORIGIN");require(mod==s.identity.mod,"FOREIGN_MOD_BUS");entryFrame(s);
        s.router=new WeakReference<>(router);return s;
    }
    public static void config(Object token,ModConfig.Type type,IConfigSpec spec) {
        var c=caller();Session s=session(token,c);construction(s);entryFrame(s);
        require(c.getClassName().replace('.','/').equals(s.plan.config),"CONFIG_OWNER_MISMATCH");
        require(!s.configured,"DUPLICATE_CONFIG_REGISTRATION");require(type!=null && type.name().equals(s.plan.configType),"CONFIG_SPEC_UNSUPPORTED");
        require(spec!=null && spec.getClass()==ForgeBusIdentity.host("net.neoforged.neoforge.common.ModConfigSpec","neoforge"),"CONFIG_SPEC_UNSUPPORTED");
        synchronized(s){try {s.identity.owner.get().registerConfig(type,spec);s.configured=true;}
            catch(RuntimeException|Error e){s.phase=Phase.UNCERTAIN;throw e;}}
    }
    @SuppressWarnings({"unchecked","rawtypes"})
    public static void addListener(IEventBus bus,EventPriority priority,boolean canceled,Class<?> event,Consumer<?> consumer,Object token) {
        var c=caller();Session s=session(token,c);
        synchronized(s){construction(s);entryFrame(s);require(c.getClassName().replace('.','/').equals(s.plan.router),"WRONG_LISTENER_OWNER");
            boolean mod=ForgeEventSemantics.modEvent(event);s.identity.route(bus,mod);ForgeEventSemantics.options(priority,canceled,event);
            require(consumer!=null && consumer.getClass().isHidden(),"WRONG_CONSUMER_TYPE");
            require(consumer.getClass().getModule()==s.identity.module && consumer.getClass().getClassLoader()==s.identity.loader,"WRONG_LISTENER_OWNER");
            require(!s.registered.containsKey(consumer),"DUPLICATE_LISTENER");require(s.configured && s.next<s.plan.listeners.size(),"WRONG_REGISTRATION_ORDER");
            var expected=s.plan.listeners.get(s.next);require(event.getName().replace('.','/').equals(ForgeDualBusPlan.TYPES.get(expected.event())) && canceled==expected.canceled(),"WRONG_REGISTRATION_ORDER");
            require(field(s.entry.get(),expected.field())==consumer,"WRONG_CONSUMER_TYPE");
            installListener(s,bus,priority,canceled,event,consumer);
        }
    }
    @SuppressWarnings({"unchecked","rawtypes"})
    private static void installListener(Session s,IEventBus bus,EventPriority priority,boolean canceled,Class<?> event,Consumer<?> consumer) {
        try {bus.addListener(priority,canceled,(Class)event,(Consumer)consumer);s.registered.put(consumer,new Registration(bus,event));s.next++;}
        catch(RuntimeException|Error e){s.phase=Phase.UNCERTAIN;throw e;}
    }
    public static void unregister(IEventBus bus,Object consumer,Class<?> event,Object token) {
        var c=caller();Session s=session(token,c);synchronized(s){require(s.phase==Phase.COMPLETE,"WRONG_REGISTRATION_ORDER");
            require(c.getClassName().replace('.','/').equals(s.plan.router),"WRONG_LISTENER_OWNER");s.identity.route(bus,ForgeEventSemantics.modEvent(event));
            Registration registered=s.registered.get(consumer);require(registered!=null && registered.bus()==bus,"FOREIGN_MOD_BUS");require(registered.event()==event,"WRONG_EVENT_CLASS");
            try {bus.unregister(consumer);s.registered.remove(consumer);}catch(RuntimeException|Error e){s.phase=Phase.UNCERTAIN;throw e;}}
    }
    public static void complete(Object entry) {
        var c=caller();Session s=current(c);require(s.entry.get()==entry,"WRONG_LISTENER_OWNER");synchronized(s){
            require(s.configured && s.next==s.plan.listeners.size() && s.router.get()!=null,"WRONG_REGISTRATION_ORDER");s.phase=Phase.COMPLETE;}
    }
    private static Session session(Object token,StackWalker.StackFrame c) {
        require(token instanceof Session,"UNKNOWN_BUS_ORIGIN");Session s=(Session)token;s.identity.sameOwner(c.getDeclaringClass());
        synchronized(STATES){var ref=STATES.get(s.identity.owner.get());require(ref!=null && ref.get()==s,"WRONG_MOD_CONTAINER");}return s;
    }
    private static void construction(Session s){require(s.phase==Phase.CONSTRUCTING,"WRONG_REGISTRATION_ORDER");require(ModLoadingContext.get().getActiveContainer()==s.identity.owner.get(),"WRONG_MOD_CONTAINER");}
    private static void entryFrame(Session s){require(WALKER.walk(frames->frames.anyMatch(f->f.getDeclaringClass()==s.entry.get().getClass() && f.getMethodName().equals("<init>"))),"UNSUPPORTED_BOOTSTRAP");}
    private static Object field(Object target,String name){require(target!=null,"WRONG_LISTENER_OWNER");try{Field f=target.getClass().getDeclaredField(name);require(f.trySetAccessible(),"WRONG_MODULE");return f.get(target);}catch(ReflectiveOperationException e){throw new IllegalStateException("WRONG_CONSUMER_TYPE",e);}}
}
