import java.nio.file.Files;
import java.nio.file.Path;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public final class ProfileRewriter {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("input class and new output class required");
        Path output = Path.of(args[1]);
        if (Files.exists(output)) throw new IllegalStateException("output exists");
        ClassNode node = new ClassNode();
        new ClassReader(Files.readAllBytes(Path.of(args[0]))).accept(node, 0);
        if (!"com/example/forgeprobe/SubscriberFixture".equals(node.name)
                || !"java/lang/Object".equals(node.superName) || node.methods.size() != 2
                || node.fields.size() != 1 || !"hits".equals(node.fields.get(0).name)
                || !"I".equals(node.fields.get(0).desc)) {
            throw new IllegalStateException("fixture profile mismatch");
        }
        int hits = 0;
        for (MethodNode method : node.methods) {
            if (!"onProbe".equals(method.name)
                    || !"(Lcom/example/forgeprobe/ProbeEvent;)V".equals(method.desc)) continue;
            if ((method.access & Opcodes.ACC_PUBLIC) == 0 || (method.access & Opcodes.ACC_STATIC) != 0
                    || method.visibleAnnotations == null || method.visibleAnnotations.size() != 1) {
                throw new IllegalStateException("subscriber method mismatch");
            }
            AnnotationNode annotation = method.visibleAnnotations.get(0);
            if (!"Lnet/minecraftforge/eventbus/api/SubscribeEvent;".equals(annotation.desc)
                    || (annotation.values != null && !annotation.values.isEmpty())) {
                throw new IllegalStateException("default annotation mismatch");
            }
            annotation.desc = "Lnet/neoforged/bus/api/SubscribeEvent;";
            hits++;
        }
        if (hits != 1) throw new IllegalStateException("expected one annotation rewrite");
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        Files.createDirectories(output.getParent());
        Files.write(output, writer.toByteArray());
        System.out.println("[SUBSCRIBER-REWRITER] annotationHits=1");
    }
}
