package io.github.gfander.dualforge;

import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TargetType;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.LdcInsnNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.annotation.ElementType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforgespi.language.ModFileScanData;

/**
 * Spike-002-0b Step 2-A probe transformer.
 * Controlled Forge entries from FML's @Mod scan, with the original <init> descriptor
 *   (Lnet/minecraftforge/eventbus/api/IEventBus;)V
 * -> (Lnet/neoforged/bus/api/IEventBus;)V
 * Spike-002-0g additionally gates the single EVENT_BUS.register(this) member-reference rule.
 * Spike-002-0l adds only the frozen subscriber/custom-event profile rule.
 */
public final class DualForgeEntryTransformer implements ITransformer<ClassNode> {
    private static final String OLD_DESC = "(Lnet/minecraftforge/eventbus/api/IEventBus;)V";
    private static final String NEW_DESC = "(Lnet/neoforged/bus/api/IEventBus;)V";
    private static final Path DUMP_PATH =
            Path.of(System.getProperty("dualforge.dump.path",
                    "D:\\DualForge\\spike000\\logs\\spike002-0b-2a-v1.1\\ForgeProbe.rewritten.class"));

    private final boolean rewriteEnabled;
    private final boolean apiRewriteEnabled;
    private final boolean emptyRegistrationEnabled;
    private final boolean subscriberRewriteEnabled;
    private final Map<String, String> entryEvents = new HashMap<>();
    private final Set<String> eventClasses = new HashSet<>();
    private final Map<String, String> registryEntries = new HashMap<>();

    public DualForgeEntryTransformer() {
        String value = System.getProperty("dualforge.2a.rewrite");
        if (value == null) {
            throw new IllegalStateException(
                    "[DUALFORGE-XFORM] required system property 'dualforge.2a.rewrite' is not set (must be explicitly true or false)");
        }
        this.rewriteEnabled = Boolean.parseBoolean(value);
        log("rewriteEnabled=" + rewriteEnabled);
        String apiValue = System.getProperty("dualforge.0g.apiRewrite");
        if (!"true".equals(apiValue) && !"false".equals(apiValue)) {
            throw new IllegalStateException("[DUALFORGE-XFORM] required dualforge.0g.apiRewrite must be true or false");
        }
        this.apiRewriteEnabled = Boolean.parseBoolean(apiValue);
        log("apiRewriteEnabled=" + apiRewriteEnabled);
        String emptyValue = System.getProperty("dualforge.0j.emptyRegistration");
        if (!"true".equals(emptyValue) && !"false".equals(emptyValue)) {
            throw new IllegalStateException("[DUALFORGE-XFORM] required dualforge.0j.emptyRegistration must be true or false");
        }
        this.emptyRegistrationEnabled = Boolean.parseBoolean(emptyValue);
        log("emptyRegistrationEnabled=" + emptyRegistrationEnabled);
        String subscriberValue = System.getProperty("dualforge.0l.subscriberRewrite");
        if (!"true".equals(subscriberValue) && !"false".equals(subscriberValue)) {
            throw new IllegalStateException("[DUALFORGE-XFORM] required dualforge.0l.subscriberRewrite must be true or false");
        }
        this.subscriberRewriteEnabled = Boolean.parseBoolean(subscriberValue);
        log("subscriberRewriteEnabled=" + subscriberRewriteEnabled);
    }

    @Override
    public ClassNode transform(ClassNode original, ITransformerVotingContext context) {
        // Never expose partial edits to the caller when a controlled profile is rejected.
        ClassNode input = new ClassNode();
        original.accept(input);
        try {
            log("offered: " + input.name);
            if (registryEntries.containsKey(input.name)) {
                if (!rewriteEnabled) return input;
                if (!apiRewriteEnabled) throw new IllegalStateException("005 requires apiRewrite=true");
                ControlledForgeItemProfile.rewrite(input, registryEntries.get(input.name));
                log("REGISTRY_PROFILE entry=" + input.name + " modId=" + registryEntries.get(input.name)
                        + " createHits=1 supplierHits=1 modBusAttachHits=1 holderGetHits=1");
                dumpProfile(input);
                return input;
            }
            if (eventClasses.contains(input.name)) {
                rewriteProbeEvent(input);
                dumpProfile(input);
                return input;
            }
            if (!entryEvents.containsKey(input.name)) return input;
            if (!rewriteEnabled) {
                return input;
            }
            validateSubscriber(input);
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
            log("REJECTED " + input.name + " " + t);
            throw new IllegalStateException("Controlled Forge transformation rejected: " + input.name, t);
        }
    }

