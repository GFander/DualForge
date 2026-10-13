package io.github.gfander.dualforge;

import cpw.mods.jarhandling.JarContents;
import cpw.mods.modlauncher.api.ITransformer;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.readers.JarModsDotTomlModFileReader;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Actual FML scan ownership, per-call mapping, unchanged suppliers and whole-target atomicity. */
public final class MultiRegistryBytecodeTest {
    private static final String FORGE = "net/minecraftforge/registries/";
    private static final String NEO = "net/neoforged/neoforge/registries/";
    private static final String ITEM = "net/minecraft/world/item/Item";
    private static final String ORIGINAL_BUS = "(Lnet/minecraftforge/eventbus/api/IEventBus;)V";
    private static final String MAPPED_BUS = "(Lnet/neoforged/bus/api/IEventBus;)V";
    private static final String ITEM_REGISTRATION = "(Ljava/lang/String;Ljava/util/function/Supplier;)L" + FORGE + "RegistryObject;";
    private static final String MAIN_ENTRY = "com/example/multiregistrycheck/MultiRegistryProbe";
    private static final String MATRIX_ENTRY = "com/example/registrymatrix/LocalRegistryProbe";

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        System.out.println("PASS " + label);
    }

    private static ClassNode read(byte[] value) {
        ClassNode node = new ClassNode();
        new ClassReader(value).accept(node, 0);
        return node;
    }

    private static byte[] bytes(ClassNode node) {
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static MethodNode method(ClassNode node, String name) {
        return node.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();
    }

    private static MethodNode method(ClassNode node, Handle handle) {
        return node.methods.stream().filter(m -> m.name.equals(handle.getName())
                && m.desc.equals(handle.getDesc())).findFirst().orElseThrow();
    }

    private static List<InvokeDynamicInsnNode> suppliers(ClassNode node) {
        List<InvokeDynamicInsnNode> result = new ArrayList<>();
        for (AbstractInsnNode insn : method(node, "<clinit>").instructions) {
            if (insn instanceof InvokeDynamicInsnNode indy) result.add(indy);
        }
        return result;
    }

    private static AbstractInsnNode previousOpcode(AbstractInsnNode node) {
        AbstractInsnNode result = node.getPrevious();
        while (result != null && result.getOpcode() < 0) result = result.getPrevious();
        return result;
    }

    private static ModFile scan(Path jar, boolean nativeMetadata) throws Exception {
        ModFile file = (ModFile) (nativeMetadata
                ? new JarModsDotTomlModFileReader().read(JarContents.of(jar), ModFileDiscoveryAttributes.DEFAULT)
                : new ForgeModReader().read(JarContents.of(jar), ModFileDiscoveryAttributes.DEFAULT));
        check(file != null, "actual " + (nativeMetadata ? "native" : "Forge") + " Reader accepted " + jar.getFileName());
        file.setScanResult(file.compileContent(), null);
        LoadingModList.of(List.of(), List.of(file), List.of(), List.of(), Map.of());
        return file;
    }

    private static byte[] supplierMethods(ClassNode node) {
        ClassNode carrier = new ClassNode();
        carrier.version = node.version;
        carrier.access = Opcodes.ACC_PUBLIC;
        carrier.name = "SupplierEvidence";
        carrier.superName = "java/lang/Object";
        Set<String> copied = new HashSet<>();
        for (InvokeDynamicInsnNode indy : suppliers(node)) {
            Handle implementation = (Handle) indy.bsmArgs[1];
            check(node.name.equals(implementation.getOwner()), "supplier implementation belongs to its actual entry");
            if (copied.add(implementation.getName() + implementation.getDesc())) {
                // Select implementations from actual handles, including named method references.
                carrier.methods.add(method(node, implementation));
            }
        }
        return bytes(carrier);
    }

    private static ClassNode positive(DualForgeEntryTransformer transformer, ClassNode source,
                                      Path output, String label, Set<String> ids) throws Exception {
        byte[] original = bytes(source);
        Files.write(output.resolve(label + ".before.class"), original);
        ClassNode rewritten = transformer.transform(source, null);
        check(Arrays.equals(original, bytes(source)), label + " successful conversion preserves caller ClassNode");
        Files.write(output.resolve(label + ".after.class"), bytes(rewritten));
        check(method(source, "<init>").desc.equals(ORIGINAL_BUS)
                && method(rewritten, "<init>").desc.equals(MAPPED_BUS), label + " existing one-bus constructor ABI mapped");
        check(suppliers(source).size() == 2, label + " actual two supplier declarations");
        Set<String> actualIds = new HashSet<>();
        for (InvokeDynamicInsnNode indy : suppliers(source)) {
            AbstractInsnNode previous = previousOpcode(indy);
            check(previous instanceof LdcInsnNode && ((LdcInsnNode) previous).cst instanceof String,
                    label + " registration ID is a literal data dependency");
            actualIds.add((String) ((LdcInsnNode) previous).cst);
        }
        check(actualIds.equals(ids), label + " independent distinct IDs preserved");
        List<InvokeDynamicInsnNode> oldSuppliers = suppliers(source);
        List<InvokeDynamicInsnNode> newSuppliers = suppliers(rewritten);
        check(oldSuppliers.size() == newSuppliers.size(), label + " supplier call sites preserved");
        for (int i = 0; i < oldSuppliers.size(); i++) {
            InvokeDynamicInsnNode old = oldSuppliers.get(i), now = newSuppliers.get(i);
            check(old.name.equals(now.name) && old.desc.equals(now.desc)
                    && old.bsm.equals(now.bsm) && Arrays.deepEquals(old.bsmArgs, now.bsmArgs),
                    label + " unchanged BootstrapMethods and implementation handle " + i);
        }
        check(Arrays.equals(supplierMethods(source), supplierMethods(rewritten)),
                label + " lambda and method-reference business methods unchanged");

        // Reverse only the exact authorized sites. Canonical full-byte equality catches every other edit.
        ClassNode restored = read(bytes(rewritten));
        check(source.fields.size() == restored.fields.size() && source.methods.size() == restored.methods.size(),
                label + " no added or removed fields/methods");
        int registerFields = 0, holderFields = 0, creates = 0, registrations = 0, attachments = 0, getters = 0;
        for (int i = 0; i < source.fields.size(); i++) {
            FieldNode old = source.fields.get(i), now = restored.fields.get(i);
            check(old.name.equals(now.name), label + " field identity unchanged " + old.name);
            String expectedDescriptor, expectedSignature;
            if (old.desc.equals("L" + FORGE + "DeferredRegister;")) {
                registerFields++;
                expectedDescriptor = "L" + NEO + "DeferredRegister;";
                expectedSignature = "L" + NEO + "DeferredRegister<L" + ITEM + ";>;";
            } else {
                check(old.desc.equals("L" + FORGE + "RegistryObject;"), label + " verified holder field " + old.name);
                holderFields++;
                expectedDescriptor = "L" + NEO + "DeferredHolder;";
                expectedSignature = "L" + NEO + "DeferredHolder<L" + ITEM + ";L" + ITEM + ";>;";
            }
            check(now.desc.equals(expectedDescriptor) && now.signature.equals(expectedSignature),
                    label + " coherent field descriptor and generic signature " + old.name);
            now.desc = old.desc;
            now.signature = old.signature;
        }
        check(registerFields == 1 && holderFields == 2, label + " one actual register and two holders");
        for (int m = 0; m < source.methods.size(); m++) {
            MethodNode oldMethod = source.methods.get(m), nowMethod = restored.methods.get(m);
            if (oldMethod.name.equals("<init>")) nowMethod.desc = oldMethod.desc;
            AbstractInsnNode[] oldOps = oldMethod.instructions.toArray(), nowOps = nowMethod.instructions.toArray();
            check(oldOps.length == nowOps.length, label + " instruction layout preserved in " + oldMethod.name);
            for (int i = 0; i < oldOps.length; i++) {
                if (oldOps[i] instanceof FieldInsnNode old && nowOps[i] instanceof FieldInsnNode now) {
                    if (old.owner.equals(FORGE + "ForgeRegistries")) {
                        check(old.getOpcode() == Opcodes.GETSTATIC && old.name.equals("ITEMS")
                                && now.owner.equals("net/minecraft/core/registries/BuiltInRegistries")
                                && now.name.equals("ITEM") && now.desc.equals("Lnet/minecraft/core/DefaultedRegistry;"),
                                label + " actual Item registry field mapped");
                        now.owner = old.owner;
                        now.name = old.name;
                        now.desc = old.desc;
                    } else if (old.owner.equals(source.name)) {
                        FieldNode mapped = rewritten.fields.stream().filter(f -> f.name.equals(old.name)).findFirst().orElseThrow();
                        check(now.owner.equals(old.owner) && now.name.equals(old.name) && now.desc.equals(mapped.desc),
                                label + " each own-field reference uses its mapped declaration " + old.name);
                        now.desc = old.desc;
                    }
                } else if (oldOps[i] instanceof MethodInsnNode old && nowOps[i] instanceof MethodInsnNode now) {
                    if (old.owner.equals(FORGE + "DeferredRegister")) {
                        check(now.owner.equals(NEO + "DeferredRegister") && old.name.equals(now.name),
                                label + " verified register member owner mapped");
                        String expected;
                        if (old.name.equals("create")) {
                            creates++;
                            expected = "(Lnet/minecraft/core/Registry;Ljava/lang/String;)L" + NEO + "DeferredRegister;";
                        } else if (old.desc.equals(ITEM_REGISTRATION)) {
                            registrations++;
                            expected = "(Ljava/lang/String;Ljava/util/function/Supplier;)L" + NEO + "DeferredHolder;";
                        } else {
                            check(old.name.equals("register") && old.desc.equals(ORIGINAL_BUS), label + " existing Mod Bus attachment call");
                            attachments++;
                            expected = MAPPED_BUS;
                        }
                        check(now.desc.equals(expected), label + " exact verified register member descriptor mapped");
                        now.owner = old.owner;
                        now.desc = old.desc;
                    } else if (old.owner.equals(FORGE + "RegistryObject")) {
                        getters++;
                        check(old.name.equals("get") && old.desc.equals("()Ljava/lang/Object;")
                                && now.owner.equals(NEO + "DeferredHolder")
                                && now.name.equals(old.name) && now.desc.equals(old.desc), label + " actual Holder.get mapping");
                        now.owner = old.owner;
                    }
                }
            }
        }
        check(creates == 1 && registrations == 2 && attachments == 1 && getters == 2,
                label + " every independent API call site accounted for");
        check(Arrays.equals(original, bytes(restored)), label + " only authorized ABI field and API member edits");
        return rewritten;
    }

    private static String exceptionText(Throwable failure) {
        StringWriter output = new StringWriter();
        failure.printStackTrace(new PrintWriter(output));
        return output.toString();
    }

    private static void reject(DualForgeEntryTransformer transformer, ClassNode source,
                               Consumer<ClassNode> mutation, Path output, String label) throws Exception {
        ClassNode bad = read(bytes(source));
        mutation.accept(bad);
        byte[] before = bytes(bad);
        Files.write(output.resolve(label + ".before.class"), before);
        IllegalStateException failure = null;
        try { transformer.transform(bad, null); }
        catch (IllegalStateException ex) { failure = ex; }
        check(failure != null && failure.getMessage().contains("Controlled Forge transformation rejected")
                && exceptionText(failure).contains("005 unsupported Item profile:"), label + " explicit controlled refusal");
        Files.writeString(output.resolve(label + ".reason.log"), exceptionText(failure));
        byte[] after = bytes(bad);
        Files.write(output.resolve(label + ".after.class"), after);
        check(Arrays.equals(before, after), label + " whole original ClassNode unchanged");
    }

    /** Use the real compiler-produced bootstrap and implementation; rebase only class ownership. */
    private static void replaceSupplier(ClassNode target, ClassNode forms, String factory, boolean captured) {
        InvokeDynamicInsnNode original = suppliers(target).getFirst();
        Handle oldHandle = (Handle) original.bsmArgs[1];
        MethodNode factoryMethod = method(forms, factory);
        InvokeDynamicInsnNode real = null;
        for (AbstractInsnNode insn : factoryMethod.instructions) {
            if (insn instanceof InvokeDynamicInsnNode indy) real = indy;
        }
        if (real == null) throw new AssertionError("missing actual compiler supplier form " + factory);
        Handle implementation = (Handle) real.bsmArgs[1];
        MethodNode copy = new MethodNode();
        method(forms, implementation).accept(copy);
        // MethodNode.accept(MethodVisitor) copies the body; the declaration metadata is explicit here.
        MethodNode actual = method(forms, implementation);
        copy.access = actual.access;
        copy.name = actual.name;
        copy.desc = actual.desc;
        copy.signature = actual.signature;
        copy.exceptions = new ArrayList<>(actual.exceptions);
        target.methods.removeIf(m -> m.name.equals(oldHandle.getName()) && m.desc.equals(oldHandle.getDesc()));
        target.methods.add(copy);
        original.name = real.name;
        original.desc = real.desc;
        original.bsm = real.bsm;
        original.bsmArgs = real.bsmArgs.clone();
        original.bsmArgs[1] = new Handle(implementation.getTag(), target.name,
                implementation.getName(), implementation.getDesc(), implementation.isInterface());
        if (captured) method(target, "<clinit>").instructions.insertBefore(original, new LdcInsnNode("captured marker"));
    }

    private static void dumpFailure(DualForgeEntryTransformer transformer, ClassNode source, Path output) throws Exception {
        Path blocked = output.resolve("dump-parent-is-a-file");
        Files.writeString(blocked, "This intentionally blocks Files.createDirectories; this is a test-only expected dump failure.\n");
        String previous = System.getProperty("dualforge.dump.profileDir");
        ClassNode input = read(bytes(source));
        byte[] before = bytes(input);
        Files.write(output.resolve("dump-io-failure.before.class"), before);
        IllegalStateException failure = null;
        try {
            System.setProperty("dualforge.dump.profileDir", blocked.toString());
            try { transformer.transform(input, null); }
            catch (IllegalStateException ex) { failure = ex; }
        } finally {
            if (previous == null) System.clearProperty("dualforge.dump.profileDir");
            else System.setProperty("dualforge.dump.profileDir", previous);
        }
        boolean ioCause = false;
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof IOException) ioCause = true;
        }
        check(failure != null && ioCause && failure.getMessage().contains("Controlled Forge transformation rejected"),
                "expected test-only dump IO failure after rewrite is explicit");
        Files.writeString(output.resolve("dump-io-failure.reason.log"), exceptionText(failure));
        Files.write(output.resolve("dump-io-failure.after.class"), bytes(input));
        check(Arrays.equals(before, bytes(input)), "failure after validated edits never exposes partial ClassNode mutation");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("fixture native-control matrix output runtime-profile-or--");
        Path output = Path.of(args[3]);
        Files.createDirectories(output);
        ModFile forge = scan(Path.of(args[0]), false);
        DualForgeEntryTransformer transformer = new DualForgeEntryTransformer();
        check(transformer.targets().equals(Set.of(ITransformer.Target.targetClass(MAIN_ENTRY.replace('/', '.')))),
                "only actual scan-owned multiregistryprobe entry targeted");
        ClassNode source = read(Files.readAllBytes(forge.findResource(MAIN_ENTRY + ".class")));
        ClassNode rewritten = positive(transformer, source, output, "entry", Set.of("alpha_item", "beta_item"));
        if (!args[4].equals("-")) {
            ClassNode actual = read(Files.readAllBytes(Path.of(args[4]).resolve("MultiRegistryProbe.class")));
            check(Arrays.equals(bytes(rewritten), bytes(actual)), "real runtime dump equals exact controlled transformation");
        }

        ModFile matrixFile = (ModFile) new ForgeModReader().read(JarContents.of(Path.of(args[2])), ModFileDiscoveryAttributes.DEFAULT);
        ClassNode forms = read(Files.readAllBytes(matrixFile.findResource("com/example/registrymatrix/SupplierForms.class")));
        reject(transformer, source, n -> {
            List<InvokeDynamicInsnNode> indy = suppliers(n);
            ((LdcInsnNode) previousOpcode(indy.get(1))).cst = ((LdcInsnNode) previousOpcode(indy.getFirst())).cst;
        }, output, "duplicate-registration-id");
        reject(transformer, source, n -> {
            InvokeDynamicInsnNode indy = suppliers(n).getFirst();
            indy.bsm = new Handle(Opcodes.H_INVOKESTATIC, "unknown/Factory", "make", indy.bsm.getDesc(), false);
        }, output, "unknown-lambda-bootstrap");
        reject(transformer, source, n -> replaceSupplier(n, forms, "serializableLambda", false), output, "actual-alt-metafactory");
        reject(transformer, source, n -> replaceSupplier(n, forms, "capturedLambda", true), output, "actual-captured-supplier");
        reject(transformer, source, n -> {
            for (AbstractInsnNode insn : method(n, "<clinit>").instructions) {
                if (insn instanceof FieldInsnNode field && field.owner.equals(FORGE + "ForgeRegistries")) field.name = "BLOCKS";
            }
        }, output, "wrong-registry-type");
        reject(transformer, source, n -> {
            InvokeDynamicInsnNode indy = suppliers(n).get(1);
            Handle impl = (Handle) indy.bsmArgs[1];
            indy.bsmArgs[1] = new Handle(impl.getTag(), "unowned/Other", impl.getName(), impl.getDesc(), impl.isInterface());
        }, output, "cross-class-supplier-handle");
        reject(transformer, source, n -> {
            MethodNode init = method(n, "<clinit>");
            FieldNode register = n.fields.stream().filter(f -> f.desc.equals("L" + FORGE + "DeferredRegister;")).findFirst().orElseThrow();
            InsnList extra = new InsnList();
            extra.add(new FieldInsnNode(Opcodes.GETSTATIC, n.name, register.name, register.desc));
            extra.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, FORGE + "DeferredRegister", "getEntries", "()Ljava/util/Collection;", false));
            extra.add(new InsnNode(Opcodes.POP));
            AbstractInsnNode last = init.instructions.getLast();
            while (last.getOpcode() < 0) last = last.getPrevious();
            init.instructions.insertBefore(last, extra);
        }, output, "extra-unvalidated-forge-api");
        reject(transformer, source, n -> {
            for (AbstractInsnNode insn : method(n, "<init>").instructions) {
                if (insn instanceof MethodInsnNode call && call.owner.equals(FORGE + "DeferredRegister")
                        && call.name.equals("register") && call.desc.equals(ORIGINAL_BUS)) {
                    ((VarInsnNode) previousOpcode(call)).var = 0;
                }
            }
        }, output, "wrong-mod-bus");
        reject(transformer, source, n -> {
            for (AbstractInsnNode insn : method(n, "betaValue").instructions) {
                if (insn instanceof MethodInsnNode call && call.owner.equals(FORGE + "RegistryObject")) call.desc = "()Ljava/lang/String;";
            }
        }, output, "late-second-holder-getter-failure");
        dumpFailure(transformer, source, output);

        ClassNode ordinary = read(bytes(source));
        String ownedName = ordinary.name;
        ordinary.name = "unclaimed/Ordinary";
        for (MethodNode m : ordinary.methods) {
            for (AbstractInsnNode insn : m.instructions) {
                if (insn instanceof FieldInsnNode field && field.owner.equals(ownedName)) field.owner = ordinary.name;
                if (insn instanceof InvokeDynamicInsnNode indy) {
                    Handle impl = (Handle) indy.bsmArgs[1];
                    indy.bsmArgs[1] = new Handle(impl.getTag(), ordinary.name, impl.getName(), impl.getDesc(), impl.isInterface());
                }
            }
        }
        byte[] ordinaryBefore = bytes(ordinary);
        ClassNode ordinaryAfter = transformer.transform(ordinary, null);
        Files.write(output.resolve("unclaimed.before.class"), ordinaryBefore);
        Files.write(output.resolve("unclaimed.after.class"), bytes(ordinaryAfter));
        check(Arrays.equals(ordinaryBefore, bytes(ordinaryAfter)), "unclaimed same API and constructor shape remains byte-identical");
        check(Arrays.equals(ordinaryBefore, bytes(ordinary)), "unclaimed original remains byte-identical");

        ModFile matrix = scan(Path.of(args[2]), false);
        DualForgeEntryTransformer matrixTransformer = new DualForgeEntryTransformer();
        check(matrixTransformer.targets().equals(Set.of(ITransformer.Target.targetClass(MATRIX_ENTRY.replace('/', '.')))),
                "independent actual matrix modId scan derives only its own entry");
        ClassNode localLayout = read(Files.readAllBytes(matrix.findResource(MATRIX_ENTRY + ".class")));
        check(Arrays.stream(method(localLayout, "<clinit>").instructions.toArray()).anyMatch(i -> i.getOpcode() == Opcodes.ASTORE),
                "actual alternate initializer contains DeferredRegister local alias");
        positive(matrixTransformer, localLayout, output, "local-layout", Set.of("gamma_item", "delta_item"));

        scan(Path.of(args[1]), true);
        check(transformer.targets().isEmpty(), "native metadata with identical Forge entry bytes is not claimed");
        byte[] nativeBefore = bytes(source);
        ClassNode nativeAfter = transformer.transform(read(nativeBefore), null);
        Files.write(output.resolve("native.before.class"), nativeBefore);
        Files.write(output.resolve("native.after.class"), bytes(nativeAfter));
        check(Arrays.equals(nativeBefore, bytes(nativeAfter)), "native path entry remains byte-identical");
        System.out.println("PASS controlled multi Item scope and atomicity");
    }
}
