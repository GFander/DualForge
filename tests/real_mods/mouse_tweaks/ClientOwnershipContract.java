package io.github.gfander.dualforge;

import cpw.mods.jarhandling.JarContents;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.*;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.readers.JarModsDotTomlModFileReader;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Actual Reader/scan/target planning with explicit offline FML version/dist setup.
 * Does not construct a GAME layer, a mod, Minecraft, a window or GUI. */
public final class ClientOwnershipContract {
    private static int assertions;
    public static void main(String[] args) throws Exception {
        var version = FMLLoader.class.getDeclaredField("versionInfo"); version.setAccessible(true);
        version.set(null, new VersionInfo("21.1.256", "4.0.45", "1.21.1", "20240808.144430"));
        var dist = FMLLoader.class.getDeclaredField("dist"); dist.setAccessible(true);
        boolean client = args[4].equals("client"); dist.set(null, client ? Dist.CLIENT : Dist.DEDICATED_SERVER);
        ModFile nativeHost = scan(Path.of(args[2]), true);
        for (String path : List.of(args[0], args[1])) {
            ModFile file = scan(Path.of(path), false); roster(nativeHost, file);
            String entry = DualForgeLanguageLoader.forgeEntrypoints(file.getModInfos().getFirst(), file.getScanResult()).getFirst().replace('.', '/');
            var transformer = new DualForgeEntryTransformer();
            if (!client) {
                refuse(() -> transformer.targets(), "CLIENT_DIST_REQUIRED");
                var plans = DualForgeEntryTransformer.class.getDeclaredField("clientEventPlans"); plans.setAccessible(true);
                check(((Map<?,?>) plans.get(transformer)).isEmpty(), "server publishes no client plan"); continue;
            }
            check(transformer.targets().size() == 1, "actual owned target selection " + entry);
            ClassNode source = new ClassNode(); new ClassReader(Files.readAllBytes(file.findResource(entry + ".class"))).accept(source, 0);
            ClassNode result = transformer.transform(source, null);
            check(result.methods.size() == 6 && source.methods.size() == 5, "owned plan transforms clone only");
            IModInfo real = file.getModInfos().getFirst();
            IModInfo proxy = (IModInfo) Proxy.newProxyInstance(IModInfo.class.getClassLoader(), new Class[]{IModInfo.class},
                (p, m, a) -> m.getName().equals("getOwningFile") ? real.getOwningFile() : m.invoke(real, a));
            refuse(() -> ForgeClientEventPlan.create(proxy, entry), "METADATA_OWNER");
            refuse(() -> ForgeClientEventPlan.create(real, "outside/Entry"), "ENTRY_OWNER");
        }
        if (client) {
            Path out = Path.of(args[3]); Files.createDirectories(out);
            Path foreign = out.resolve("foreign-native.jar");
            try (var jar = new JarFile(args[1]); var zip = new ZipOutputStream(Files.newOutputStream(foreign))) {
                zip.putNextEntry(new ZipEntry("META-INF/neoforge.mods.toml"));
                zip.write("modLoader=\"javafml\"\nloaderVersion=\"[4,)\"\nlicense=\"test-only\"\n[[mods]]\nmodId=\"foreignnative\"\nversion=\"1\"\n".getBytes(java.nio.charset.StandardCharsets.UTF_8)); zip.closeEntry();
                for (var e : jar.stream().filter(e -> e.getName().endsWith(".class")).toList()) {
                    zip.putNextEntry(new ZipEntry(e.getName())); zip.write(jar.getInputStream(e).readAllBytes()); zip.closeEntry();
                }
            }
            ModFile fixture = scan(Path.of(args[1]), false); ModFile duplicate = scan(foreign, true);
            roster(nativeHost, fixture, duplicate);
            refuse(() -> new DualForgeEntryTransformer().targets(), "CROSS_MODFILE_CLASS_CONFLICT");
            // Explicit module mutation uses only the new first-party fixture, not Mouse Tweaks.
            Path explicit = out.resolve("explicit-fixture.jar");
            try (var jar = new JarFile(args[1]); var zip = new ZipOutputStream(Files.newOutputStream(explicit))) {
                for (var e : jar.stream().filter(e -> !e.isDirectory()).toList()) {
                    zip.putNextEntry(new ZipEntry(e.getName())); zip.write(jar.getInputStream(e).readAllBytes()); zip.closeEntry();
                }
                ClassWriter module = new ClassWriter(0); module.visit(Opcodes.V21, Opcodes.ACC_MODULE, "module-info", null, null, null);
                var mv = module.visitModule("foreign.explicit", 0, null); mv.visitRequire("java.base", Opcodes.ACC_MANDATED, null); mv.visitEnd(); module.visitEnd();
                zip.putNextEntry(new ZipEntry("module-info.class")); zip.write(module.toByteArray()); zip.closeEntry();
            }
            refuse(() -> scan(explicit, false), "unsupported explicit Forge module identity");
        }
        System.out.println("CLIENT OWNERSHIP CONTRACT PASS mode=" + args[4] + " assertions=" + assertions + "; GAME_LAYER_AND_CONSTRUCTION=NOT_RUN");
    }
    private static ModFile scan(Path path, boolean nativeMetadata) throws Exception {
        ModFile file = (ModFile) (nativeMetadata ? new JarModsDotTomlModFileReader().read(JarContents.of(path), ModFileDiscoveryAttributes.DEFAULT)
            : new ForgeModReader().read(JarContents.of(path), ModFileDiscoveryAttributes.DEFAULT));
        check(file != null, "actual Reader accepts " + path.getFileName()); file.setScanResult(file.compileContent(), null); return file;
    }
    private static void roster(ModFile... files) { LoadingModList.of(List.of(), List.of(files), List.of(), List.of(), Map.of()); }
    private interface Action { void run() throws Exception; }
    private static void refuse(Action action, String fragment) throws Exception {
        try { action.run(); throw new AssertionError("Accepted " + fragment); }
        catch (IllegalStateException expected) { check(expected.getMessage().contains(fragment), "explicit refusal " + fragment); }
    }
    private static void check(boolean value, String why) { if (!value) throw new AssertionError(why); assertions++; }
}
