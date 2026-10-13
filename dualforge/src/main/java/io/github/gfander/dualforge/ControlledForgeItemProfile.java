package io.github.gfander.dualforge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

/** Scan-owned Item declarations. All validation precedes mutation of the caller's copy. */
final class ControlledForgeItemProfile {
    private static final String F = "net/minecraftforge/registries/";
    private static final String N = "net/neoforged/neoforge/registries/";
    private static final String ITEM = "net/minecraft/world/item/Item";
    private static final String OLD_BUS = "(Lnet/minecraftforge/eventbus/api/IEventBus;)V";
    private static final String NEW_BUS = "(Lnet/neoforged/bus/api/IEventBus;)V";
    private static final String CREATE = "(L" + F + "IForgeRegistry;Ljava/lang/String;)L" + F + "DeferredRegister;";
    private static final String REGISTER = "(Ljava/lang/String;Ljava/util/function/Supplier;)L" + F + "RegistryObject;";
    private static final String HOLDER_FIELD = "L" + F + "RegistryObject;";
    private static final String ITEM_FACTORY = "()L" + ITEM + ";";
    private static final String PRINT = "(Ljava/lang/String;)V";
    private static final String OUT = field(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");

    record Stats(int declarations, int suppliers, int accessors) {}

    private enum Value { ITEM_REGISTRY, REGISTER }
    private record Supplier(MethodNode method) {}
    private record Declaration(MethodInsnNode call, Supplier supplier, String id) {}
    private record FieldUse(FieldInsnNode instruction, FieldNode field) {}

    /** Instruction references belong only to this validated ClassNode, never a discovery-time node. */
    private static final class Plan {
        final FieldNode register;
        final List<FieldNode> holders;
        final MethodNode constructor;
        final List<FieldUse> fieldUses = new ArrayList<>();
        final List<Declaration> declarations = new ArrayList<>();
        final Set<MethodNode> suppliers = new HashSet<>();
        final List<MethodInsnNode> getters = new ArrayList<>();
        FieldInsnNode registry;
        MethodInsnNode create;
        MethodInsnNode attach;

        Plan(FieldNode register, List<FieldNode> holders, MethodNode constructor) {
            this.register = register;
            this.holders = holders;
            this.constructor = constructor;
        }

        Stats stats() { return new Stats(declarations.size(), suppliers.size(), getters.size()); }

        void apply() {
            register.desc = "L" + N + "DeferredRegister;";
            register.signature = "L" + N + "DeferredRegister<L" + ITEM + ";>;";
            for (FieldNode holder : holders) {
                holder.desc = "L" + N + "DeferredHolder;";
                holder.signature = "L" + N + "DeferredHolder<L" + ITEM + ";L" + ITEM + ";>;";
            }
            for (FieldUse use : fieldUses) use.instruction.desc = use.field.desc;
            registry.owner = "net/minecraft/core/registries/BuiltInRegistries";
            registry.name = "ITEM";
            registry.desc = "Lnet/minecraft/core/DefaultedRegistry;";
            create.owner = N + "DeferredRegister";
            create.desc = "(Lnet/minecraft/core/Registry;Ljava/lang/String;)L" + N + "DeferredRegister;";
            for (Declaration declaration : declarations) {
                declaration.call.owner = N + "DeferredRegister";
                declaration.call.desc = "(Ljava/lang/String;Ljava/util/function/Supplier;)L" + N + "DeferredHolder;";
            }
            constructor.desc = NEW_BUS;
            attach.owner = N + "DeferredRegister";
            attach.desc = NEW_BUS;
            for (MethodInsnNode getter : getters) getter.owner = N + "DeferredHolder";
            // Supplier methods and invokedynamic bootstrap arguments are deliberately untouched.
        }
    }

    static Stats validate(ClassNode node, String modId) { return plan(node, modId).stats(); }

    static Stats rewrite(ClassNode node, String modId) {
        Plan plan = plan(node, modId);
        plan.apply();
        return plan.stats();
    }

    private static Plan plan(ClassNode node, String modId) {
        require(node.access == (Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER)
                && "java/lang/Object".equals(node.superName) && node.interfaces.isEmpty()
                && node.signature == null, "class structure");
        require(node.visibleAnnotations != null && node.visibleAnnotations.size() == 1
                && "Lnet/minecraftforge/fml/common/Mod;".equals(node.visibleAnnotations.getFirst().desc)
                && List.of("value", modId).equals(node.visibleAnnotations.getFirst().values)
                && (node.invisibleAnnotations == null || node.invisibleAnnotations.isEmpty()), "owned @Mod");
        FieldNode register = onlyField(node, "DeferredRegister");
        List<FieldNode> holders = node.fields.stream().filter(f -> HOLDER_FIELD.equals(f.desc)).toList();
        require(!holders.isEmpty(), "Item holder fields");
        Set<String> fieldNames = new HashSet<>();
        for (FieldNode f : node.fields) {
            require((f == register || holders.contains(f)) && fieldNames.add(f.name), "accounted unique field");
            require(f.access == (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)
                    && f.value == null && (f.visibleAnnotations == null || f.visibleAnnotations.isEmpty())
                    && (f.invisibleAnnotations == null || f.invisibleAnnotations.isEmpty()), "field flags");
            require((f.desc.substring(0, f.desc.length() - 1) + "<L" + ITEM + ";>;").equals(f.signature), "Item generic field");
        }
        MethodNode ctor = onlyMethod(node, "<init>", OLD_BUS, Opcodes.ACC_PUBLIC);
        MethodNode init = onlyMethod(node, "<clinit>", "()V", Opcodes.ACC_STATIC);
        Set<String> methodNames = new HashSet<>();
        for (MethodNode m : node.methods) {
            require(methodNames.add(m.name + m.desc) && m.signature == null && m.tryCatchBlocks.isEmpty()
                    && (m.exceptions == null || m.exceptions.isEmpty())
                    && (m.visibleAnnotations == null || m.visibleAnnotations.isEmpty())
                    && (m.invisibleAnnotations == null || m.invisibleAnnotations.isEmpty()), "method metadata");
        }
        Plan plan = new Plan(register, holders, ctor);
        validateInitializer(node, init, modId, plan);
        // Preserve the original constructor's ABI, branch layout and injected argument provenance.
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
        List<AbstractInsnNode> ctorOps = opcodes(ctor);
        plan.fieldUses.add(new FieldUse((FieldInsnNode) ctorOps.get(15), register));
        plan.attach = (MethodInsnNode) ctorOps.get(17);

        Set<FieldNode> accessed = new HashSet<>();
        for (MethodNode getter : node.methods) {
            if (getter == ctor || getter == init || plan.suppliers.contains(getter)) continue;
            require(getter.access == (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC) && ITEM_FACTORY.equals(getter.desc),
                    "get-only Item accessor");
            List<AbstractInsnNode> ops = opcodes(getter);
            require(!ops.isEmpty() && ops.getFirst() instanceof FieldInsnNode, "holder accessor source");
            FieldInsnNode use = (FieldInsnNode) ops.getFirst();
            FieldNode holder = ownedField(node, use);
            require(holders.contains(holder) && accessed.add(holder), "one accessor per holder");
            expect(getter, field(Opcodes.GETSTATIC, node.name, holder.name, holder.desc),
                    call(Opcodes.INVOKEVIRTUAL, F + "RegistryObject", "get", "()Ljava/lang/Object;"), "T192:" + ITEM, "176");
            plan.fieldUses.add(new FieldUse(use, holder));
            plan.getters.add((MethodInsnNode) ops.get(1));
        }
        require(accessed.size() == holders.size(), "all holders have verified accessors");
        return plan;
    }

    /** A restricted data-flow walk, not an instruction-count template or namespace remapper. */
    private static void validateInitializer(ClassNode node, MethodNode init, String modId, Plan plan) {
        List<Object> stack = new ArrayList<>();
        Set<Integer> locals = new HashSet<>();
        Set<Integer> readLocals = new HashSet<>();
        Set<String> ids = new HashSet<>();
        Map<FieldNode, Declaration> assignedHolders = new HashMap<>();
        boolean registerAssigned = false;
        boolean returned = false;
        for (AbstractInsnNode instruction : opcodes(init)) {
            require(!returned, "initializer instructions after return");
            if (instruction instanceof LdcInsnNode literal) {
                require(literal.cst instanceof String, "literal initializer argument");
                stack.add(literal.cst);
            } else if (instruction instanceof FieldInsnNode use) {
                if (use.getOpcode() == Opcodes.GETSTATIC && use.owner.equals(F + "ForgeRegistries")) {
                    require(plan.registry == null && "ITEMS".equals(use.name)
                            && ("L" + F + "IForgeRegistry;").equals(use.desc), "actual Item registry source");
                    plan.registry = use;
                    stack.add(Value.ITEM_REGISTRY);
                } else {
                    FieldNode target = ownedField(node, use);
                    plan.fieldUses.add(new FieldUse(use, target));
                    if (use.getOpcode() == Opcodes.GETSTATIC) {
                        require(target == plan.register && registerAssigned, "initialized register field receiver");
                        stack.add(Value.REGISTER);
                    } else {
                        require(use.getOpcode() == Opcodes.PUTSTATIC, "initializer static field operation");
                        Object value = pop(stack);
                        if (target == plan.register) {
                            require(value == Value.REGISTER && !registerAssigned, "one register field assignment");
                            registerAssigned = true;
                        } else {
                            require(value instanceof Declaration && !assignedHolders.containsKey(target),
                                    "one consumed registration per holder");
                            assignedHolders.put(target, (Declaration) value);
                        }
                    }
                }
            } else if (instruction instanceof VarInsnNode local) {
                // Gate 0's independent layout aliases only the verified register; no captured suppliers.
                if (local.getOpcode() == Opcodes.ASTORE) {
                    require(local.var >= 0 && pop(stack) == Value.REGISTER && locals.add(local.var), "register local alias");
                } else {
                    require(local.getOpcode() == Opcodes.ALOAD && locals.contains(local.var), "verified register local read");
                    readLocals.add(local.var);
                    stack.add(Value.REGISTER);
                }
            } else if (instruction instanceof InvokeDynamicInsnNode indy) {
                Supplier supplier = validateSupplier(node, indy);
                plan.suppliers.add(supplier.method);
                stack.add(supplier);
            } else if (instruction instanceof MethodInsnNode method) {
                if (matches(method, Opcodes.INVOKESTATIC, F + "DeferredRegister", "create", CREATE)) {
                    require(plan.create == null && modId.equals(pop(stack)) && pop(stack) == Value.ITEM_REGISTRY,
                            "one owned Item register creation");
                    plan.create = method;
                    stack.add(Value.REGISTER);
                } else if (matches(method, Opcodes.INVOKEVIRTUAL, F + "DeferredRegister", "register", REGISTER)) {
                    Object source = pop(stack);
                    Object id = pop(stack);
                    require(source instanceof Supplier && id instanceof String name && name.matches("[a-z0-9_./-]+")
                            && ids.add((String) id) && registerAssigned && pop(stack) == Value.REGISTER,
                            "unique Item declaration and verified register/supplier");
                    Declaration declaration = new Declaration(method, (Supplier) source, (String) id);
                    plan.declarations.add(declaration);
                    stack.add(declaration);
                } else {
                    require(false, "unverified initializer API " + method.owner + "." + method.name + method.desc);
                }
            } else {
                require(instruction instanceof InsnNode && instruction.getOpcode() == Opcodes.RETURN && stack.isEmpty(),
                        "initializer result must be consumed, or unsupported instruction");
                returned = true;
            }
        }
        require(returned && plan.create != null && registerAssigned && !plan.declarations.isEmpty()
                && assignedHolders.size() == plan.holders.size()
                && new HashSet<>(assignedHolders.values()).size() == plan.declarations.size()
                && readLocals.equals(locals), "complete initializer assignments and data flow");
    }

    private static Supplier validateSupplier(ClassNode node, InvokeDynamicInsnNode indy) {
        Handle metafactory = new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory",
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false);
        require(indy.bsm.equals(metafactory) && "get".equals(indy.name)
                && "()Ljava/util/function/Supplier;".equals(indy.desc) && indy.bsmArgs.length == 3
                && Type.getMethodType("()Ljava/lang/Object;").equals(indy.bsmArgs[0])
                && indy.bsmArgs[1] instanceof Handle && Type.getMethodType(ITEM_FACTORY).equals(indy.bsmArgs[2]), "lambda bootstrap");
        Handle impl = (Handle) indy.bsmArgs[1];
        require(impl.getTag() == Opcodes.H_INVOKESTATIC && !impl.isInterface() && node.name.equals(impl.getOwner())
                && ITEM_FACTORY.equals(impl.getDesc()), "same-entry zero-capture supplier");
        List<MethodNode> methods = node.methods.stream().filter(m -> impl.getName().equals(m.name)
                && impl.getDesc().equals(m.desc)).toList();
        require(methods.size() == 1, "one supplier implementation");
        MethodNode method = methods.getFirst();
        require(method.access == (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC)
                || method.access == (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC), "supplier method role");
        expect(method, OUT, "S", call(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", PRINT),
                "T187:" + ITEM, "89", "T187:" + ITEM + "$Properties", "89",
                call(Opcodes.INVOKESPECIAL, ITEM + "$Properties", "<init>", "()V"),
                call(Opcodes.INVOKESPECIAL, ITEM, "<init>", "(L" + ITEM + "$Properties;)V"), "176");
        return new Supplier(method);
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
