package io.github.gfander.dualforge;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforgespi.locating.InvalidModFileException;
import net.neoforged.neoforgespi.locating.ModFileInfoParser;

/** Standard TOML and Forge validation, followed by audited platform projection for FML. */
public class ForgeModFileInfoParser implements ModFileInfoParser {
    static final int MAX_METADATA_BYTES = 1024 * 1024;
    private static final Set<String> DEPENDENCY_KEYS = Set.of("modId", "mandatory", "versionRange",
            "ordering", "side", "referralUrl", "referralURL", "reason", "customNote");

    @Override
    public IModFileInfo build(IModFile file) throws InvalidModFileException {
        System.out.println("[DUALFORGE] CALLED ForgeModFileInfoParser.build for " + file.getFilePath());
        try (var stream = Files.newInputStream(file.findResource("META-INF", "mods.toml"))) {
            Map<String, Object> parsed = parseForgeModsToml(readMetadata(stream));
            System.out.println("[DUALFORGE] ForgeModFileInfoParser: parsed keys=" + parsed.keySet());
            Map<String, Object> projected = ForgePlatformDependencyResolver.forFile(file.getFilePath(), parsed);
            return new ModFileInfo((ModFile) file, ForgeModFileInfo.configOf(projected), info -> { });
        } catch (IOException | IllegalArgumentException e) {
            throw new InvalidModFileException("Invalid Forge metadata in " + file.getFilePath() + ": " + e.getMessage(), null);
        }
    }

    static String readMetadata(InputStream stream) throws IOException {
        byte[] bytes = stream.readNBytes(MAX_METADATA_BYTES + 1);
        if (bytes.length > MAX_METADATA_BYTES) throw invalid("metadata", "exceeds 1 MiB size limit");
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
    }

    static Map<String, Object> parseForgeModsToml(String text) {
        if (text.getBytes(StandardCharsets.UTF_8).length > MAX_METADATA_BYTES)
            throw invalid("metadata", "exceeds 1 MiB size limit");
        final Map<String, Object> root;
        try {
            root = table(copy(new TomlParser().parse(text)), "metadata");
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid Forge TOML syntax: " + e.getMessage(), e);
        }
        string(root, "modLoader", true);
        range(string(root, "loaderVersion", true), "loaderVersion");
        string(root, "license", true);
        for (String key : List.of("issueTrackerURL", "logoFile")) string(root, key, false);
        for (String key : List.of("showAsResourcePack", "showAsDataPack", "logoBlur")) typed(root, key, Boolean.class);
        if (root.containsKey("services")) {
            for (Object value : list(root.get("services"), "services"))
                if (!(value instanceof String)) throw invalid("services", "must contain strings");
        }
        for (String key : List.of("properties", "modproperties", "features"))
            if (root.containsKey(key)) table(root.get(key), key);
        Set<String> ids = new HashSet<>();
        List<?> mods = list(root.get("mods"), "mods");
        if (mods.isEmpty()) throw invalid("mods", "must not be empty");
        for (Object value : mods) {
            Map<String, Object> mod = table(value, "mods entry");
            String id = modId(string(mod, "modId", true));
            if (!ids.add(id)) throw invalid("modId", "duplicate " + id);
            for (String key : List.of("version", "namespace", "displayName", "description", "logoFile",
                    "updateJSONURL", "modUrl")) string(mod, key, false);
            typed(mod, "logoBlur", Boolean.class);
        }
        for (String key : List.of("modproperties", "features")) {
            if (!root.containsKey(key)) continue;
            for (var group : table(root.get(key), key).entrySet()) {
                Map<String, Object> values = table(group.getValue(), key + "." + group.getKey());
                if (key.equals("features")) {
                    for (Object value : values.values())
                        if (!(value instanceof String)) throw invalid(key, "bounds must be strings");
                }
            }
        }
        if (root.containsKey("dependencies")) {
            Map<String, Object> owners = table(root.get("dependencies"), "dependencies");
            for (var group : owners.entrySet()) {
                if (!ids.contains(group.getKey())) throw invalid("dependencies", "unknown owner " + group.getKey());
                for (Object value : list(group.getValue(), "dependencies." + group.getKey())) {
                    Map<String, Object> dep = table(value, "dependency");
                    if (dep.containsKey("type")) throw invalid("dependency.type", "conflicts with Forge mandatory");
                    for (String key : dep.keySet()) {
                        if (!DEPENDENCY_KEYS.contains(key)) throw invalid("dependency." + key, "unsupported constraint");
                    }
                    modId(string(dep, "modId", true));
                    if (!(dep.get("mandatory") instanceof Boolean mandatory))
                        throw invalid("dependency.mandatory", "must be a boolean");
                    for (String key : List.of("versionRange", "ordering", "side", "referralUrl", "referralURL", "reason"))
                        string(dep, key, false);
                    if (dep.containsKey("versionRange")) range((String) dep.get("versionRange"), "dependency.versionRange");
                    if (dep.containsKey("ordering")) enumValue(IModInfo.Ordering.class, dep.get("ordering"), "ordering");
                    if (dep.containsKey("side")) enumValue(IModInfo.DependencySide.class, dep.get("side"), "side");
                    // Keep mandatory and every original value. No id or range substitution.
                    dep.put("type", mandatory ? "required" : "optional");
                }
            }
        }
        return root;
    }

    private static Object copy(Object value) {
        if (value instanceof UnmodifiableConfig config) {
            Map<String, Object> result = new LinkedHashMap<>();
            config.valueMap().forEach((key, item) -> result.put(key, copy(item)));
            return result;
        }
        if (value instanceof List<?> list) {
            List<Object> result = new ArrayList<>();
            list.forEach(item -> result.add(copy(item)));
            return result;
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> table(Object value, String name) {
        if (!(value instanceof Map<?, ?>)) throw invalid(name, "must be a table");
        return (Map<String, Object>) value;
    }
    private static List<?> list(Object value, String name) {
        if (!(value instanceof List<?> values)) throw invalid(name, "must be an array of tables");
        return values;
    }
    private static String string(Map<String, Object> map, String key, boolean required) {
        Object value = map.get(key);
        if (!required && value == null) return null;
        if (!(value instanceof String text) || (required && text.isBlank())) throw invalid(key, "must be " + (required ? "a nonblank string" : "a string"));
        return text;
    }
    private static void typed(Map<String, Object> map, String key, Class<?> type) {
        if (map.containsKey(key) && !type.isInstance(map.get(key))) throw invalid(key, "must be " + type.getSimpleName());
    }
    private static String modId(String id) {
        if (!id.matches("[a-z][a-z0-9_]{1,63}")) throw invalid("modId", "invalid " + id);
        return id;
    }
    private static void range(String range, String name) {
        try { VersionRange.createFromVersionSpec(range); }
        catch (InvalidVersionSpecificationException e) { throw invalid(name, "invalid version range " + range); }
    }
    private static <E extends Enum<E>> void enumValue(Class<E> type, Object value, String name) {
        try { Enum.valueOf(type, (String) value); }
        catch (IllegalArgumentException e) { throw invalid(name, "unsupported " + value); }
    }
    private static IllegalArgumentException invalid(String field, String reason) {
        return new IllegalArgumentException("Forge metadata " + field + ": " + reason);
    }
}
