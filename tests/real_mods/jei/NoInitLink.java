package org.example.jeiresearch;

import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;

/** Classpath SDK linkage only: never initializes a JEI class or starts a client. */
public final class NoInitLink {
    public static void main(String[] args) throws Exception {
        List<String> evidence = new ArrayList<>();
        int success = 0, failed = 0;
        try (var jar = new JarFile(args[0])) {
            for (String name : jar.stream().map(e -> e.getName()).filter(n -> n.endsWith(".class"))
                    .map(n -> n.substring(0, n.length() - 6).replace('/', '.')).sorted().toList()) {
                try {
                    Class<?> type = Class.forName(name, false, NoInitLink.class.getClassLoader());
                    type.getDeclaredMethods(); type.getDeclaredFields(); type.getDeclaredConstructors();
                    evidence.add("LINKED\t" + name); success++;
                } catch (LinkageError | ClassNotFoundException failure) {
                    evidence.add("UNRESOLVED\t" + name + "\t" + failure.toString().replace('\n', ' ')); failed++;
                }
            }
        }
        Files.write(Path.of(args[1]), evidence);
        System.out.println("JEI_SDK_NO_INIT_LINK linked=" + success + " unresolved=" + failed
            + "; CLIENT_BOOT=NOT_RUN; MODULE_LAYER=NOT_RUN; AT_APPLICATION=NOT_RUN");
    }
}
