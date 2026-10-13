package io.github.gfander.dualforge.test;

import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Read-only test observer; no transform callback or private reflective access. */
public final class ModuleLayersAgent {
    public static void premain(String output, Instrumentation instrumentation) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Class<?>[] loaded = instrumentation.getAllLoadedClasses();
                Class<?> launcher = Arrays.stream(loaded)
                        .filter(c -> c.getName().equals("cpw.mods.modlauncher.Launcher"))
                        .findFirst().orElseThrow();
                Object instance = launcher.getField("INSTANCE").get(null);
                Object optional = launcher.getMethod("findLayerManager").invoke(instance);
                Object manager = ((java.util.Optional<?>) optional).orElseThrow();
                Class<?> api = Arrays.stream(loaded)
                        .filter(c -> c.getName().equals("cpw.mods.modlauncher.api.IModuleLayerManager"))
                        .findFirst().orElseThrow();
                Class<?> kind = Arrays.stream(loaded)
                        .filter(c -> c.getName().equals("cpw.mods.modlauncher.api.IModuleLayerManager$Layer"))
                        .findFirst().orElseThrow();
                List<String> layers = new ArrayList<>();
                for (Object label : kind.getEnumConstants()) {
                    var layer = (java.util.Optional<?>) api.getMethod("getLayer", kind).invoke(manager, label);
                    if (layer.isPresent()) {
                        ModuleLayer actual = (ModuleLayer) layer.get();
                        String modules = actual.modules().stream().map(Module::getName).sorted()
                                .map(n -> "\"" + n + "\"").collect(java.util.stream.Collectors.joining(","));
                        layers.add("\"" + label + "\":[" + modules + "]");
                    }
                }
                List<String> providers = new ArrayList<>();
                for (String name : List.of("DualForgeTransformationService", "DualForgeLanguageLoader", "DualForgeEntryTransformer", "ForgeModReader", "ForgeModLocator")) {
                    Class<?> type = Arrays.stream(loaded)
                            .filter(c -> c.getName().equals("io.github.gfander.dualforge." + name))
                            .findFirst().orElseThrow();
                    Module module = type.getModule();
                    if (!module.isNamed() || !"dualforge".equals(module.getName()) || module.getLayer() == null)
                        throw new IllegalStateException("Wrong runtime module for " + name);
                    providers.add("\"" + name + "\":\"" + module.getName() + "\"");
                }
                Files.writeString(Path.of(output), "{\"status\":\"PASS\",\"layers\":{" + String.join(",", layers)
                        + "},\"loadedProviders\":{" + String.join(",", providers) + "}}\n");
            } catch (Throwable error) {
                try { Files.writeString(Path.of(output), "{\"status\":\"FAIL\"}\n"); }
                catch (Exception ignored) { /* Runner also rejects a missing report. */ }
                error.printStackTrace();
            }
        }, "dualforge-module-audit"));
    }
}
