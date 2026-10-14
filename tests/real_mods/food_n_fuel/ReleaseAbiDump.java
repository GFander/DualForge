package io.github.gfander.dualforge;

import java.nio.file.Path;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

/** Read-only ASM node/descriptor evidence; never loads or rewrites third-party classes. */
public final class ReleaseAbiDump {
    public static void main(String[] args) throws Exception {
        Path input = Path.of(args[0]);
        System.out.println("CURRENT_CANDIDATE=" + ForgeModLocator.isForgeCandidate(input));
        try (JarFile jar = new JarFile(input.toFile())) {
            for (var entry : jar.stream().filter(e -> e.getName().endsWith(".class")).toList()) {
                ClassNode node = new ClassNode();
                new ClassReader(jar.getInputStream(entry).readAllBytes()).accept(node, 0);
                System.out.println("CLASS " + node.name + " access=" + node.access + " super=" + node.superName);
                if (node.visibleAnnotations != null)
                    for (var a : node.visibleAnnotations) System.out.println("ANNOTATION " + a.desc + " " + a.values);
                for (var f : node.fields)
                    System.out.println("FIELD " + f.access + " " + f.name + " " + f.desc + " signature=" + f.signature);
                for (var m : node.methods) {
                    System.out.println("METHOD " + m.access + " " + m.name + m.desc + " signature=" + m.signature);
                    for (AbstractInsnNode i : m.instructions) {
                        if (i.getOpcode() < 0) continue;
                        String detail = "";
                        if (i instanceof FieldInsnNode f) detail = f.owner + "." + f.name + " " + f.desc;
                        if (i instanceof MethodInsnNode c) detail = c.owner + "." + c.name + c.desc + " itf=" + c.itf;
                        if (i instanceof TypeInsnNode t) detail = t.desc;
                        if (i instanceof VarInsnNode v) detail = "local=" + v.var;
                        if (i instanceof LdcInsnNode l) detail = "constant=" + l.cst;
                        if (i instanceof IntInsnNode n) detail = "operand=" + n.operand;
                        if (i instanceof InvokeDynamicInsnNode d) {
                            detail = d.name + d.desc + " bootstrap=" + d.bsm;
                            for (Object a : d.bsmArgs) detail += " argument=" + a;
                        }
                        System.out.println("  " + i.getClass().getSimpleName() + " opcode=" + i.getOpcode() + " " + detail);
                    }
                }
            }
        }
    }
}
