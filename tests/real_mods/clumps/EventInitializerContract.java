package io.github.gfander.dualforge;

import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Exercise actual source admission, complete-plan rejection, and immutable transform inputs. */
public final class EventInitializerContract {
    private static int assertions, negatives;
    private static Map<String,byte[]> source;
    private static Map<String,String> resources;
    private static JarFile host;
    private static String entry;
    private static Path output;

    public static void main(String[] args) throws Exception {
        host = new JarFile(args[2]);
        output = Path.of(args[3]); Files.createDirectories(output);
        load(args[0], "com/blamejared/clumps/Clumps");
        positive("clumps");
        reject("metadata-owner", raw -> mutate(raw, entry, n -> n.visibleAnnotations.getFirst().values.set(1, "other")));
        reject("extra-entry-effect", raw -> mutate(raw, entry, n -> ctor(n).instructions.insertBefore(ctor(n).instructions.getLast(), new InsnNode(Opcodes.NOP))));
        reject("captured-predicate", raw -> mutate(raw, entry, n -> dynamic(n).desc = "(Ljava/lang/Object;)Ljava/util/function/BiPredicate;"));
        reject("bootstrap", raw -> mutate(raw, entry, n -> dynamic(n).bsm = new Handle(Opcodes.H_INVOKESTATIC, "unknown/Bootstrap", "make", dynamic(n).bsm.getDesc(), false)));
        reject("lambda-owner", raw -> mutate(raw, entry, n -> { var h = (Handle) dynamic(n).bsmArgs[1]; dynamic(n).bsmArgs[1] = new Handle(h.getTag(), "outside/Entry", h.getName(), h.getDesc(), false); }));
        reject("lambda-erasure", raw -> mutate(raw, entry, n -> dynamic(n).bsmArgs[0] = Type.getMethodType("()Z")));
        reject("predicate-field-owner", raw -> mutate(raw, entry, n -> store(n).owner = "outside/Common"));
        reject("constructor-access", raw -> mutate(raw, entry, n -> ctor(n).access = Opcodes.ACC_PRIVATE));
        reject("extra-initializer", raw -> mutate(raw, entry, n -> { var m = new MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null); m.instructions.add(new InsnNode(Opcodes.RETURN)); n.methods.add(m); }));
        reject("post-return-type", raw -> mutate(raw, entry, n -> post(n).desc = "(Ljava/lang/Object;)Z"));
        reject("post-owner", raw -> mutate(raw, entry, n -> post(n).owner = "net/minecraftforge/eventbus/api/Unknown"));
        reject("post-to-register", raw -> mutate(raw, entry, n -> post(n).name = "register"));
        reject("common-default", raw -> mutate(raw, "com/blamejared/clumps/ClumpsCommon", n -> {
            var m = n.methods.stream().filter(x -> x.name.startsWith("lambda$")).findFirst().orElseThrow();
            var value = Arrays.stream(m.instructions.toArray()).filter(i -> i.getOpcode() == Opcodes.ICONST_0).findFirst().orElseThrow();
            m.instructions.set(value, new InsnNode(Opcodes.ICONST_1));
        }));
        reject("extra-common-field", raw -> mutate(raw, "com/blamejared/clumps/ClumpsCommon", n -> n.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "unexpected", "I", null, null))));
        reject("provider-side-effect", raw -> mutate(raw, "com/blamejared/clumps/platform/ForgeEventHandler", n -> n.methods.get(1).instructions.insertBefore(n.methods.get(1).instructions.getLast(), new InsnNode(Opcodes.NOP))));
        reject("cancelable-custom-event", raw -> mutate(raw, "com/blamejared/clumps/api/events/ValueEvent", n -> n.visibleAnnotations = new ArrayList<>(List.of(new AnnotationNode("Lnet/minecraftforge/eventbus/api/Cancelable;")))));
        reject("custom-event-super", raw -> mutate(raw, "com/blamejared/clumps/api/events/ValueEvent", n -> n.superName = "java/lang/Object"));
        reject("custom-event-setter", raw -> mutate(raw, "com/blamejared/clumps/api/events/RepairEvent", n -> {
            for (var m : n.methods) if (m.desc.equals("(I)V")) for (var op : m.instructions)
                if (op instanceof FieldInsnNode f) f.name = "outside";
        }));
        reject("unknown-source-api", raw -> mutate(raw, "com/blamejared/clumps/platform/ForgePlatformHelper", n -> ctor(n).instructions.insertBefore(ctor(n).instructions.getLast(), new LdcInsnNode(Type.getObjectType("net/minecraftforge/unknown/API")))));
        reject("missing-common", raw -> raw.remove("com/blamejared/clumps/ClumpsCommon"));
        reject("inherited-event-api", raw -> mutate(raw, "com/blamejared/clumps/platform/ForgePlatformHelper", n -> {
            MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, "test", "(Lcom/blamejared/clumps/api/events/ValueEvent;)Z", null, null);
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
            method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "com/blamejared/clumps/api/events/ValueEvent", "isCancelable", "()Z", false));
            method.instructions.add(new InsnNode(Opcodes.IRETURN)); method.maxStack = 1; method.maxLocals = 2; n.methods.add(method);
        }));
        reject("inherited-event-handle", raw -> mutate(raw, "com/blamejared/clumps/platform/ForgePlatformHelper", n -> {
            ctor(n).instructions.insertBefore(ctor(n).instructions.getLast(), new LdcInsnNode(new Handle(Opcodes.H_INVOKEVIRTUAL,
                "com/blamejared/clumps/api/events/ValueEvent", "getResult", "()Ljava/lang/Object;", false)));
        }));
        reject("unverified-event-subclass", raw -> mutate(raw, "com/blamejared/clumps/platform/ForgePlatformHelper", n -> n.superName = "com/blamejared/clumps/api/events/ValueEvent"));
        reject("reflective-member-access", raw -> mutate(raw, "com/blamejared/clumps/platform/ForgePlatformHelper", n -> {
            var method = new MethodNode(Opcodes.ACC_PUBLIC, "test", "(Ljava/lang/String;)Ljava/lang/Class;", null, null);
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
            method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false));
            method.instructions.add(new InsnNode(Opcodes.ARETURN)); method.maxStack = 1; method.maxLocals = 2; n.methods.add(method);
        }));
        var saved = resources;
        resources = new TreeMap<>(resources);
        resources.put("META-INF/services/com.blamejared.clumps.platform.IEventHelper", "foreign.Provider\n");
        reject("foreign-service-provider", raw -> {});
        resources = saved;
        try { plan("clumps", source, true); throw new AssertionError("Unverified host admitted"); }
        catch (IllegalStateException expected) { check(expected.getMessage().contains("UNVERIFIED_EVENT_SEMANTICS"), "host ABI rejection"); negatives++; }
        var node = read(source.get(entry));
        try { ForgeEntryProfile.select(node, true); throw new AssertionError("Ambiguous entry admitted"); }
        catch (IllegalStateException expected) { check(expected.getMessage().startsWith("AMBIGUOUS_ENTRY"), "profile ambiguity rejected"); negatives++; }
        ClassNode unknown = read(source.get(entry)); unknown.methods.removeIf(m -> !m.name.equals("<init>"));
        ctor(unknown).instructions.clear(); ctor(unknown).instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        ctor(unknown).instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
        ctor(unknown).instructions.add(new InsnNode(Opcodes.RETURN));
        try { ForgeEntryProfile.select(unknown, false); throw new AssertionError("Unknown constructor admitted"); }
        catch (IllegalStateException expected) { check(expected.getMessage().startsWith("UNSUPPORTED_ENTRY"), "unknown constructor still rejected"); negatives++; }
        load(args[1], "org/example/publisher/Publisher"); positive("otherpublisher");
        System.out.println("EVENT INITIALIZER CONTRACT PASS assertions=" + assertions + " negatives=" + negatives);
        host.close();
    }

    private static void load(String path, String name) throws Exception {
        source = new TreeMap<>(); resources = new TreeMap<>(); entry = name;
        try (var jar = new JarFile(path)) {
            for (var item : jar.stream().filter(e -> !e.isDirectory()).toList()) {
                byte[] bytes = jar.getInputStream(item).readAllBytes();
                if (item.getName().endsWith(".class")) source.put(item.getName().substring(0, item.getName().length() - 6), bytes);
                else if (item.getName().startsWith("META-INF/services/")) resources.put(item.getName(), new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
            }
        }
    }
    private static ForgeEventInitializerPlan plan(String id, Map<String,byte[]> raw, boolean brokenHost) {
        return ForgeEventInitializerPlan.validate(id, id, entry, raw, resources::get, name -> {
            if (brokenHost) return new byte[0];
            try { var item = host.getJarEntry(name + ".class"); return item == null ? null : host.getInputStream(item).readAllBytes(); }
            catch (Exception failure) { throw new IllegalStateException(failure); }
        });
    }
    private static void positive(String id) {
        var plan = plan(id, source, false);
        check(plan.targets().size() == 4, "four actual targets " + id);
        check(ForgeEntryProfile.select(read(source.get(entry)), false) == ForgeEntryProfile.Kind.EVENT_INITIALIZER, "independent candidate routing " + id);
        for (String target : plan.targets()) {
            ClassNode original = read(source.get(target)); byte[] before = bytes(original);
            ClassNode result = plan.transform(original, java.util.HexFormat.of().parseHex(ForgeRegistryTransformationPlan.digest(source.get(target))));
            check(Arrays.equals(before, bytes(original)), "transform leaves caller unchanged " + target);
            check(!Arrays.equals(before, bytes(result)), "actual rewrite " + target);
            try { plan.transform(result, null); throw new AssertionError("Changed input accepted"); }
            catch (IllegalStateException expected) { check(expected.getMessage().contains("TRANSFORMATION_INPUT_CHANGED"), "changed input rejected " + target); }
            try { plan.transform(original, new byte[32]); throw new AssertionError("Wrong source hash accepted"); }
            catch (IllegalStateException expected) { check(expected.getMessage().contains("INPUT_JAR_SHA_CHANGED"), "source hash bound " + target); }
        }
        // Diagnostic injection tests only the real transform branch. It does not
        // emulate FML ownership, a Module Layer, or a live game lifecycle.
        try {
            var field = DualForgeEntryTransformer.class.getDeclaredField("eventInitializerPlans"); field.setAccessible(true);
            Map<String,ForgeEventInitializerPlan> targets = new HashMap<>();
            plan.targets().forEach(name -> targets.put(name, plan));
            var transformer = new DualForgeEntryTransformer(); field.set(transformer, Map.copyOf(targets));
            ClassNode original = read(source.get(entry)); byte[] before = bytes(original);
            check(!Arrays.equals(before, bytes(transformer.transform(original, null))), "real transform branch " + id);
            Path blocker = output.resolve(id + "-dump-blocker"); Files.writeString(blocker, "not a directory");
            System.setProperty("dualforge.dump.profileDir", blocker.toString());
            try { transformer.transform(original, null); throw new AssertionError("Dump failure returned transformed input"); }
            catch (IllegalStateException expected) { check(Arrays.equals(before, bytes(original)), "dump failure preserves caller " + id); }
            finally { System.clearProperty("dualforge.dump.profileDir"); }
            for (String property : List.of("dualforge.2a.rewrite", "dualforge.0g.apiRewrite")) {
                System.setProperty(property, "false");
                var disabled = new DualForgeEntryTransformer(); field.set(disabled, Map.copyOf(targets));
                try { disabled.transform(original, null); throw new AssertionError("Disabled rewrite returned transformed input"); }
                catch (IllegalStateException expected) { check(Arrays.equals(before, bytes(original)), "disabled rewrite fails closed " + property); }
                finally { System.clearProperty(property); }
            }
            ClassNode unclaimed = read(source.get(entry)); unclaimed.name = "unclaimed/Publisher";
            byte[] untouched = bytes(unclaimed);
            check(Arrays.equals(untouched, bytes(transformer.transform(unclaimed, null))), "unclaimed class unchanged " + id);
        } catch (ReflectiveOperationException | java.io.IOException failure) {
            throw new AssertionError(failure);
        }
    }
    private static void reject(String label, Consumer<Map<String,byte[]>> change) {
        Map<String,byte[]> raw = new TreeMap<>(source); change.accept(raw);
        Map<String,byte[]> before = new TreeMap<>(); raw.forEach((key, value) -> before.put(key, value.clone()));
        try { plan("clumps", raw, false); throw new AssertionError("Accepted " + label); }
        catch (IllegalStateException expected) {
            check(expected.getMessage() != null, "reject " + label); negatives++;
            System.out.println("REJECT " + label + " " + expected.getMessage());
        }
        check(raw.keySet().equals(before.keySet()) && raw.entrySet().stream().allMatch(e -> Arrays.equals(e.getValue(), before.get(e.getKey()))), "rejection unchanged " + label);
    }
    private static void mutate(Map<String,byte[]> raw, String name, Consumer<ClassNode> change) {
        ClassNode node = read(raw.get(name)); change.accept(node); raw.put(name, bytes(node));
    }
    private static MethodNode ctor(ClassNode node) { return node.methods.stream().filter(m -> m.name.equals("<init>")).findFirst().orElseThrow(); }
    private static InvokeDynamicInsnNode dynamic(ClassNode node) { return (InvokeDynamicInsnNode) Arrays.stream(ctor(node).instructions.toArray()).filter(i -> i instanceof InvokeDynamicInsnNode).findFirst().orElseThrow(); }
    private static FieldInsnNode store(ClassNode node) { return (FieldInsnNode) Arrays.stream(ctor(node).instructions.toArray()).filter(i -> i instanceof FieldInsnNode).findFirst().orElseThrow(); }
    private static MethodInsnNode post(ClassNode node) { return (MethodInsnNode) node.methods.stream().flatMap(m -> Arrays.stream(m.instructions.toArray())).filter(i -> i instanceof MethodInsnNode c && c.name.equals("post")).findFirst().orElseThrow(); }
    private static ClassNode read(byte[] bytes) { return ForgeRegistryTransformationPlan.read(bytes); }
    private static byte[] bytes(ClassNode node) { return ForgeRegistryTransformationPlan.bytes(node); }
    private static void check(boolean condition, String label) { if (!condition) throw new AssertionError(label); assertions++; }
}
