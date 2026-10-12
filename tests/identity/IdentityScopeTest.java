package com.example.forgecompat;

import cpw.mods.jarhandling.JarContents;
import cpw.mods.modlauncher.api.ITransformer;
import java.lang.annotation.ElementType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.readers.JarModsDotTomlModFileReader;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

/** Real FML metadata/scanning; no candidate class initialization or game layer. */
public final class IdentityScopeTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        System.out.println("PASS " + message);
    }

    private static ModFile file(Path path, boolean nativeFile) throws Exception {
        var contents = JarContents.of(path);
        var reader = nativeFile ? new JarModsDotTomlModFileReader() : new ForgeModReader();
        var file = (ModFile) reader.read(contents, ModFileDiscoveryAttributes.DEFAULT);
        check(file != null, "real reader accepted " + path.getFileName());
        file.setScanResult(file.compileContent(), null);
        return file;
    }

    private static byte[] transform(ForgeProbeConstructorTransformer transformer, byte[] before) {
        ClassNode node = new ClassNode();
        new ClassReader(before).accept(node, 0);
        transformer.transform(node, null);
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static void unchanged(ForgeProbeConstructorTransformer transformer, ModFile file,
                                  String name, Path out) throws Exception {
        Files.createDirectories(out);
        byte[] original = Files.readAllBytes(file.findResource(name + ".class"));
        // ASM canonicalization is identical on both sides, so byte equality detects edits.
        ClassNode node = new ClassNode();
        new ClassReader(original).accept(node, 0);
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        byte[] before = writer.toByteArray();
        byte[] after = transform(transformer, original);
        Files.write(out.resolve(name.substring(name.lastIndexOf('/') + 1) + ".before.class"), before);
        Files.write(out.resolve(name.substring(name.lastIndexOf('/') + 1) + ".after.class"), after);
        check(Arrays.equals(before, after), "byte-identical unclaimed class " + name);
    }

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[2]);
        Files.createDirectories(out);
        ModFile forge = file(Path.of(args[0]), false);
        var scan = forge.getScanResult();
        var info = forge.getModInfos().getFirst();
        check(ForgeCompatLanguageLoader.forgeEntrypoints(info, scan)
                        .equals(List.of("com.example.identitycheck.IdentityProbe")),
                "@Mod.value matched to owning modId, unlistedprobe excluded");
        LoadingModList.of(List.of(), List.of(forge), List.of(), List.of(), Map.of());
        var transformer = new ForgeProbeConstructorTransformer();
        Set<ITransformer.Target<ClassNode>> targets = transformer.targets();
        check(targets.equals(Set.of(ITransformer.Target.targetClass("com.example.identitycheck.IdentityProbe"),
                        ITransformer.Target.targetClass("com.example.identitycheck.IdentityEvent"))),
                "exact targets: entry plus referenced same-file Event; no package wildcard");
        for (String name : List.of("Ordinary", "UnownedEntry", "UnrelatedEvent", "NativeControl")) {
            unchanged(transformer, forge, "com/example/identitycheck/" + name, out);
        }
        byte[] entry = Files.readAllBytes(forge.findResource("com/example/identitycheck/IdentityProbe.class"));
        byte[] rewritten = transform(transformer, entry);
        Files.write(out.resolve("IdentityProbe.before.class"), entry);
        Files.write(out.resolve("IdentityProbe.after.class"), rewritten);
        ClassNode node = new ClassNode();
        new ClassReader(rewritten).accept(node, 0);
        check(node.methods.stream().filter(m -> m.name.equals("<init>"))
                        .allMatch(m -> m.desc.equals("(Lnet/neoforged/bus/api/IEventBus;)V")),
                "identified entry constructor rewritten");
        check(node.methods.stream().anyMatch(m -> m.name.equals("onIdentity") && m.visibleAnnotations.stream()
                        .anyMatch(a -> a.desc.equals("Lnet/neoforged/bus/api/SubscribeEvent;"))),
                "renamed listener keeps original subscriber profile");
        // The SAME Forge entry bytes are also present in the native dual-metadata input.
        ModFile nativeFile = file(Path.of(args[1]), true);
        check(Arrays.equals(entry, Files.readAllBytes(nativeFile.findResource("com/example/identitycheck/IdentityProbe.class"))),
                "native control contains identical Forge-shaped entry bytes");
        check(ForgeCompatLanguageLoader.forgeEntrypoints(nativeFile.getModInfos().getFirst(), nativeFile.getScanResult()).isEmpty(),
                "native metadata remains on builtin language path even with matching Forge annotation");
        check(nativeFile.getScanResult().getAnnotatedBy(net.neoforged.fml.common.Mod.class, ElementType.TYPE)
                        .anyMatch(a -> a.clazz().equals(Type.getObjectType("com/example/identitycheck/NativeControl"))
                                && a.annotationData().get("value").equals("identityprobe")),
                "native @Mod entry remains in FML scan");
        LoadingModList.of(List.of(), List.of(nativeFile), List.of(), List.of(), Map.of());
        check(transformer.targets().isEmpty(), "native file yields no Forge transformation targets");
        unchanged(transformer, nativeFile, "com/example/identitycheck/IdentityProbe", out.resolve("native"));
        unchanged(transformer, nativeFile, "com/example/identitycheck/NativeControl", out.resolve("native"));
        System.out.println("PASS identity scope controls");
    }
}
