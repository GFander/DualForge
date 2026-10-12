package com.example.forgecompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

/** One scan-owned Item declaration. All validation precedes mutation of the caller's copy. */
final class ControlledForgeItemProfile {
    private static final String F = "net/minecraftforge/registries/";
    private static final String N = "net/neoforged/neoforge/registries/";
    private static final String ITEM = "net/minecraft/world/item/Item";
    private static final String OLD_BUS = "(Lnet/minecraftforge/eventbus/api/IEventBus;)V";
    private static final String NEW_BUS = "(Lnet/neoforged/bus/api/IEventBus;)V";
    private static final String CREATE = "(L" + F + "IForgeRegistry;Ljava/lang/String;)L" + F + "DeferredRegister;";
    private static final String REGISTER = "(Ljava/lang/String;Ljava/util/function/Supplier;)L" + F + "RegistryObject;";
    private static final String PRINT = "(Ljava/lang/String;)V";
    private static final String OUT = field(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");

    static void validate(ClassNode node, String modId) {
        require(node.access == (Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER)
                && "java/lang/Object".equals(node.superName) && node.interfaces.isEmpty()
                && node.signature == null && node.fields.size() == 2 && node.methods.size() == 4,
                "class structure");
        require(node.visibleAnnotations != null && node.visibleAnnotations.size() == 1
                && "Lnet/minecraftforge/fml/common/Mod;".equals(node.visibleAnnotations.getFirst().desc)
                && List.of("value", modId).equals(node.visibleAnnotations.getFirst().values)
                && (node.invisibleAnnotations == null || node.invisibleAnnotations.isEmpty()), "owned @Mod");
        FieldNode register = onlyField(node, "DeferredRegister");
        FieldNode holder = onlyField(node, "RegistryObject");
        for (FieldNode f : node.fields) {
            require(f.access == (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)
                    && f.value == null && (f.visibleAnnotations == null || f.visibleAnnotations.isEmpty())
                    && (f.invisibleAnnotations == null || f.invisibleAnnotations.isEmpty()), "field flags");
            require((f.desc.substring(0, f.desc.length() - 1) + "<L" + ITEM + ";>;").equals(f.signature), "Item generic field");
        }
        MethodNode ctor = onlyMethod(node, "<init>", OLD_BUS, Opcodes.ACC_PUBLIC);
        MethodNode init = onlyMethod(node, "<clinit>", "()V", Opcodes.ACC_STATIC);
        List<AbstractInsnNode> initOps = opcodes(init);
        require(initOps.size() == 10 && initOps.get(1) instanceof LdcInsnNode namespace
                && modId.equals(namespace.cst) && initOps.get(5) instanceof LdcInsnNode id
                && id.cst instanceof String name && name.matches("[a-z0-9_./-]+"), "one namespaced item");
        require(initOps.get(6) instanceof InvokeDynamicInsnNode, "supplier declaration");
        InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) initOps.get(6);
        Handle metafactory = new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory",
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false);
        require(indy.bsm.equals(metafactory) && "get".equals(indy.name)
                && "()Ljava/util/function/Supplier;".equals(indy.desc) && indy.bsmArgs.length == 3
                && Type.getMethodType("()Ljava/lang/Object;").equals(indy.bsmArgs[0])
                && indy.bsmArgs[1] instanceof Handle && Type.getMethodType("()L" + ITEM + ";").equals(indy.bsmArgs[2]), "lambda bootstrap");
        Handle impl = (Handle) indy.bsmArgs[1];
        require(impl.getTag() == Opcodes.H_INVOKESTATIC && !impl.isInterface() && node.name.equals(impl.getOwner())
                && impl.getName().startsWith("lambda$") && ("()L" + ITEM + ";").equals(impl.getDesc()), "same-entry zero-capture lambda");
        MethodNode supplier = onlyMethod(node, impl.getName(), impl.getDesc(),
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC);
        List<MethodNode> getters = node.methods.stream().filter(m -> !List.of(ctor, init, supplier).contains(m)).toList();
        require(getters.size() == 1, "one accessor");
        MethodNode getter = getters.getFirst();
        require(getter.access == (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC) && ("()L" + ITEM + ";").equals(getter.desc)
                && getter.signature == null, "get-only Item accessor");
        for (MethodNode m : node.methods) {
            require(m.signature == null && m.tryCatchBlocks.isEmpty()
                    && (m.exceptions == null || m.exceptions.isEmpty())
                    && (m.visibleAnnotations == null || m.visibleAnnotations.isEmpty())
                    && (m.invisibleAnnotations == null || m.invisibleAnnotations.isEmpty()), "method metadata");
        }
        expect(init, field(Opcodes.GETSTATIC, F + "ForgeRegistries", "ITEMS", "L" + F + "IForgeRegistry;"), "S",
                call(Opcodes.INVOKESTATIC, F + "DeferredRegister", "create", CREATE),
                field(Opcodes.PUTSTATIC, node.name, register.name, register.desc),
                field(Opcodes.GETSTATIC, node.name, register.name, register.desc), "S", "D",
                call(Opcodes.INVOKEVIRTUAL, F + "DeferredRegister", "register", REGISTER),
                field(Opcodes.PUTSTATIC, node.name, holder.name, holder.desc), "177");
        expect(ctor, "V25:0", call(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V"),
                OUT, "S", call(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", PRINT),
                OUT, "S", call(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", PRINT),
                OUT, "V25:1", call(Opcodes.INVOKESTATIC, "java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I"),
                call(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(I)V"),
                "S", call(Opcodes.INVOKESTATIC, "java/lang/Boolean", "getBoolean", "(Ljava/lang/String;)Z"), "J153:22",
                field(Opcodes.GETSTATIC, node.name, register.name, register.desc), "V25:1",
                call(Opcodes.INVOKEVIRTUAL, F + "DeferredRegister", "register", OLD_BUS),
                OUT, "S", call(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", PRINT), "J167:25",
                OUT, "S", call(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", PRINT), "177");
        expect(supplier, OUT, "S", call(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", PRINT),
                "T187:" + ITEM, "89", "T187:" + ITEM + "$Properties", "89",
                call(Opcodes.INVOKESPECIAL, ITEM + "$Properties", "<init>", "()V"),
                call(Opcodes.INVOKESPECIAL, ITEM, "<init>", "(L" + ITEM + "$Properties;)V"), "176");
        expect(getter, field(Opcodes.GETSTATIC, node.name, holder.name, holder.desc),
                call(Opcodes.INVOKEVIRTUAL, F + "RegistryObject", "get", "()Ljava/lang/Object;"), "T192:" + ITEM, "176");
    }

    static void rewrite(ClassNode node, String modId) {
        validate(node, modId);
        for (FieldNode f : node.fields) {
            String owner = f.desc.equals("L" + F + "DeferredRegister;") ? "DeferredRegister" : "DeferredHolder";
            f.desc = "L" + N + owner + ";";
            f.signature = "L" + N + owner + "<L" + ITEM + ";>;";
            // DeferredHolder has two type arguments (registry base and concrete value).
            if (owner.equals("DeferredHolder")) f.signature = "L" + N + owner + "<L" + ITEM + ";L" + ITEM + ";>;";
        }
        for (MethodNode m : node.methods) {
            if (m.name.equals("<init>")) m.desc = NEW_BUS;
            for (AbstractInsnNode insn : m.instructions) {
                if (insn instanceof FieldInsnNode f) {
                    if (f.owner.equals(F + "ForgeRegistries")) {
                        f.owner = "net/minecraft/core/registries/BuiltInRegistries";
                        f.name = "ITEM";
                        f.desc = "Lnet/minecraft/core/DefaultedRegistry;";
                    } else if (f.owner.equals(node.name)) {
                        f.desc = node.fields.stream().filter(d -> d.name.equals(f.name)).findFirst().orElseThrow().desc;
                    }
                } else if (insn instanceof MethodInsnNode c) {
                    if (c.owner.equals(F + "DeferredRegister")) {
                        c.owner = N + "DeferredRegister";
                        if (c.desc.equals(CREATE)) c.desc = "(Lnet/minecraft/core/Registry;Ljava/lang/String;)L" + N + "DeferredRegister;";
                        else if (c.desc.equals(REGISTER)) c.desc = "(Ljava/lang/String;Ljava/util/function/Supplier;)L" + N + "DeferredHolder;";
                        else c.desc = NEW_BUS;
                    } else if (c.owner.equals(F + "RegistryObject")) c.owner = N + "DeferredHolder";
                }
            }
        }
    }

    private static FieldNode onlyField(ClassNode node, String kind) {
        List<FieldNode> fields = node.fields.stream().filter(f -> f.desc.equals("L" + F + kind + ";")).toList();
        require(fields.size() == 1, "one " + kind + " field");
        return fields.getFirst();
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
        if (!value) throw new IllegalStateException("005 unsupported Item profile: " + reason);
    }
}
