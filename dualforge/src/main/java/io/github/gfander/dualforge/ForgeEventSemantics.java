package io.github.gfander.dualforge;

import java.util.Map;
import net.neoforged.bus.api.EventPriority;
import static io.github.gfander.dualforge.ForgeDualBusPlan.require;

/** Finite event contracts, independent of owner resolution and host side effects. */
final class ForgeEventSemantics {
    private ForgeEventSemantics() { }
    static boolean modEvent(Class<?> event) {
        require(event!=null,"WRONG_EVENT_CLASS");
        for(int i=0;i<ForgeDualBusPlan.EVENTS.size();i++) {
            String name=ForgeDualBusPlan.TYPES.get(ForgeDualBusPlan.EVENTS.get(i)).replace('/','.');
            if(event.getName().equals(name)) {
                require(event==ForgeBusIdentity.host(name,i==0?"fml_loader":"neoforge"),"WRONG_EVENT_CLASS");
                boolean mod=net.neoforged.fml.event.IModBusEvent.class.isAssignableFrom(event);
                require(mod==(i==0),"WRONG_EVENT_DOMAIN");
                require(net.neoforged.bus.api.ICancellableEvent.class.isAssignableFrom(event)==(i==2),"UNSUPPORTED_EVENT_SEMANTICS");
                return mod;
            }
        }
        throw new IllegalStateException("Dual bus rejected: WRONG_EVENT_CLASS "+event.getName());
    }
    static void options(EventPriority priority,boolean receiveCanceled,Class<?> event) {
        require(priority==EventPriority.NORMAL,"UNVERIFIED_PRIORITY");
        require(!receiveCanceled || net.neoforged.bus.api.ICancellableEvent.class.isAssignableFrom(event),"UNVERIFIED_RECEIVE_CANCELED");
    }
    static byte[] hostBytes(String name) {
        if(!name.startsWith("net/neoforged/fml/"))return ControlledItemSupplier.hostResource(name);
        try(var in=net.neoforged.fml.ModContainer.class.getResourceAsStream("/"+name+".class")){return in==null?null:in.readAllBytes();}
        catch(java.io.IOException e){throw new IllegalStateException("HOST_FML_ABI_UNAVAILABLE",e);}
    }
    static void verifyHost(java.util.function.Function<String,byte[]> host) {
        Map.ofEntries(
            Map.entry("net/neoforged/fml/event/lifecycle/FMLCommonSetupEvent","62ce6bab9da7b4dca981fbba645f89adb794fcfa5005395bbc95398e753b60a1"),
            Map.entry("net/neoforged/fml/event/IModBusEvent","af1f7d5238b786c6e5bf3080de7478edf503269ac205b186ad74d856a2842783"),
            Map.entry("net/neoforged/neoforge/event/server/ServerStartedEvent","7b769996d7eb8a21061afe07ec4086ef6f660745e677d976f3ecb4e899941afb"),
            Map.entry("net/neoforged/neoforge/event/server/ServerLifecycleEvent","8308c211b25991967fbc2cc21bd8bf1fd834bae93b0f52cc411756d547bf2557"),
            Map.entry("net/neoforged/neoforge/event/entity/living/LivingDeathEvent","8793d2deab130ab73cf2f94f1e857dc35e71222ac17179e820e8191db7636b49"),
            Map.entry("net/neoforged/neoforge/event/entity/living/LivingEvent","265386d0ed8ea8c8e04af0d601b2baec65e466a81137f05dae7f455e4c392cc5"),
            Map.entry("net/neoforged/neoforge/common/ModConfigSpec","7edd46a6754ef61d80804bb4a239883dd5c53bc0173cf3ba545f0e42770e0d36"),
            Map.entry("net/neoforged/neoforge/common/ModConfigSpec$Builder","4ed71b6fa3e6cb468fa3e998cfbcc0c33248e76c81bf668e61f91a992abbf2a2"),
            Map.entry("net/neoforged/neoforge/common/ModConfigSpec$BooleanValue","0efd283af0543e3f8b6995e563cd07b15ee457de1a6067ecc6d216c0678f5016"),
            Map.entry("net/neoforged/neoforge/common/ModConfigSpec$ConfigValue","54d1b02a155617bd24f0d7751683bfab37ff354bec9b50e6a82e4ed69946f7c7")
        ).forEach((name,sha)->{byte[] b=host.apply(name);require(b!=null && ForgeDualBusPlan.digest(b).equals(sha),"UNVERIFIED_HOST_ABI "+name);});
    }
}
