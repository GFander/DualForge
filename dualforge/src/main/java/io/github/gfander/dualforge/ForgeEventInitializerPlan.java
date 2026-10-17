package io.github.gfander.dualforge;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import net.neoforged.neoforgespi.language.IModInfo;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** One zero-capture pickup predicate and a same-file mutable-int event publisher.
 * This is not a general Forge Event or ServiceLoader implementation. */
final class ForgeEventInitializerPlan {
    static final String SOURCE_BUS = "net/minecraftforge/eventbus/api/IEventBus";
    static final String SOURCE_EVENT = "net/minecraftforge/eventbus/api/Event";
    static final String SOURCE_GLOBAL = "net/minecraftforge/common/MinecraftForge";
    static final String SOURCE_PICKUP = "net/minecraftforge/event/entity/player/PlayerXpEvent$PickupXp";
    static final String HOST_BUS = "net/neoforged/bus/api/IEventBus";
    static final String HOST_EVENT = "net/neoforged/bus/api/Event";
    static final String HOST_GLOBAL = "net/neoforged/neoforge/common/NeoForge";
    static final String HOST_PICKUP = "net/neoforged/neoforge/event/entity/player/PlayerXpEvent$PickupXp";
    static final String PLAYER = "Lnet/minecraft/world/entity/player/Player;";
    static final String ORB = "Lnet/minecraft/world/entity/ExperienceOrb;";
    static final String PREDICATE = "Ljava/util/function/BiPredicate;";
    static final String PICKUP_DESC = "(" + PLAYER + ORB + ")Z";
    static final String VALUE_CTOR = "(" + PLAYER + "I)V";
    static final String VALUE_CALL = "(" + PLAYER + "I)Lcom/mojang/datafixers/util/Either;";
    private static final Pattern FORGE = Pattern.compile("net/minecraftforge/[A-Za-z0-9_/$]+");
    private static final Map<String,String> HOST_SHA = Map.of(
        HOST_PICKUP, "cc979d1e9325d32d97fe27ff66d1dffddf10220a59c1a471fbeb104ee4894b86",
        "net/neoforged/neoforge/event/entity/player/PlayerXpEvent", "8013d10b16bb62f4a01323d58a26424e5db54e8e13b264b4121634751924af66",
        "net/neoforged/neoforge/event/entity/player/PlayerEvent", "d3656a5ce4bb57d008a9a81d1653be43891c5a6890560a6967fcb3643367b9f3",
        "net/neoforged/neoforge/event/entity/living/LivingEvent", "265386d0ed8ea8c8e04af0d601b2baec65e466a81137f05dae7f455e4c392cc5",
        "net/neoforged/neoforge/event/entity/EntityEvent", "23e99c7a8e2a144ed15c3c755c6a81403bd677215ccb6c967f6f95fd2d25d86e");
    final String modId, module, entry, fingerprint;
    private final Map<String,byte[]> original, normalized, output;

