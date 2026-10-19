package io.github.gfander.dualforge;
import java.util.Map;
import java.util.function.Function;
import static io.github.gfander.dualforge.ForgeDualBusPlan.require;
/** Fixed host registration/queue ABI. This proves no connection or packet delivery. */
final class ForgePayloadSemantics {
    private ForgePayloadSemantics() { }
    static void verifyHost(Function<String,byte[]> host) {
        Map.ofEntries(
            Map.entry("net/neoforged/neoforge/network/event/RegisterPayloadHandlersEvent","41e57663d541e64dda3ed1eb18f28940bf62dbed4d42c2425478ecb3f1aa3ad1"),
            Map.entry("net/neoforged/neoforge/network/handling/IPayloadContext","8812a7faba3bd2a66159d2b388054a7628868415f9227061603cee8f56494a59"),
            Map.entry("net/neoforged/neoforge/network/handling/IPayloadHandler","e14ad9de95ef6c544f752b54b3601360d534ab40ece97ee2a782d9cbaa230824"),
            Map.entry("net/neoforged/neoforge/network/handling/MainThreadPayloadHandler","3a7313538c0834ab98ea0e14db431c062a158e598c850224f934f0b173d0a60e"),
            Map.entry("net/neoforged/neoforge/network/registration/HandlerThread","ccec8f4d5646d4c19a2ab000ce9757c3771e334830b6a904c7820020ac9f4a5b"),
            Map.entry("net/neoforged/neoforge/network/registration/NetworkRegistry","68d5cf8a94f4d15812e3bc2ec8a547a66b717eebb3ae0490b791eab2dedbdbb8"),
            Map.entry("net/neoforged/neoforge/network/registration/PayloadRegistrar","e8689c5139a03ee1740752c9190c5ee3812d647f961aee327692a4a1114ce025"),
            Map.entry("net/neoforged/neoforge/network/registration/PayloadRegistration","5bc077cae564adba4eb4726e0628d8efab655eb0c4a5490d73ef101a61d07e6f")
        ).forEach((name,sha)->{byte[] b=host.apply(name);require(b!=null && ForgeDualBusPlan.digest(b).equals(sha),"UNVERIFIED_HOST_NETWORK_ABI "+name);});
    }
}
