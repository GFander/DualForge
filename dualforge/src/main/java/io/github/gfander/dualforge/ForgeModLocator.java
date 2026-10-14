package io.github.gfander.dualforge;

import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.util.jar.Manifest;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.language.MavenVersionAdapter;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator;
import net.neoforged.neoforgespi.locating.IncompatibleFileReporting;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;

/**
 * Forge candidates with validated metadata and same-JAR entry annotations from the host's configured mods folder.
 * Native metadata/explicit libraries stay with FML; the Reader validates accepted metadata.
 */
public class ForgeModLocator implements IModFileCandidateLocator {

    static {
        System.out.println("[DUALFORGE] <clinit> ForgeModLocator");
    }

    public ForgeModLocator() {
        System.out.println("[DUALFORGE] INSTANTIATED ForgeModLocator");
    }

    @Override
    public void findCandidates(ILaunchContext context, IDiscoveryPipeline pipeline) {
        System.out.println("[DUALFORGE] CALLED ForgeModLocator.findCandidates");
        Path folder = FMLPaths.MODSDIR.get();
        try (var files = Files.list(folder)) {
            // Extension is only a container filter; identity comes from metadata below.
            for (Path path : files.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString())).toList()) {
                path = path.toAbsolutePath().normalize();
                if (context.isLocated(path)) continue;
                if (!isForgeCandidate(path)) continue;
                System.out.println("[DUALFORGE] ForgeModLocator: adding candidate " + path);
                // addPath owns addLocated; do not pre-mark and thereby skip Reader handoff.
                pipeline.addPath(path, ModFileDiscoveryAttributes.DEFAULT.withLocator(this),
                        IncompatibleFileReporting.WARN_ALWAYS);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to inspect mods folder " + folder, e);
        }
    }

    enum Identity { FORGE_CONFIRMED, NEOFORGE_NATIVE, AMBIGUOUS, INVALID_METADATA, UNSUPPORTED }
    record Admission(Identity identity, String reason, boolean nativeMetadata) { }

    static boolean isForgeCandidate(Path path) {
        Admission result = inspect(path);
        System.out.println("[DUALFORGE-ADMISSION] " + result.identity() + " " + path + " reason=" + result.reason());
        // Keep native metadata on its official Reader, including legacy payloads.
        if (result.identity() == Identity.AMBIGUOUS && !result.nativeMetadata())
            throw new IllegalArgumentException("Ambiguous Forge/NeoForge identity in " + path + ": " + result.reason());
        return result.identity() == Identity.FORGE_CONFIRMED;
    }

    static Admission inspect(Path path) {
        if (path.getFileSystem() != FileSystems.getDefault() || !Files.isRegularFile(path))
            return new Admission(Identity.UNSUPPORTED, "nonphysical input", false);
        boolean nativeMetadata = false;
        try (ZipFile zip = new ZipFile(path.toFile())) {
            nativeMetadata = zip.getEntry("META-INF/neoforge.mods.toml") != null;
            var metadata = zip.getEntry(ForgeModReader.MODS_TOML);
            if (metadata == null) return new Admission(nativeMetadata ? Identity.NEOFORGE_NATIVE : Identity.UNSUPPORTED,
                    "no Forge metadata", nativeMetadata);
            var manifest = zip.getEntry("META-INF/MANIFEST.MF");
            if (manifest != null) {
                try (var stream = zip.getInputStream(manifest)) {
                    String type = new Manifest(stream).getMainAttributes().getValue("FMLModType");
                    if (type != null && !type.equals("MOD"))
                        return new Admission(Identity.UNSUPPORTED, "explicit FMLModType=" + type, nativeMetadata);
                }
            }
            if (nativeMetadata) {
                EntryAnnotations annotations = scanAnnotations(zip);
                return new Admission(annotations.forge().isEmpty() ? Identity.NEOFORGE_NATIVE : Identity.AMBIGUOUS,
                        annotations.forge().isEmpty() ? "native metadata authoritative; legacy metadata not claimed"
                                : "mixed native metadata and Forge @Mod; not claimed by Forge Reader", true);
            }
            Map<String, Object> parsed;
            try (var stream = zip.getInputStream(metadata)) {
                parsed = ForgeModFileInfoParser.parseForgeModsToml(ForgeModFileInfoParser.readMetadata(stream));
            }
            if (!"javafml".equals(parsed.get("modLoader")))
                return new Admission(Identity.UNSUPPORTED, "non-javafml language", false);
            Set<String> ids = new HashSet<>();
            for (Object mod : (List<?>) parsed.get("mods")) ids.add((String) ((Map<?, ?>) mod).get("modId"));
            EntryAnnotations annotations = scanAnnotations(zip);
            Set<String> forge = annotations.forge(), neo = annotations.neo();
            if (ids.stream().anyMatch(neo::contains))
                return new Admission(Identity.AMBIGUOUS, "native @Mod owns Forge metadata id", false);
            if (parsed.get("dependencies") instanceof Map<?, ?> owners) {
                for (Object group : owners.values()) for (Object value : (List<?>) group) {
                    Map<?, ?> dep = (Map<?, ?>) value;
                    if ("neoforge".equals(dep.get("modId")) && Boolean.TRUE.equals(dep.get("mandatory")))
                        return new Admission(Identity.AMBIGUOUS, "required NeoForge dependency conflicts with Forge entry metadata", false);
                }
            }
            if (!forge.containsAll(ids)) return new Admission(Identity.UNSUPPORTED,
                    "metadata modId lacks same-JAR Forge @Mod entry", false);
            // Compatibility policy is distinct from origin evidence. No lower-bound identity heuristic.
            var range = MavenVersionAdapter.createFromVersionSpec((String) parsed.get("loaderVersion"));
            if (!range.containsVersion(new DefaultArtifactVersion("52")))
                return new Admission(Identity.UNSUPPORTED, "loader range excludes supported Forge 52 ABI", false);
            return new Admission(Identity.FORGE_CONFIRMED, "javafml + same-JAR Forge @Mod owners=" + ids
                    + "; loader range supports 52 (host provider selection remains FML-owned)", false);
        } catch (IOException | IllegalArgumentException e) {
            return new Admission(Identity.INVALID_METADATA, e.getClass().getSimpleName() + ": " + e.getMessage(), nativeMetadata);
        }
    }

    private record EntryAnnotations(Set<String> forge, Set<String> neo) { }
    private static EntryAnnotations scanAnnotations(ZipFile zip) throws IOException {
        Set<String> forge = new HashSet<>(), neo = new HashSet<>();
        for (var entries = zip.entries(); entries.hasMoreElements();) {
            var entry = entries.nextElement();
            if (entry.isDirectory() || !entry.getName().endsWith(".class") || entry.getName().startsWith("META-INF/")) continue;
            try (var stream = zip.getInputStream(entry)) {
                byte[] bytes = stream.readNBytes(8 * 1024 * 1024 + 1);
                if (bytes.length > 8 * 1024 * 1024) throw new IllegalArgumentException("class exceeds scan limit");
                ClassReader reader = new ClassReader(bytes);
                if (!entry.getName().equals(reader.getClassName() + ".class"))
                    throw new IllegalArgumentException("class path and internal name disagree: " + entry.getName());
                reader.accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                        Set<String> destination = switch (descriptor) {
                            case "Lnet/minecraftforge/fml/common/Mod;" -> forge;
                            case "Lnet/neoforged/fml/common/Mod;" -> neo;
                            default -> null;
                        };
                        if (destination == null) return null;
                        return new AnnotationVisitor(Opcodes.ASM9) {
                            @Override public void visit(String name, Object value) {
                                if (name.equals("value") && value instanceof String id) destination.add(id);
                            }
                        };
                    }
                }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            }
        }
        return new EntryAnnotations(forge, neo);
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public String toString() {
        return "dualforge locator (controlled Forge metadata)";
    }
}
