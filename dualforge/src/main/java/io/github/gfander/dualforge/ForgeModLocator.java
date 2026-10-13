package io.github.gfander.dualforge;

import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.language.MavenVersionAdapter;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator;
import net.neoforged.neoforgespi.locating.IncompatibleFileReporting;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;

/**
 * Controlled Forge javafml-52 candidates from the host's configured mods folder.
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

    static boolean isForgeCandidate(Path path) {
        // Embedded/virtual filesystem inputs remain with their existing native readers.
        if (path.getFileSystem() != FileSystems.getDefault() || !Files.isRegularFile(path)) return false;
        try (ZipFile zip = new ZipFile(path.toFile())) {
            if (zip.getEntry("META-INF/neoforge.mods.toml") != null) return false;
            var metadata = zip.getEntry(ForgeModReader.MODS_TOML);
            if (metadata == null) return false;
            var manifest = zip.getEntry("META-INF/MANIFEST.MF");
            if (manifest != null) {
                try (var stream = zip.getInputStream(manifest)) {
                    String type = new Manifest(stream).getMainAttributes().getValue("FMLModType");
                    if (type != null && !type.equals("MOD")) return false;
                }
            }
            byte[] bytes;
            try (var stream = zip.getInputStream(metadata)) {
                bytes = stream.readNBytes(1024 * 1024 + 1);
            }
            if (bytes.length > 1024 * 1024) {
                throw new IllegalArgumentException("Forge metadata exceeds controlled size limit: " + path);
            }
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            // Match the existing Parser's documented simple scalar subset, without extending it.
            if (text.contains("\"\"\"") || text.contains("'''")) return false;
            String header = text.split("(?m)^\\s*\\[", 2)[0];
            Map<String, Object> parsed = ForgeModFileInfoParser.parseForgeModsToml(text);
            if (!Pattern.compile("(?m)^\\s*modLoader\\s*=\\s*\"javafml\"\\s*$").matcher(header).find()
                    || !Pattern.compile("(?m)^\\s*loaderVersion\\s*=\\s*\"[^\"]+\"\\s*$").matcher(header).find()
                    || !"javafml".equals(parsed.get("modLoader"))
                    || !(parsed.get("loaderVersion") instanceof String version)
                    || !(parsed.get("mods") instanceof List<?> mods) || mods.isEmpty()) return false;
            var range = MavenVersionAdapter.createFromVersionSpec(version);
            if (!range.containsVersion(new DefaultArtifactVersion("52"))) return false;
            if (range.containsVersion(new DefaultArtifactVersion("4.0.45"))) {
                throw new IllegalArgumentException("Ambiguous Forge/NeoForge javafml range in " + path);
            }
            if (range.getRestrictions().size() != 1
                    || !new DefaultArtifactVersion("52").equals(range.getRestrictions().getFirst().getLowerBound())
                    || !range.getRestrictions().getFirst().isLowerBoundInclusive()) return false;
            return true;
        } catch (IOException e) {
            // FML's built-in pipeline reports broken containers; never feed them to this Reader.
            System.out.println("[DUALFORGE] ForgeModLocator: unreadable candidate " + path + ": " + e);
            return false;
        }
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
