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

/** Finite client constructor/listener ABI. Unchanged mod business is not a security sandbox. */
final class ForgeClientEventPlan {
    static final String SOURCE = "net/minecraftforge/client/event/ScreenEvent";
    static final String HOST = "net/neoforged/neoforge/client/event/ScreenEvent";
    static final String GLOBAL = "net/minecraftforge/common/MinecraftForge";
    static final String BUS = "net/minecraftforge/eventbus/api/IEventBus";
    static final String SUBSCRIBE = "Lnet/minecraftforge/eventbus/api/SubscribeEvent;";
    static final String SCREEN = "Lnet/minecraft/client/gui/screens/Screen;";
    static final String FACTORY = "net/neoforged/neoforge/client/gui/IConfigScreenFactory";
    static final String BRIDGE = "io/github/gfander/dualforge/ForgeClientEventBridge";
    static final String ADAPTER = "dualforge$configFactory";
    static final List<String> EVENTS = List.of("$MouseButtonPressed$Pre", "$MouseButtonReleased$Pre",
        "$MouseDragged$Pre", "$MouseScrolled$Post");
    private static final Pattern FORGE = Pattern.compile("net/minecraftforge/[A-Za-z0-9_/$]+");
    private static final Map<String,String> HOST_SHA = Map.ofEntries(
        Map.entry(HOST, "9dd50de21470e362aef2fec524191ccafa1ecc3436d91db7f8b80926de3a408a"),
        Map.entry(HOST + "$MouseInput", "949dae449631c1c8169c7cd1167e00de29e6145fe558859169d020f7e0c918dc"),
        Map.entry(HOST + "$MouseButtonPressed", "773b95ef1701db7044f2141a8ef95d659299c911bab4c796cf7af7f6ecfc5556"),
        Map.entry(HOST + "$MouseButtonPressed$Pre", "50641d7e61e7af2bd9a29ac1687e91daba0bf779218cb6dd907e8c474619fadd"),
        Map.entry(HOST + "$MouseButtonReleased", "598b63b4bc5ff248efb610987ff6a18edceba8dc282c78bc8cdfaa01166e5bdc"),
        Map.entry(HOST + "$MouseButtonReleased$Pre", "0753508f4199ce838cf199184cbefa96c6358718d0cbf3b158076aeddcf61bbf"),
        Map.entry(HOST + "$MouseDragged", "11b430a49411908e11dc46f7950ba5892175a9f7f1bede2be08286b18f36efd8"),
        Map.entry(HOST + "$MouseDragged$Pre", "2a899637eb052d1bab5426072c4e1e087e53853508486f8d552c31815f0e71bf"),
        Map.entry(HOST + "$MouseScrolled", "b6cdf43936f20d941e698c604bbacff797cbc4cca669c1ba30d58ca5517c9139"),
        Map.entry(HOST + "$MouseScrolled$Post", "27b5479b503cc97fb0abbac8cee3b30502f1de64768d7efa51ab8c3842f1374c"),
        Map.entry(FACTORY, "3302ed37db4f7c35522c3bb3d9016cffe9a1e3a8ad593fce7f6a1a41a087722f"),
        Map.entry("net/neoforged/neoforge/client/ClientHooks", "a4f8d6145b6e98628a7ccccde23d9e09deb8ad785b627e4c68aa5f05e2bfab46"));
    final String modId, module, entry, fingerprint;
    final Map<String,String> listeners;
    private final Set<String> closure;
    private final byte[] original, normalized, output;

