package io.github.gfander.dualforge;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** Only two Context calls are mapped; owned helper and Registry business bytes are never rewritten. */
final class ControlledForgeContextProfile {
    static final String SOURCE = "net/minecraftforge/fml/javafmlmod/FMLJavaModLoadingContext";
    static final String BRIDGE = "io/github/gfander/dualforge/ForgeConstructorContext";
    static final String BUS = "Lnet/minecraftforge/eventbus/api/IEventBus;";
    private static final String DEFERRED = "net/minecraftforge/registries/DeferredRegister";
    record Scope(String modId, Set<String> classes, Function<String, byte[]> read) { }
    record Stats(int helpers, int receipts, int pendingGlobalCalls) { }
    private record Plan(MethodInsnNode get, MethodInsnNode bus, Stats stats) {
        void apply() { get.owner = BRIDGE; get.desc = "()L" + BRIDGE + ";"; bus.owner = BRIDGE; }
    }

    static boolean isCandidate(ClassNode node) {
        return node.methods.stream().flatMap(m -> java.util.Arrays.stream(m.instructions.toArray()))
                .anyMatch(i -> i instanceof MethodInsnNode m && SOURCE.equals(m.owner));
    }
    static Stats validate(ClassNode node, Scope scope) { return plan(node, scope).stats; }
    static Stats rewrite(ClassNode node, Scope scope) { Plan plan = plan(node, scope); plan.apply(); return plan.stats; }

