package io.github.gfander.dualforge;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Exact member/descriptor/access checks against official client files, plus JVM
 * verification without class initialization. Neither check applies a GUI Mixin. */
public final class ClientLinkContract {
    private static final Map<String,ClassNode> own = new TreeMap<>(), host = new HashMap<>();
    private static final Map<String,String[]> providers = new HashMap<>();
    private static int links, mixinLinks, verified;
    public static void main(String[] args) throws Exception {
        Path original = Path.of(args[0]), transformed = Path.of(args[1]);
        for (String line : Files.readAllLines(Path.of(args[2]))) { String[] values = line.split("\t"); providers.put(values[0], values); }
        try (var jar = new JarFile(original.toFile())) {
            for (var e : jar.stream().filter(e -> e.getName().endsWith(".class")).toList()) {
                String name = e.getName().substring(0, e.getName().length() - 6);
                byte[] bytes = Files.isRegularFile(transformed.resolve(e.getName())) ? Files.readAllBytes(transformed.resolve(e.getName())) : jar.getInputStream(e).readAllBytes();
                own.put(name, read(bytes));
            }
        }
        for (var node : own.values()) for (var method : node.methods) for (var op : method.instructions) {
            if (op instanceof MethodInsnNode c) member(node.name, c.owner, c.name, c.desc, true, c.getOpcode() == Opcodes.INVOKESTATIC);
            if (op instanceof FieldInsnNode f) member(node.name, f.owner, f.name, f.desc, false, f.getOpcode() == Opcodes.GETSTATIC || f.getOpcode() == Opcodes.PUTSTATIC);
            if (op instanceof InvokeDynamicInsnNode d) for (Object value : d.bsmArgs) if (value instanceof Handle h)
                member(node.name, h.getOwner(), h.getName(), h.getDesc(), h.getTag() >= Opcodes.H_INVOKEVIRTUAL,
                    h.getTag() == Opcodes.H_INVOKESTATIC || h.getTag() == Opcodes.H_GETSTATIC || h.getTag() == Opcodes.H_PUTSTATIC);
        }
        ClassNode accessor = own.get("yalter/mousetweaks/mixin/AbstractContainerScreenAccessor");
        if (accessor == null) throw new AssertionError("Original accessor missing");
        String target = "net/minecraft/client/gui/screens/inventory/AbstractContainerScreen";
        for (MethodNode m : accessor.methods) {
            AnnotationNode annotation = m.visibleAnnotations.getFirst();
            String name = (String) annotation.values.get(1);
            if (annotation.desc.endsWith("/Invoker;")) require(find(target, name, m.desc, true, new HashSet<>()) != null, "Mixin invoker " + name);
            else {
                Type[] params = Type.getArgumentTypes(m.desc);
                String desc = params.length == 0 ? Type.getReturnType(m.desc).getDescriptor() : params[0].getDescriptor();
                require(find(target, name, desc, false, new HashSet<>()) != null, "Mixin accessor " + name);
            }
            mixinLinks++;
        }
        // Check lifecycle order on the actual host client patch, not the reference source.
        ClassNode minecraft = node("net/minecraft/client/Minecraft");
        boolean lifecycle = false;
        for (var m : minecraft.methods) if (m.name.equals("<init>")) {
            boolean instanceAssigned = false;
            for (var op : m.instructions) {
                if (op instanceof FieldInsnNode f && f.getOpcode() == Opcodes.PUTSTATIC
                    && f.owner.equals(minecraft.name) && f.desc.equals("L" + minecraft.name + ";")) instanceAssigned = true;
                if (op instanceof MethodInsnNode c && c.owner.equals("net/neoforged/neoforge/client/loading/ClientModLoader") && c.name.equals("begin")) {
                    require(instanceAssigned, "Minecraft instance assigned before client mod loading"); lifecycle = true;
                }
            }
        }
        require(lifecycle, "actual client loading call located");
        try (var loader = new URLClassLoader(new URL[]{transformed.toUri().toURL(), original.toUri().toURL()}, ClientLinkContract.class.getClassLoader())) {
            for (String name : own.keySet()) {
                Class<?> type = Class.forName(name.replace('/', '.'), false, loader);
                type.getDeclaredMethods(); type.getDeclaredConstructors(); verified++;
            }
        }
        System.out.println("CLIENT LINK CONTRACT PASS members=" + links + " mixinMembers=" + mixinLinks + " jvmVerifiedNoInit=" + verified
            + "; CLIENT_LIFECYCLE_ORDER_STATIC=PASS; MIXIN_APPLICATION=NOT_RUN; GUI=NOT_RUN");
    }

