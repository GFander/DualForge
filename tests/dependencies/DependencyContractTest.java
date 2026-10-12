package com.example.forgecompat;

import cpw.mods.jarhandling.JarContents;
import cpw.mods.jarhandling.SecureJar;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.neoforgespi.language.IConfigurable;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.language.MavenVersionAdapter;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

/** Plain-Java boundary test against the installed FML, not a replacement dependency validator. */
public final class DependencyContractTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static ModFileInfo fml(Map<String, Object> root, Path probe) throws Exception {
        // The parser callback deliberately uses the same configOf -> real ModFileInfo boundary.
        try (JarContents contents = JarContents.of(probe)) {
            SecureJar jar = SecureJar.from(contents);
            ModFile file = new ModFile(jar,
                    f -> new ModFileInfo((ModFile) f, ForgeModFileInfo.configOf(root), info -> { }),
                    ModFileDiscoveryAttributes.DEFAULT);
            return (ModFileInfo) file.getModFileInfo();
        }
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        boolean before = args[0].equals("before");
        Path directory = Path.of(args[1]);
        Path probe = Path.of(args[2]);
        int cases = 0;
        List<Path> paths;
        try (var stream = Files.list(directory)) {
            paths = stream.filter(p -> p.toString().endsWith(".toml")).sorted().toList();
        }
        for (Path path : paths) {
            String name = path.getFileName().toString();
            Map<String, Object> parsed = ForgeModFileInfoParser.parseForgeModsToml(Files.readString(path));
            IConfigurable config = ForgeModFileInfo.configOf(parsed);
            ModFileInfo info = fml(parsed, probe);
            System.out.println("CASE " + name + " parsed=" + parsed);
            for (IModInfo mod : info.getMods()) {
                int actual = config.getConfigList("dependencies", mod.getModId()).size();
                System.out.println("READ owner=" + mod.getModId() + " configCount=" + actual
                        + " fmlCount=" + mod.getDependencies().size());
                if (before) {
                    check(actual == 0 && mod.getDependencies().isEmpty(), "Expected original dependency loss");
                    continue;
                }
                int expected = name.startsWith("B-") && mod.getModId().equals("forgeprobe") ? 2 : 1;
                check(actual == expected && mod.getDependencies().size() == expected, name + " count");
                check(config.getConfigList("dependencies", "unknown_owner").isEmpty(), "Unknown owner leaked");
                List<Map<String, Object>> raw = ((Map<String, List<Map<String, Object>>>) parsed.get("dependencies"))
                        .get(mod.getModId());
                for (int i = 0; i < expected; i++) {
                    IModInfo.ModVersion dep = mod.getDependencies().get(i);
                    Map<String, Object> entry = raw.get(i);
                    IConfigurable adapted = config.getConfigList("dependencies", mod.getModId()).get(i);
                    check(dep.getOwner() == mod, "FML dependency owner identity");
                    check(dep.getModId().equals(entry.get("modId")), "Owner table mixed");
                    check(entry.get("mandatory") instanceof Boolean, "Forge mandatory must remain boolean");
                    check(dep.getType() == ((Boolean) entry.get("mandatory")
                            ? IModInfo.DependencyType.REQUIRED : IModInfo.DependencyType.OPTIONAL), "Dependency type");
                    for (var field : entry.entrySet()) {
                        check(adapted.getConfigElement(field.getKey()).orElseThrow().equals(field.getValue()),
                                "Field lost: " + field.getKey());
                    }
                    check(dep.getVersionRange().toString().equals(MavenVersionAdapter
                            .createFromVersionSpec((String) entry.get("versionRange")).toString()), "Version range semantics lost");
                    check(dep.getOrdering().name().equals(entry.getOrDefault("ordering", "NONE")), "Ordering");
                    check(dep.getSide().name().equals(entry.getOrDefault("side", "BOTH")), "Side");
                    System.out.println("FML_DEP owner=" + dep.getOwner().getModId() + " target=" + dep.getModId()
                            + " type=" + dep.getType() + " range=" + dep.getVersionRange()
                            + " ordering=" + dep.getOrdering() + " side=" + dep.getSide());
                }
            }
            if (!before) {
                var first = info.getMods().getFirst().getDependencies().getFirst();
                if (name.startsWith("C-")) check(first.getVersionRange().containsVersion(new DefaultArtifactVersion("1.21.1")), "Satisfied range");
                if (name.startsWith("F-")) check(!first.getVersionRange().containsVersion(new DefaultArtifactVersion("1.21.1")), "Rejected range");
                if (name.startsWith("B-")) {
                    check(info.getMods().getFirst().getDependencies().getFirst().getModId().equals("minecraft"), "Owner 1");
                    check(info.getMods().get(1).getDependencies().getFirst().getModId().equals("neoforge"), "Owner 2");
                }
                if (name.startsWith("A-")) {
                    check(first.getReferralURL().orElseThrow().toString().equals("https://example.com/dependency"), "Referral URL");
                    check(config.getConfigList("dependencies", "forgeprobe").getFirst()
                            .getConfigElement("customNote").orElseThrow().equals("preserved"), "Extra field");
                }
            } else if (name.startsWith("E-")) {
                // Isolate the second defect without changing production: repair ONLY the shape in test memory.
                Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
                for (Map<String, Object> entry : (List<Map<String, Object>>) parsed.get("dependencies")) {
                    grouped.computeIfAbsent((String) entry.get("owner"), id -> new ArrayList<>()).add(entry);
                }
                Map<String, Object> shapeOnly = new LinkedHashMap<>(parsed);
                shapeOnly.put("dependencies", grouped);
                var dep = fml(shapeOnly, probe).getMods().getFirst().getDependencies().getFirst();
                System.out.println("SHAPE_ONLY optionalMandatory=" + grouped.get("forgeprobe").getFirst().get("mandatory")
                        + " actualFmlType=" + dep.getType());
                check(dep.getType() == IModInfo.DependencyType.REQUIRED, "Expected FML to ignore legacy mandatory");
            }
            cases++;
        }
        check(cases == 6, "Six input cases required");
        if (!before) {
            String valid = Files.readString(directory.resolve("A-single.toml"));
            for (String invalid : List.of(valid.replace("mandatory=true", "mandatory=\"true\""),
                    valid.replace("mandatory=true", "mandatory=invalid"),
                    valid.replace("mandatory=true", ""),
                    valid.replace("mandatory=true", "mandatory=true\ntype=\"optional\""))) {
                try {
                    ForgeModFileInfoParser.parseForgeModsToml(invalid);
                    throw new AssertionError("Invalid mandatory/mixed type accepted");
                } catch (IllegalArgumentException expected) {
                    System.out.println("REJECT_INVALID " + expected.getMessage());
                }
            }
            Map<String, Object> defaults = ForgeModFileInfoParser.parseForgeModsToml(valid
                    .replace("versionRange=\"[1.21.1,1.22)\"", "")
                    .replace("ordering=\"NONE\"", "").replace("side=\"BOTH\"", ""));
            var dep = fml(defaults, probe).getMods().getFirst().getDependencies().getFirst();
            check(dep.getVersionRange().containsVersion(new DefaultArtifactVersion("99.0")), "Unbounded default");
            check(dep.getOrdering() == IModInfo.Ordering.NONE && dep.getSide() == IModInfo.DependencySide.BOTH, "FML defaults");
            System.out.println("DEFAULTS range=" + dep.getVersionRange() + " ordering=" + dep.getOrdering() + " side=" + dep.getSide());
        }
        System.out.println(before ? "REPRODUCED cases=6 dependency-loss + shape-only-optional-required"
                : "PASS cases=6 owner-isolation + preserved-fields + FML-type/version/defaults");
    }
}
