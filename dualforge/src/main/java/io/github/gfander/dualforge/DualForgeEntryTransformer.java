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
    private final boolean rewriteEnabled;
    private final boolean apiRewriteEnabled;
    private final boolean emptyRegistrationEnabled;
    private final boolean subscriberRewriteEnabled;
    private final Map<String, String> entryEvents = new HashMap<>();
    private final Set<String> eventClasses = new HashSet<>();
    private final Map<String, String> registryEntries = new HashMap<>();
    private final Set<String> normalRegistryEntries = new HashSet<>();
    private final Map<String, String> blockEntries = new HashMap<>();
    private final Map<String, ControlledForgeContextProfile.Scope> contextEntries = new HashMap<>();
    private volatile Map<String, ForgeRegistryTransformationPlan> helperPlans = Map.of();
    private volatile Map<String, ForgeEventInitializerPlan> eventInitializerPlans = Map.of();

    public DualForgeEntryTransformer() {
        this.rewriteEnabled = enabled("dualforge.2a.rewrite");
        log("rewriteEnabled=" + rewriteEnabled);
        this.apiRewriteEnabled = enabled("dualforge.0g.apiRewrite");
        log("apiRewriteEnabled=" + apiRewriteEnabled);
        this.emptyRegistrationEnabled = enabled("dualforge.0j.emptyRegistration");
        log("emptyRegistrationEnabled=" + emptyRegistrationEnabled);
        this.subscriberRewriteEnabled = enabled("dualforge.0l.subscriberRewrite");
        log("subscriberRewriteEnabled=" + subscriberRewriteEnabled);
    }

    private static boolean enabled(String property) {
        String value = System.getProperty(property);
        if (value == null) return true;
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new IllegalStateException("[DUALFORGE-XFORM] invalid system property '"
                    + property + "': expected true or false");
        }
        return Boolean.parseBoolean(value);
    }

    @Override
    public ClassNode transform(ClassNode original, ITransformerVotingContext context) {
        // Never expose partial edits to the caller when a controlled profile is rejected.
        ClassNode input = new ClassNode();
        original.accept(input);
        try {
            log("offered: " + input.name);
            var eventPlan = eventInitializerPlans.get(input.name);
            if (eventPlan != null) {
                if (!rewriteEnabled || !apiRewriteEnabled)
                    throw new IllegalStateException("Event initializer requires enabled rewrites");
                input = eventPlan.transform(input, context == null ? null : context.getInitialClassSha256());
                log("EVENT_INITIALIZER_TRANSFORM class=" + input.name + " modId=" + eventPlan.modId + " plan=" + eventPlan.fingerprint);
                dumpProfile(input);
                return input;
            }
            var registryPlan = helperPlans.get(input.name);
            if (registryPlan != null) {
                if (!rewriteEnabled || !apiRewriteEnabled) throw new IllegalStateException("Registry helper requires enabled rewrites");
                input = registryPlan.transform(input, context == null ? null : context.getInitialClassSha256());
                log("HELPER_TRANSFORM class=" + input.name + " modId=" + registryPlan.modId + " plan=" + registryPlan.fingerprint);
                dumpProfile(input);
                return input;
            }
            if (contextEntries.containsKey(input.name)) {
                if (!rewriteEnabled) return input;
                if (!apiRewriteEnabled) throw new IllegalStateException("007C-3 requires apiRewrite=true");
                var stats = ControlledForgeContextProfile.rewrite(input, contextEntries.get(input.name));
                log("CONTEXT_PROFILE entry=" + input.name + " contextCalls=2 helpersUnchanged=" + stats.helpers()
                        + " busReceipts=" + stats.receipts() + " pendingGlobalCalls=" + stats.pendingGlobalCalls());
                dumpProfile(input);
                return input;
            }
            if (blockEntries.containsKey(input.name)) {
                if (!rewriteEnabled) return input;
                if (!apiRewriteEnabled) throw new IllegalStateException("006B requires apiRewrite=true");
                var stats = ControlledForgeBlockProfile.rewrite(input, blockEntries.get(input.name));
                log("BLOCK_REGISTRY_PROFILE entry=" + input.name + " modId=" + blockEntries.get(input.name)
                        + " createHits=" + stats.creates() + " supplierHits=" + stats.declarations()
                        + " modBusAttachHits=" + stats.attachments() + " holderGetHits=" + stats.getters()
                        + " supplierImplementationHits=" + stats.suppliers());
                dumpProfile(input);
                return input;
            }
            if (registryEntries.containsKey(input.name)) {
                if (!rewriteEnabled) return input;
                if (!apiRewriteEnabled) throw new IllegalStateException("005 requires apiRewrite=true");
                var stats = normalRegistryEntries.contains(input.name)
                        ? ControlledForgeItemProfile.rewriteNormal(input, registryEntries.get(input.name))
                        : ControlledForgeItemProfile.rewrite(input, registryEntries.get(input.name));
                log("REGISTRY_PROFILE entry=" + input.name + " modId=" + registryEntries.get(input.name)
                        + " createHits=1 supplierHits=" + stats.declarations()
                        + " modBusAttachHits=1 holderGetHits=" + stats.accessors()
                        + " supplierImplementationHits=" + stats.suppliers());
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
        String directory = System.getProperty("dualforge.dump.profileDir");
        if (directory == null) return;
        Path path = Path.of(directory,
                node.name.substring(node.name.lastIndexOf('/') + 1) + ".class");
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        byte[] bytes = writer.toByteArray();
        Files.createDirectories(path.toAbsolutePath().getParent());
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

    private void dump(ClassNode node) throws java.io.IOException {
        String destination = System.getProperty("dualforge.dump.path");
        if (destination == null) return;
        Path path = Path.of(destination);
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        byte[] bytes = writer.toByteArray();
        Files.createDirectories(path.toAbsolutePath().getParent());
        Files.write(path, bytes);
        log("dump OK path=" + path + " bytes=" + bytes.length);
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
        normalRegistryEntries.clear();
        blockEntries.clear();
        contextEntries.clear();
        helperPlans = Map.of();
        eventInitializerPlans = Map.of();
        Map<String, ForgeRegistryTransformationPlan> pendingHelpers = new HashMap<>();
        Map<String, ForgeEventInitializerPlan> pendingEvents = new HashMap<>();
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
                    // Candidate routing is separate from validation; no Item fallback for unknown entries.
                    ClassNode candidate = new ClassNode();
                    try {
                        new ClassReader(Files.readAllBytes(file.getFile().findResource(internal + ".class"))).accept(candidate, 0);
                    } catch (java.io.IOException e) {
                        throw new IllegalStateException("Cannot inspect owned Forge entry " + entry, e);
                    }
                    var kind = ForgeEntryProfile.select(candidate, !events.isEmpty());
                    log("ENTRY_PROFILE modId=" + info.getModId() + " entry=" + entry + " selected=" + kind);
                    if (kind == ForgeEntryProfile.Kind.EVENT_INITIALIZER) {
                        var plan = ForgeEventInitializerPlan.create(info, internal);
                        for (String name : plan.targets()) {
                            for (var other : loading.getModFiles()) {
                                if (other == file) continue;
                                if (other.getFile().getScanResult().getClasses().stream()
                                        .anyMatch(c -> c.clazz().getInternalName().equals(name)))
                                    throw new IllegalStateException("TRANSFORMATION_RULE_CONFLICT foreign event target " + name);
                            }
                            if (pendingEvents.putIfAbsent(name, plan) != null)
                                throw new IllegalStateException("TRANSFORMATION_RULE_CONFLICT duplicate event target " + name);
                            targets.add(ITransformer.Target.targetClass(name.replace('/', '.')));
                        }
                        log("EVENT_INITIALIZER_PREPLAN modId=" + info.getModId() + " module=" + plan.module
                                + " targets=" + new java.util.TreeSet<>(plan.targets()) + " plan=" + plan.fingerprint);
                        continue;
                    }
                    if (kind == ForgeEntryProfile.Kind.CONTEXT) {
                        long entries = scan.getAnnotations().stream().filter(a -> a.targetType() == ElementType.TYPE
                                && a.annotationType().getClassName().equals("net.minecraftforge.fml.common.Mod")).count();
                        if (entries != 1 || file.getMods().size() != 1)
                            throw new IllegalStateException("Context entry requires one same-file Forge entry and metadata owner");
                        var names = scan.getClasses().stream().map(c -> c.clazz().getInternalName()).collect(java.util.stream.Collectors.toUnmodifiableSet());
                        var scope = new ControlledForgeContextProfile.Scope(info.getModId(), names, owner -> {
                            try { return Files.readAllBytes(file.getFile().findResource(owner + ".class")); }
                            catch (java.io.IOException e) { throw new IllegalStateException("Cannot read owned Context helper " + owner, e); }
                        });
                        if (ForgeRegistryTransformationPlan.isCandidate(candidate)) {
                            var plan = ForgeRegistryTransformationPlan.create(info, internal);
                            for (String name : plan.targets()) {
                                for (var other : loading.getModFiles()) {
                                    if (other == file) continue;
                                    if (other.getFile().getScanResult().getClasses().stream().anyMatch(c -> c.clazz().getInternalName().equals(name)))
                                        throw new IllegalStateException("TRANSFORMATION_RULE_CONFLICT foreign ModFile target " + name);
                                }
                                if (pendingHelpers.putIfAbsent(name, plan) != null)
                                    throw new IllegalStateException("TRANSFORMATION_RULE_CONFLICT duplicate target " + name);
                                targets.add(ITransformer.Target.targetClass(name.replace('/', '.')));
                            }
                            log("HELPER_PREPLAN modId=" + info.getModId() + " module=" + plan.module + " targets=" + new java.util.TreeSet<>(plan.targets()) + " plan=" + plan.fingerprint);
                            continue;
                        }
                        var stats = ControlledForgeContextProfile.validate(candidate, scope);
                        contextEntries.put(internal, scope);
                        targets.add(ITransformer.Target.targetClass(entry));
                        log("TARGET modId=" + info.getModId() + " entry=" + entry + " profile=forge-context"
                                + " helpersUnchanged=" + stats.helpers() + " file=" + file.getFile().getFilePath());
                        continue;
                    }
                    if (kind != ForgeEntryProfile.Kind.EVENT) {
                        if (kind == ForgeEntryProfile.Kind.BLOCK) {
                            long entries = scan.getAnnotations().stream().filter(a -> a.targetType() == ElementType.TYPE
                                    && a.annotationType().getClassName().equals("net.minecraftforge.fml.common.Mod")).count();
                            if (entries != 1 || file.getMods().size() != 1) {
                                throw new IllegalStateException("006B requires one same-file Forge @Mod entry and metadata owner");
                            }
                            ControlledForgeBlockProfile.validate(candidate, info.getModId());
                            blockEntries.put(internal, info.getModId());
                            targets.add(ITransformer.Target.targetClass(entry));
                            log("TARGET modId=" + info.getModId() + " entry=" + entry + " profile=block-item-pair"
                                    + " file=" + file.getFile().getFilePath());
                            continue;
                        }
                        boolean normal = kind == ForgeEntryProfile.Kind.NORMAL_ITEM;
                        var stats = normal ? ControlledForgeItemProfile.validateNormal(candidate, info.getModId())
                                : ControlledForgeItemProfile.validate(candidate, info.getModId());
                        if (normal) normalRegistryEntries.add(internal);
                        registryEntries.put(internal, info.getModId());
                        targets.add(ITransformer.Target.targetClass(entry));
                        log("TARGET modId=" + info.getModId() + " entry=" + entry
                                + " profile=" + (normal ? "normal-items"
                                        : stats.declarations() == 1 ? "single-item" : "controlled-items")
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
        for (String name : pendingHelpers.keySet()) {
            if (contextEntries.containsKey(name) || registryEntries.containsKey(name) || blockEntries.containsKey(name)
                    || entryEvents.containsKey(name) || eventClasses.contains(name) || pendingEvents.containsKey(name))
                throw new IllegalStateException("TRANSFORMATION_RULE_CONFLICT existing profile " + name);
        }
        for (String name : pendingEvents.keySet()) {
            if (contextEntries.containsKey(name) || registryEntries.containsKey(name) || blockEntries.containsKey(name)
                    || entryEvents.containsKey(name) || eventClasses.contains(name))
                throw new IllegalStateException("TRANSFORMATION_RULE_CONFLICT existing event profile " + name);
        }
        helperPlans = Map.copyOf(pendingHelpers);
        eventInitializerPlans = Map.copyOf(pendingEvents);
        if (!helperPlans.isEmpty()) log("HELPER_PLANS_PUBLISHED targets=" + helperPlans.size() + " beforeGameLayer=true");
        if (!eventInitializerPlans.isEmpty()) log("EVENT_INITIALIZER_PLANS_PUBLISHED targets=" + eventInitializerPlans.size() + " beforeGameLayer=true");
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