    private ForgeEventInitializerPlan(String modId, String module, String entry,
            Map<String,byte[]> original, Set<String> events, String provider) {
        this.modId = modId; this.module = module; this.entry = entry;
        this.original = Map.copyOf(original);
        Map<String,byte[]> before = new TreeMap<>(), after = new TreeMap<>();
        Set<String> targets = new TreeSet<>(events); targets.add(entry); targets.add(provider);
        for (String name : targets) {
            ClassNode node = read(original.get(name));
            before.put(name, bytes(node));
            if (events.contains(name)) {
                node.superName = HOST_EVENT;
                for (MethodNode method : node.methods) for (var op : method.instructions)
                    if (op instanceof MethodInsnNode call && call.owner.equals(SOURCE_EVENT)) call.owner = HOST_EVENT;
            } else {
                for (MethodNode method : node.methods) for (var op : method.instructions.toArray()) {
                    if (op instanceof FieldInsnNode field && field.owner.equals(SOURCE_GLOBAL)) {
                        field.owner = HOST_GLOBAL; field.desc = "L" + HOST_BUS + ";";
                    }
                    if (op instanceof TypeInsnNode type && type.desc.equals(SOURCE_PICKUP)) type.desc = HOST_PICKUP;
                    if (op instanceof MethodInsnNode call && call.owner.equals(SOURCE_PICKUP)) call.owner = HOST_PICKUP;
                    if (op instanceof MethodInsnNode call && call.owner.equals(SOURCE_BUS)) {
                        call.owner = HOST_BUS; call.desc = "(L" + HOST_EVENT + ";)L" + HOST_EVENT + ";";
                        if (name.equals(entry)) {
                            InsnList result = new InsnList();
                            result.add(new TypeInsnNode(Opcodes.CHECKCAST, HOST_PICKUP));
                            result.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                "net/neoforged/bus/api/ICancellableEvent", "isCanceled", "()Z", true));
                            method.instructions.insert(call, result);
                        }
                    }
                }
                for (InnerClassNode inner : node.innerClasses) {
                    if (inner.name.equals(SOURCE_PICKUP)) inner.name = HOST_PICKUP;
                    if (Objects.equals(inner.outerName, SOURCE_PICKUP.substring(0, SOURCE_PICKUP.indexOf('$'))))
                        inner.outerName = HOST_PICKUP.substring(0, HOST_PICKUP.indexOf('$'));
                }
            }
            RegistryBytecodeVerifier.verify(node);
            after.put(name, bytes(node));
        }
        normalized = Map.copyOf(before); output = Map.copyOf(after);
        StringBuilder id = new StringBuilder("event-initializer-v1\n" + modId + "\n" + module + "\n");
        original.forEach((name, value) -> id.append(name).append(':').append(digest(value)).append('\n'));
        fingerprint = digest(id.toString().getBytes(StandardCharsets.UTF_8));
    }

    static boolean isCandidate(ClassNode node) {
        return node.methods.stream().filter(m -> m.name.equals("<init>")).anyMatch(m ->
            Arrays.stream(m.instructions.toArray()).anyMatch(op -> op instanceof InvokeDynamicInsnNode d
                && d.desc.equals("()" + PREDICATE)));
    }

    static ForgeEventInitializerPlan create(IModInfo info, String entry) {
        var file = info.getOwningFile(); var scan = file.getFile().getScanResult();
        require(scan != null && ForgeModLocator.isForgeCandidate(file.getFile().getFilePath()), "SOURCE_OWNER");
        require(!Files.exists(file.getFile().findResource("module-info.class"))
            && !Files.exists(file.getFile().findResource("META-INF/versions")), "EXPLICIT_OR_MULTIRELEASE_MODULE");
        require(file.getMods().size() == 1 && file.getMods().getFirst() == info, "METADATA_OWNER");
        require(file.moduleName() != null && !file.moduleName().isBlank(), "MODULE_IDENTITY");
        require(DualForgeLanguageLoader.forgeEntrypoints(info, scan).equals(List.of(entry.replace('/', '.'))), "ENTRY_OWNER");
        long entries = scan.getAnnotations().stream().filter(a -> a.targetType() == java.lang.annotation.ElementType.TYPE
            && a.annotationType().getClassName().equals("net.minecraftforge.fml.common.Mod")).count();
        require(entries == 1, "ENTRY_COUNT");
        Map<String,byte[]> raw = new TreeMap<>();
        try {
            for (var source : scan.getClasses()) {
                String name = source.clazz().getInternalName();
                require(raw.putIfAbsent(name, Files.readAllBytes(file.getFile().findResource(name + ".class"))) == null,
                    "DUPLICATE_CLASS");
            }
            return validate(info.getModId(), file.moduleName(), entry, raw,
                name -> {
                    try { var path = file.getFile().findResource(name); return Files.exists(path) ? Files.readString(path) : null; }
                    catch (IOException failure) { throw new IllegalStateException("Cannot read event service", failure); }
                }, ControlledItemSupplier::hostResource);
        } catch (IOException failure) { throw new IllegalStateException("Cannot read event closure", failure); }
    }

    // Also used by the independent bytecode refusal contract, without defining source classes.
    static ForgeEventInitializerPlan validate(String modId, String module, String entry,
            Map<String,byte[]> raw, Function<String,String> resources, Function<String,byte[]> host) {
        require(module != null && !module.isBlank() && modId != null && !modId.isBlank(), "OWNER_IDENTITY");
        Map<String,ClassNode> nodes = new TreeMap<>();
        for (var source : raw.entrySet()) {
            ClassNode node = read(source.getValue());
            require(source.getKey().equals(node.name) && !platform(node.name), "CLASS_OWNER");
            nodes.put(node.name, node);
        }
        ClassNode initializer = nodes.get(entry); require(initializer != null, "ENTRY_MISSING");
        structure(initializer, true);
        require(initializer.superName.equals("java/lang/Object") && initializer.interfaces.isEmpty()
            && initializer.fields.isEmpty() && initializer.methods.size() == 2, "ENTRY_STRUCTURE");
        require(initializer.visibleAnnotations != null && initializer.visibleAnnotations.size() == 1
            && initializer.visibleAnnotations.getFirst().desc.equals("Lnet/minecraftforge/fml/common/Mod;")
            && List.of("value", modId).equals(initializer.visibleAnnotations.getFirst().values), "ENTRY_ANNOTATION_OWNER");
        MethodNode ctor = method(initializer, "<init>", "()V"); metadata(ctor);
        require(ctor.access == Opcodes.ACC_PUBLIC, "CONSTRUCTOR_ACCESS");
        var code = ops(ctor);
        require(code.size() == 5 && load(code.get(0), Opcodes.ALOAD, 0)
            && call(code.get(1), Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V")
            && code.get(2) instanceof InvokeDynamicInsnNode && code.get(3) instanceof FieldInsnNode
            && code.get(4).getOpcode() == Opcodes.RETURN, "CONSTRUCTOR_BODY");
        var field = (FieldInsnNode) code.get(3);
        require(field.getOpcode() == Opcodes.PUTSTATIC && field.desc.equals(PREDICATE)
            && !field.owner.equals(entry) && nodes.containsKey(field.owner), "PREDICATE_FIELD_OWNER");
        MethodNode lambda = predicate(initializer, (InvokeDynamicInsnNode) code.get(2));
        var body = ops(lambda);
        require(body.size() == 8 && bus(body.get(0)) && type(body.get(1), Opcodes.NEW, SOURCE_PICKUP)
            && body.get(2).getOpcode() == Opcodes.DUP && load(body.get(3), Opcodes.ALOAD, 0)
            && load(body.get(4), Opcodes.ALOAD, 1)
            && call(body.get(5), Opcodes.INVOKESPECIAL, SOURCE_PICKUP, "<init>", "(" + PLAYER + ORB + ")V")
            && post(body.get(6)) && body.get(7).getOpcode() == Opcodes.IRETURN, "PICKUP_PUBLISH_BODY");
        common(nodes.get(field.owner), field.name);
        Set<String> events = new TreeSet<>(); String provider = null;
        for (ClassNode node : nodes.values()) {
            if (node.name.equals(entry) || !references(raw.get(node.name)).contains(SOURCE_GLOBAL)) continue;
            require(provider == null, "EVENT_PROVIDER_COUNT"); provider = node.name;
            structure(node, false);
            require(node.superName.equals("java/lang/Object") && node.interfaces.size() == 1
                && nodes.containsKey(node.interfaces.getFirst()) && node.fields.isEmpty() && node.methods.size() == 3,
                "EVENT_PROVIDER_STRUCTURE");
            emptyConstructor(node);
            String service = resources.apply("META-INF/services/" + node.interfaces.getFirst().replace('/', '.'));
            require(service != null && service.lines().map(s -> s.split("#", 2)[0].trim()).filter(s -> !s.isEmpty())
                .toList().equals(List.of(node.name.replace('/', '.'))), "SERVICE_PROVIDER_IDENTITY");
            for (MethodNode method : node.methods) {
                if (method.name.equals("<init>")) continue;
                metadata(method);
                require(method.access == Opcodes.ACC_PUBLIC && method.desc.equals(VALUE_CALL), "PUBLISHER_SIGNATURE");
                var c = ops(method);
                require(c.size() == 13 && c.get(0) instanceof TypeInsnNode && c.get(1).getOpcode() == Opcodes.DUP
                    && load(c.get(2), Opcodes.ALOAD, 1) && load(c.get(3), Opcodes.ILOAD, 2), "PUBLISHER_BODY");
                String event = ((TypeInsnNode) c.get(0)).desc;
                require(type(c.get(0), Opcodes.NEW, event) && nodes.containsKey(event)
                    && !event.equals(entry) && !event.equals(field.owner) && events.add(event), "CUSTOM_EVENT_OWNER");
                require(call(c.get(4), Opcodes.INVOKESPECIAL, event, "<init>", VALUE_CTOR)
                    && load(c.get(5), Opcodes.ASTORE, 3) && bus(c.get(6)) && load(c.get(7), Opcodes.ALOAD, 3)
                    && post(c.get(8)) && c.get(9).getOpcode() == Opcodes.POP && load(c.get(10), Opcodes.ALOAD, 3)
                    && call(c.get(11), Opcodes.INVOKESTATIC, "com/mojang/datafixers/util/Either", "left",
                        "(Ljava/lang/Object;)Lcom/mojang/datafixers/util/Either;")
                    && c.get(12).getOpcode() == Opcodes.ARETURN, "PUBLISHER_DATAFLOW");
            }
        }
        require(provider != null && events.size() == 2, "MUTABLE_EVENT_PAIR_REQUIRED");
        for (String event : events) {
            mutableEvent(nodes.get(event));
            require(nodes.containsKey(nodes.get(event).interfaces.getFirst()), "EVENT_INTERFACE_OWNER");
        }
        for (ClassNode node : nodes.values()) {
            Set<String> allowed = node.name.equals(entry) ? Set.of(SOURCE_BUS, SOURCE_EVENT, SOURCE_GLOBAL, SOURCE_PICKUP,
                SOURCE_PICKUP.substring(0, SOURCE_PICKUP.indexOf('$')), "net/minecraftforge/fml/common/Mod")
                : node.name.equals(provider) ? Set.of(SOURCE_BUS, SOURCE_EVENT, SOURCE_GLOBAL)
                : events.contains(node.name) ? Set.of(SOURCE_EVENT) : Set.of();
            require(allowed.containsAll(references(raw.get(node.name))), "UNSUPPORTED_FORGE_CLOSURE " + node.name);
            require(!events.contains(node.superName), "UNVERIFIED_EVENT_SUBCLASS");
            // Only the initializer and the validated common <clinit> may assign the predicate.
            for (MethodNode method : node.methods) for (var op : method.instructions) {
                if (op instanceof FieldInsnNode use && use.owner.equals(field.owner) && use.name.equals(field.name))
                    require(use.desc.equals(PREDICATE) && (use.getOpcode() == Opcodes.GETSTATIC
                        || use.getOpcode() == Opcodes.PUTSTATIC && (node.name.equals(entry)
                        || node.name.equals(field.owner) && method.name.equals("<clinit>"))), "FOREIGN_PREDICATE_WRITE");
                if (op instanceof MethodInsnNode call) {
                    eventMember(nodes, events, node.name, call.owner, call.name, call.desc, true);
                    require(!call.owner.startsWith("java/lang/reflect/")
                        && !call.owner.equals("java/lang/invoke/MethodHandles$Lookup")
                        && !(call.owner.equals("java/lang/Class") && Set.of("forName", "getMethod", "getDeclaredMethod",
                            "getField", "getDeclaredField", "getMethods", "getDeclaredMethods", "getFields",
                            "getDeclaredFields", "getConstructor", "getDeclaredConstructor", "getConstructors",
                            "getDeclaredConstructors", "newInstance").contains(call.name)),
                        "DYNAMIC_EVENT_ACCESS_UNSUPPORTED");
                }
                if (op instanceof FieldInsnNode use) eventMember(nodes, events, node.name, use.owner, use.name, use.desc, false);
                if (op instanceof InvokeDynamicInsnNode dynamic) {
                    eventConstant(nodes, events, node.name, dynamic.bsm);
                    for (Object argument : dynamic.bsmArgs) eventConstant(nodes, events, node.name, argument);
                }
                if (op instanceof LdcInsnNode literal) eventConstant(nodes, events, node.name, literal.cst);
            }
        }
        verifyHost(host);
        return new ForgeEventInitializerPlan(modId, module, entry, new TreeMap<>(raw), events, provider);
    }

    private static void eventConstant(Map<String,ClassNode> nodes, Set<String> events, String caller, Object constant) {
        if (constant instanceof Handle handle) eventMember(nodes, events, caller, handle.getOwner(), handle.getName(),
            handle.getDesc(), handle.getTag() >= Opcodes.H_INVOKEVIRTUAL);
        require(!(constant instanceof ConstantDynamic), "DYNAMIC_EVENT_ACCESS_UNSUPPORTED");
    }
    private static void eventMember(Map<String,ClassNode> nodes, Set<String> events, String caller,
            String owner, String name, String desc, boolean method) {
        if (!events.contains(owner)) return;
        ClassNode event = nodes.get(owner);
        // These event classes change their superclass. A reference through the
        // concrete owner must not accidentally inherit an unimplemented Forge API.
        if (method) require(event.methods.stream().anyMatch(m -> m.name.equals(name) && m.desc.equals(desc)),
            "INHERITED_EVENT_API_UNSUPPORTED " + owner + "." + name);
        else require(caller.equals(owner) && event.fields.stream().anyMatch(f -> f.name.equals(name) && f.desc.equals(desc)),
            "EVENT_FIELD_ACCESS_UNSUPPORTED");
    }

    private static void common(ClassNode node, String field) {
        structure(node, false);
        require(node.superName.equals("java/lang/Object") && node.interfaces.isEmpty()
            && node.fields.size() == 2 && node.methods.size() == 3, "COMMON_STRUCTURE");
        FieldNode predicate = node.fields.stream().filter(f -> f.name.equals(field)).findFirst().orElseThrow();
        require(predicate.access == (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC) && predicate.desc.equals(PREDICATE)
            && ("Ljava/util/function/BiPredicate<" + PLAYER + ORB + ">;").equals(predicate.signature), "COMMON_FIELD");
        FieldNode logger = node.fields.stream().filter(f -> f != predicate).findFirst().orElseThrow();
        require(logger.access == (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)
            && logger.desc.equals("Lorg/apache/logging/log4j/Logger;") && logger.signature == null, "COMMON_LOGGER");
        emptyConstructor(node);
        MethodNode init = method(node, "<clinit>", "()V"); metadata(init);
        var code = ops(init);
        require(init.access == Opcodes.ACC_STATIC && code.size() == 6 && code.get(0) instanceof LdcInsnNode l
            && l.cst instanceof String
            && call(code.get(1), Opcodes.INVOKESTATIC, "org/apache/logging/log4j/LogManager", "getLogger",
                "(Ljava/lang/String;)Lorg/apache/logging/log4j/Logger;")
            && field(code.get(2), Opcodes.PUTSTATIC, node.name, logger.name, logger.desc)
            && code.get(3) instanceof InvokeDynamicInsnNode
            && field(code.get(4), Opcodes.PUTSTATIC, node.name, predicate.name, predicate.desc)
            && code.get(5).getOpcode() == Opcodes.RETURN, "COMMON_INITIALIZATION");
        MethodNode defaultPredicate = predicate(node, (InvokeDynamicInsnNode) code.get(3));
        var body = ops(defaultPredicate);
        require(body.size() == 2 && body.get(0).getOpcode() == Opcodes.ICONST_0
            && body.get(1).getOpcode() == Opcodes.IRETURN, "COMMON_DEFAULT_PREDICATE");
    }

    private static void mutableEvent(ClassNode node) {
        structure(node, false);
        require(node.superName.equals(SOURCE_EVENT) && node.interfaces.size() == 1
            && node.fields.size() == 2 && node.methods.size() == 4, "MUTABLE_EVENT_STRUCTURE");
        FieldNode player = node.fields.stream().filter(f -> f.desc.equals(PLAYER)).findFirst().orElseThrow();
        FieldNode value = node.fields.stream().filter(f -> f.desc.equals("I")).findFirst().orElseThrow();
        require(player.access == (Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL) && value.access == Opcodes.ACC_PRIVATE,
            "MUTABLE_EVENT_FIELDS");
        Set<String> signatures = new HashSet<>();
        for (MethodNode method : node.methods) {
            metadata(method); require(method.access == Opcodes.ACC_PUBLIC, "EVENT_METHOD_ACCESS");
            var c = ops(method);
            require(signatures.add(method.desc), "EVENT_METHOD_DUPLICATE");
            boolean valid = switch (method.desc) {
                case VALUE_CTOR -> method.name.equals("<init>") && c.size() == 9
                    && load(c.get(0), Opcodes.ALOAD, 0)
                    && call(c.get(1), Opcodes.INVOKESPECIAL, SOURCE_EVENT, "<init>", "()V")
                    && load(c.get(2), Opcodes.ALOAD, 0) && load(c.get(3), Opcodes.ALOAD, 1)
                    && field(c.get(4), Opcodes.PUTFIELD, node.name, player.name, PLAYER)
                    && load(c.get(5), Opcodes.ALOAD, 0) && load(c.get(6), Opcodes.ILOAD, 2)
                    && field(c.get(7), Opcodes.PUTFIELD, node.name, value.name, "I") && c.get(8).getOpcode() == Opcodes.RETURN;
                case "(I)V" -> c.size() == 4 && load(c.get(0), Opcodes.ALOAD, 0) && load(c.get(1), Opcodes.ILOAD, 1)
                    && field(c.get(2), Opcodes.PUTFIELD, node.name, value.name, "I") && c.get(3).getOpcode() == Opcodes.RETURN;
                case "()I" -> c.size() == 3 && load(c.get(0), Opcodes.ALOAD, 0)
                    && field(c.get(1), Opcodes.GETFIELD, node.name, value.name, "I") && c.get(2).getOpcode() == Opcodes.IRETURN;
                case "()" + PLAYER -> c.size() == 3 && load(c.get(0), Opcodes.ALOAD, 0)
                    && field(c.get(1), Opcodes.GETFIELD, node.name, player.name, PLAYER) && c.get(2).getOpcode() == Opcodes.ARETURN;
                default -> false;
            };
            require(valid, "MUTABLE_EVENT_BODY");
        }
    }

    private static MethodNode predicate(ClassNode owner, InvokeDynamicInsnNode dynamic) {
        require(dynamic.name.equals("test") && dynamic.desc.equals("()" + PREDICATE)
            && dynamic.bsm.equals(ForgeRegistryTransformationPlan.META) && dynamic.bsmArgs.length == 3
            && dynamic.bsmArgs[0].equals(Type.getMethodType("(Ljava/lang/Object;Ljava/lang/Object;)Z"))
            && dynamic.bsmArgs[1] instanceof Handle && dynamic.bsmArgs[2].equals(Type.getMethodType(PICKUP_DESC)), "LAMBDA_BOOTSTRAP");
        Handle impl = (Handle) dynamic.bsmArgs[1];
        require(impl.getTag() == Opcodes.H_INVOKESTATIC && !impl.isInterface()
            && impl.getOwner().equals(owner.name) && impl.getDesc().equals(PICKUP_DESC), "LAMBDA_OWNER");
        MethodNode method = method(owner, impl.getName(), impl.getDesc()); metadata(method);
        require(method.access == (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC), "LAMBDA_FLAGS");
        return method;
    }

    private static void verifyHost(Function<String,byte[]> host) {
        ControlledForgeGlobalBus.verifyHostBus();
        Map<String,String> bus = Map.of("net/neoforged/bus/api/Event", "fb3fff27d3ca55f205a1a50e2caf5e9f8c7906eb68e2bfd7a0101272fce23f43",
            "net/neoforged/bus/api/ICancellableEvent", "3791d961e1b27960d93cd09f3dad7a979643a13a679970ea13811c0351ee5d6d");
        for (var expected : bus.entrySet()) {
            try (var in = net.neoforged.bus.api.Event.class.getResourceAsStream("/" + expected.getKey() + ".class")) {
                require(in != null && digest(in.readAllBytes()).equals(expected.getValue()), "UNVERIFIED_EVENT_BUS_ABI");
            } catch (IOException failure) { throw new IllegalStateException("Cannot inspect event bus ABI", failure); }
        }
        HOST_SHA.forEach((name, expected) -> {
            byte[] actual = host.apply(name);
            require(actual != null && digest(actual).equals(expected), "UNVERIFIED_EVENT_SEMANTICS " + name);
        });
    }

    Set<String> targets() { return output.keySet(); }
    ClassNode transform(ClassNode node, byte[] initialSHA) {
        require(normalized.containsKey(node.name), "UNAUTHORIZED_TARGET");
        require(initialSHA == null || Arrays.equals(initialSHA, hexDigest(original.get(node.name))), "INPUT_JAR_SHA_CHANGED");
        require(Arrays.equals(bytes(node), normalized.get(node.name)), "TRANSFORMATION_INPUT_CHANGED");
        return read(output.get(node.name));
    }
    private static byte[] hexDigest(byte[] bytes) {
        return java.util.HexFormat.of().parseHex(digest(bytes));
    }
    private static boolean platform(String name) {
        return name.startsWith("java/") || name.startsWith("javax/") || name.startsWith("net/minecraft/")
            || name.startsWith("net/minecraftforge/") || name.startsWith("net/neoforged/")
            || name.startsWith("io/github/gfander/dualforge/");
    }
    private static Set<String> references(byte[] bytes) {
        Set<String> found = new HashSet<>();
        var matches = FORGE.matcher(new String(bytes, StandardCharsets.ISO_8859_1));
        while (matches.find()) found.add(matches.group());
        return found;
    }
    private static void structure(ClassNode node, boolean entry) {
        RegistryBytecodeVerifier.verify(node);
        require(node.access == (Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER)
            || node.access == (Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER | Opcodes.ACC_FINAL), "CLASS_FLAGS");
        require(node.signature == null && node.outerClass == null && node.nestHostClass == null
            && empty(node.nestMembers) && empty(node.permittedSubclasses) && empty(node.recordComponents)
            && empty(node.invisibleAnnotations) && empty(node.visibleTypeAnnotations) && empty(node.invisibleTypeAnnotations)
            && empty(node.attrs) && (entry || empty(node.visibleAnnotations)), "CLASS_METADATA");
        Set<String> members = new HashSet<>();
        for (FieldNode field : node.fields) require(members.add(field.name) && field.value == null
            && empty(field.visibleAnnotations) && empty(field.invisibleAnnotations)
            && empty(field.visibleTypeAnnotations) && empty(field.invisibleTypeAnnotations) && empty(field.attrs), "FIELD_METADATA");
        members.clear();
        for (MethodNode method : node.methods) require(members.add(method.name + method.desc), "DUPLICATE_METHOD");
    }
    private static void metadata(MethodNode method) {
        require(empty(method.exceptions) && method.tryCatchBlocks.isEmpty()
            && empty(method.visibleAnnotations) && empty(method.invisibleAnnotations)
            && empty(method.visibleTypeAnnotations) && empty(method.invisibleTypeAnnotations)
            && method.visibleParameterAnnotations == null && method.invisibleParameterAnnotations == null
            && empty(method.visibleLocalVariableAnnotations) && empty(method.invisibleLocalVariableAnnotations)
            && method.annotationDefault == null && empty(method.attrs), "METHOD_METADATA");
        for (var op : method.instructions) require(!(op instanceof FrameNode)
            && empty(op.visibleTypeAnnotations) && empty(op.invisibleTypeAnnotations), "METHOD_CONTROL_FLOW");
    }
    private static void emptyConstructor(ClassNode node) {
        MethodNode ctor = method(node, "<init>", "()V"); metadata(ctor);
        var code = ops(ctor);
        require(ctor.access == Opcodes.ACC_PUBLIC && code.size() == 3 && load(code.get(0), Opcodes.ALOAD, 0)
            && call(code.get(1), Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V")
            && code.get(2).getOpcode() == Opcodes.RETURN, "EMPTY_CONSTRUCTOR");
    }
    private static MethodNode method(ClassNode node, String name, String desc) {
        return node.methods.stream().filter(m -> m.name.equals(name) && m.desc.equals(desc)).findFirst()
            .orElseThrow(() -> new IllegalStateException("Event initializer rejected: MISSING_METHOD " + name + desc));
    }
    private static boolean bus(AbstractInsnNode op) { return field(op, Opcodes.GETSTATIC, SOURCE_GLOBAL, "EVENT_BUS", "L" + SOURCE_BUS + ";"); }
    private static boolean post(AbstractInsnNode op) { return call(op, Opcodes.INVOKEINTERFACE, SOURCE_BUS, "post", "(L" + SOURCE_EVENT + ";)Z"); }
    private static boolean field(AbstractInsnNode op, int code, String owner, String name, String desc) {
        return op instanceof FieldInsnNode f && f.getOpcode() == code && f.owner.equals(owner) && f.name.equals(name) && f.desc.equals(desc);
    }
    private static boolean load(AbstractInsnNode op, int code, int local) { return op instanceof VarInsnNode v && v.getOpcode() == code && v.var == local; }
    private static boolean type(AbstractInsnNode op, int code, String name) { return op instanceof TypeInsnNode t && t.getOpcode() == code && t.desc.equals(name); }
    private static boolean call(AbstractInsnNode op, int code, String owner, String name, String desc) {
        return op instanceof MethodInsnNode m && m.getOpcode() == code && m.itf == (code == Opcodes.INVOKEINTERFACE)
            && m.owner.equals(owner) && m.name.equals(name) && m.desc.equals(desc);
    }
    private static List<AbstractInsnNode> ops(MethodNode method) { return ForgeRegistryTransformationPlan.ops(method); }
    private static boolean empty(Collection<?> values) { return values == null || values.isEmpty(); }
    private static byte[] bytes(ClassNode node) { return ForgeRegistryTransformationPlan.bytes(node); }
    private static ClassNode read(byte[] bytes) { return ForgeRegistryTransformationPlan.read(bytes); }
    private static String digest(byte[] bytes) { return ForgeRegistryTransformationPlan.digest(bytes); }
    private static void require(boolean condition, String reason) { if (!condition) throw new IllegalStateException("Event initializer rejected: " + reason); }
}
