package io.github.gfander.dualforge;

import cpw.mods.jarhandling.JarContents;
import cpw.mods.modlauncher.api.ITransformer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
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
import org.objectweb.asm.tree.*;

/** Actual FML ownership, plain suppliers, exact call mapping and refusal atomicity. */
public final class NormalItemBytecodeTest {
    private static final String ENTRY = "com/example/dropin/DropInItems";
    private static final String FORGE = "net/minecraftforge/registries/";
    private static final String NEO = "net/neoforged/neoforge/registries/";
    private static Path output;

    public static void main(String[] args) throws Exception {
        output = Path.of(args[2]);
        Files.createDirectories(output);
        for (String key : List.of("2a.rewrite", "0g.apiRewrite", "0j.emptyRegistration", "0l.subscriberRewrite",
                                  "dump.path", "dump.profileDir")) System.clearProperty("dualforge." + key);
        ModFile forge = scan(Path.of(args[0]), false);
        ClassNode source = read(Files.readAllBytes(forge.findResource(ENTRY + ".class")));
        byte[] original = bytes(source);
        DualForgeEntryTransformer transformer = new DualForgeEntryTransformer();
        check(transformer.targets().equals(Set.of(ITransformer.Target.targetClass(ENTRY.replace('/', '.')))), "exact scan-owned target");
        ClassNode changed = transformer.transform(source, null);
        check(Arrays.equals(original, bytes(source)), "success preserves caller");
        Files.write(output.resolve("positive.before.class"), original);
        Files.write(output.resolve("positive.after.class"), bytes(changed));
        check(method(changed, "<init>").desc.equals("(Lnet/neoforged/bus/api/IEventBus;)V"), "existing one-bus ABI mapped");
        for (MethodNode m : source.methods) {
            if (m.name.startsWith("lambda$") || m.name.equals("createRaw")) {
                check(Arrays.equals(methodBytes(m), methodBytes(method(changed, m.name))), "unchanged supplier " + m.name);
            }
        }
        int creates = 0, declarations = 0, attachments = 0, getters = 0, indy = 0;
        ClassNode restored = read(bytes(changed));
        for (int i = 0; i < source.fields.size(); i++) {
            var before = source.fields.get(i);
            var after = restored.fields.get(i);
            String kind = before.desc.endsWith("DeferredRegister;") ? "DeferredRegister" : "DeferredHolder";
            check(after.desc.equals("L" + NEO + kind + ";"), "typed field mapped " + before.name);
            after.desc = before.desc;
            after.signature = before.signature;
        }
        for (int i = 0; i < source.methods.size(); i++) {
            MethodNode before = source.methods.get(i), after = restored.methods.get(i);
            after.desc = before.desc;
            var old = before.instructions.toArray();
            var now = after.instructions.toArray();
            check(old.length == now.length, "instruction layout preserved " + before.name);
            for (int j = 0; j < old.length; j++) {
                if (old[j] instanceof FieldInsnNode f && now[j] instanceof FieldInsnNode g) {
                    if (f.owner.equals(FORGE + "ForgeRegistries")) {
                        check(g.owner.equals("net/minecraft/core/registries/BuiltInRegistries") && g.name.equals("ITEM"), "actual Item registry mapped");
                    }
                    g.owner = f.owner; g.name = f.name; g.desc = f.desc;
                } else if (old[j] instanceof MethodInsnNode m && now[j] instanceof MethodInsnNode n && m.owner.startsWith(FORGE)) {
                    String expectedOwner = NEO + (m.owner.endsWith("RegistryObject") ? "DeferredHolder" : "DeferredRegister");
                    check(n.owner.equals(expectedOwner) && n.getOpcode() == m.getOpcode() && n.name.equals(m.name), "only verified API owner mapped");
                    if (m.name.equals("create")) {
                        creates++;
                        check(n.desc.equals("(Lnet/minecraft/core/Registry;Ljava/lang/String;)L" + NEO + "DeferredRegister;"), "create descriptor");
                    } else if (m.name.equals("get")) getters++;
                    else if (m.desc.startsWith("(Ljava/lang/String;")) {
                        declarations++;
                        check(n.desc.equals("(Ljava/lang/String;Ljava/util/function/Supplier;)L" + NEO + "DeferredHolder;"), "declaration descriptor");
                    } else {
                        attachments++;
                        check(n.desc.equals("(Lnet/neoforged/bus/api/IEventBus;)V"), "injected bus descriptor");
                    }
                    n.owner = m.owner; n.desc = m.desc;
                } else if (old[j] instanceof InvokeDynamicInsnNode a && now[j] instanceof InvokeDynamicInsnNode b) {
                    indy++;
                    check(a.bsm.equals(b.bsm) && a.desc.equals(b.desc) && a.name.equals(b.name)
                            && Arrays.deepEquals(a.bsmArgs, b.bsmArgs), "unchanged bootstrap and implementation handle");
                }
            }
        }
        check(creates == 1 && declarations == 2 && attachments == 1 && getters == 2 && indy == 2, "exact verified site counts");
        check(Arrays.equals(original, bytes(restored)), "every other byte unchanged after reversing exact mapped sites");
        reject(transformer, source, "duplicate-id", n -> literals(n).get(2).cst = "ice_ether");
        reject(transformer, source, "wrong-registry", n -> {
            for (var i : method(n, "<clinit>").instructions) if (i instanceof FieldInsnNode f && f.owner.equals(FORGE + "ForgeRegistries")) f.name = "BLOCKS";
        });
        reject(transformer, source, "unknown-bootstrap", n -> {
            var d = supplier(n); d.bsm = new Handle(Opcodes.H_INVOKESTATIC, "unknown/Bootstrap", "metafactory", d.bsm.getDesc(), false);
        });
        reject(transformer, source, "captured-supplier", n -> supplier(n).desc = "(Ljava/lang/Object;)Ljava/util/function/Supplier;");
        reject(transformer, source, "wrong-bus", n -> {
            for (var i : method(n, "<init>").instructions) if (i instanceof VarInsnNode v && v.var == 1) v.var = 0;
        });
        reject(transformer, source, "extra-api", n -> {
            var code = method(n, "createRaw").instructions;
            var extra = new InsnList();
            extra.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false));
            extra.add(new InsnNode(Opcodes.POP2)); code.insert(extra);
        });
        reject(transformer, source, "extra-attachment", n -> {
            var ctor = method(n, "<init>"); var extra = new InsnList();
            extra.add(new FieldInsnNode(Opcodes.GETSTATIC, ENTRY, "ITEMS", "L" + FORGE + "DeferredRegister;"));
            extra.add(new VarInsnNode(Opcodes.ALOAD, 1));
            extra.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, FORGE + "DeferredRegister", "register", "(Lnet/minecraftforge/eventbus/api/IEventBus;)V", false));
            ctor.instructions.insertBefore(ctor.instructions.getLast(), extra);
        });
        reject(transformer, source, "other-constructor-abi", n -> method(n, "<init>").desc = "()V");
        reject(transformer, source, "wrong-holder-generic", n -> n.fields.get(1).signature = null);
        reject(transformer, source, "wrong-metadata-owner", n -> n.visibleAnnotations.getFirst().values.set(1, "otherowner"));
        Path blocker = output.resolve("blocked"); Files.writeString(blocker, "blocked");
        System.setProperty("dualforge.dump.profileDir", blocker.toString());
        reject(transformer, source, "dump-io-after-mapping", n -> {});
        System.clearProperty("dualforge.dump.profileDir");
        ClassNode ordinary = read(original); ordinary.name = "ordinary/SameShape";
        check(Arrays.equals(bytes(ordinary), bytes(transformer.transform(ordinary, null))), "same shape unclaimed class unchanged");
        System.setProperty("dualforge.2a.rewrite", "false");
        var disabled = new DualForgeEntryTransformer(); disabled.targets();
        check(Arrays.equals(original, bytes(disabled.transform(source, null))), "explicit rewrite false preserved");
        System.clearProperty("dualforge.2a.rewrite");
        scan(Path.of(args[1]), true);
        var nativeTransformer = new DualForgeEntryTransformer();
        check(nativeTransformer.targets().isEmpty(), "native metadata never claimed");
        check(Arrays.equals(original, bytes(nativeTransformer.transform(source, null))), "unowned Forge-shaped class unchanged");
        System.out.println("NORMAL ITEM CONTRACT PASS negatives=11 supplierLogicUnchanged=true");
    }

    private static ModFile scan(Path jar, boolean nativeMetadata) throws Exception {
        ModFile file = (ModFile) (nativeMetadata
                ? new JarModsDotTomlModFileReader().read(JarContents.of(jar), ModFileDiscoveryAttributes.DEFAULT)
                : new ForgeModReader().read(JarContents.of(jar), ModFileDiscoveryAttributes.DEFAULT));
        check(file != null, "actual Reader accepted " + jar.getFileName());
        file.setScanResult(file.compileContent(), null);
        LoadingModList.of(List.of(), List.of(file), List.of(), List.of(), Map.of());
        return file;
    }
    private static void reject(DualForgeEntryTransformer transformer, ClassNode source, String label, Consumer<ClassNode> mutate) throws Exception {
        ClassNode changed = read(bytes(source)); mutate.accept(changed); byte[] before = bytes(changed);
        Files.write(output.resolve(label + ".before.class"), before);
        try { transformer.transform(changed, null); throw new AssertionError("Accepted " + label); }
        catch (IllegalStateException expected) { check(expected.getMessage().contains("Controlled Forge transformation rejected"), "explicit rejection " + label); }
        check(Arrays.equals(before, bytes(changed)), "caller unchanged on rejection " + label);
        Files.write(output.resolve(label + ".caller-after.class"), bytes(changed));
    }
    private static ClassNode read(byte[] data) { ClassNode n = new ClassNode(); new ClassReader(data).accept(n, 0); return n; }
    private static byte[] bytes(ClassNode n) { ClassWriter w = new ClassWriter(0); n.accept(w); return w.toByteArray(); }
    private static byte[] methodBytes(MethodNode m) { ClassNode n = new ClassNode(); n.version = Opcodes.V21; n.name = "Supplier"; n.superName = "java/lang/Object"; n.methods.add(m); return bytes(n); }
    private static MethodNode method(ClassNode n, String name) { return n.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow(); }
    private static List<LdcInsnNode> literals(ClassNode n) { return Arrays.stream(method(n, "<clinit>").instructions.toArray()).filter(i -> i instanceof LdcInsnNode).map(i -> (LdcInsnNode) i).toList(); }
    private static InvokeDynamicInsnNode supplier(ClassNode n) { return Arrays.stream(method(n, "<clinit>").instructions.toArray()).filter(i -> i instanceof InvokeDynamicInsnNode).map(i -> (InvokeDynamicInsnNode) i).findFirst().orElseThrow(); }
    private static void check(boolean condition, String label) { if (!condition) throw new AssertionError(label); System.out.println("PASS " + label); }
}
