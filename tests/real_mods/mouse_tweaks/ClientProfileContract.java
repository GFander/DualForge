package io.github.gfander.dualforge;

import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Real release + independent identity. All fault injection is explicitly offline. */
public final class ClientProfileContract {
    private static int assertions, negatives;
    private static Map<String,byte[]> source;
    private static String entry, id;
    private static JarFile host;
    private static Path output;

    public static void main(String[] args) throws Exception {
        output = Path.of(args[3]); Files.createDirectories(output); host = new JarFile(args[2]);
        load(args[0], "yalter/mousetweaks/forge/MouseTweaksForge", "mousetweaks");
        positive();
        reject("metadata-owner", raw -> mutate(raw, entry, n -> n.visibleAnnotations.getFirst().values.set(1, "wrongowner")));
        reject("constructor-access", raw -> mutate(raw, entry, n -> ctor(n).access = Opcodes.ACC_PRIVATE));
        reject("constructor-parameters", raw -> mutate(raw, entry, n -> ctor(n).desc = "(I)V"));
        reject("extra-entry-side-effect", raw -> mutate(raw, entry, n -> ctor(n).instructions.insertBefore(ctor(n).instructions.getLast(), new InsnNode(Opcodes.NOP))));
        reject("extra-static-initializer", raw -> mutate(raw, entry, n -> { var m = new MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null); m.instructions.add(new InsnNode(Opcodes.RETURN)); n.methods.add(m); }));
        reject("extra-entry-field", raw -> mutate(raw, entry, n -> n.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "unexpected", "I", null, null))));
        reject("foreign-global-owner", raw -> mutate(raw, entry, n -> {
            for (var op : ctor(n).instructions) if (op instanceof FieldInsnNode f) f.owner = "outside/Global";
        }));
        reject("duplicate-registration", raw -> mutate(raw, entry, n -> {
            InsnList extra = new InsnList(); extra.add(new FieldInsnNode(Opcodes.GETSTATIC, ForgeClientEventPlan.GLOBAL, "EVENT_BUS", "L" + ForgeClientEventPlan.BUS + ";"));
            extra.add(new VarInsnNode(Opcodes.ALOAD, 0)); extra.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, ForgeClientEventPlan.BUS, "register", "(Ljava/lang/Object;)V", true));
            ctor(n).instructions.insertBefore(ctor(n).instructions.getLast(), extra);
        }));
        reject("wrong-subscribe-annotation", raw -> mutate(raw, entry, n -> listener(n, "$MouseButtonPressed$Pre").visibleAnnotations.getFirst().desc = "Loutside/SubscribeEvent;"));
        reject("explicit-listener-options", raw -> mutate(raw, entry, n -> listener(n, "$MouseButtonPressed$Pre").visibleAnnotations.getFirst().values = new ArrayList<>(List.of("receiveCanceled", true))));
        reject("static-listener", raw -> mutate(raw, entry, n -> listener(n, "$MouseButtonPressed$Pre").access |= Opcodes.ACC_STATIC));
        reject("unsupported-event", raw -> mutate(raw, entry, n -> listener(n, "$MouseButtonPressed$Pre").desc = "(Lnet/minecraftforge/client/event/ScreenEvent$Opening;)V"));
        reject("duplicate-event-signature", raw -> mutate(raw, entry, n -> listener(n, "$MouseButtonReleased$Pre").desc = listener(n, "$MouseButtonPressed$Pre").desc));
        reject("wrong-listener-parameter-count", raw -> mutate(raw, entry, n -> listener(n, "$MouseButtonPressed$Pre").desc = "(L" + ForgeClientEventPlan.SOURCE + "$MouseButtonPressed$Pre;I)V"));
        reject("wrong-scroll-getter", raw -> mutate(raw, entry, n -> {
            for (var op : listener(n, "$MouseScrolled$Post").instructions) if (op instanceof MethodInsnNode c && c.name.equals("getDeltaY")) c.name = "getDeltaX";
        }));
        reject("false-cancellation", raw -> mutate(raw, entry, n -> {
            var m = listener(n, "$MouseButtonPressed$Pre");
            for (var op : m.instructions.toArray()) if (op.getOpcode() == Opcodes.ICONST_1) m.instructions.set(op, new InsnNode(Opcodes.ICONST_0));
        }));
        reject("unverified-handler-owner", raw -> mutate(raw, entry, n -> {
            for (var op : listener(n, "$MouseButtonPressed$Pre").instructions) if (op instanceof MethodInsnNode c && c.desc.endsWith(")Z")) c.owner = "outside/Logic";
        }));
        reject("unknown-log-bootstrap", raw -> mutate(raw, entry, n -> {
            for (var op : listener(n, "$MouseDragged$Pre").instructions) if (op instanceof InvokeDynamicInsnNode d) d.bsm = new Handle(Opcodes.H_INVOKESTATIC, "outside/Bootstrap", "make", d.bsm.getDesc(), false);
        }));
        reject("captured-config-factory", raw -> mutate(raw, entry, n -> factory(n).desc = "(Ljava/lang/Object;)Ljava/util/function/Function;"));
        reject("wrong-config-sam", raw -> mutate(raw, entry, n -> factory(n).bsmArgs[0] = Type.getMethodType("()Ljava/lang/Object;")));
        reject("foreign-config-constructor", raw -> mutate(raw, entry, n -> {
            var d = factory(n); var h = (Handle) d.bsmArgs[1]; d.bsmArgs[1] = new Handle(h.getTag(), "outside/ConfigScreen", h.getName(), h.getDesc(), false);
        }));
        reject("config-parent-dataflow", raw -> mutate(raw, "yalter/mousetweaks/ConfigScreen", n -> {
            for (var op : ctor(n).instructions) if (op instanceof FieldInsnNode f) f.name = "wrongParent";
        }));
        reject("config-constructor-side-effect", raw -> mutate(raw, "yalter/mousetweaks/ConfigScreen", n -> ctor(n).instructions.insertBefore(ctor(n).instructions.getLast(), new InsnNode(Opcodes.NOP))));
        reject("initializer-extra-effect", raw -> mutate(raw, "yalter/mousetweaks/Main", n -> method(n, "initialize").instructions.insertBefore(method(n, "initialize").instructions.getLast(), new InsnNode(Opcodes.NOP))));
        reject("initializer-unset-ready", raw -> mutate(raw, "yalter/mousetweaks/Main", n -> {
            var m = method(n, "initialize"); for (var op : m.instructions.toArray()) if (op.getOpcode() == Opcodes.ICONST_1) m.instructions.set(op, new InsnNode(Opcodes.ICONST_0));
        }));
        reject("initializer-missing-ready-field", raw -> mutate(raw, "yalter/mousetweaks/Main", n -> n.fields.removeIf(f -> f.name.equals("initialized"))));
        reject("initializer-field-not-static", raw -> mutate(raw, "yalter/mousetweaks/Main", n -> n.fields.stream().filter(f -> f.name.equals("mc")).findFirst().orElseThrow().access = Opcodes.ACC_PRIVATE));
        reject("private-config-constructor", raw -> mutate(raw, "yalter/mousetweaks/Config", n -> ctor(n).access = Opcodes.ACC_PRIVATE));
        reject("missing-business-owner", raw -> raw.remove("yalter/mousetweaks/Main"));
        reject("extra-forge-api-in-business", raw -> mutate(raw, "yalter/mousetweaks/Logger", n -> n.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "unknown", "Lnet/minecraftforge/eventbus/api/IEventBus;", null, null))));
        reject("class-owner-key", raw -> { byte[] value = raw.remove(entry); raw.put("outside/Entry", value); });
        expect(() -> ForgeClientEventPlan.validate(id, id, entry, source, false, ClientProfileContract::host), "CLIENT_DIST_REQUIRED");
        expect(() -> ForgeClientEventPlan.validate(id, "", entry, source, true, ClientProfileContract::host), "OWNER_IDENTITY");
        expect(() -> ForgeClientEventPlan.validate(id, id, entry, source, true, n -> new byte[0]), "UNVERIFIED_CLIENT_HOST_ABI");
        expect(() -> ForgeEntryProfile.select(read(source.get(entry)), true), "AMBIGUOUS_ENTRY");
        ClassNode unknown = read(source.get(entry)); ctor(unknown).instructions.clear();
        ctor(unknown).instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); ctor(unknown).instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)); ctor(unknown).instructions.add(new InsnNode(Opcodes.RETURN));
        expect(() -> ForgeEntryProfile.select(unknown, false), "UNSUPPORTED_ENTRY");
        load(args[1], "org/example/gesture/ClientInput", "gestureinput"); positive();
        System.out.println("CLIENT PROFILE CONTRACT PASS assertions=" + assertions + " negatives=" + negatives);
        host.close();
    }

    private static void positive() throws Exception {
        var plan = plan(source); ClassNode original = read(source.get(entry)); byte[] before = bytes(original);
        check(plan.targets().equals(Set.of(entry)), "only owned entry target " + id);
        plan.rejectForeignClasses(Set.of("unrelated/NativeClass"));
        expect(() -> plan.rejectForeignClasses(Set.of(entry)), "CROSS_MODFILE_CLASS_CONFLICT");
        expect(() -> plan.rejectForeignClasses(Set.of(source.keySet().stream().filter(n -> !n.equals(entry)).findFirst().orElseThrow())), "CROSS_MODFILE_CLASS_CONFLICT");
        check(ForgeEntryProfile.select(original, false) == ForgeEntryProfile.Kind.CLIENT_EVENTS, "separate candidate " + id);
        ClassNode result = plan.transform(original, HexFormat.of().parseHex(ForgeRegistryTransformationPlan.digest(source.get(entry))));
        check(Arrays.equals(before, bytes(original)), "caller unmodified " + id);
        check(!Arrays.equals(before, bytes(result)), "actual rewrite " + id);
        long bridgeCalls = result.methods.stream().flatMap(m -> Arrays.stream(m.instructions.toArray())).filter(op -> op instanceof MethodInsnNode c && c.owner.equals(ForgeClientEventPlan.BRIDGE)).count();
        check(bridgeCalls == 3 && result.methods.size() == 6, "guard/register/config plus private factory adapter " + id);
        check(result.methods.stream().anyMatch(m -> m.name.equals(ForgeClientEventPlan.ADAPTER)), "real factory adapter " + id);
        expect(() -> plan.transform(result, null), "TRANSFORMATION_INPUT_CHANGED");
        expect(() -> plan.transform(original, new byte[32]), "INPUT_JAR_SHA_CHANGED");
        Path root = output.resolve(id); Files.createDirectories(root.resolve(entry).getParent()); Files.write(root.resolve(entry + ".class"), bytes(result));
        // Exercise the actual transformer branch, explicitly outside live FML/Module ownership.
        var field = DualForgeEntryTransformer.class.getDeclaredField("clientEventPlans"); field.setAccessible(true);
        var transformer = new DualForgeEntryTransformer(); field.set(transformer, Map.of(entry, plan));
        check(Arrays.equals(bytes(result), bytes(transformer.transform(original, null))), "actual transformer path " + id);
        ClassNode other = read(source.values().iterator().next()); other.name = "unclaimed/ClientBusiness";
        byte[] untouched = bytes(other); check(Arrays.equals(untouched, bytes(transformer.transform(other, null))), "unclaimed class unchanged " + id);
        for (String property : List.of("dualforge.2a.rewrite", "dualforge.0g.apiRewrite", "dualforge.0l.subscriberRewrite")) {
            System.setProperty(property, "false");
            try { var disabled = new DualForgeEntryTransformer(); field.set(disabled, Map.of(entry, plan)); expect(() -> disabled.transform(original, null), "Controlled Forge transformation rejected"); }
            finally { System.clearProperty(property); }
        }
        Path blocker = output.resolve(id + "-dump-blocker"); Files.writeString(blocker, "not a directory"); System.setProperty("dualforge.dump.profileDir", blocker.toString());
        try { expect(() -> transformer.transform(original, null), "Controlled Forge transformation rejected"); }
        finally { System.clearProperty("dualforge.dump.profileDir"); }
        check(Arrays.equals(before, bytes(original)), "all failed branches preserve original " + id);
    }
    private static void load(String path, String name, String modId) throws Exception {
        source = new TreeMap<>(); entry = name; id = modId;
        try (var jar = new JarFile(path)) { for (var item : jar.stream().filter(i -> i.getName().endsWith(".class")).toList())
            source.put(item.getName().substring(0, item.getName().length() - 6), jar.getInputStream(item).readAllBytes()); }
    }
    private static byte[] host(String name) {
        try { var item = host.getJarEntry(name + ".class"); return item == null ? null : host.getInputStream(item).readAllBytes(); }
        catch (Exception failure) { throw new IllegalStateException(failure); }
    }
    private static ForgeClientEventPlan plan(Map<String,byte[]> raw) { return ForgeClientEventPlan.validate(id, id, entry, raw, true, ClientProfileContract::host); }
    private static void reject(String label, Consumer<Map<String,byte[]>> change) {
        Map<String,byte[]> raw = new TreeMap<>(source); change.accept(raw);
        Map<String,byte[]> snapshots = new TreeMap<>(); raw.forEach((key, value) -> snapshots.put(key, value.clone()));
        try { plan(raw); throw new AssertionError("Accepted " + label); }
        catch (IllegalStateException expected) {
            check(expected.getMessage().startsWith("Client event plan rejected:") || expected.getMessage().startsWith("Registry helper rejected: BYTECODE_FRAME_VERIFICATION"), "explicit reject " + label);
            negatives++; System.out.println("REJECT " + label + " " + expected.getMessage());
        }
        check(raw.entrySet().stream().allMatch(e -> Arrays.equals(e.getValue(), snapshots.get(e.getKey()))), "refusal leaves raw unchanged " + label);
    }
    private static void expect(Runnable action, String fragment) {
        try { action.run(); throw new AssertionError("Expected " + fragment); }
        catch (IllegalStateException expected) { check(expected.getMessage().contains(fragment), "explicit refusal " + fragment); negatives++; }
    }
    private static void mutate(Map<String,byte[]> raw, String name, Consumer<ClassNode> change) { ClassNode n = read(raw.get(name)); change.accept(n); raw.put(name, bytes(n)); }
    private static MethodNode ctor(ClassNode n) { return method(n, "<init>"); }
    private static MethodNode method(ClassNode n, String name) { return n.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow(); }
    private static MethodNode listener(ClassNode n, String suffix) { return n.methods.stream().filter(m -> m.desc.equals("(L" + ForgeClientEventPlan.SOURCE + suffix + ";)V")).findFirst().orElseThrow(); }
    private static InvokeDynamicInsnNode factory(ClassNode n) { return (InvokeDynamicInsnNode) Arrays.stream(ctor(n).instructions.toArray()).filter(op -> op instanceof InvokeDynamicInsnNode).findFirst().orElseThrow(); }
    private static ClassNode read(byte[] b) { return ForgeRegistryTransformationPlan.read(b); }
    private static byte[] bytes(ClassNode n) { return ForgeRegistryTransformationPlan.bytes(n); }
    private static void check(boolean valid, String label) { if (!valid) throw new AssertionError(label); assertions++; }
}
