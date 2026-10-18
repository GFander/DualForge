package io.github.gfander.dualforge;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Isolated real native Bus/Event API; no GAME layer or FML construction is claimed. */
public final class NativeBusContract {
    static int assertions;
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);assertions++;}
    static Object enumValue(Class<?> c,String name){return Enum.valueOf(c.asSubclass(Enum.class),name);}
    public static void main(String[] args)throws Exception {
        boolean forge=args[0].equals("forge");String busPackage=forge?"net.minecraftforge.eventbus.api.":"net.neoforged.bus.api.";
        Object transformer=forge?Class.forName("net.minecraftforge.eventbus.EventSubclassTransformer").getConstructor().newInstance():null;
        ClassLoader loader=new ClassLoader(NativeBusContract.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                if(transformer!=null && name.startsWith("net.minecraftforge.event.entity."))synchronized(getClassLoadingLock(name)) {
                    Class<?> c=findLoadedClass(name);if(c==null)try(var in=getParent().getResourceAsStream(name.replace('.','/')+".class")) {
                        if(in==null)throw new ClassNotFoundException(name);ClassNode n=new ClassNode();new ClassReader(in.readAllBytes()).accept(n,0);
                        transformer.getClass().getMethod("transform",ClassNode.class,org.objectweb.asm.Type.class).invoke(transformer,n,org.objectweb.asm.Type.getObjectType(name.replace('.','/')));
                        ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);n.accept(w);byte[] b=w.toByteArray();c=defineClass(name,b,0,b.length);
                    } catch(Exception e){throw new ClassNotFoundException(name,e);}if(resolve)resolveClass(c);return c;
                }
                return super.loadClass(name,resolve);
            }
        };
        Thread.currentThread().setContextClassLoader(loader);
        Class<?> eventBase=Class.forName(busPackage+"Event"), busAPI=Class.forName(busPackage+"IEventBus"),priority=Class.forName(busPackage+"EventPriority");
        Class<?> builder=Class.forName(busPackage+"BusBuilder");Object b=builder.getMethod("builder").invoke(null);Object bus=builder.getMethod("build").invoke(b);
        Class<?> event=Class.forName((forge?"net.minecraftforge.":"net.neoforged.neoforge.")+"event.entity.living.LivingDeathEvent",true,loader);
        Constructor<?> ctor=event.getConstructors()[0];Object[] params=new Object[ctor.getParameterCount()];
        Method add=busAPI.getMethod("addListener",priority,boolean.class,Class.class,Consumer.class),post=busAPI.getMethod("post",eventBase),unregister=busAPI.getMethod("unregister",Object.class);
        Method set=event.getMethod("setCanceled",boolean.class),get=event.getMethod("isCanceled");
        List<String> order=new ArrayList<>();Thread current=Thread.currentThread();Object[] actual={null};
        Consumer<Object> high=e->{check(Thread.currentThread()==current,"synchronous callback thread");actual[0]=e;order.add("high");try{set.invoke(e,true);}catch(Exception ex){throw new RuntimeException(ex);}};
        Consumer<Object> skip=e->order.add("skip"),yes=e->{check(e==actual[0],"same native event argument");order.add("receive");};
        add.invoke(bus,enumValue(priority,"HIGH"),false,event,high);add.invoke(bus,enumValue(priority,"NORMAL"),false,event,skip);add.invoke(bus,enumValue(priority,"LOW"),true,event,yes);
        Object e=ctor.newInstance(params);Object result=post.invoke(bus,e);
        check(order.equals(List.of("high","receive")) && (boolean)get.invoke(e),"cancellation receiveCanceled priority count");
        check(forge?result.equals(true):result==e,"source/native post ABI explicitly differs; production post unsupported");
        check(event.getMethod("getEntity").invoke(e)==null && event.getMethod("getSource").invoke(e)==null,"parameters/outcome unchanged");
        for(Object listener:List.of(high,skip,yes))unregister.invoke(bus,listener);
        order.clear();post.invoke(bus,ctor.newInstance(params));check(order.isEmpty(),"unregister actual same Consumer objects");
        RuntimeException sentinel=new RuntimeException("010B_NATIVE_SENTINEL");Consumer<Object> failing=x->{throw sentinel;};
        add.invoke(bus,enumValue(priority,"NORMAL"),false,event,failing);
        try{post.invoke(bus,ctor.newInstance(params));throw new AssertionError("exception swallowed");}catch(InvocationTargetException failure){check(failure.getCause()==sentinel,"callback exception propagated without substitution");}
        unregister.invoke(bus,failing);
        System.out.println("NATIVE_BUS_API_PASS side="+args[0]+" assertions="+assertions+" scope=ISOLATED_NATIVE_API");
    }
}
