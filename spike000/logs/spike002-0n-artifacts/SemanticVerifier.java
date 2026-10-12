import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public final class SemanticVerifier {
    private static ClassNode read(String path) throws Exception {
        ClassNode node = new ClassNode();
        new ClassReader(Files.readAllBytes(Path.of(path))).accept(node, 0);
        return node;
    }
    private static byte[] canonical(ClassNode node) {
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }
    public static void main(String[] args) throws Exception {
        ClassNode original = read(args[0]);
        ClassNode translated = read(args[1]);
        int reversals = 0;
        for (MethodNode method : translated.methods) {
            if ("onProbe".equals(method.name)
                    && "(Lcom/example/forgeprobe/ProbeEvent;)V".equals(method.desc)
                    && method.visibleAnnotations != null && method.visibleAnnotations.size() == 1
                    && "Lnet/neoforged/bus/api/SubscribeEvent;".equals(method.visibleAnnotations.get(0).desc)) {
                method.visibleAnnotations.get(0).desc = "Lnet/minecraftforge/eventbus/api/SubscribeEvent;";
                reversals++;
            }
        }
        if (reversals != 1 || !Arrays.equals(canonical(original), canonical(translated))) {
            throw new IllegalStateException("extra class change beyond one method annotation descriptor");
        }
        System.out.println("Canonical complete class bytes equal after reversing ONLY one annotation descriptor; no extra field/method/code/debug/bootstrap change.");
    }
}
