package io.github.gfander.dualforge;

import cpw.mods.jarhandling.JarContents;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;

/** Gate 0 public metadata -> automatic module -> public constructor experiment.
 * The stock JDK layer is only a standalone model; the driver uses FML's existing GAME layer.
 */
public final class VisibilityCheck {
    public static void main(String[] args) throws Exception {
        var file = (ModFile) new ForgeModReader().read(JarContents.of(Path.of(args[0])), ModFileDiscoveryAttributes.DEFAULT);
        LoadingModList.of(List.of(), List.of(file), List.of(), List.of(), Map.of());
        String name = LoadingModList.get().getModFileById("dispatchprobe").getFile().getSecureJar().name();
        if (!file.getModFileInfo().moduleName().equals(name)) throw new AssertionError("FML metadata/module identity mismatch");
        var finder = ModuleFinder.of(Path.of(args[0]));
        if (!finder.find(name).orElseThrow().descriptor().isAutomatic()) throw new AssertionError("Expected automatic module");
        var config = ModuleLayer.boot().configuration().resolve(finder, ModuleFinder.of(), Set.of(name));
        var layer = ModuleLayer.boot().defineModulesWithOneLoader(config, ClassLoader.getSystemClassLoader());
        var type = Class.forName("com.example.contract.VisibilityEvent", true, layer.findLoader(name));
        Object instance = type.getConstructor().newInstance();
        if (!type.getModule().getName().equals(name) || !type.getModule().isExported(type.getPackageName())) throw new AssertionError("Visibility");
        if (!Boolean.toString(Boolean.parseBoolean(System.getProperty("DF004_REGISTER"))).equals(args[1])) throw new AssertionError("Switch");
        System.out.println("PASS public metadata/module loader/constructor " + instance.getClass().getName() + " module=" + name);
        System.out.println("PASS switch reaches loaded module value=" + type.getMethod("switchValue").invoke(instance));
        if (!args[1].equals(type.getMethod("switchValue").invoke(instance))) throw new AssertionError("Loaded switch");
    }
}
