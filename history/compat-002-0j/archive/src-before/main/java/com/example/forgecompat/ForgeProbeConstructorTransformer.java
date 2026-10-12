package com.example.forgecompat;

import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TargetType;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * Spike-002-0b Step 2-A probe transformer.
 * Sole rewrite: com/example/forgeprobe/ForgeProbe <init> descriptor
 *   (Lnet/minecraftforge/eventbus/api/IEventBus;)V
 * -> (Lnet/neoforged/bus/api/IEventBus;)V
 * Spike-002-0g additionally gates the single EVENT_BUS.register(this) member-reference rule.
 * No other class or statement is touched.
 */
public final class ForgeProbeConstructorTransformer implements ITransformer<ClassNode> {
    private static final String TARGET_FQCN = "com.example.forgeprobe.ForgeProbe";
    private static final String OLD_DESC = "(Lnet/minecraftforge/eventbus/api/IEventBus;)V";
    private static final String NEW_DESC = "(Lnet/neoforged/bus/api/IEventBus;)V";
    private static final Path DUMP_PATH =
            Path.of("D:\\DualForge\\spike000\\logs\\spike002-0b-2a-v1.1\\ForgeProbe.rewritten.class");

    private final boolean rewriteEnabled;
    private final boolean apiRewriteEnabled;

    public ForgeProbeConstructorTransformer() {
        String value = System.getProperty("forgecompat.2a.rewrite");
        if (value == null) {
            throw new IllegalStateException(
                    "[FORGECOMPAT-XFORM] required system property 'forgecompat.2a.rewrite' is not set (must be explicitly true or false)");
        }
        this.rewriteEnabled = Boolean.parseBoolean(value);
        log("rewriteEnabled=" + rewriteEnabled);
        String apiValue = System.getProperty("forgecompat.0g.apiRewrite");
        if (!"true".equals(apiValue) && !"false".equals(apiValue)) {
            throw new IllegalStateException("[FORGECOMPAT-XFORM] required forgecompat.0g.apiRewrite must be true or false");
        }
        this.apiRewriteEnabled = Boolean.parseBoolean(apiValue);
        log("apiRewriteEnabled=" + apiRewriteEnabled);
    }

    @Override
    public ClassNode transform(ClassNode input, ITransformerVotingContext context) {
        try {
            log("offered: " + input.name);
            if (!rewriteEnabled) {
                return input;
            }
            int hits = 0;
            for (MethodNode method : input.methods) {
                if ("<init>".equals(method.name) && OLD_DESC.equals(method.desc)) {
                    method.desc = NEW_DESC;
                    hits++;
                }
            }
            if (hits == 0) {
                log("NO-MATCH: no <init>" + OLD_DESC + " found");
                return input;
            }
            log("REWRITE hits=" + hits + " old=" + OLD_DESC + " new=" + NEW_DESC);
            int ctorCount = 0;
            boolean oldAbsent = true;
            boolean newPresent = false;
            for (MethodNode method : input.methods) {
                if ("<init>".equals(method.name)) {
                    ctorCount++;
                    if (OLD_DESC.equals(method.desc)) {
                        oldAbsent = false;
                    }
                    if (NEW_DESC.equals(method.desc)) {
                        newPresent = true;
                    }
                }
            }
            log("post-check ctorCount=" + ctorCount + " oldAbsent=" + oldAbsent + " newPresent=" + newPresent);
            rewriteEventBusCall(input);
            dump(input);
            return input;
        } catch (Throwable t) {
            log("ERROR " + t);
            return input;
        }
    }

    private void rewriteEventBusCall(ClassNode node) {
        if (!"com/example/forgeprobe/ForgeProbe".equals(node.name)) {
            throw new IllegalStateException("0g unexpected target " + node.name);
        }
        FieldInsnNode field = null;
        MethodInsnNode call = null;
        int matches = 0;
        for (MethodNode method : node.methods) {
            if (!"<init>".equals(method.name) || !NEW_DESC.equals(method.desc)) continue;
            for (AbstractInsnNode insn : method.instructions) {
                if (!(insn instanceof FieldInsnNode f)) continue;
                if (f.getOpcode() != Opcodes.GETSTATIC
                        || !"net/minecraftforge/common/MinecraftForge".equals(f.owner)
                        || !"EVENT_BUS".equals(f.name)
                        || !"Lnet/minecraftforge/eventbus/api/IEventBus;".equals(f.desc)) continue;
                AbstractInsnNode receiver = nextOpcode(insn);
                AbstractInsnNode invocation = receiver == null ? null : nextOpcode(receiver);
                if (receiver == null || receiver.getOpcode() != Opcodes.ALOAD
                        || !(receiver instanceof org.objectweb.asm.tree.VarInsnNode v) || v.var != 0
                        || !(invocation instanceof MethodInsnNode m)
                        || m.getOpcode() != Opcodes.INVOKEINTERFACE || !m.itf
                        || !"net/minecraftforge/eventbus/api/IEventBus".equals(m.owner)
                        || !"register".equals(m.name) || !"(Ljava/lang/Object;)V".equals(m.desc)) {
                    throw new IllegalStateException("0g member sequence mismatch");
                }
                field = f;
                call = m;
                matches++;
            }
        }
        if (matches != 1) throw new IllegalStateException("0g expected one member sequence, found " + matches);
        log("API original-pattern hits=" + matches);
        if (apiRewriteEnabled) {
            field.owner = "net/neoforged/neoforge/common/NeoForge";
            field.desc = "Lnet/neoforged/bus/api/IEventBus;";
            call.owner = "net/neoforged/bus/api/IEventBus";
        }
        log("API_REWRITE enabled=" + apiRewriteEnabled + " hits=" + (apiRewriteEnabled ? 1 : 0));
        log("API post-check field=" + field.owner + "." + field.name + ":" + field.desc
                + " call=" + call.owner + "." + call.name + call.desc + " itf=" + call.itf);
    }

    private static AbstractInsnNode nextOpcode(AbstractInsnNode node) {
        AbstractInsnNode next = node.getNext();
        while (next != null && next.getOpcode() < 0) next = next.getNext();
        return next;
    }

    private void dump(ClassNode node) {
        try {
            ClassWriter writer = new ClassWriter(0);
            node.accept(writer);
            byte[] bytes = writer.toByteArray();
            Files.createDirectories(DUMP_PATH.getParent());
            Files.write(DUMP_PATH, bytes);
            log("dump OK path=" + DUMP_PATH + " bytes=" + bytes.length);
        } catch (Throwable t) {
            log("dump FAIL " + t);
        }
    }

    @Override
    public TransformerVoteResult castVote(ITransformerVotingContext context) {
        return TransformerVoteResult.YES;
    }

    @Override
    public Set<ITransformer.Target<ClassNode>> targets() {
        return Set.of(ITransformer.Target.targetClass(TARGET_FQCN));
    }

    @Override
    public TargetType<ClassNode> getTargetType() {
        return TargetType.CLASS;
    }

    private static void log(String message) {
        System.out.println("[FORGECOMPAT-XFORM] " + message);
    }
}
