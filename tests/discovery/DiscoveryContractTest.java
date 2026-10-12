package com.example.forgecompat;

import cpw.mods.jarhandling.JarContents;
import cpw.mods.modlauncher.api.IEnvironment;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Stream;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.moddiscovery.ModDiscoverer;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.locators.ModsFolderLocator;
import net.neoforged.fml.loading.moddiscovery.readers.JarModsDotTomlModFileReader;
import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforgespi.locating.IModFileReader;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;

/** Exercises the installed FML discovery pipeline and unchanged metadata/parser boundary. */
public final class DiscoveryContractTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static final class Context implements ILaunchContext {
        final Set<Path> located = new HashSet<>();
        final List<IModFileReader> readers = List.of(new ForgeModReader(), new JarModsDotTomlModFileReader());
        public IEnvironment environment() { return null; }
        public List<String> modLists() { return List.of(); }
        public List<String> mods() { return List.of(); }
        public List<String> mavenRoots() { return List.of(); }
        public boolean isLocated(Path path) { return located.contains(path); }
        public boolean addLocated(Path path) { return located.add(path); }
        @SuppressWarnings("unchecked")
        public <T> Stream<ServiceLoader.Provider<T>> loadServices(Class<T> type) {
            if (type != IModFileReader.class) return Stream.empty();
            return readers.stream().map(reader -> new ServiceLoader.Provider<T>() {
                public Class<? extends T> type() { return (Class<? extends T>) reader.getClass(); }
                public T get() { return (T) reader; }
            });
        }
    }

    private record Run(Context context, IDiscoveryPipeline pipeline,
                       List<ModFile> loaded, List<ModLoadingIssue> issues) {}

    private static Run pipeline(Path directory) throws Exception {
        FMLPaths.loadAbsolutePaths(directory);
        Context context = new Context();
        ForgeModLocator locator = new ForgeModLocator();
        ModDiscoverer discoverer = new ModDiscoverer(context, List.of(locator));
        Class<?> type = Class.forName("net.neoforged.fml.loading.moddiscovery.ModDiscoverer$DiscoveryPipeline");
        Constructor<?> ctor = type.getDeclaredConstructor(ModDiscoverer.class,
                ModFileDiscoveryAttributes.class, List.class, List.class);
        ctor.setAccessible(true);
        List<ModFile> loaded = new ArrayList<>();
        List<ModLoadingIssue> issues = new ArrayList<>();
        IDiscoveryPipeline pipeline = (IDiscoveryPipeline) ctor.newInstance(discoverer,
                ModFileDiscoveryAttributes.DEFAULT.withLocator(locator), loaded, issues);
        return new Run(context, pipeline, loaded, issues);
    }

    private static void forgeCase(Path root, String name, String id) throws Exception {
        Run run = pipeline(root.resolve(name));
        new ForgeModLocator().findCandidates(run.context(), run.pipeline());
        check(run.loaded().size() == 1 && run.issues().isEmpty(), name + " initial discovery count");
        ModFile file = run.loaded().getFirst();
        check(file.getModFileInfo().getMods().getFirst().getModId().equals(id), name + " real metadata path");
        check(file.getDiscoveryAttributes().locator() instanceof ForgeModLocator, name + " Locator handoff");
        new ModsFolderLocator().findCandidates(run.context(), run.pipeline());
        new ForgeModLocator().findCandidates(run.context(), run.pipeline());
        check(run.loaded().size() == 1 && run.issues().isEmpty(), name + " no duplicate discovery");
        System.out.println("CASE " + name + " PASS file=" + file.getFileName() + " modId=" + id
                + " real-FML-reader-metadata no-duplicate");
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]);
        forgeCase(root, "A", "forgeprobe");
        forgeCase(root, "B", "forgeprobe");
        forgeCase(root, "C", "controlled_candidate");
        for (String name : List.of("D", "E", "F", "native-dual", "missing-loader", "wrong-loader")) {
            Run run = pipeline(root.resolve(name));
            new ForgeModLocator().findCandidates(run.context(), run.pipeline());
            check(run.loaded().isEmpty() && run.issues().isEmpty(), name + " must not be claimed by Locator");
            if (!name.equals("F")) {
                Path path;
                try (var files = Files.list(root.resolve(name).resolve("mods"))) { path = files.findFirst().orElseThrow(); }
                try (JarContents contents = JarContents.of(path)) {
                    check(new ForgeModReader().read(contents, ModFileDiscoveryAttributes.DEFAULT) == null,
                            name + " must not be claimed through another locator");
                }
            }
            new ModsFolderLocator().findCandidates(run.context(), run.pipeline());
            if (name.equals("D") || name.equals("native-dual")) {
                check(run.loaded().size() == 1, name + " native Reader still works");
                check(run.loaded().getFirst().getModFileInfo().getMods().getFirst().getModId().equals("neoforge"),
                        name + " native mod identity");
                check(run.loaded().getFirst().getDiscoveryAttributes().reader() instanceof JarModsDotTomlModFileReader,
                        name + " native Reader ownership");
            } else if (name.equals("F")) {
                check(run.loaded().isEmpty() && run.issues().stream().anyMatch(i -> i.severity() == ModLoadingIssue.Severity.ERROR),
                        "F must produce explicit FML invalid ZIP error");
            } else {
                check(run.loaded().isEmpty(), name + " not accepted as a Forge mod");
            }
            System.out.println("CASE " + name + " PASS loaded=" + run.loaded().size() + " issues=" + run.issues());
        }
        Run multiple = pipeline(root.resolve("multiple"));
        new ForgeModLocator().findCandidates(multiple.context(), multiple.pipeline());
        check(multiple.loaded().stream().map(ModFile::getFileName).toList().equals(List.of("a-candidate.jar", "z-probe.jar")),
                "Multiple candidates must be handed off in stable filename order");
        check(multiple.loaded().stream().flatMap(f -> f.getModFileInfo().getMods().stream()).map(m -> m.getModId()).distinct().count() == 2,
                "Multiple different candidates must keep distinct modIds");
        new ModsFolderLocator().findCandidates(multiple.context(), multiple.pipeline());
        check(multiple.loaded().size() == 2 && multiple.issues().isEmpty(), "Multiple candidates duplicate guard");
        Run ambiguous = pipeline(root.resolve("ambiguous"));
        try {
            new ForgeModLocator().findCandidates(ambiguous.context(), ambiguous.pipeline());
            throw new AssertionError("Ambiguous metadata was silently accepted");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains("Ambiguous"), "Explicit ambiguity diagnostic");
            check(ambiguous.loaded().isEmpty(), "Ambiguous candidate must not reach Reader");
            System.out.println("CASE ambiguous PASS explicit-stop=" + expected.getMessage());
        }
        System.out.println("PASS A-F + native-dual + missing/wrong-loader + stable-multiple + explicit-ambiguity-stop");
    }
}
