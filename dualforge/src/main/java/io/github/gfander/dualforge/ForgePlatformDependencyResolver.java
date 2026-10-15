package io.github.gfander.dualforge;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.neoforged.fml.loading.FMLLoader;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;

/** Verified source-platform version projection, not a Forge runtime or API capability provider. */
final class ForgePlatformDependencyResolver {
    static final String REFERENCE_ABI = "52.1.0";
    static final String ORIGINAL = "dualforgeOriginalDependencies";
    static final String AUDIT = "dualforgePlatformDependencyAudit";

    record Host(String minecraft, String neoforge, String fml, String dist, int javaVersion) {
        static Host current() {
            var version = FMLLoader.versionInfo();
            var dist = FMLLoader.getDist();
            if (version == null || dist == null) throw rejected("discovery host context unavailable");
            return new Host(version.mcVersion(), version.neoForgeVersion(), version.fmlVersion(),
                    dist.name(), Runtime.version().feature());
        }
        void validate() {
            if (!"1.21.1".equals(minecraft) || !"21.1.256".equals(neoforge) || !"4.0.45".equals(fml)
                    || javaVersion != 21 || dist == null || !Set.of("CLIENT", "DEDICATED_SERVER").contains(dist))
                throw rejected("unsupported host " + this);
        }
    }

    private ForgePlatformDependencyResolver() { }

    static Map<String, Object> forFile(Path path, Map<String, Object> parsed) {
        if (!hasPlatformDependency(parsed)) return parsed;
        var source = ForgeModLocator.inspect(path);
        if (source.identity() == ForgeModLocator.Identity.NEOFORGE_NATIVE) return parsed;
        if (source.identity() != ForgeModLocator.Identity.FORGE_CONFIRMED)
            throw rejected("unconfirmed Forge source: " + source.identity() + " " + source.reason());
        Map<String, Object> projected = resolve(parsed, Host.current());
        // Only emit decisions after all entries have passed; failures never return a partial map.
        for (Object decision : (List<?>) projected.get(AUDIT))
            System.out.println("[DUALFORGE-PLATFORM] file=" + path + " decision=" + decision);
        return projected;
    }

    static Map<String, Object> resolve(Map<String, Object> parsed, Host host) {
        if (!hasPlatformDependency(parsed)) return parsed;
        host.validate();
        if (parsed.containsKey(ORIGINAL) || parsed.containsKey(AUDIT))
            throw rejected("reserved audit key collision");
        Map<String, Object> result = table(copy(parsed));
        Map<String, Object> owners = table(result.get("dependencies"));
        Object original = copy(owners);
        List<Map<String, Object>> decisions = new ArrayList<>();
        for (var group : owners.entrySet()) {
            List<Object> retained = new ArrayList<>();
            for (Object value : list(group.getValue())) {
                Map<String, Object> dep = table(value);
                if (!"forge".equals(dep.get("modId"))) { retained.add(dep); continue; }
                if (!(dep.get("mandatory") instanceof Boolean mandatory)) throw rejected("mandatory must be boolean");
                String type = mandatory ? "required" : "optional";
                if (!type.equals(dep.get("type"))) throw rejected("unsupported or conflicting platform dependency type");
                String side = string(dep, "side", "BOTH");
                if (!Set.of("BOTH", "CLIENT", "SERVER").contains(side)) throw rejected("unsupported side " + side);
                String ordering = string(dep, "ordering", "NONE");
                if (!Set.of("NONE", "BEFORE", "AFTER").contains(ordering)) throw rejected("unsupported ordering " + ordering);
                String range = string(dep, "versionRange", " ");
                final VersionRange versions;
                try { versions = VersionRange.createFromVersionSpec(range); }
                catch (InvalidVersionSpecificationException e) { throw rejected("invalid platform range " + range); }
                boolean active = side.equals("BOTH") || side.equals("CLIENT") && host.dist().equals("CLIENT")
                        || side.equals("SERVER") && host.dist().equals("DEDICATED_SERVER");
                Map<String, Object> declaration = table(copy(dep)); declaration.remove("type");
                Map<String, Object> decision = new LinkedHashMap<>();
                decision.put("owner", group.getKey()); decision.put("forgeDeclaration", declaration);
                decision.put("fmlBeforeProjection", copy(dep)); decision.put("referenceSourceABI", REFERENCE_ABI);
                decision.put("hostMinecraft", host.minecraft()); decision.put("hostNeoForge", host.neoforge());
                decision.put("hostFML", host.fml()); decision.put("hostDist", host.dist());
                decision.put("forgeRuntimeApiCompatibility", "NOT RUN");
                if (!active) {
                    retained.add(dep);
                    decision.put("action", "DELEGATE_INACTIVE_SIDE");
                    decision.put("sourcePlatformDependency", "NOT APPLICABLE");
                } else {
                    if (!ordering.equals("NONE")) throw rejected("owner=" + group.getKey()
                            + " forge ordering=" + ordering + " requires an unsupported real platform ordering node");
                    if (!versions.containsVersion(new DefaultArtifactVersion(REFERENCE_ABI)))
                        throw rejected("owner=" + group.getKey() + " forge range=" + range
                                + " excludes reference source ABI " + REFERENCE_ABI);
                    decision.put("action", "PROJECT_VERIFIED_PLATFORM");
                    decision.put("sourcePlatformDependency", "VERIFIED");
                }
                decisions.add(decision);
            }
            group.setValue(retained);
        }
        result.put(ORIGINAL, original);
        result.put(AUDIT, decisions);
        return result;
    }

    private static boolean hasPlatformDependency(Map<String, Object> parsed) {
        if (!parsed.containsKey("dependencies")) return false;
        for (Object group : table(parsed.get("dependencies")).values())
            for (Object dep : list(group)) if ("forge".equals(table(dep).get("modId"))) return true;
        return false;
    }
    private static Object copy(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, item) -> result.put((String) key, copy(item)));
            return result;
        }
        if (value instanceof List<?> values) return values.stream().map(ForgePlatformDependencyResolver::copy).toList();
        return value;
    }
    @SuppressWarnings("unchecked")
    private static Map<String, Object> table(Object value) {
        if (!(value instanceof Map<?, ?>)) throw rejected("dependency table required");
        return (Map<String, Object>) value;
    }
    private static List<?> list(Object value) {
        if (!(value instanceof List<?> list)) throw rejected("dependency array required");
        return list;
    }
    private static String string(Map<String, Object> values, String key, String fallback) {
        Object value = values.getOrDefault(key, fallback);
        if (!(value instanceof String text)) throw rejected(key + " must be string");
        return text;
    }
    private static IllegalArgumentException rejected(String reason) {
        return new IllegalArgumentException("Forge platform dependency rejected: " + reason);
    }
}
