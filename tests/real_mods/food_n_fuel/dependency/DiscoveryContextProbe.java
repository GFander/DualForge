package io.github.gfander.dualforge.dependency;

import cpw.mods.jarhandling.JarContents;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforgespi.locating.*;

/** DIAGNOSTIC_ONLY: observes the real discovery lifecycle without claiming or editing a file. */
public final class DiscoveryContextProbe implements IModFileReader {
    @Override public int getPriority() { return 200; }
    @Override public IModFile read(JarContents jar, ModFileDiscoveryAttributes attributes) {
        if (jar.findFile("META-INF/mods.toml").isPresent())
            System.out.println("DIAGNOSTIC_ONLY [DF007C2-CONTEXT] versions=" + FMLLoader.versionInfo()
                    + " dist=" + FMLLoader.getDist() + " java=" + Runtime.version().feature());
        return null;
    }
}
