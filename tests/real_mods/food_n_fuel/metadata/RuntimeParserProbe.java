package io.github.gfander.dualforge.metadata;

import cpw.mods.jarhandling.JarContents;
import io.github.gfander.dualforge.ForgeModReader;
import java.util.zip.ZipFile;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforgespi.locating.IModFileReader;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;

/** Test-only discovery SPI: resolves the parser through the actual production Reader loader. */
public final class RuntimeParserProbe implements IModFileReader {
    @Override public int getPriority() { return 200; }
    @Override public IModFile read(JarContents jar, ModFileDiscoveryAttributes attributes) {
        if (jar.findFile("META-INF/mods.toml").isEmpty()) return null;
        try (var zip = new ZipFile(jar.getPrimaryPath().toFile())) {
            Class<?> parser = Class.forName("com.electronwill.nightconfig.toml.TomlParser", false,
                    ForgeModReader.class.getClassLoader());
            Object config = parser.getMethod("parse", String.class).invoke(parser.getConstructor().newInstance(),
                    new String(zip.getInputStream(zip.getEntry("META-INF/mods.toml")).readAllBytes(),
                            java.nio.charset.StandardCharsets.UTF_8));
            Module reader = ForgeModReader.class.getModule();
            if (!reader.canRead(parser.getModule())) throw new AssertionError("Reader cannot read parser module");
            System.out.println("[DF007C1-NIGHTCONFIG] PASS readerModule=" + reader.getName()
                    + " parserModule=" + parser.getModule().getName() + " readerLoader=" + reader.getClassLoader()
                    + " canRead=true config=" + config);
        } catch (Exception e) {
            throw new IllegalStateException("Runtime NightConfig probe failed", e);
        }
        return null; // Never claims a file or changes its contents.
    }
}
