package io.github.gfander.dualforge;

import com.electronwill.nightconfig.toml.TomlParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarFile;
import cpw.mods.jarhandling.JarContents;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import net.neoforged.neoforgespi.language.MavenVersionAdapter;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

/** Actual-release boundary checks; these are not a repaired discovery or runtime result. */
public final class ReleaseBoundaryTest {
    public static void main(String[] args) throws Exception {
        Path input = Path.of(args[0]);
        byte[] before = java.nio.file.Files.readAllBytes(input);
        try (JarFile jar = new JarFile(input.toFile())) {
            String text = new String(jar.getInputStream(jar.getJarEntry("META-INF/mods.toml")).readAllBytes(), StandardCharsets.UTF_8);
            var parsed = new TomlParser().parse(text);
            require("javafml".equals(parsed.get("modLoader")), "host TOML parser reads actual loader");
            require("[0,)".equals(parsed.get("loaderVersion")), "original loader range retained");
            require("MIT".equals(parsed.get("license")), "original license retained");
            List<com.electronwill.nightconfig.core.Config> mods = parsed.get("mods");
            require(mods.size() == 1 && "fnf".equals(mods.getFirst().get("modId")), "actual mod identity");
            require("Adding new varieties of food and fuel obtainable from vanilla resources.".equals(mods.getFirst().get("description")), "triple-quoted description parsed");
            List<com.electronwill.nightconfig.core.Config> dependencies = parsed.get("dependencies.fnf");
            require(dependencies.size() == 2 && dependencies.stream().allMatch(d -> Boolean.TRUE.equals(d.get("mandatory"))), "required dependencies retained");
            var range = MavenVersionAdapter.createFromVersionSpec(parsed.get("loaderVersion"));
            require(range.containsVersion(new DefaultArtifactVersion("52.1.0"))
                    && range.containsVersion(new DefaultArtifactVersion("4.0.45")), "range overlaps both loaders");
            require(!ForgeModLocator.isForgeCandidate(input), "current candidate guard declines original release");
            try (JarContents contents = JarContents.of(input)) {
                require(new ForgeModReader().read(contents, ModFileDiscoveryAttributes.DEFAULT) == null,
                        "Reader preserves shared admission boundary");
            }
            ClassNode entry = new ClassNode();
            new ClassReader(jar.getInputStream(jar.getJarEntry("net/natevg/fnf/FoodNFuel.class")).readAllBytes()).accept(entry, 0);
            byte[] classBefore = bytes(entry);
            require(!ControlledForgeItemProfile.isNormalCandidate(entry), "no injected-bus constructor match");
            try {
                ControlledForgeItemProfile.validateNormal(entry, "fnf");
                throw new AssertionError("Release unexpectedly accepted by the current normal profile");
            } catch (IllegalStateException expected) {
                System.out.println("STATIC_PROFILE_REFUSAL " + expected.getMessage());
            }
            require(Arrays.equals(classBefore, bytes(entry)), "validation leaves original ClassNode unchanged");
        }
        require(Arrays.equals(before, java.nio.file.Files.readAllBytes(input)), "original JAR unchanged");
        System.out.println("RELEASE BOUNDARY CONTRACT PASS; compatibility remains unimplemented");
    }

    private static byte[] bytes(ClassNode node) {
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static void require(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
        System.out.println("PASS " + description);
    }
}