    private record Found(String owner, int access) { }
    private static void member(String caller, String owner, String name, String desc, boolean method, boolean isStatic) {
        if (!own.containsKey(owner) && !owner.startsWith("net/minecraft/") && !owner.startsWith("net/neoforged/")
            && !owner.startsWith("io/github/gfander/dualforge/")) return;
        Found found = find(owner, name, desc, method, new HashSet<>());
        require(found != null, "Missing member " + caller + " -> " + owner + "." + name + desc);
        require(((found.access & Opcodes.ACC_STATIC) != 0) == isStatic, "Static member kind " + owner + "." + name);
        boolean accessible = (found.access & Opcodes.ACC_PUBLIC) != 0 || caller.equals(found.owner)
            || nest(caller).equals(nest(found.owner))
            || (found.access & Opcodes.ACC_PRIVATE) == 0 && (pkg(caller).equals(pkg(found.owner))
                || (found.access & Opcodes.ACC_PROTECTED) != 0 && subtype(caller, found.owner, new HashSet<>()));
        require(accessible, "Inaccessible member " + caller + " -> " + found.owner + "." + name + desc);
        links++;
    }
    private static Found find(String owner, String name, String desc, boolean method, Set<String> seen) {
        if (owner == null || !seen.add(owner)) return null; ClassNode n = node(owner);
        if (method) { for (var m : n.methods) if (m.name.equals(name) && m.desc.equals(desc)) return new Found(owner, m.access); }
        else for (var f : n.fields) if (f.name.equals(name) && f.desc.equals(desc)) return new Found(owner, f.access);
        if (name.equals("<init>")) return null;
        Found parent = find(n.superName, name, desc, method, seen); if (parent != null) return parent;
        for (String iface : n.interfaces) { Found value = find(iface, name, desc, method, seen); if (value != null) return value; }
        return null;
    }
    private static boolean subtype(String child, String parent, Set<String> seen) {
        if (child == null || !seen.add(child)) return false; if (child.equals(parent)) return true;
        ClassNode n = node(child); if (subtype(n.superName, parent, seen)) return true;
        return n.interfaces.stream().anyMatch(i -> subtype(i, parent, seen));
    }
    private static String nest(String name) { ClassNode n = node(name); return n.nestHostClass == null ? name : n.nestHostClass; }
    private static String pkg(String name) { return name.substring(0, name.lastIndexOf('/') + 1); }
    private static ClassNode node(String name) {
        if (own.containsKey(name)) return own.get(name);
        if (host.containsKey(name)) return host.get(name);
        byte[] bytes;
        try {
            if (name.startsWith("java/") || name.startsWith("javax/")) {
                try (var in = ClassLoader.getSystemResourceAsStream(name + ".class")) { if (in == null) throw new FileNotFoundException(name); bytes = in.readAllBytes(); }
            } else {
                String[] provider = providers.get(name); if (provider == null) throw new FileNotFoundException(name);
                try (var jar = new JarFile(provider[1])) { bytes = jar.getInputStream(jar.getJarEntry(provider[2])).readAllBytes(); }
            }
        } catch (IOException failure) { throw new IllegalStateException("Cannot read host " + name, failure); }
        ClassNode n = read(bytes); host.put(name, n); return n;
    }
    private static ClassNode read(byte[] b) { ClassNode n = new ClassNode(); new ClassReader(b).accept(n, 0); return n; }
    private static void require(boolean value, String why) { if (!value) throw new AssertionError(why); }
}
