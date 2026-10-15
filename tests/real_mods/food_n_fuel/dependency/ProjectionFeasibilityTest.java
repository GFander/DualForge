package io.github.gfander.dualforge;

import cpw.mods.jarhandling.JarContents;
import cpw.mods.jarhandling.SecureJar;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.ModSorter;
import net.neoforged.fml.loading.VersionInfo;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.neoforgespi.locating.IModFile;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;

/** DIAGNOSTIC_ONLY: a pre-implementation experiment with the official ModSorter and memory metadata. */
public final class ProjectionFeasibilityTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("DIAGNOSTIC_ONLY PASS " + message);
    }
    private static ModFile file(Path jar, Map<String,Object> config) {
        var contents = JarContents.of(jar);
        var metadata = new ForgeJarMetadata(contents);
        ModFile result = (ModFile) IModFile.create(SecureJar.from(contents, metadata),
                f -> new ModFileInfo((ModFile) f, ForgeModFileInfo.configOf(config), info -> { }));
        metadata.bind(result);
        return result;
    }
    private static Map<String,Object> system(String id, String version) {
        return new LinkedHashMap<>(Map.of("modLoader", "javafml", "loaderVersion", "[0,)", "license", "diagnostic",
                "mods", List.of(Map.of("modId", id, "version", version))));
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        var version = FMLLoader.class.getDeclaredField("versionInfo"); version.setAccessible(true);
        version.set(null, new VersionInfo("21.1.256", "4.0.45", "1.21.1", "20240808.144430"));
        Path fnf = Path.of(args[0]), probe = Path.of(args[1]);
        Map<String,Object> raw;
        try (var zip = new ZipFile(fnf.toFile())) {
            raw = ForgeModFileInfoParser.parseForgeModsToml(new String(zip.getInputStream(zip.getEntry("META-INF/mods.toml")).readAllBytes(), StandardCharsets.UTF_8));
        }
        check(ForgeModLocator.isForgeCandidate(fnf), "actual same-JAR Forge identity confirmed");
        var original = file(fnf, raw);
        var minecraft = file(probe, system("minecraft", "1.21.1"));
        var neoforge = file(probe, system("neoforge", "21.1.256"));
        var rejected = ModSorter.sort(List.of(), List.of(minecraft, neoforge, original), new ArrayList<>());
        check(rejected.hasErrors(), "official sorter rejects original missing Forge");
        var owners = (Map<String,Object>) raw.get("dependencies");
        var entries = (List<Map<String,Object>>) owners.get("fnf");
        var platform = entries.stream().filter(d -> "forge".equals(d.get("modId"))).findFirst().orElseThrow();
        check(VersionRange.createFromVersionSpec((String)platform.get("versionRange")).containsVersion(new DefaultArtifactVersion("52.1.0")), "declared range accepts reference ABI 52.1.0, not provider 9999");
        check("NONE".equals(platform.get("ordering")) && "BOTH".equals(platform.get("side")), "platform ordering and side explicit");
        var projected = new LinkedHashMap<>(raw); var projectedOwners = new LinkedHashMap<>(owners);
        projectedOwners.put("fnf", entries.stream().filter(d -> !"forge".equals(d.get("modId"))).toList());
        projected.put("dependencies", projectedOwners);
        var candidate = file(fnf, projected);
        var accepted = ModSorter.sort(List.of(), List.of(minecraft, neoforge, candidate), new ArrayList<>());
        check(!accepted.hasErrors() && accepted.getModFileById("fnf") != null, "official sorter accepts independently validated in-memory projection");
        check(accepted.getModFileById("forge") == null, "no fake Forge mod/platform container or version");
        check(entries.size() == 2 && original.getModInfos().getFirst().getDependencies().size() == 2,
                "original declaration/model unchanged");
        check(candidate.getModInfos().getFirst().getDependencies().size() == 1
                && candidate.getModInfos().getFirst().getDependencies().getFirst().getModId().equals("minecraft"), "ordinary required Minecraft retained");
        System.out.println("DIAGNOSTIC_ONLY PROJECTION FEASIBILITY PASS");
    }
}