    static Stats validateRegistry(ClassNode node, Scope scope, Set<String> verifiedHelpers) {
        return plan(node, scope, verifiedHelpers).stats;
    }
    static Stats rewriteRegistry(ClassNode node, Scope scope, Set<String> verifiedHelpers) {
        Plan plan = plan(node, scope, verifiedHelpers); plan.apply(); return plan.stats;
    }
    private static Plan plan(ClassNode node, Scope scope) { return plan(node, scope, Set.of()); }
    private static Plan plan(ClassNode node, Scope scope, Set<String> verifiedHelpers) {
        require(node.version == Opcodes.V21 && (node.access == (Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER)
                || node.access == (Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER))
                && "java/lang/Object".equals(node.superName) && node.interfaces.isEmpty() && node.signature == null, "class structure");
        require(node.visibleAnnotations != null && node.visibleAnnotations.size() == 1
                && (node.invisibleAnnotations == null || node.invisibleAnnotations.isEmpty())
                && "Lnet/minecraftforge/fml/common/Mod;".equals(node.visibleAnnotations.getFirst().desc)
                && List.of("value", scope.modId).equals(node.visibleAnnotations.getFirst().values), "owned @Mod");
        require(scope.classes.contains(node.name), "entry absent from owning scan");
        var constructors = node.methods.stream().filter(m -> "<init>".equals(m.name)).toList();
        require(constructors.size() == 1 && constructors.getFirst().access == Opcodes.ACC_PUBLIC
                && "()V".equals(constructors.getFirst().desc), "unique public no-arg constructor");
        MethodNode ctor = constructors.getFirst();
        require(node.methods.stream().allMatch(m -> m == ctor || "<clinit>".equals(m.name))
                && node.methods.size() <= 2, "extra entry methods");
        for (MethodNode method : node.methods) {
            require(method.signature == null && method.tryCatchBlocks.isEmpty()
                    && (method.exceptions == null || method.exceptions.isEmpty())
                    && (method.visibleAnnotations == null || method.visibleAnnotations.isEmpty())
                    && (method.invisibleAnnotations == null || method.invisibleAnnotations.isEmpty())
                    && java.util.Arrays.stream(method.instructions.toArray()).noneMatch(i -> i instanceof FrameNode), "method metadata or frames");
        }
        long contexts = node.methods.stream().flatMap(m -> java.util.Arrays.stream(m.instructions.toArray()))
                .filter(i -> i instanceof MethodInsnNode m && SOURCE.equals(m.owner)).count();
        require(contexts == 2, "unique direct context source");
        Set<String> receipts = new HashSet<>(); FieldNode logger = null; Set<String> fields = new HashSet<>();
        for (FieldNode field : node.fields) {
            require(fields.add(field.name) && (field.visibleAnnotations == null || field.visibleAnnotations.isEmpty())
                    && (field.invisibleAnnotations == null || field.invisibleAnnotations.isEmpty()) && field.signature == null, "field metadata");
            if (field.access == (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC) && "Ljava/lang/Object;".equals(field.desc) && field.value == null) receipts.add(field.name);
            else if (field.access == (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)
                    && "Ljava/lang/String;".equals(field.desc) && scope.modId.equals(field.value)) { /* original mod-id constant */ }
            else if (logger == null && field.access == (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)
                    && "Lorg/slf4j/Logger;".equals(field.desc) && field.value == null) logger = field;
            else throw rejected("unverified entry field " + field.name);
        }
        var initializers = node.methods.stream().filter(m -> "<clinit>".equals(m.name)).toList();
        require(initializers.size() == (logger == null ? 0 : 1), "static initializer ownership");
        if (logger != null) {
            MethodNode init = initializers.getFirst(); var ops = ops(init);
            require(init.access == Opcodes.ACC_STATIC && "()V".equals(init.desc) && ops.size() == 3
                    && call(ops.get(0), Opcodes.INVOKESTATIC, "com/mojang/logging/LogUtils", "getLogger", "()Lorg/slf4j/Logger;", false)
                    && field(ops.get(1), Opcodes.PUTSTATIC, node.name, logger.name, logger.desc)
                    && ops.get(2).getOpcode() == Opcodes.RETURN && init.maxStack >= 1, "static logger initializer");
        }
        var code = ops(ctor); require(code.size() >= 6, "constructor shape");
        require(local(code.get(0), Opcodes.ALOAD, 0)
                && call(code.get(1), Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
                && call(code.get(2), Opcodes.INVOKESTATIC, SOURCE, "get", "()L" + SOURCE + ";", false)
                && call(code.get(3), Opcodes.INVOKEVIRTUAL, SOURCE, "getModEventBus", "()" + BUS, false)
                && code.get(4) instanceof VarInsnNode v && v.getOpcode() == Opcodes.ASTORE && v.var > 0 && v.var < ctor.maxLocals,
                "direct constructor context chain/local");
        int busLocal = ((VarInsnNode) code.get(4)).var; Set<String> stored = new HashSet<>(); Set<String> helpers = new HashSet<>(); int globals = 0; int peak = 1;
        int i = 5;
        while (i < code.size() - 1) {
            AbstractInsnNode instruction = code.get(i);
            if (local(instruction, Opcodes.ALOAD, busLocal) && i + 1 < code.size() && code.get(i+1) instanceof FieldInsnNode f
                    && field(f, Opcodes.PUTSTATIC, node.name, f.name, "Ljava/lang/Object;") && receipts.contains(f.name)) {
                require(stored.add(f.name), "duplicate observation receipt"); i += 2;
            } else if (field(instruction, Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;") && i+2 < code.size()
                    && code.get(i+1) instanceof LdcInsnNode ldc && ldc.cst instanceof String
                    && call(code.get(i+2), Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false)) {
                peak = 2; i += 3;
            } else if (local(instruction, Opcodes.ALOAD, busLocal) && i+1 < code.size() && code.get(i+1) instanceof MethodInsnNode helper
                    && helper.getOpcode() == Opcodes.INVOKESTATIC && !helper.itf && ("(" + BUS + ")V").equals(helper.desc)) {
                require(!helper.owner.equals(node.name) && helpers.add(helper.owner + "." + helper.name), "duplicate/self helper");
                if (!verifiedHelpers.contains(helper.owner + "." + helper.name + helper.desc)) validateHelper(helper, scope);
                i += 2;
            } else if (field(instruction, Opcodes.GETSTATIC, "net/minecraftforge/common/MinecraftForge", "EVENT_BUS", BUS) && i+2 < code.size()
                    && local(code.get(i+1), Opcodes.ALOAD, 0)
                    && call(code.get(i+2), Opcodes.INVOKEINTERFACE, "net/minecraftforge/eventbus/api/IEventBus", "register", "(Ljava/lang/Object;)V", true)) {
                require(++globals == 1, "duplicate global bus call"); peak = 2; i += 3;
            } else throw rejected("unverified constructor effect at opcode " + i);
        }
        require(i == code.size()-1 && code.getLast().getOpcode() == Opcodes.RETURN && ctor.maxStack >= peak
                && stored.equals(receipts), "constructor stack/return/receipt accounting");
        return new Plan((MethodInsnNode)code.get(2), (MethodInsnNode)code.get(3), new Stats(helpers.size(), stored.size(), globals));
    }

    private static void validateHelper(MethodInsnNode call, Scope scope) {
        require(scope.classes.contains(call.owner), "helper outside owning scan");
        ClassNode node = new ClassNode(); new ClassReader(scope.read.apply(call.owner)).accept(node, 0);
        require(node.name.equals(call.owner) && (node.visibleAnnotations == null || node.visibleAnnotations.stream().noneMatch(a ->
                a.desc.equals("Lnet/minecraftforge/fml/common/Mod;") || a.desc.equals("Lnet/neoforged/fml/common/Mod;"))), "foreign helper owner");
        var matches = node.methods.stream().filter(m -> m.name.equals(call.name) && m.desc.equals(call.desc)).toList();
        require(matches.size() == 1, "helper target descriptor"); MethodNode method = matches.getFirst(); var code = ops(method);
        require(method.access == (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC) && method.tryCatchBlocks.isEmpty()
                && method.maxStack >= 2 && method.maxLocals >= 1 && method.signature == null
                && (method.visibleAnnotations == null || method.visibleAnnotations.isEmpty())
                && (method.invisibleAnnotations == null || method.invisibleAnnotations.isEmpty())
                && java.util.Arrays.stream(method.instructions.toArray()).noneMatch(i -> i instanceof FrameNode)
                && (method.exceptions == null || method.exceptions.isEmpty()) && code.size() == 4
                && code.getFirst() instanceof FieldInsnNode f && field(f, Opcodes.GETSTATIC, node.name, f.name, "L" + DEFERRED + ";")
                && node.fields.stream().anyMatch(field -> field.name.equals(f.name) && field.desc.equals(f.desc) && (field.access & Opcodes.ACC_STATIC) != 0)
                && local(code.get(1), Opcodes.ALOAD, 0)
                && call(code.get(2), Opcodes.INVOKEVIRTUAL, DEFERRED, "register", "(" + BUS + ")V", false)
                && code.get(3).getOpcode() == Opcodes.RETURN, "unverified helper bus boundary");
    }

    private static List<AbstractInsnNode> ops(MethodNode method) { return java.util.Arrays.stream(method.instructions.toArray()).filter(i -> i.getOpcode() >= 0).toList(); }
    private static boolean local(AbstractInsnNode i, int opcode, int slot) { return i instanceof VarInsnNode v && v.getOpcode() == opcode && v.var == slot; }
    private static boolean call(AbstractInsnNode i, int opcode, String owner, String name, String desc, boolean itf) { return i instanceof MethodInsnNode m && m.getOpcode() == opcode && m.owner.equals(owner) && m.name.equals(name) && m.desc.equals(desc) && m.itf == itf; }
    private static boolean field(AbstractInsnNode i, int opcode, String owner, String name, String desc) { return i instanceof FieldInsnNode f && f.getOpcode() == opcode && f.owner.equals(owner) && f.name.equals(name) && f.desc.equals(desc); }
    private static void require(boolean valid, String reason) { if (!valid) throw rejected(reason); }
    private static IllegalStateException rejected(String reason) { return new IllegalStateException("Unsupported Forge Context entry: " + reason); }
}
