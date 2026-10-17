package io.github.gfander.dualforge;

import java.nio.file.Path;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

/** Controlled candidate-selection reproduction; does not launch a client. */
public final class ProfileFailure {
    public static void main(String[] args) throws Exception {
        ClassNode entry = new ClassNode();
        try (var jar = new ZipFile(Path.of(args[0]).toFile())) {
            new ClassReader(jar.getInputStream(jar.getEntry(
                "yalter/mousetweaks/forge/MouseTweaksForge.class"))).accept(entry, 0);
        }
        try {
            ForgeEntryProfile.select(entry, false);
            throw new AssertionError("Baseline unexpectedly admitted the client entry");
        } catch (IllegalStateException expected) {
            if (!expected.getMessage().equals("UNSUPPORTED_ENTRY " + entry.name
                    + ": no controlled Forge entry profile")) throw expected;
            expected.printStackTrace(System.out);
            System.out.println("CONTROLLED_PROFILE_FAILURE_REPRODUCED=PASS; REAL_CLIENT_BOOT=NOT_RUN");
        }
    }
}
