package io.github.gfander.dualforge;

import java.nio.file.Files;
import java.nio.file.Path;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

/** Newly generated metadata/annotation association control; never a frozen Forge fixture. */
public final class DiscoveryEntryFixture {
    public static void main(String[] args) throws Exception {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "com/example/discovery/ControlledEntry", null,
                "java/lang/Object", null);
        var mod = writer.visitAnnotation("Lnet/minecraftforge/fml/common/Mod;", true);
        mod.visit("value", "controlled_candidate");
        mod.visitEnd();
        writer.visitEnd();
        Files.write(Path.of(args[0]), writer.toByteArray());
    }
}