    private AnnotationNode validateSubscriber(ClassNode node) {
        if (!entryEvents.containsKey(node.name) || node.methods.size() != 2) {
            throw new IllegalStateException("0l unexpected subscriber class profile");
        }
        MethodNode listener = null;
        for (MethodNode method : node.methods) {
            if (("(L" + entryEvents.get(node.name) + ";)V").equals(method.desc)) {
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
        AbstractInsnNode first = listener.instructions.getFirst();
        while (first != null && first.getOpcode() < 0) first = first.getNext();
        boolean empty = first != null && first.getOpcode() == Opcodes.RETURN && nextOpcode(first) == null;
        if (!empty) {
            AbstractInsnNode constant = first == null ? null : nextOpcode(first);
            AbstractInsnNode call = constant == null ? null : nextOpcode(constant);
            AbstractInsnNode last = call == null ? null : nextOpcode(call);
            if (listener.access != Opcodes.ACC_PUBLIC || !listener.tryCatchBlocks.isEmpty()
                    || (listener.exceptions != null && !listener.exceptions.isEmpty())
                    || (listener.invisibleAnnotations != null && !listener.invisibleAnnotations.isEmpty())
                    || !(first instanceof FieldInsnNode field) || field.getOpcode() != Opcodes.GETSTATIC
                    || !"java/lang/System".equals(field.owner) || !"out".equals(field.name)
                    || !"Ljava/io/PrintStream;".equals(field.desc)
                    || !(constant instanceof LdcInsnNode ldc) || !(ldc.cst instanceof String)
                    || !(call instanceof MethodInsnNode print) || print.getOpcode() != Opcodes.INVOKEVIRTUAL
                    || print.itf || !"java/io/PrintStream".equals(print.owner)
                    || !"println".equals(print.name) || !"(Ljava/lang/String;)V".equals(print.desc)
                    || last == null || last.getOpcode() != Opcodes.RETURN || nextOpcode(last) != null) {
                throw new IllegalStateException("004 unsupported listener: expected empty or marker-only profile");
            }
        }
        return annotation;
    }

    private void rewriteSubscriber(ClassNode node) {
        AnnotationNode annotation = validateSubscriber(node);
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
        Path path = Path.of(System.getProperty("dualforge.dump.profileDir",
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
        if (!entryEvents.containsKey(node.name)) {
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
            call.owner = "io/github/gfander/dualforge/ForgeEmptyRegistration";
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
        entryEvents.clear();
        eventClasses.clear();
        registryEntries.clear();
        Set<ITransformer.Target<ClassNode>> targets = new HashSet<>();
        // ModLauncher registers targets after FML completeScan, before the GAME layer.
        // getScanResult waits for the host's background scan; no classes are loaded here.
        var loading = LoadingModList.get();
        if (loading.hasErrors()) return Set.of();
        for (var file : loading.getModFiles()) {
            if (!ForgeModLocator.isForgeCandidate(file.getFile().getFilePath())) continue;
            ModFileScanData scan = file.getFile().getScanResult();
            for (var info : file.getMods()) {
                for (String entry : DualForgeLanguageLoader.forgeEntrypoints(info, scan)) {
                    String internal = entry.replace('.', '/');
                    Set<String> events = new HashSet<>();
                    for (var annotation : scan.getAnnotations()) {
                        if (annotation.targetType() != ElementType.METHOD
                                || !annotation.clazz().getInternalName().equals(internal)
                                || !annotation.annotationType().getClassName().equals("net.minecraftforge.eventbus.api.SubscribeEvent")) continue;
                        String member = annotation.memberName();
                        String desc = member.substring(member.indexOf('('));
                        Type[] args = Type.getArgumentTypes(desc);
                        if (!Type.VOID_TYPE.equals(Type.getReturnType(desc)) || args.length != 1
                                || args[0].getSort() != Type.OBJECT) continue;
                        // Only the listener's direct Forge Event subclass in this same file.
                        if (scan.getClasses().stream().anyMatch(c -> c.clazz().equals(args[0])
                                && Type.getObjectType("net/minecraftforge/eventbus/api/Event").equals(c.parent()))) {
                            events.add(args[0].getInternalName());
                        }
                    }
                    if (events.isEmpty()) {
                        // Read only this @Mod-owned entry from the same scanned file; never load it here.
                        ClassNode candidate = new ClassNode();
                        try {
                            new ClassReader(Files.readAllBytes(file.getFile().findResource(internal + ".class"))).accept(candidate, 0);
                        } catch (java.io.IOException e) {
                            throw new IllegalStateException("005 cannot inspect owned entry " + entry, e);
                        }
                        ControlledForgeItemProfile.validate(candidate, info.getModId());
                        registryEntries.put(internal, info.getModId());
                        targets.add(ITransformer.Target.targetClass(entry));
                        log("TARGET modId=" + info.getModId() + " entry=" + entry + " profile=single-item"
                                + " file=" + file.getFile().getFilePath());
                        continue;
                    }
                    if (events.size() != 1) throw new IllegalStateException("0l expected one same-file event profile for " + entry);
                    String event = events.iterator().next();
                    entryEvents.put(internal, event);
                    eventClasses.add(event);
                    targets.add(ITransformer.Target.targetClass(entry));
                    targets.add(ITransformer.Target.targetClass(event.replace('/', '.')));
                    log("TARGET modId=" + info.getModId() + " entry=" + entry + " event=" + event
                            + " file=" + file.getFile().getFilePath());
                }
            }
        }
        return targets;
    }

    @Override
    public TargetType<ClassNode> getTargetType() {
        return TargetType.CLASS;
    }

    private static void log(String message) {
        System.out.println("[DUALFORGE-XFORM] " + message);
    }
}
