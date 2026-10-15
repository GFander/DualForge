package io.github.gfander.dualforge;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLLoader;

/** Exact public host API invocation across the service/GAME boundary; no raw bus escapes. */
public final class ForgeRegistryBridge {
    private static final StackWalker WALKER=StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private enum Attachment { ATTACHING, ATTACHED, UNCERTAIN }
    private static final Map<ModContainer,Map<Object,Attachment>> ATTACHED=new WeakHashMap<>();
    private ForgeRegistryBridge() { }

    public static void register(Object receiver, net.minecraftforge.eventbus.api.IEventBus source, String fingerprint) {
        var caller=WALKER.walk(f -> f.skip(1).findFirst().orElseThrow());
        Class<?> helper=caller.getDeclaringClass();
        ModContainer owner=ForgeConstructorContext.registryOwner(source,helper);
        var info=owner.getModInfo();var scan=info.getOwningFile().getFile().getScanResult();
        String entry=DualForgeLanguageLoader.forgeEntrypoints(info,scan).getFirst().replace('.','/');
        var plan=ForgeRegistryTransformationPlan.create(info,entry);
        String internal=helper.getName().replace('.','/');
        require(plan.fingerprint.equals(fingerprint),"PLAN_FINGERPRINT_MISMATCH");
        require(plan.helperMethods.contains(internal+"."+caller.getMethodName()+"("+ForgeRegistryTransformationPlan.BUS+")V"),"UNVERIFIED_HELPER_CALLER");
        try {
            Class<?> register=validateReceiver(receiver,helper,owner,plan,internal);
            synchronized(ATTACHED) {
                Map<Object,Attachment> attached=ATTACHED.computeIfAbsent(owner,k->new IdentityHashMap<>());
                Attachment state=attached.get(receiver);
                require(state!=Attachment.UNCERTAIN,"REGISTRY_ATTACHMENT_UNCERTAIN");
                require(state!=Attachment.ATTACHING,"REGISTRY_ATTACHMENT_IN_PROGRESS");
                require(state==null,"DUPLICATE_REGISTRY_ATTACHMENT");
                var official=register.getMethod("register",net.neoforged.bus.api.IEventBus.class);
                attached.put(receiver,Attachment.ATTACHING);
                try {official.invoke(receiver,owner.getEventBus());attached.put(receiver,Attachment.ATTACHED);}
                catch(ReflectiveOperationException|RuntimeException|Error failure){attached.put(receiver,Attachment.UNCERTAIN);throw failure;}
            }
            System.out.println("[DUALFORGE-REGISTRY-BRIDGE] ATTACHED modId="+owner.getModId()+" helper="+helper.getName()
                    +" registry="+plan.registryKind(internal).id+" nativeBus="+System.identityHashCode(owner.getEventBus())+" plan="+fingerprint);
        } catch(InvocationTargetException e) {
            throw new IllegalStateException("Registry bridge official register failed",e.getCause());
        } catch(ReflectiveOperationException e) {
            throw new IllegalStateException("Registry bridge fixed host ABI unavailable",e);
        }
    }
    private static Class<?> validateReceiver(Object receiver,Class<?> helper,ModContainer owner,ForgeRegistryTransformationPlan plan,String internal) throws ReflectiveOperationException {
        var game=FMLLoader.getGameLayer();
        require(helper.getModule().getLayer()==game,"WRONG_GAME_LAYER");
        Class<?> register=Class.forName(game.findModule("neoforge").orElseThrow(),"net.neoforged.neoforge.registries.DeferredRegister");
        require(receiver!=null && receiver.getClass()==register,"NON_NATIVE_DEFERRED_REGISTER");
        require(helper.getField(plan.registerField(internal)).get(null)==receiver,"FOREIGN_REGISTER_RECEIVER");
        require(register.getMethod("getNamespace").invoke(receiver).equals(owner.getModId()),"WRONG_REGISTER_NAMESPACE");
        registryIdentity(register,receiver,plan.registryKind(internal).field);
        return register;
    }
    private static void registryIdentity(Class<?> register,Object receiver,String field) throws ReflectiveOperationException {
        Class<?> registries=Class.forName(FMLLoader.getGameLayer().findModule("minecraft").orElseThrow(),"net.minecraft.core.registries.Registries");
        require(register.getMethod("getRegistryKey").invoke(receiver)==registries.getField(field).get(null),"WRONG_REGISTRY_IDENTITY");
    }
    private static void require(boolean valid,String reason){if(!valid)throw new IllegalStateException("Registry bridge rejected: "+reason);}
}
