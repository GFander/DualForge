package io.github.gfander.dualforge;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

/** Configuration and diagnostic behavior; full transformation atomicity stays in the old suites. */
public final class BootstrapContractTest {
    private static final List<String> KEYS = List.of("2a.rewrite", "0g.apiRewrite",
            "0j.emptyRegistration", "0l.subscriberRewrite");
    private static final List<String> FIELDS = List.of("rewriteEnabled", "apiRewriteEnabled",
            "emptyRegistrationEnabled", "subscriberRewriteEnabled");

    public static void main(String[] args) throws Exception {
        for (String key : KEYS) System.clearProperty("dualforge." + key);
        assertFlags(new DualForgeEntryTransformer(), -1, true);
        for (int index = 0; index < KEYS.size(); index++) {
            String key = "dualforge." + KEYS.get(index);
            for (boolean value : List.of(false, true)) {
                System.setProperty(key, Boolean.toString(value));
                assertFlags(new DualForgeEntryTransformer(), index, value);
            }
            for (String value : List.of("invalid", "", "TRUE", " true", "false ")) {
                System.setProperty(key, value);
                try {
                    new DualForgeEntryTransformer();
                    throw new AssertionError("Invalid value accepted: " + key);
                } catch (IllegalStateException expected) {
                    if (!expected.getMessage().contains(key)) throw expected;
                }
            }
            System.clearProperty(key);
        }
        Path output = Path.of(args[0]);
        Files.createDirectories(output);
        ClassNode node = new ClassNode();
        node.version = Opcodes.V21;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = "sample/Diagnostics";
        node.superName = "java/lang/Object";
        var transformer = new DualForgeEntryTransformer();
        for (String property : List.of("dualforge.dump.path", "dualforge.dump.profileDir")) {
            System.clearProperty(property);
        }
        diagnostic(transformer, "dump", node);
        diagnostic(transformer, "dumpProfile", node);
        try (var files = Files.list(output)) {
            if (files.findAny().isPresent()) throw new AssertionError("Default dump created files");
        }
        System.setProperty("dualforge.dump.path", output.resolve("explicit.class").toString());
        System.setProperty("dualforge.dump.profileDir", output.resolve("profile").toString());
        diagnostic(transformer, "dump", node);
        diagnostic(transformer, "dumpProfile", node);
        for (Path path : List.of(output.resolve("explicit.class"), output.resolve("profile/Diagnostics.class"))) {
            if (!new ClassReader(Files.readAllBytes(path)).getClassName().equals(node.name))
                throw new AssertionError("Invalid explicit dump");
        }
        Path blocker = output.resolve("not-a-directory");
        Files.writeString(blocker, "blocker");
        System.setProperty("dualforge.dump.path", blocker.resolve("dump.class").toString());
        System.setProperty("dualforge.dump.profileDir", blocker.toString());
        for (String method : List.of("dump", "dumpProfile")) {
            try {
                diagnostic(transformer, method, node);
                throw new AssertionError("Dump I/O failure swallowed: " + method);
            } catch (InvocationTargetException expected) {
                if (!(expected.getCause() instanceof java.io.IOException)) throw expected;
            }
        }
        System.out.println("BOOTSTRAP CONTRACT PASS defaults=4 overrides=8 invalid=20 optionalDumps=2 ioFailures=2");
    }

    private static void assertFlags(DualForgeEntryTransformer transformer, int index, boolean value) throws Exception {
        for (int fieldIndex = 0; fieldIndex < FIELDS.size(); fieldIndex++) {
            var field = DualForgeEntryTransformer.class.getDeclaredField(FIELDS.get(fieldIndex));
            field.setAccessible(true);
            if (field.getBoolean(transformer) != (fieldIndex == index ? value : true))
                throw new AssertionError("Wrong flag: " + field.getName());
        }
    }

    private static void diagnostic(DualForgeEntryTransformer transformer, String name, ClassNode node) throws Exception {
        var method = DualForgeEntryTransformer.class.getDeclaredMethod(name, ClassNode.class);
        method.setAccessible(true);
        method.invoke(transformer, node);
    }
}
