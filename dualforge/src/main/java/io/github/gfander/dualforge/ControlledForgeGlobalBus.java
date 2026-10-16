package io.github.gfander.dualforge;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** One proven constructor registration, independently validated before any combined output. */
final class ControlledForgeGlobalBus {
    static final String SOURCE = "net/minecraftforge/common/MinecraftForge";
    static final String ANNOTATION = "Lnet/minecraftforge/eventbus/api/SubscribeEvent;";
    static final String EVENT = "net/minecraftforge/event/server/ServerStartedEvent";
    static final String HOST_EVENT = "net/neoforged/neoforge/event/server/ServerStartedEvent";
    static final String HOST_ANNOTATION = "Lnet/neoforged/bus/api/SubscribeEvent;";
    static final String BRIDGE = "io/github/gfander/dualforge/ForgeGlobalEventBridge";
    // The empty-object rejection branch was verified in this exact official 8.0.5 ClassFile.
    static final String HOST_BUS_SHA = "cda8876c904d1374f77bbff02e8cdb01fba48ae0f528c69713eaefa11b5128a4";
    final String listener;
    final Map<String,String> hostFingerprints;

    private ControlledForgeGlobalBus(String listener, Map<String,String> hostFingerprints) {
        this.listener = listener;
        this.hostFingerprints = Map.copyOf(hostFingerprints);
    }
    int listeners() { return listener == null ? 0 : 1; }
    static boolean isCandidate(ClassNode n) {
        return n.methods.stream().flatMap(m -> Arrays.stream(m.instructions.toArray()))
                .anyMatch(i -> i instanceof FieldInsnNode f && f.owner.equals(SOURCE)
                        || i instanceof MethodInsnNode c && c.owner.equals("net/minecraftforge/eventbus/api/IEventBus")
                        && c.name.equals("register") && c.desc.equals("(Ljava/lang/Object;)V"));
    }
    static ControlledForgeGlobalBus validate(ClassNode n, ControlledForgeContextProfile.Scope scope,
            Set<String> helpers, Function<String,byte[]> host) {
        if (!isCandidate(n)) return null;
        RegistryBytecodeVerifier.verify(n);
        require(scope.classes().stream().noneMatch(name -> name.startsWith("java/") || name.startsWith("javax/")
                || name.startsWith("net/minecraft/") || name.startsWith("net/minecraftforge/")
                || name.startsWith("net/neoforged/") || name.startsWith("io/github/gfander/dualforge/")), "PLATFORM_CLASS_OWNER");
        require(empty(n.visibleTypeAnnotations) && empty(n.invisibleTypeAnnotations), "ENTRY_ANNOTATION");
        for (FieldNode f : n.fields) require(empty(f.visibleTypeAnnotations) && empty(f.invisibleTypeAnnotations), "FIELD_ANNOTATION");
        String listener = null; Set<String> methods = new HashSet<>();
        for (MethodNode m : n.methods) {
            require(methods.add(m.name + m.desc), "DUPLICATE_METHOD");
            require(empty(m.visibleTypeAnnotations) && empty(m.invisibleTypeAnnotations)
                    && m.visibleParameterAnnotations == null && m.invisibleParameterAnnotations == null
                    && empty(m.visibleLocalVariableAnnotations) && empty(m.invisibleLocalVariableAnnotations)
                    && m.annotationDefault == null, "METHOD_ANNOTATION");
            for (var op : m.instructions) require(empty(op.visibleTypeAnnotations) && empty(op.invisibleTypeAnnotations), "INSTRUCTION_ANNOTATION");
            if (m.name.equals("<init>") || m.name.equals("<clinit>")) continue;
            require(listener == null && m.access == Opcodes.ACC_PUBLIC && m.desc.equals("(L" + EVENT + ";)V")
                    && m.signature == null && empty(m.exceptions) && m.tryCatchBlocks.isEmpty()
                    && m.visibleAnnotations != null && m.visibleAnnotations.size() == 1
                    && m.visibleAnnotations.getFirst().desc.equals(ANNOTATION)
                    && empty(m.visibleAnnotations.getFirst().values) && empty(m.invisibleAnnotations), "LISTENER_CONTRACT");
            var code = ForgeRegistryTransformationPlan.ops(m);
            require(code.size() == 4 && code.get(0) instanceof FieldInsnNode f && f.getOpcode() == Opcodes.GETSTATIC
                    && f.owner.equals("java/lang/System") && f.name.equals("out") && f.desc.equals("Ljava/io/PrintStream;")
                    && code.get(1) instanceof LdcInsnNode l && l.cst instanceof String
                    && ForgeRegistryTransformationPlan.call(code.get(2), Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V")
                    && code.get(3).getOpcode() == Opcodes.RETURN
                    && Arrays.stream(m.instructions.toArray()).noneMatch(i -> i instanceof FrameNode), "LISTENER_BUSINESS");
            listener = m.name;
        }
        ControlledForgeGlobalBus result = new ControlledForgeGlobalBus(listener, Map.of());
        require(ControlledForgeContextProfile.validateRegistry(result.contextCopy(n), scope, helpers).pendingGlobalCalls() == 1,
                "ONE_CONSTRUCTOR_GLOBAL_REGISTRATION");
        Map<String,String> fingerprints = new TreeMap<>();
        fingerprints.put("net/neoforged/bus/EventBus", verifyHostBus());
        if (listener != null) {
            byte[] event = host.apply(HOST_EVENT);
            require(event != null, "HOST_EVENT_MISSING"); ClassNode target = ForgeRegistryTransformationPlan.read(event);
            require(target.name.equals(HOST_EVENT) && (target.access & Opcodes.ACC_PUBLIC) != 0
                    && target.superName.equals("net/neoforged/neoforge/event/server/ServerLifecycleEvent")
                    && target.interfaces.isEmpty(), "HOST_EVENT_IDENTITY");
            fingerprints.put(HOST_EVENT, ForgeRegistryTransformationPlan.digest(event));
        }
        return new ControlledForgeGlobalBus(listener, fingerprints);
    }
    private ClassNode contextCopy(ClassNode n) {
        ClassNode copy = ForgeRegistryTransformationPlan.read(ForgeRegistryTransformationPlan.bytes(n));
        if (listener != null) copy.methods.removeIf(m -> m.name.equals(listener) && m.desc.equals("(L" + EVENT + ";)V"));
        return copy;
    }
    void rewrite(ClassNode n, ControlledForgeContextProfile.Scope scope, Set<String> helpers, String fingerprint) {
        ClassNode context = contextCopy(n);
        ControlledForgeContextProfile.rewriteRegistry(context, scope, helpers);
        MethodNode ctor = context.methods.stream().filter(m -> m.name.equals("<init>")).findFirst().orElseThrow();
        int index = n.methods.indexOf(n.methods.stream().filter(m -> m.name.equals("<init>")).findFirst().orElseThrow());
        n.methods.set(index, ctor);
        for (var op : ctor.instructions.toArray()) {
            if (op instanceof FieldInsnNode f && f.owner.equals(SOURCE)) ctor.instructions.remove(f);
            if (op instanceof MethodInsnNode c && c.owner.equals("net/minecraftforge/eventbus/api/IEventBus")) {
                ctor.instructions.insertBefore(c, new LdcInsnNode(fingerprint));
                c.setOpcode(Opcodes.INVOKESTATIC); c.owner = BRIDGE; c.desc = "(Ljava/lang/Object;Ljava/lang/String;)V"; c.itf = false;
            }
        }
        if (listener != null) for (MethodNode m : n.methods) if (m.name.equals(listener) && m.desc.equals("(L" + EVENT + ";)V")) {
            m.desc = "(L" + HOST_EVENT + ";)V"; m.visibleAnnotations.getFirst().desc = HOST_ANNOTATION;
            if (m.localVariables != null) for (LocalVariableNode v : m.localVariables)
                if (v.desc.equals("L" + EVENT + ";")) v.desc = "L" + HOST_EVENT + ";";
        }
    }
    static String verifyHostBus() {
        try (var in = net.neoforged.bus.api.IEventBus.class.getResourceAsStream("/net/neoforged/bus/EventBus.class")) {
            require(in != null, "HOST_BUS_RESOURCE"); String digest = ForgeRegistryTransformationPlan.digest(in.readAllBytes());
            require(HOST_BUS_SHA.equals(digest), "UNVERIFIED_HOST_BUS_SEMANTICS"); return digest;
        } catch (IOException e) { throw new IllegalStateException("Global bus rejected: HOST_BUS_READ", e); }
    }
    private static boolean empty(Collection<?> c) { return c == null || c.isEmpty(); }
    private static void require(boolean valid, String reason) { if (!valid) throw new IllegalStateException("Global bus rejected: " + reason); }
}
