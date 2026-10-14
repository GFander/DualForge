package io.github.gfander.dualforge;

import java.util.*;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

/** Exactly one scan-owned Block and its BlockItem; one whole-class plan before edits. */
final class ControlledForgeBlockProfile {
    private static final String F = "net/minecraftforge/registries/";
    private static final String N = "net/neoforged/neoforge/registries/";
    private static final String BLOCK = "net/minecraft/world/level/block/Block";
    private static final String ITEM = "net/minecraft/world/item/Item";
    private static final String BLOCK_ITEM = "net/minecraft/world/item/BlockItem";
    private static final String PROPERTIES = "net/minecraft/world/level/block/state/BlockBehaviour$Properties";
    private static final String OLD_BUS = "(Lnet/minecraftforge/eventbus/api/IEventBus;)V";
    private static final String NEW_BUS = "(Lnet/neoforged/bus/api/IEventBus;)V";
    private static final String CREATE = "(L" + F + "IForgeRegistry;Ljava/lang/String;)L" + F + "DeferredRegister;";
    private static final String REGISTER = "(Ljava/lang/String;Ljava/util/function/Supplier;)L" + F + "RegistryObject;";
    private static final String PRINT = "(Ljava/lang/String;)V";
    private static final String OUT = field(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
    private enum Kind {
        BLOCK(ControlledForgeBlockProfile.BLOCK, "BLOCKS", "BLOCK"),
        ITEM(ControlledForgeBlockProfile.ITEM, "ITEMS", "ITEM");
        final String type, forge, neo;
        Kind(String type, String forge, String neo) { this.type = type; this.forge = forge; this.neo = neo; }
        String valueType() { return this == BLOCK ? type : BLOCK_ITEM; }
    }
    record Stats(int creates, int declarations, int attachments, int getters, int suppliers) {}
    private record Register(Kind kind) {}
    private record Supplier(MethodNode method, String type) {}
    private record Declaration(Register register, Supplier supplier, String id) {}
    private record FieldUse(FieldInsnNode instruction, FieldNode field) {}
    private record RegistryUse(FieldInsnNode instruction, Kind kind) {}

    static boolean isCandidate(ClassNode node) {
        return node.fields.stream().filter(f -> ("L" + F + "DeferredRegister;").equals(f.desc)).count() > 1
                || node.fields.stream().anyMatch(f -> generic("DeferredRegister", BLOCK).equals(f.signature));
    }
    private static final class Plan {
        final Map<Kind, FieldNode> registers = new EnumMap<>(Kind.class);
        final Map<Kind, FieldNode> holders = new EnumMap<>(Kind.class);
        final List<FieldUse> fields = new ArrayList<>();
        final List<RegistryUse> registries = new ArrayList<>();
        final List<MethodInsnNode> creates = new ArrayList<>(), declarations = new ArrayList<>(), attachments = new ArrayList<>(), gets = new ArrayList<>();
        final Set<MethodNode> suppliers = new HashSet<>();
        MethodNode constructor;
        Stats stats() { return new Stats(creates.size(), declarations.size(), attachments.size(), gets.size(), suppliers.size()); }
        void apply() {
            for (Kind kind : Kind.values()) {
                FieldNode register = registers.get(kind), holder = holders.get(kind);
                register.desc = "L" + N + "DeferredRegister;";
                register.signature = "L" + N + "DeferredRegister<L" + kind.type + ";>;";
                holder.desc = "L" + N + "DeferredHolder;";
                holder.signature = "L" + N + "DeferredHolder<L" + kind.type + ";L" + kind.valueType() + ";>;";
            }
            for (FieldUse use : fields) use.instruction.desc = use.field.desc;
            for (RegistryUse use : registries) {
                use.instruction.owner = "net/minecraft/core/registries/BuiltInRegistries";
                use.instruction.name = use.kind.neo;
                use.instruction.desc = "Lnet/minecraft/core/DefaultedRegistry;";
            }
            for (MethodInsnNode call : creates) {
                call.owner = N + "DeferredRegister";
                call.desc = "(Lnet/minecraft/core/Registry;Ljava/lang/String;)L" + N + "DeferredRegister;";
            }
            for (MethodInsnNode call : declarations) {
                call.owner = N + "DeferredRegister";
                call.desc = "(Ljava/lang/String;Ljava/util/function/Supplier;)L" + N + "DeferredHolder;";
            }
            constructor.desc = NEW_BUS;
            for (MethodInsnNode call : attachments) { call.owner = N + "DeferredRegister"; call.desc = NEW_BUS; }
            for (MethodInsnNode call : gets) call.owner = N + "DeferredHolder";
        }
    }
    static Stats validate(ClassNode node, String modId) { return plan(node, modId).stats(); }
    static Stats rewrite(ClassNode node, String modId) { Plan p = plan(node, modId); p.apply(); return p.stats(); }
    private static String generic(String owner, String value) { return "L" + F + owner + "<L" + value + ";>;"; }
    private static Plan plan(ClassNode node, String modId) {
        require(node.access == (Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER)
                && "java/lang/Object".equals(node.superName) && node.interfaces.isEmpty() && node.signature == null, "class structure");
        require(node.visibleAnnotations != null && node.visibleAnnotations.size() == 1
                && "Lnet/minecraftforge/fml/common/Mod;".equals(node.visibleAnnotations.getFirst().desc)
                && List.of("value", modId).equals(node.visibleAnnotations.getFirst().values)
                && (node.invisibleAnnotations == null || node.invisibleAnnotations.isEmpty()), "owned @Mod");
        Plan p = new Plan();
        Set<String> names = new HashSet<>();
        for (FieldNode f : node.fields) {
            require(names.add(f.name) && f.access == (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)
                    && f.value == null && (f.visibleAnnotations == null || f.visibleAnnotations.isEmpty())
                    && (f.invisibleAnnotations == null || f.invisibleAnnotations.isEmpty()), "field metadata");
            Kind matched = null;
            for (Kind k : Kind.values()) {
                if (("L" + F + "DeferredRegister;").equals(f.desc) && generic("DeferredRegister", k.type).equals(f.signature)) {
                    require(p.registers.put(k, f) == null, "one typed register per Registry"); matched = k;
                } else if (("L" + F + "RegistryObject;").equals(f.desc) && generic("RegistryObject", k.valueType()).equals(f.signature)) {
                    require(p.holders.put(k, f) == null, "one typed holder per Registry"); matched = k;
                }
            }
            require(matched != null, "unsupported field or Holder type");
        }
        require(p.registers.size() == 2 && p.holders.size() == 2, "complete Block/Item pair");
        p.constructor = onlyMethod(node, "<init>", OLD_BUS, Opcodes.ACC_PUBLIC);
        MethodNode init = onlyMethod(node, "<clinit>", "()V", Opcodes.ACC_STATIC);
        Set<String> methods = new HashSet<>();
        for (MethodNode m : node.methods) require(methods.add(m.name + m.desc) && m.signature == null && m.tryCatchBlocks.isEmpty()
                && (m.exceptions == null || m.exceptions.isEmpty()) && (m.visibleAnnotations == null || m.visibleAnnotations.isEmpty())
                && (m.invisibleAnnotations == null || m.invisibleAnnotations.isEmpty()), "method metadata");
        initializer(node, init, modId, p);
        FieldNode br = p.registers.get(Kind.BLOCK), ir = p.registers.get(Kind.ITEM);
        expect(p.constructor, "V25:0", call(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V"),
                OUT, "S", call(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", PRINT),
                OUT, "S", call(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", PRINT),
                OUT, "V25:1", call(Opcodes.INVOKESTATIC, "java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I"),
                call(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(I)V"),
                "S", call(Opcodes.INVOKESTATIC, "java/lang/Boolean", "getBoolean", "(Ljava/lang/String;)Z"), "J153:25",
                field(Opcodes.GETSTATIC, node.name, br.name, br.desc), "V25:1", call(Opcodes.INVOKEVIRTUAL, F + "DeferredRegister", "register", OLD_BUS),
                field(Opcodes.GETSTATIC, node.name, ir.name, ir.desc), "V25:1", call(Opcodes.INVOKEVIRTUAL, F + "DeferredRegister", "register", OLD_BUS),
                OUT, "S", call(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", PRINT), "J167:28",
                OUT, "S", call(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", PRINT), "177");
        List<AbstractInsnNode> ctor = opcodes(p.constructor);
        for (int i : new int[]{15, 18}) {
            FieldInsnNode field = (FieldInsnNode) ctor.get(i);
            p.fields.add(new FieldUse(field, ownedField(node, field)));
            p.attachments.add((MethodInsnNode) ctor.get(i + 2));
        }
        Set<Kind> accessed = EnumSet.noneOf(Kind.class);
        for (MethodNode getter : node.methods) {
            if (getter == p.constructor || getter == init || p.suppliers.contains(getter)) continue;
            List<AbstractInsnNode> ops = opcodes(getter);
            require(!ops.isEmpty() && ops.getFirst() instanceof FieldInsnNode, "get-only accessor source");
            FieldInsnNode use = (FieldInsnNode) ops.getFirst();
            FieldNode f = ownedField(node, use);
            Kind k = p.holders.entrySet().stream().filter(e -> e.getValue() == f).map(Map.Entry::getKey).findFirst().orElseThrow(() -> new IllegalStateException("006B unsupported Block profile: accessor not holder"));
            require(accessed.add(k) && getter.access == (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
                    && ("()L" + k.valueType() + ";").equals(getter.desc), "one typed get-only accessor per holder");
            expect(getter, field(Opcodes.GETSTATIC, node.name, f.name, f.desc), call(Opcodes.INVOKEVIRTUAL, F + "RegistryObject", "get", "()Ljava/lang/Object;"), "T192:" + k.valueType(), "176");
            p.fields.add(new FieldUse(use, f)); p.gets.add((MethodInsnNode) ops.get(1));
        }
        require(accessed.size() == 2, "both holders have accessors");
        return p;
    }
    private static void initializer(ClassNode node, MethodNode init, String modId, Plan p) {
        List<Object> stack = new ArrayList<>();
        Map<Kind, Register> created = new EnumMap<>(Kind.class);
        Set<FieldNode> assigned = new HashSet<>();
        Set<String> ids = new HashSet<>();
        boolean returned = false;
        for (AbstractInsnNode instruction : opcodes(init)) {
            require(!returned, "instructions after initializer return");
            if (instruction instanceof LdcInsnNode literal) {
                require(literal.cst instanceof String, "initializer string literal"); stack.add(literal.cst);
            } else if (instruction instanceof FieldInsnNode use) {
                if (use.getOpcode() == Opcodes.GETSTATIC && (F + "ForgeRegistries").equals(use.owner)) {
                    require(("L" + F + "IForgeRegistry;").equals(use.desc), "registry source descriptor");
                    Kind k = Arrays.stream(Kind.values()).filter(v -> v.forge.equals(use.name)).findFirst().orElseThrow(() -> new IllegalStateException("006B unsupported Block profile: unsupported Registry"));
                    p.registries.add(new RegistryUse(use, k)); stack.add(k);
                } else {
                    FieldNode target = ownedField(node, use); p.fields.add(new FieldUse(use, target));
                    if (use.getOpcode() == Opcodes.GETSTATIC) {
                        Kind k = p.registers.entrySet().stream().filter(e -> e.getValue() == target).map(Map.Entry::getKey).findFirst().orElse(null);
                        require(k != null && assigned.contains(target), "initialized register receiver"); stack.add(created.get(k));
                    } else {
                        require(use.getOpcode() == Opcodes.PUTSTATIC && assigned.add(target), "one static field assignment");
                        Object value = pop(stack);
                        if (value instanceof Register register) require(p.registers.get(register.kind) == target, "register Registry/type association");
                        else {
                            require(value instanceof Declaration, "consumed declaration result"); Declaration d = (Declaration) value;
                            require(p.holders.get(d.register.kind) == target, "holder Registry/type association");
                        }
                    }
                }
            } else if (instruction instanceof InvokeDynamicInsnNode indy) {
                Supplier s = supplier(node, indy, p); p.suppliers.add(s.method); stack.add(s);
            } else if (instruction instanceof MethodInsnNode method) {
                if (matches(method, Opcodes.INVOKESTATIC, F + "DeferredRegister", "create", CREATE)) {
                    require(modId.equals(pop(stack)), "owned register namespace"); Object key = pop(stack);
                    require(key instanceof Kind && !created.containsKey(key), "one actual register per Registry");
                    Register register = new Register((Kind) key); created.put(register.kind, register); p.creates.add(method); stack.add(register);
                } else if (matches(method, Opcodes.INVOKEVIRTUAL, F + "DeferredRegister", "register", REGISTER)) {
                    Object s = pop(stack), id = pop(stack), receiver = pop(stack);
                    require(s instanceof Supplier && id instanceof String && ((String) id).matches("[a-z0-9_./-]+") && receiver instanceof Register, "declaration data dependencies");
                    Register register = (Register) receiver; Supplier source = (Supplier) s;
                    require(source.type.equals(register.kind.valueType()), "Supplier return type matches Registry");
                    require(ids.add(register.kind + ":" + id), "duplicate registration ID in Registry");
                    require(assigned.contains(p.registers.get(register.kind)), "initialized declaration receiver");
                    p.declarations.add(method); stack.add(new Declaration(register, source, (String) id));
                } else require(false, "unverified initializer API " + method.owner + "." + method.name);
            } else {
                require(instruction instanceof InsnNode && instruction.getOpcode() == Opcodes.RETURN && stack.isEmpty(), "unconsumed result or unsupported initializer opcode"); returned = true;
            }
        }
        require(returned && assigned.size() == 4 && created.size() == 2 && p.registries.size() == 2 && p.declarations.size() == 2 && p.suppliers.size() == 2, "complete two-Registry initializer");
    }
    private static Supplier supplier(ClassNode node, InvokeDynamicInsnNode indy, Plan p) {
        Handle bsm = new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory",
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false);
        require(indy.bsm.equals(bsm) && "get".equals(indy.name) && "()Ljava/util/function/Supplier;".equals(indy.desc)
                && indy.bsmArgs.length == 3 && Type.getMethodType("()Ljava/lang/Object;").equals(indy.bsmArgs[0]) && indy.bsmArgs[1] instanceof Handle, "zero-capture lambda bootstrap");
        Handle impl = (Handle) indy.bsmArgs[1];
        Type resultType = Type.getReturnType(impl.getDesc());
        require(resultType.getSort() == Type.OBJECT, "Supplier object return type");
        String type = resultType.getInternalName();
        require((type.equals(BLOCK) || type.equals(BLOCK_ITEM)) && ("()L" + type + ";").equals(impl.getDesc())
                && Type.getMethodType(impl.getDesc()).equals(indy.bsmArgs[2]) && impl.getTag() == Opcodes.H_INVOKESTATIC && !impl.isInterface() && node.name.equals(impl.getOwner()), "same-entry Supplier type/handle");
        List<MethodNode> methods = node.methods.stream().filter(m -> m.name.equals(impl.getName()) && m.desc.equals(impl.getDesc())).toList(); require(methods.size() == 1, "one Supplier implementation");
        MethodNode method = methods.getFirst();
        require(method.access == (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC) || method.access == (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC), "Supplier flags");
        if (type.equals(BLOCK)) expect(method, OUT, "S", call(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", PRINT), "T187:" + BLOCK, "89", call(Opcodes.INVOKESTATIC, PROPERTIES, "of", "()L" + PROPERTIES + ";"), call(Opcodes.INVOKESPECIAL, BLOCK, "<init>", "(L" + PROPERTIES + ";)V"), "176");
        else {
            FieldNode holder = p.holders.get(Kind.BLOCK);
            expect(method, OUT, "S", call(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", PRINT), "T187:" + BLOCK_ITEM, "89",
                    field(Opcodes.GETSTATIC, node.name, holder.name, holder.desc), call(Opcodes.INVOKEVIRTUAL, F + "RegistryObject", "get", "()Ljava/lang/Object;"), "T192:" + BLOCK,
                    "T187:" + ITEM + "$Properties", "89", call(Opcodes.INVOKESPECIAL, ITEM + "$Properties", "<init>", "()V"), call(Opcodes.INVOKESPECIAL, BLOCK_ITEM, "<init>", "(L" + BLOCK + ";L" + ITEM + "$Properties;)V"), "176");
            List<AbstractInsnNode> ops = opcodes(method); p.fields.add(new FieldUse((FieldInsnNode) ops.get(5), holder)); p.gets.add((MethodInsnNode) ops.get(6));
        }
        return new Supplier(method, type);
    }
    private static FieldNode ownedField(ClassNode node, FieldInsnNode use) {
        require(node.name.equals(use.owner), "owned field reference");
        List<FieldNode> fields = node.fields.stream().filter(f -> f.name.equals(use.name) && f.desc.equals(use.desc)).toList();
        require(fields.size() == 1, "declared field reference " + use.name);
        return fields.getFirst();
    }

    private static Object pop(List<Object> stack) {
        require(!stack.isEmpty(), "initializer stack underflow");
        return stack.removeLast();
    }

    private static boolean matches(MethodInsnNode method, int opcode, String owner, String name, String desc) {
        return method.getOpcode() == opcode && !method.itf && owner.equals(method.owner)
                && name.equals(method.name) && desc.equals(method.desc);
    }

    private static MethodNode onlyMethod(ClassNode node, String name, String desc, int access) {
        List<MethodNode> methods = node.methods.stream().filter(m -> m.name.equals(name) && m.desc.equals(desc) && m.access == access).toList();
        require(methods.size() == 1, "method " + name + desc);
        return methods.getFirst();
    }

    private static List<AbstractInsnNode> opcodes(MethodNode method) {
        List<AbstractInsnNode> result = new ArrayList<>();
        for (AbstractInsnNode i : method.instructions) if (i.getOpcode() >= 0) result.add(i);
        return result;
    }

    private static void expect(MethodNode method, String... expected) {
        List<AbstractInsnNode> ops = opcodes(method);
        List<String> actual = new ArrayList<>();
        for (AbstractInsnNode i : ops) {
            String value;
            if (i instanceof FieldInsnNode f) value = field(f.getOpcode(), f.owner, f.name, f.desc);
            else if (i instanceof MethodInsnNode m) {
                require(!m.itf, "non-interface invocation");
                value = call(m.getOpcode(), m.owner, m.name, m.desc);
            } else if (i instanceof VarInsnNode v) value = "V" + v.getOpcode() + ":" + v.var;
            else if (i instanceof TypeInsnNode t) value = "T" + t.getOpcode() + ":" + t.desc;
            else if (i instanceof LdcInsnNode ldc && ldc.cst instanceof String) value = "S";
            else if (i instanceof InvokeDynamicInsnNode) value = "D";
            else if (i instanceof JumpInsnNode jump) {
                AbstractInsnNode target = jump.label;
                while (target != null && target.getOpcode() < 0) target = target.getNext();
                value = "J" + jump.getOpcode() + ":" + ops.indexOf(target);
            } else {
                require(i instanceof InsnNode, "unsupported instruction");
                value = Integer.toString(i.getOpcode());
            }
            actual.add(value);
        }
        require(actual.equals(Arrays.asList(expected)), "instructions " + method.name + ": " + actual);
    }

    private static String field(int op, String owner, String name, String desc) { return "F" + op + ":" + owner + "." + name + desc; }
    private static String call(int op, String owner, String name, String desc) { return "M" + op + ":" + owner + "." + name + desc; }
    private static void require(boolean value, String reason) {
        if (!value) throw new IllegalStateException("006B unsupported Block profile: " + reason);
    }
}
