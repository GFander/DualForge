package com.example.forgecompat;

import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TargetType;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
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
 * Spike-002-0l adds only the frozen subscriber/custom-event profile rule.
 */
public final class ForgeProbeConstructorTransformer implements ITransformer<ClassNode> {
    private static final String TARGET_FQCN = "com.example.forgeprobe.ForgeProbe";
    private static final String OLD_DESC = "(Lnet/minecraftforge/eventbus/api/IEventBus;)V";
    private static final String NEW_DESC = "(Lnet/neoforged/bus/api/IEventBus;)V";
    private static final Path DUMP_PATH =
            Path.of(System.getProperty("forgecompat.dump.path",
                    "D:\\DualForge\\spike000\\logs\\spike002-0b-2a-v1.1\\ForgeProbe.rewritten.class"));

    private final boolean rewriteEnabled;
    private final boolean apiRewriteEnabled;
    private final boolean emptyRegistrationEnabled;
    private final boolean subscriberRewriteEnabled;

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
        String emptyValue = System.getProperty("forgecompat.0j.emptyRegistration");
        if (!"true".equals(emptyValue) && !"false".equals(emptyValue)) {
            throw new IllegalStateException("[FORGECOMPAT-XFORM] required forgecompat.0j.emptyRegistration must be true or false");
        }
        this.emptyRegistrationEnabled = Boolean.parseBoolean(emptyValue);
        log("emptyRegistrationEnabled=" + emptyRegistrationEnabled);
        String subscriberValue = System.getProperty("forgecompat.0l.subscriberRewrite");
        if (!"true".equals(subscriberValue) && !"false".equals(subscriberValue)) {
            throw new IllegalStateException("[FORGECOMPAT-XFORM] required forgecompat.0l.subscriberRewrite must be true or false");
        }
        this.subscriberRewriteEnabled = Boolean.parseBoolean(subscriberValue);
        log("subscriberRewriteEnabled=" + subscriberRewriteEnabled);
    }

    @Override
    public ClassNode transform(ClassNode input, ITransformerVotingContext context) {
        try {
            log("offered: " + input.name);
            if ("com/example/forgeprobe/ProbeEvent".equals(input.name)) {
                rewriteProbeEvent(input);
                dumpProfile(input);
                return input;
            }
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
            rewriteSubscriber(input);
            dump(input);
            dumpProfile(input);
            return input;
        } catch (Throwable t) {
            log("ERROR " + t);
            return input;
        }
    }

    private void rewriteSubscriber(ClassNode node) {
        if (!"com/example/forgeprobe/ForgeProbe".equals(node.name) || node.methods.size() != 2) {
            throw new IllegalStateException("0l unexpected subscriber class profile");
        }
        MethodNode listener = null;
        for (MethodNode method : node.methods) {
            if ("onProbe".equals(method.name)
                    && "(Lcom/example/forgeprobe/ProbeEvent;)V".equals(method.desc)) {
                if (listener != null) throw new IllegalStateException("0l duplicate listener");
                listener = method;
            }
        }
        if (listener == null || (listener.access & Opcodes.ACC_PUBLIC) == 0
                || (listener.access & Opcodes.ACC_STATIC) != 0
                || listener.visibleAnnotations == null || listener.visibleAnnotations.size() != 1) {
            throw new IllegalStateException("0l subscriber signature/annotation mismatch");
        }
        AnnotationNode annotation = listener.visibleAnnotations.get(0);
        if (!"Lnet/minecraftforge/eventbus/api/SubscribeEvent;".equals(annotation.desc)
                || (annotation.values != null && !annotation.values.isEmpty())) {
            throw new IllegalStateException("0l default annotation profile mismatch");
        }
        int opcodes = 0;
        for (AbstractInsnNode insn : listener.instructions) {
            if (insn.getOpcode() >= 0) {
                opcodes++;
                if (insn.getOpcode() != Opcodes.RETURN) throw new IllegalStateException("0l nonempty listener");
            }
        }
        if (opcodes != 1) throw new IllegalStateException("0l listener instruction count mismatch");
        if (subscriberRewriteEnabled) annotation.desc = "Lnet/neoforged/bus/api/SubscribeEvent;";
        log("SUBSCRIBER enabled=" + subscriberRewriteEnabled + " annotationHits="
                + (subscriberRewriteEnabled ? 1 : 0) + " desc=" + annotation.desc);
    }

    private void rewriteProbeEvent(ClassNode node) {
        if (!"net/minecraftforge/eventbus/api/Event".equals(node.superName)
                || (node.access & Opcodes.ACC_FINAL) == 0 || (node.access & Opcodes.ACC_ABSTRACT) != 0
                || !node.fields.isEmpty() || !node.interfaces.isEmpty() || node.methods.size() != 1) {
            throw new IllegalStateException("0l custom event profile mismatch");
        }
        MethodNode ctor = node.methods.get(0);
        if (!"<init>".equals(ctor.name) || !"()V".equals(ctor.desc)) {
            throw new IllegalStateException("0l event constructor mismatch");
        }
        AbstractInsnNode first = ctor.instructions.getFirst();
        while (first != null && first.getOpcode() < 0) first = first.getNext();
        AbstractInsnNode second = first == null ? null : nextOpcode(first);
        AbstractInsnNode third = second == null ? null : nextOpcode(second);
        if (!(first instanceof org.objectweb.asm.tree.VarInsnNode v) || v.getOpcode() != Opcodes.ALOAD || v.var != 0
                || !(second instanceof MethodInsnNode parent) || parent.getOpcode() != Opcodes.INVOKESPECIAL
                || parent.itf || !node.superName.equals(parent.owner) || !"<init>".equals(parent.name)
                || !"()V".equals(parent.desc) || third == null || third.getOpcode() != Opcodes.RETURN
                || nextOpcode(third) != null) {
            throw new IllegalStateException("0l event constructor instruction mismatch");
        }
        if (subscriberRewriteEnabled) {
            node.superName = "net/neoforged/bus/api/Event";
            parent.owner = node.superName;
        }
        log("EVENT_PROFILE enabled=" + subscriberRewriteEnabled + " superHits="
                + (subscriberRewriteEnabled ? 1 : 0) + " constructorHits="
                + (subscriberRewriteEnabled ? 1 : 0) + " super=" + node.superName);
    }

    private void dumpProfile(ClassNode node) throws java.io.IOException {
        Path path = Path.of(System.getProperty("forgecompat.dump.profileDir",
                        "D:\\DualForge\\spike000\\logs\\spike002-0l-current"),
                node.name.substring(node.name.lastIndexOf('/') + 1) + ".class");
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        byte[] bytes = writer.toByteArray();
        Files.createDirectories(path.getParent());
        Files.write(path, bytes);
        log("PROFILE dump OK path=" + path + " bytes=" + bytes.length);
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
        if (emptyRegistrationEnabled) {
            if (!apiRewriteEnabled) throw new IllegalStateException("0j requires apiRewrite=true");
            call.setOpcode(Opcodes.INVOKESTATIC);
            call.owner = "com/example/forgecompat/ForgeEmptyRegistration";
            call.desc = "(Lnet/neoforged/bus/api/IEventBus;Ljava/lang/Object;)V";
            call.itf = false;
        }
        log("EMPTY_ROUTE enabled=" + emptyRegistrationEnabled + " hits=" + (emptyRegistrationEnabled ? 1 : 0));
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
        return Set.of(ITransformer.Target.targetClass(TARGET_FQCN),
                ITransformer.Target.targetClass("com.example.forgeprobe.ProbeEvent"));
    }

    @Override
    public TargetType<ClassNode> getTargetType() {
        return TargetType.CLASS;
    }

    private static void log(String message) {
        System.out.println("[FORGECOMPAT-XFORM] " + message);
    }
}