    private ForgeClientEventPlan(String modId, String module, String entry, Map<String,byte[]> raw,
            Map<String,String> listeners) {
        this.modId = modId; this.module = module; this.entry = entry;
        this.listeners = Map.copyOf(listeners); original = raw.get(entry).clone();
        closure = Set.copyOf(raw.keySet());
        StringBuilder identity = new StringBuilder("client-events-v1\n" + modId + "\n" + module + "\n");
        new TreeMap<>(raw).forEach((name, bytes) -> identity.append(name).append(':').append(digest(bytes)).append('\n'));
        fingerprint = digest(identity.toString().getBytes(StandardCharsets.UTF_8));
        ClassNode node = read(original); normalized = bytes(node);
        MethodNode ctor = method(node, "<init>", "()V");
        var init = ops(ctor).get(1);
        InsnList guard = new InsnList(); guard.add(new VarInsnNode(Opcodes.ALOAD, 0));
        guard.add(new LdcInsnNode(fingerprint)); guard.add(bridge("begin")); ctor.instructions.insert(init, guard);
        for (var op : ctor.instructions.toArray()) {
            if (op instanceof FieldInsnNode f && f.owner.equals(GLOBAL)) ctor.instructions.remove(op);
            if (op instanceof MethodInsnNode m && m.owner.equals(BUS)) {
                ctor.instructions.insertBefore(op, new LdcInsnNode(fingerprint));
                ctor.instructions.set(op, bridge("register"));
            }
            if (op instanceof MethodInsnNode m && m.owner.equals(GLOBAL)) {
                InsnList adapt = new InsnList();
                adapt.add(new InvokeDynamicInsnNode("createScreen", "(Ljava/util/function/Function;)L" + FACTORY + ";",
                    ForgeRegistryTransformationPlan.META, Type.getMethodType("(Lnet/neoforged/fml/ModContainer;" + SCREEN + ")" + SCREEN),
                    new Handle(Opcodes.H_INVOKESTATIC, entry, ADAPTER,
                        "(Ljava/util/function/Function;Lnet/neoforged/fml/ModContainer;" + SCREEN + ")" + SCREEN, false),
                    Type.getMethodType("(Lnet/neoforged/fml/ModContainer;" + SCREEN + ")" + SCREEN)));
                adapt.add(new LdcInsnNode(fingerprint)); ctor.instructions.insertBefore(op, adapt);
                ctor.instructions.set(op, bridge("configure"));
            }
        }
        for (MethodNode m : node.methods) {
            m.desc = mapped(m.desc);
            if (m.localVariables != null) for (var local : m.localVariables) local.desc = mapped(local.desc);
            if (m.visibleAnnotations != null) for (var ann : m.visibleAnnotations)
                if (ann.desc.equals(SUBSCRIBE)) ann.desc = "Lnet/neoforged/bus/api/SubscribeEvent;";
            for (var op : m.instructions) if (op instanceof MethodInsnNode call && call.owner.startsWith(SOURCE + "$")) {
                call.owner = mapped(call.owner);
                if (call.name.equals("getDeltaY")) call.name = "getScrollDeltaY";
                if (call.name.equals("setCanceled")) {
                    call.owner = "net/neoforged/bus/api/ICancellableEvent";
                    call.setOpcode(Opcodes.INVOKEINTERFACE); call.itf = true;
                }
            }
        }
        for (var inner : node.innerClasses) { inner.name = mapped(inner.name); inner.outerName = mapped(inner.outerName); }
        MethodNode adapter = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
            ADAPTER, "(Ljava/util/function/Function;Lnet/neoforged/fml/ModContainer;" + SCREEN + ")" + SCREEN, null, null);
        adapter.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        adapter.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        adapter.instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/function/Function", "apply",
            "(Ljava/lang/Object;)Ljava/lang/Object;", true));
        adapter.instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, "net/minecraft/client/gui/screens/Screen"));
        adapter.instructions.add(new InsnNode(Opcodes.ARETURN)); adapter.maxStack = 2; adapter.maxLocals = 3;
        node.methods.add(adapter); ctor.maxStack = Math.max(ctor.maxStack, 2);
        RegistryBytecodeVerifier.verify(node); output = bytes(node);
    }

    static boolean isCandidate(ClassNode node) {
        return node.methods.stream().filter(m -> m.name.equals("<init>")).flatMap(m -> Arrays.stream(m.instructions.toArray()))
            .anyMatch(op -> op instanceof MethodInsnNode c && c.owner.equals(GLOBAL) && c.name.equals("registerConfigScreen"));
    }

    static ForgeClientEventPlan create(IModInfo info, String entry) {
        var file = info.getOwningFile(); var scan = file.getFile().getScanResult();
        require(scan != null && ForgeModLocator.isForgeCandidate(file.getFile().getFilePath()), "SOURCE_OWNER");
        require(!Files.exists(file.getFile().findResource("module-info.class"))
            && !Files.exists(file.getFile().findResource("META-INF/versions")), "EXPLICIT_OR_MULTIRELEASE_MODULE");
        require(file.getMods().size() == 1 && file.getMods().getFirst() == info, "METADATA_OWNER");
        require(DualForgeLanguageLoader.forgeEntrypoints(info, scan).equals(List.of(entry.replace('/', '.'))), "ENTRY_OWNER");
        require(scan.getAnnotations().stream().filter(a -> a.targetType() == java.lang.annotation.ElementType.TYPE
            && a.annotationType().getClassName().equals("net.minecraftforge.fml.common.Mod")).count() == 1, "ENTRY_COUNT");
        Map<String,byte[]> raw = new TreeMap<>();
        try {
            for (var c : scan.getClasses()) require(raw.putIfAbsent(c.clazz().getInternalName(),
                Files.readAllBytes(file.getFile().findResource(c.clazz().getInternalName() + ".class"))) == null, "DUPLICATE_CLASS");
        } catch (IOException failure) { throw new IllegalStateException("Cannot read client event closure", failure); }
        return validate(info.getModId(), file.moduleName(), entry, raw,
            net.neoforged.fml.loading.FMLEnvironment.dist != null && net.neoforged.fml.loading.FMLEnvironment.dist.isClient(),
            ControlledItemSupplier::hostResource);
    }

    static ForgeClientEventPlan validate(String modId, String module, String entry, Map<String,byte[]> raw,
            boolean client, Function<String,byte[]> host) {
        require(client, "CLIENT_DIST_REQUIRED");
        require(modId != null && !modId.isBlank() && module != null && !module.isBlank(), "OWNER_IDENTITY");
        require(!raw.containsKey("module-info"), "EXPLICIT_OR_MULTIRELEASE_MODULE");
        Map<String,ClassNode> nodes = new TreeMap<>();
        for (var row : raw.entrySet()) {
            ClassNode n = read(row.getValue()); require(n.name.equals(row.getKey()) && !platform(n.name), "CLASS_OWNER"); nodes.put(n.name, n);
        }
        ClassNode node = nodes.get(entry); require(node != null, "ENTRY_MISSING");
        structure(node);
        require(node.superName.equals("java/lang/Object") && node.interfaces.isEmpty() && node.fields.isEmpty()
            && node.methods.size() == 5 && node.signature == null, "ENTRY_STRUCTURE");
        require(node.visibleAnnotations != null && node.visibleAnnotations.size() == 1
            && node.visibleAnnotations.getFirst().desc.equals("Lnet/minecraftforge/fml/common/Mod;")
            && List.of("value", modId).equals(node.visibleAnnotations.getFirst().values), "ENTRY_ANNOTATION_OWNER");
        MethodNode ctor = method(node, "<init>", "()V"); metadata(ctor, false);
        require(ctor.access == Opcodes.ACC_PUBLIC, "CONSTRUCTOR_ACCESS");
        Cursor c = new Cursor(ctor); c.var(Opcodes.ALOAD, 0); c.call(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
        MethodInsnNode initialize = c.ownedCall(nodes, "()V");
        ClassNode main = nodes.get(initialize.owner); initializer(main, initialize.name, nodes);
        c.field(Opcodes.GETSTATIC, GLOBAL, "EVENT_BUS", "L" + BUS + ";"); c.var(Opcodes.ALOAD, 0);
        c.call(Opcodes.INVOKEINTERFACE, BUS, "register", "(Ljava/lang/Object;)V");
        AbstractInsnNode factory = c.next(); require(factory instanceof InvokeDynamicInsnNode, "CONFIG_FACTORY_BOOTSTRAP");
        configFactory((InvokeDynamicInsnNode) factory, nodes);
        c.call(Opcodes.INVOKESTATIC, GLOBAL, "registerConfigScreen", "(Ljava/util/function/Function;)V"); c.op(Opcodes.RETURN); c.end();
        Map<String,String> listeners = new TreeMap<>(); Set<String> business = new HashSet<>(); String buttonOwner = null;
        for (MethodNode m : node.methods) {
            if (m == ctor) continue;
            metadata(m, true);
            require(m.access == Opcodes.ACC_PUBLIC && m.signature == null && m.visibleAnnotations != null
                && m.visibleAnnotations.size() == 1 && m.visibleAnnotations.getFirst().desc.equals(SUBSCRIBE)
                && empty(m.visibleAnnotations.getFirst().values), "LISTENER_ANNOTATION");
            String event = null;
            for (String suffix : EVENTS) if (m.desc.equals("(L" + SOURCE + suffix + ";)V")) event = suffix;
            require(event != null && !listeners.containsValue(event), "EVENT_SIGNATURE_OR_DUPLICATE");
            require(!m.name.equals(ADAPTER), "ADAPTER_NAME_CONFLICT");
            String button = listener(m, event, main.name, nodes, business);
            if (button != null) { require(buttonOwner == null || buttonOwner.equals(button), "BUTTON_IDENTITY"); buttonOwner = button; }
            listeners.put(m.name, event);
        }
        require(listeners.size() == 4 && new HashSet<>(listeners.values()).equals(new HashSet<>(EVENTS)), "FOUR_EVENTS_REQUIRED");
        Set<String> allowed = new HashSet<>(List.of(GLOBAL, BUS, "net/minecraftforge/fml/common/Mod",
            "net/minecraftforge/eventbus/api/SubscribeEvent", SOURCE));
        for (String suffix : EVENTS) { allowed.add(SOURCE + suffix); allowed.add(SOURCE + suffix.substring(0, suffix.lastIndexOf('$'))); }
        for (var row : raw.entrySet()) {
            var match = FORGE.matcher(new String(row.getValue(), StandardCharsets.ISO_8859_1));
            while (match.find()) require(row.getKey().equals(entry) && allowed.contains(match.group()), "UNSUPPORTED_FORGE_CLOSURE " + row.getKey());
        }
        ControlledForgeGlobalBus.verifyHostBus();
        HOST_SHA.forEach((name, expected) -> {
            byte[] actual = host.apply(name); require(actual != null && digest(actual).equals(expected), "UNVERIFIED_CLIENT_HOST_ABI " + name);
        });
        return new ForgeClientEventPlan(modId, module, entry, raw, listeners);
    }

    private static String listener(MethodNode m, String suffix, String main, Map<String,ClassNode> nodes, Set<String> business) {
        Cursor c = new Cursor(m); String event = SOURCE + suffix;
        boolean scroll = suffix.equals("$MouseScrolled$Post"), drag = suffix.equals("$MouseDragged$Pre");
        String buttonGetter = drag ? "getMouseButton" : "getButton";
        c.getter(event, scroll ? "getDeltaY" : buttonGetter, scroll ? "()D" : "()I");
        if (drag) { c.getter(event, "getDragX", "()D"); c.getter(event, "getDragY", "()D"); }
        concat(c.next(), scroll ? "(D)Ljava/lang/String;" : drag ? "(IDD)Ljava/lang/String;" : "(I)Ljava/lang/String;");
        c.ownedCall(nodes, "(Ljava/lang/String;)V");
        String button = null; LabelNode exit = null;
        if (!scroll) {
            c.getter(event, buttonGetter, "()I");
            AbstractInsnNode op = c.next(); require(op instanceof MethodInsnNode, "BUTTON_CONVERTER");
            MethodInsnNode call = (MethodInsnNode) op; Type type = Type.getReturnType(call.desc);
            require(type.getSort() == Type.OBJECT && nodes.containsKey(type.getInternalName()), "BUTTON_OWNER");
            button = type.getInternalName(); require(call.desc.equals("(I)L" + button + ";") && call.owner.equals(button)
                && call.getOpcode() == Opcodes.INVOKESTATIC && !call.itf && nodes.get(button).superName.equals("java/lang/Enum"), "BUTTON_CONVERTER");
            publicStatic(nodes.get(button), call.name, call.desc);
            c.var(Opcodes.ASTORE, 2); c.var(Opcodes.ALOAD, 2); exit = c.jump(Opcodes.IFNULL);
        }
        c.getter(event, "getScreen", "()" + SCREEN); c.getter(event, "getMouseX", "()D"); c.getter(event, "getMouseY", "()D");
        if (scroll) c.getter(event, "getDeltaY", "()D"); else c.var(Opcodes.ALOAD, 2);
        AbstractInsnNode op = c.next(); require(op instanceof MethodInsnNode, "BUSINESS_HANDLER");
        MethodInsnNode call = (MethodInsnNode) op;
        require(call.owner.equals(main) && call.getOpcode() == Opcodes.INVOKESTATIC && !call.itf
            && call.desc.equals("(" + SCREEN + "DD" + (scroll ? "D" : "L" + button + ";") + ")Z")
            && business.add(call.name + call.desc), "BUSINESS_DATAFLOW");
        publicStatic(nodes.get(main), call.name, call.desc);
        if (scroll) c.op(Opcodes.POP);
        else {
            require(c.jump(Opcodes.IFEQ) == exit, "CANCELLATION_BRANCH");
            c.var(Opcodes.ALOAD, 1); c.op(Opcodes.ICONST_1); c.call(Opcodes.INVOKEVIRTUAL, event, "setCanceled", "(Z)V");
            require(nextReal(exit) == c.peek(), "CANCELLATION_TARGET");
        }
        c.op(Opcodes.RETURN); c.end(); return button;
    }

    private static void initializer(ClassNode main, String name, Map<String,ClassNode> nodes) {
        require(main.superName.equals("java/lang/Object") && main.interfaces.isEmpty(), "INITIALIZER_CLASS");
        publicStatic(main, name, "()V"); MethodNode method = method(main, name, "()V"); metadata(method, false);
        Cursor c = new Cursor(method); c.string(); c.ownedCall(nodes, "(Ljava/lang/String;)V");
        FieldInsnNode flag = c.ownField(Opcodes.GETSTATIC, main.name, "Z"); LabelNode start = c.jump(Opcodes.IFEQ);
        declaredField(main, flag, Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC);
        c.op(Opcodes.RETURN); require(nextReal(start) == c.peek(), "INITIALIZER_GUARD");
        c.call(Opcodes.INVOKESTATIC, "net/minecraft/client/Minecraft", "getInstance", "()Lnet/minecraft/client/Minecraft;");
        FieldInsnNode mc = c.ownField(Opcodes.PUTSTATIC, main.name, "Lnet/minecraft/client/Minecraft;");
        declaredField(main, mc, Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC);
        AbstractInsnNode allocation = c.next(); require(allocation instanceof TypeInsnNode t && t.getOpcode() == Opcodes.NEW
            && nodes.containsKey(t.desc), "CONFIG_DATA_OWNER"); String config = ((TypeInsnNode) allocation).desc;
        c.op(Opcodes.DUP); c.field(Opcodes.GETSTATIC, main.name, mc.name, mc.desc);
        c.field(Opcodes.GETFIELD, "net/minecraft/client/Minecraft", "gameDirectory", "Ljava/io/File;");
        c.call(Opcodes.INVOKEVIRTUAL, "java/io/File", "getAbsolutePath", "()Ljava/lang/String;");
        c.field(Opcodes.GETSTATIC, "java/io/File", "separator", "Ljava/lang/String;");
        c.field(Opcodes.GETSTATIC, "java/io/File", "separator", "Ljava/lang/String;");
        concat(c.next(), "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
        c.call(Opcodes.INVOKESPECIAL, config, "<init>", "(Ljava/lang/String;)V");
        int access = method(nodes.get(config), "<init>", "(Ljava/lang/String;)V").access;
        require(access == Opcodes.ACC_PUBLIC || access == 0
            && main.name.substring(0, main.name.lastIndexOf('/') + 1).equals(config.substring(0, config.lastIndexOf('/') + 1)),
            "CONFIG_CONSTRUCTOR_ACCESS");
        FieldInsnNode data = c.ownField(Opcodes.PUTSTATIC, main.name, "L" + config + ";");
        declaredField(main, data, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
        c.field(Opcodes.GETSTATIC, main.name, data.name, data.desc);
        AbstractInsnNode read = c.next(); require(read instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKEVIRTUAL
            && call.owner.equals(config) && call.desc.equals("()V") && method(nodes.get(config), call.name, call.desc).access == Opcodes.ACC_PUBLIC,
            "CONFIG_READ");
        c.string(); c.ownedCall(nodes, "(Ljava/lang/String;)V"); c.op(Opcodes.ICONST_1);
        c.field(Opcodes.PUTSTATIC, main.name, flag.name, "Z"); c.op(Opcodes.RETURN); c.end();
    }

    private static void configFactory(InvokeDynamicInsnNode d, Map<String,ClassNode> nodes) {
        require(d.name.equals("apply") && d.desc.equals("()Ljava/util/function/Function;")
            && d.bsm.equals(ForgeRegistryTransformationPlan.META) && d.bsmArgs.length == 3
            && d.bsmArgs[0].equals(Type.getMethodType("(Ljava/lang/Object;)Ljava/lang/Object;"))
            && d.bsmArgs[1] instanceof Handle && d.bsmArgs[2].equals(Type.getMethodType("(" + SCREEN + ")" + SCREEN)), "CONFIG_FACTORY_BOOTSTRAP");
        Handle h = (Handle) d.bsmArgs[1];
        require(h.getTag() == Opcodes.H_NEWINVOKESPECIAL && !h.isInterface() && h.getName().equals("<init>")
            && h.getDesc().equals("(" + SCREEN + ")V") && nodes.containsKey(h.getOwner()), "CONFIG_FACTORY_OWNER");
        ClassNode screen = nodes.get(h.getOwner());
        require(screen.superName.equals("net/minecraft/client/gui/screens/Screen") && screen.interfaces.isEmpty()
            && screen.methods.stream().filter(m -> m.name.equals("<init>")).count() == 1, "CONFIG_SCREEN_STRUCTURE");
        MethodNode ctor = method(screen, "<init>", h.getDesc()); metadata(ctor, false);
        require(ctor.access == Opcodes.ACC_PUBLIC, "CONFIG_SCREEN_CONSTRUCTOR");
        Cursor c = new Cursor(ctor); c.var(Opcodes.ALOAD, 0); c.string();
        c.call(Opcodes.INVOKESTATIC, "net/minecraft/network/chat/Component", "literal",
            "(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;");
        c.call(Opcodes.INVOKESPECIAL, screen.superName, "<init>", "(Lnet/minecraft/network/chat/Component;)V");
        c.var(Opcodes.ALOAD, 0); c.var(Opcodes.ALOAD, 1); FieldInsnNode parent = c.ownField(Opcodes.PUTFIELD, screen.name, SCREEN);
        require(screen.fields.stream().anyMatch(f -> f.name.equals(parent.name) && f.desc.equals(SCREEN)
            && f.access == (Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL)), "CONFIG_SCREEN_PARENT"); c.op(Opcodes.RETURN); c.end();
    }

    Set<String> targets() { return Set.of(entry); }
    void rejectForeignClasses(Set<String> foreign) {
        require(Collections.disjoint(closure, foreign), "CROSS_MODFILE_CLASS_CONFLICT");
    }
    ClassNode transform(ClassNode node, byte[] initialSHA) {
        require(node.name.equals(entry), "UNAUTHORIZED_TARGET");
        require(initialSHA == null || Arrays.equals(initialSHA, HexFormat.of().parseHex(digest(original))), "INPUT_JAR_SHA_CHANGED");
        require(Arrays.equals(bytes(node), normalized), "TRANSFORMATION_INPUT_CHANGED"); return read(output);
    }
    private static String mapped(String value) {
        if (value == null) return null;
        for (String suffix : EVENTS) {
            String source = SOURCE + suffix; value = value.replace(source, HOST + suffix);
            String parent = suffix.substring(0, suffix.lastIndexOf('$')); value = value.replace(SOURCE + parent, HOST + parent);
        }
        return value.equals(SOURCE) ? HOST : value;
    }
    private static MethodInsnNode bridge(String name) { return new MethodInsnNode(Opcodes.INVOKESTATIC, BRIDGE, name, "(Ljava/lang/Object;Ljava/lang/String;)V", false); }
    private static void publicStatic(ClassNode owner, String name, String desc) {
        require(owner != null && method(owner, name, desc).access == (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC), "BUSINESS_METHOD_OWNER");
    }
    private static void declaredField(ClassNode owner, FieldInsnNode use, int access) {
        require(owner.fields.stream().filter(f -> f.name.equals(use.name)).count() == 1
            && owner.fields.stream().anyMatch(f -> f.name.equals(use.name) && f.desc.equals(use.desc) && f.access == access),
            "INITIALIZER_FIELD_IDENTITY");
    }
    private static void concat(AbstractInsnNode op, String desc) {
        require(op instanceof InvokeDynamicInsnNode d && d.name.equals("makeConcatWithConstants") && d.desc.equals(desc)
            && d.bsm.getTag() == Opcodes.H_INVOKESTATIC && !d.bsm.isInterface()
            && d.bsm.getOwner().equals("java/lang/invoke/StringConcatFactory") && d.bsm.getName().equals("makeConcatWithConstants")
            && d.bsm.getDesc().equals("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;")
            && d.bsmArgs.length == 1 && d.bsmArgs[0] instanceof String, "LOG_CONCAT_BOOTSTRAP");
    }
    private static void structure(ClassNode n) {
        RegistryBytecodeVerifier.verify(n);
        require(n.access == (Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER) || n.access == (Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER | Opcodes.ACC_FINAL), "CLASS_FLAGS");
        require(n.outerClass == null && n.nestHostClass == null && empty(n.nestMembers) && empty(n.permittedSubclasses)
            && empty(n.recordComponents) && empty(n.invisibleAnnotations) && empty(n.visibleTypeAnnotations)
            && empty(n.invisibleTypeAnnotations) && empty(n.attrs), "CLASS_METADATA");
        Set<String> names = new HashSet<>(); for (var m : n.methods) require(names.add(m.name + m.desc), "DUPLICATE_METHOD");
    }
    private static void metadata(MethodNode m, boolean listener) {
        require(m.signature == null && empty(m.exceptions) && m.tryCatchBlocks.isEmpty() && empty(m.invisibleAnnotations)
            && (listener || empty(m.visibleAnnotations)) && empty(m.visibleTypeAnnotations) && empty(m.invisibleTypeAnnotations)
            && m.visibleParameterAnnotations == null && m.invisibleParameterAnnotations == null
            && empty(m.visibleLocalVariableAnnotations) && empty(m.invisibleLocalVariableAnnotations)
            && m.annotationDefault == null && empty(m.attrs), "METHOD_METADATA");
        for (var op : m.instructions) require(empty(op.visibleTypeAnnotations) && empty(op.invisibleTypeAnnotations), "INSTRUCTION_METADATA");
    }
    private static boolean platform(String n) { return n.startsWith("java/") || n.startsWith("javax/") || n.startsWith("net/minecraft/")
        || n.startsWith("net/minecraftforge/") || n.startsWith("net/neoforged/") || n.startsWith("io/github/gfander/dualforge/"); }
    private static AbstractInsnNode nextReal(AbstractInsnNode n) { do { n = n.getNext(); } while (n != null && n.getOpcode() < 0); return n; }
    private static MethodNode method(ClassNode n, String name, String desc) { return n.methods.stream().filter(m -> m.name.equals(name) && m.desc.equals(desc))
        .findFirst().orElseThrow(() -> new IllegalStateException("Client event plan rejected: MISSING_METHOD " + name + desc)); }
    private static boolean empty(Collection<?> c) { return c == null || c.isEmpty(); }
    private static List<AbstractInsnNode> ops(MethodNode m) { return ForgeRegistryTransformationPlan.ops(m); }
    private static ClassNode read(byte[] b) { return ForgeRegistryTransformationPlan.read(b); }
    private static byte[] bytes(ClassNode n) { return ForgeRegistryTransformationPlan.bytes(n); }
    private static String digest(byte[] b) { return ForgeRegistryTransformationPlan.digest(b); }
    private static void require(boolean valid, String why) { if (!valid) throw new IllegalStateException("Client event plan rejected: " + why); }
    private static final class Cursor {
        final List<AbstractInsnNode> code; int pos;
        Cursor(MethodNode m) { code = ops(m); }
        AbstractInsnNode next() { require(pos < code.size(), "INCOMPLETE_BODY"); return code.get(pos++); }
        AbstractInsnNode peek() { require(pos < code.size(), "INCOMPLETE_BODY"); return code.get(pos); }
        void op(int code) { require(next().getOpcode() == code, "BODY_OPCODE"); }
        void var(int code, int slot) { require(next() instanceof VarInsnNode v && v.getOpcode() == code && v.var == slot, "BODY_LOCAL"); }
        void call(int code, String owner, String name, String desc) {
            require(next() instanceof MethodInsnNode m && m.getOpcode() == code && m.owner.equals(owner) && m.name.equals(name)
                && m.desc.equals(desc) && m.itf == (code == Opcodes.INVOKEINTERFACE || owner.equals("net/minecraft/network/chat/Component")), "BODY_CALL " + owner + "." + name);
        }
        MethodInsnNode ownedCall(Map<String,ClassNode> nodes, String desc) {
            AbstractInsnNode op = next(); require(op instanceof MethodInsnNode m && m.getOpcode() == Opcodes.INVOKESTATIC && !m.itf
                && nodes.containsKey(m.owner) && m.desc.equals(desc), "OWNED_CALL");
            MethodInsnNode call = (MethodInsnNode) op; publicStatic(nodes.get(call.owner), call.name, desc); return call;
        }
        void field(int code, String owner, String name, String desc) { require(next() instanceof FieldInsnNode f && f.getOpcode() == code
            && f.owner.equals(owner) && f.name.equals(name) && f.desc.equals(desc), "BODY_FIELD"); }
        FieldInsnNode ownField(int code, String owner, String desc) { AbstractInsnNode op = next(); require(op instanceof FieldInsnNode f
            && f.getOpcode() == code && f.owner.equals(owner) && f.desc.equals(desc), "OWNED_FIELD"); return (FieldInsnNode) op; }
        void getter(String owner, String name, String desc) { var(Opcodes.ALOAD, 1); call(Opcodes.INVOKEVIRTUAL, owner, name, desc); }
        LabelNode jump(int code) { AbstractInsnNode op = next(); require(op instanceof JumpInsnNode j && j.getOpcode() == code, "BODY_BRANCH"); return ((JumpInsnNode) op).label; }
        void string() { require(next() instanceof LdcInsnNode l && l.cst instanceof String, "BODY_STRING"); }
        void end() { require(pos == code.size(), "EXTRA_SIDE_EFFECT"); }
    }
}
