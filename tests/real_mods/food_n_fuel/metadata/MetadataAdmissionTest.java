package io.github.gfander.dualforge;

import cpw.mods.jarhandling.JarContents;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

/** Unmodified release inputs, actual FML config/scan, generated invalid controls; no class initialization. */
public final class MetadataAdmissionTest {
    private static Path output;
    private static int assertions;
    private static final String BASE = "modLoader=\"javafml\" # inline\nloaderVersion=\"[0,)\"\nlicense=\"MIT\"\n"
            + "[[mods]]\nmodId=\"metaprobe\"\nversion=\"1.0\"\ndescription='''A valid description'''\n";
    private static final String DEP = "\n[[dependencies.metaprobe]]\nmodId=\"forge\"\nmandatory=true\n"
            + "versionRange=\"[0,)\"\nordering=\"NONE\"\nside=\"BOTH\"\n";
    private static void check(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
        assertions++;
        System.out.println("PASS " + message);
    }
    private static Map<String,Object> parse(String text) { return ForgeModFileInfoParser.parseForgeModsToml(text); }
    private static void invalid(String name, String text) {
        try { parse(text); throw new AssertionError("Accepted " + name); }
        catch (IllegalArgumentException expected) { check(!expected.getMessage().isBlank(), "reject " + name + ": " + expected.getMessage()); }
    }
    private static byte[] entry(String annotation, String id) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "sample/Entry", null, "java/lang/Object", null);
        if (annotation != null) {
            var ann = writer.visitAnnotation(annotation, true); ann.visit("value", id); ann.visitEnd();
        }
        if (annotation == null) writer.visitField(Opcodes.ACC_PRIVATE, "referenceOnly",
                "Lnet/minecraftforge/registries/RegistryObject;", null, null).visitEnd();
        // Identifying a class must never execute third-party initialization.
        var init = writer.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        init.visitCode(); init.visitTypeInsn(Opcodes.NEW, "java/lang/AssertionError"); init.visitInsn(Opcodes.DUP);
        init.visitLdcInsn("Third-party initialization executed");
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/AssertionError", "<init>", "(Ljava/lang/Object;)V", false);
        init.visitInsn(Opcodes.ATHROW); init.visitMaxs(3, 0); init.visitEnd();
        writer.visitEnd(); return writer.toByteArray();
    }
    private static Path jar(String name, byte[] toml, String type, byte[] entry, boolean nativeMetadata) throws Exception {
        Path path = output.resolve(name + ".jar");
        try (var zip = new ZipOutputStream(Files.newOutputStream(path))) {
            write(zip, "META-INF/MANIFEST.MF", ("Manifest-Version: 1.0\n" + (type == null ? "" : "FMLModType: " + type + "\n") + "\n").getBytes(StandardCharsets.UTF_8));
            write(zip, "META-INF/mods.toml", toml);
            if (nativeMetadata) write(zip, "META-INF/neoforge.mods.toml", BASE.replace("[0,)", "[4,)").getBytes(StandardCharsets.UTF_8));
            if (entry != null) write(zip, "sample/Entry.class", entry);
        }
        return path;
    }
    private static void write(ZipOutputStream zip, String name, byte[] bytes) throws Exception {
        zip.putNextEntry(new ZipEntry(name)); zip.write(bytes); zip.closeEntry();
    }
    private static void classified(String name, String text, String type, byte[] entry, boolean neo,
                                   ForgeModLocator.Identity expected) throws Exception {
        Path path = jar(name, text.getBytes(StandardCharsets.UTF_8), type, entry, neo);
        var result = ForgeModLocator.inspect(path);
        check(result.identity() == expected, "classification " + name + "=" + result);
        if (expected != ForgeModLocator.Identity.FORGE_CONFIRMED) {
            if (expected == ForgeModLocator.Identity.AMBIGUOUS && !neo) {
                try { new ForgeModReader().read(JarContents.of(path), ModFileDiscoveryAttributes.DEFAULT);
                    throw new AssertionError("Reader accepted ambiguity"); }
                catch (IllegalArgumentException e) { check(e.getMessage().contains("Ambiguous"), "Reader rejects conflicting ownership " + name); }
            } else check(new ForgeModReader().read(JarContents.of(path), ModFileDiscoveryAttributes.DEFAULT) == null,
                         "Reader does not claim " + name);
            if (entry != null) {
                LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
                var transformer = new DualForgeEntryTransformer();
                check(transformer.targets().isEmpty(), "no targets for unclaimed " + name);
                ClassNode node = new ClassNode(); new ClassReader(entry).accept(node, 0);
                byte[] before = bytes(node), after = bytes(transformer.transform(node, null));
                check(Arrays.equals(before, after), "unclaimed bytes unchanged " + name);
                Files.write(output.resolve(name + ".before.class"), before); Files.write(output.resolve(name + ".after.class"), after);
            }
        }
    }
    private static byte[] bytes(ClassNode node) { ClassWriter w = new ClassWriter(0); node.accept(w); return w.toByteArray(); }
    public static void main(String[] args) throws Exception {
        output = Path.of(args[7]); Files.createDirectories(output);
        // FnF, 007B, 006A, 006B, Clumps: use complete original TOML and bytes.
        for (int i = 0; i < 5; i++) {
            Path path = Path.of(args[i]);
            try (ZipFile zip = new ZipFile(path.toFile()); var stream = zip.getInputStream(zip.getEntry("META-INF/mods.toml"))) {
                Map<String,Object> root = parse(ForgeModFileInfoParser.readMetadata(stream));
                check("javafml".equals(root.get("modLoader")), "original TOML " + path.getFileName());
            }
            if (i < 4) {
                check(ForgeModLocator.inspect(path).identity() == ForgeModLocator.Identity.FORGE_CONFIRMED, "original Forge confirmed " + path.getFileName());
                ModFile file = (ModFile) new ForgeModReader().read(JarContents.of(path), ModFileDiscoveryAttributes.DEFAULT);
                check(file != null && file.getModInfos().size() == 1, "real FML ModFile " + path.getFileName());
                file.setScanResult(file.compileContent(), null);
                check(DualForgeLanguageLoader.forgeEntrypoints(file.getModInfos().getFirst(), file.getScanResult()).size() == 1,
                        "actual FML @Mod owner " + path.getFileName());
                if (i == 0) {
                    var info = file.getModInfos().getFirst();
                    check(info.getModId().equals("fnf") && file.getSecureJar().name().equals("fnf"), "FnF module and mod identity");
                    check(info.getDescription().equals("Adding new varieties of food and fuel obtainable from vanilla resources."), "FnF description matches native Forge");
                    var dep = info.getDependencies().stream().filter(d -> d.getModId().equals("forge")).findFirst().orElseThrow();
                    check(dep.getType().name().equals("REQUIRED") && dep.getVersionRange().toString().equals("[0,)")
                            && dep.getOrdering().name().equals("NONE") && dep.getSide().name().equals("BOTH"), "real FML Forge dependency preserved");
                    check(info.getDependencies().size() == 2, "FnF dependency count unchanged");
                    check(file.getModFileInfo().requiredLanguageLoaders().getFirst().acceptedVersions().toString().equals("[0,)"), "FnF language range unchanged");
                }
            }
        }
        check(ForgeModLocator.inspect(Path.of(args[5])).identity() == ForgeModLocator.Identity.NEOFORGE_NATIVE, "official native NeoForge isolation");
        check(ForgeModLocator.inspect(Path.of(args[6])).identity() == ForgeModLocator.Identity.FORGE_CONFIRMED, "frozen original Probe confirmed");
        Map<String,Object> multiline = parse(BASE.replace("'''A valid description'''", "'''\nA multiline description\nwith # and = and [text]\n'''") + "\n[properties]\nanswer=42\n[properties.nested]\nflag=true\n");
        check(((Map<?,?>)((List<?>)multiline.get("mods")).getFirst()).get("description").equals("A multiline description\nwith # and = and [text]\n"), "true multiline literal content");
        check(((Map<?,?>)multiline.get("properties")).get("nested") instanceof Map, "nested tables retained");
        parse(BASE + DEP); parse(BASE + DEP.replace("mandatory=true", "mandatory=false"));
        check(true, "required and optional retain declaration");
        invalid("bad TOML", BASE + "bad=\"unterminated\n");
        invalid("unclosed triple", BASE.replace("'''A valid description'''", "'''unclosed"));
        invalid("duplicate field", BASE.replace("license=\"MIT\"", "license=\"MIT\"\nlicense=\"MIT\""));
        invalid("missing loader", BASE.replace("modLoader=\"javafml\" # inline\n", ""));
        invalid("missing range", BASE.replace("loaderVersion=\"[0,)\"\n", ""));
        invalid("missing license", BASE.replace("license=\"MIT\"\n", ""));
        invalid("wrong loader type", BASE.replace("modLoader=\"javafml\"", "modLoader=true"));
        invalid("wrong mods type", BASE.replace("[[mods]]", "[mods]"));
        invalid("wrong id type", BASE.replace("modId=\"metaprobe\"", "modId=1"));
        invalid("wrong description type", BASE.replace("'''A valid description'''", "false"));
        invalid("bad version range", BASE.replace("[0,)", "[0,broken"));
        invalid("wrong root boolean", BASE.replace("[[mods]]", "showAsDataPack=\"true\"\n[[mods]]"));
        invalid("wrong feature table", BASE + "\n[features]\nmetaprobe=true\n");
        invalid("wrong feature bound", BASE + "\n[features.metaprobe]\njavaVersion=true\n");
        invalid("wrong properties table", BASE + "\n[modproperties]\nmetaprobe=\"bad\"\n");
        invalid("invalid dependency owner", BASE + DEP.replace("dependencies.metaprobe", "dependencies.foreign"));
        invalid("invalid mandatory type", BASE + DEP.replace("mandatory=true", "mandatory=\"true\""));
        invalid("missing mandatory", BASE + DEP.replace("mandatory=true\n", ""));
        invalid("conflicting type", BASE + DEP + "type=\"required\"\n");
        invalid("unknown dependency constraint", BASE + DEP + "requiredPlatform=\"other\"\n");
        invalid("wrong ordering", BASE + DEP.replace("NONE", "SOMETIMES"));
        invalid("wrong side", BASE + DEP.replace("BOTH", "OTHER"));
        invalid("oversized text", BASE + "#".repeat(ForgeModFileInfoParser.MAX_METADATA_BYTES));
        byte[] forge = entry("Lnet/minecraftforge/fml/common/Mod;", "metaprobe");
        classified("broad-confirmed", BASE, null, forge, false, ForgeModLocator.Identity.FORGE_CONFIRMED);
        classified("broad-unidentified", BASE, null, null, false, ForgeModLocator.Identity.UNSUPPORTED);
        classified("package-reference-only", BASE, null, entry(null, null), false, ForgeModLocator.Identity.UNSUPPORTED);
        classified("annotation-owner-mismatch", BASE, null, entry("Lnet/minecraftforge/fml/common/Mod;", "othermod"), false, ForgeModLocator.Identity.UNSUPPORTED);
        classified("explicit-library", BASE, "LIBRARY", forge, false, ForgeModLocator.Identity.UNSUPPORTED);
        classified("native-metadata", BASE, null, entry("Lnet/neoforged/fml/common/Mod;", "metaprobe"), true, ForgeModLocator.Identity.NEOFORGE_NATIVE);
        classified("mixed-metadata", BASE, null, forge, true, ForgeModLocator.Identity.AMBIGUOUS);
        classified("neo-annotation-conflict", BASE, null, entry("Lnet/neoforged/fml/common/Mod;", "metaprobe"), false, ForgeModLocator.Identity.AMBIGUOUS);
        classified("neo-dependency-conflict", BASE + DEP.replace("modId=\"forge\"", "modId=\"neoforge\""), null, forge, false, ForgeModLocator.Identity.AMBIGUOUS);
        classified("unsupported-loader-version", BASE.replace("[0,)", "[53,)"), null, forge, false, ForgeModLocator.Identity.UNSUPPORTED);
        classified("invalid-syntax", BASE + "wrong=\"unclosed\n", null, forge, false, ForgeModLocator.Identity.INVALID_METADATA);
        Path utf = jar("invalid-utf8", new byte[]{(byte)0xc3, 0x28}, null, forge, false);
        check(ForgeModLocator.inspect(utf).identity() == ForgeModLocator.Identity.INVALID_METADATA, "invalid UTF-8 classified");
        Path oversized = jar("oversized-bytes", new byte[ForgeModFileInfoParser.MAX_METADATA_BYTES + 1], null, forge, false);
        check(ForgeModLocator.inspect(oversized).identity() == ForgeModLocator.Identity.INVALID_METADATA, "oversized bytes classified");
        Files.writeString(output.resolve("assertions.txt"), "METADATA ADMISSION PASS assertions=" + assertions + "\n");
        System.out.println("METADATA ADMISSION PASS assertions=" + assertions);
    }
}
