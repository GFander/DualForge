package io.github.gfander.dualforge;

import cpw.mods.jarhandling.JarContents;
import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.TypesafeMap;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.ModSorter;
import net.neoforged.fml.loading.VersionInfo;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.neoforgespi.Environment;
import net.neoforged.neoforgespi.language.IModInfo;

/** Contract-only memory metadata/context; uses the official sorter. Never part of the stock runtime. */
public final class PlatformDependencyContract {
    // Official key builders require their owning registry before Environment.Keys is accessed.
    private static final TypesafeMap KEY_REGISTRY = new TypesafeMap(IEnvironment.class);
    private static final ForgePlatformDependencyResolver.Host SERVER = new ForgePlatformDependencyResolver.Host("1.21.1", "21.1.256", "4.0.45", "DEDICATED_SERVER", 21);
    private static final ForgePlatformDependencyResolver.Host CLIENT = new ForgePlatformDependencyResolver.Host("1.21.1", "21.1.256", "4.0.45", "CLIENT", 21);
    private static Path probe;
    private static int assertions;
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        assertions++; System.out.println("PASS " + message);
    }
    @SuppressWarnings("unchecked")
    private static void context(Dist dist) throws Exception {
        var version = FMLLoader.class.getDeclaredField("versionInfo"); version.setAccessible(true);
        version.set(null, new VersionInfo("21.1.256", "4.0.45", "1.21.1", "20240808.144430"));
        var side = FMLLoader.class.getDeclaredField("dist"); side.setAccessible(true); side.set(null, dist);
        Map<Object,Object> properties = new HashMap<>(); properties.put(Environment.Keys.DIST.get(), dist);
        IEnvironment environment = (IEnvironment) Proxy.newProxyInstance(IEnvironment.class.getClassLoader(), new Class<?>[]{IEnvironment.class}, (self, method, args) -> switch (method.getName()) {
            case "getProperty" -> Optional.ofNullable(properties.get(args[0]));
            case "computePropertyIfAbsent" -> properties.computeIfAbsent(args[0], (Function<Object,Object>)args[1]);
            default -> throw new AssertionError("Unexpected diagnostic environment call " + method);
        });
        Environment.build(environment);
    }
    private static Map<String,Object> base(String id) {
        return ForgeModFileInfoParser.parseForgeModsToml("modLoader=\"javafml\"\nloaderVersion=\"[52,)\"\nlicense=\"MIT\"\n[[mods]]\nmodId=\"" + id + "\"\nversion=\"1.0.0\"\n");
    }
    private static Map<String,Object> dep(String id, String range, boolean required, String side, String ordering) {
        Map<String,Object> result = new LinkedHashMap<>(Map.of("modId", id, "versionRange", range, "mandatory", required,
                "type", required ? "required" : "optional", "side", side, "ordering", ordering));
        return result;
    }
    private static Map<String,Object> root(String id, Map<String,Object>... deps) {
        var result = base(id); result.put("dependencies", new LinkedHashMap<>(Map.of(id, List.of(deps)))); return result;
    }
    private static Map<String,Object> platform(String range) { return dep("forge", range, true, "BOTH", "NONE"); }
    @SuppressWarnings("unchecked")
    private static List<Map<String,Object>> deps(Map<String,Object> root, String key, String id) {
        return (List<Map<String,Object>>)((Map<?,?>)root.get(key)).get(id);
    }
    @SuppressWarnings("unchecked")
    private static List<Map<String,Object>> audit(Map<String,Object> root) {
        return (List<Map<String,Object>>)root.get(ForgePlatformDependencyResolver.AUDIT);
    }
    private static Map<String,Object> project(Map<String,Object> input) {
        String before = input.toString(); var result = ForgePlatformDependencyResolver.resolve(input, SERVER);
        check(before.equals(input.toString()), "success leaves source tree unchanged"); return result;
    }
    private static void reject(String name, Map<String,Object> input, ForgePlatformDependencyResolver.Host host) {
        String before = input.toString();
        try { ForgePlatformDependencyResolver.resolve(input, host); throw new AssertionError("Accepted " + name); }
        catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains("Forge platform dependency rejected"), "explicit reject " + name);
            check(before.equals(input.toString()), "failed projection atomic " + name);
        }
    }
    private static ModFile model(String id, String version, Map<String,Object> config) {
        var contents = JarContents.of(probe); var metadata = new ForgeJarMetadata(contents);
        ModFile file = (ModFile) net.neoforged.neoforgespi.locating.IModFile.create(SecureJar.from(contents, metadata),
                f -> new ModFileInfo((ModFile)f, ForgeModFileInfo.configOf(config), info -> { }));
        metadata.bind(file); return file;
    }
    private static ModFile system(String id, String version) {
        var config = base(id); config.put("mods", List.of(Map.of("modId", id, "version", version)));
        return model(id,version,config);
    }
    private static LoadingModList sort(Map<String,Object> config, ModFile... extras) {
        List<ModFile> files = new ArrayList<>(List.of(system("minecraft","1.21.1"),system("neoforge","21.1.256"),model("contractprobe","1",config)));
        files.addAll(List.of(extras)); return ModSorter.sort(List.of(),files,new ArrayList<>());
    }
    public static void main(String[] args) throws Exception {
        probe = Path.of(args[0]); context(Dist.DEDICATED_SERVER);
        for (String range : List.of("[0,)","[52,)","[52.1.0,)","[52,53)")) {
            var input = root("contractprobe",platform(range),dep("minecraft","[1.21.1]",true,"BOTH","NONE"));
            var result = project(input);
            check(deps(result,"dependencies","contractprobe").size()==1 && deps(result,"dependencies","contractprobe").getFirst().get("modId").equals("minecraft"), "only active verified platform projected " + range);
            check(deps(result,ForgePlatformDependencyResolver.ORIGINAL,"contractprobe").equals(deps(input,"dependencies","contractprobe")), "complete original dependencies retained " + range);
            check(audit(result).getFirst().get("sourcePlatformDependency").equals("VERIFIED") && audit(result).getFirst().get("referenceSourceABI").equals("52.1.0"), "auditable reference version " + range);
            check(audit(result).getFirst().get("forgeRuntimeApiCompatibility").equals("NOT RUN"), "no API compatibility claim " + range);
            var sorted = sort(result); check(!sorted.hasErrors() && sorted.getModFileById("forge")==null, "real FML accepts without fake forge " + range);
        }
        for (String range : List.of("[53,)","[52.2,)","[9999,)","[21,22)","[broken")) reject("version " + range,root("contractprobe",platform(range)),SERVER);
        var noRange=platform("[0,)");noRange.remove("versionRange");check(deps(project(root("contractprobe",noRange)),"dependencies","contractprobe").isEmpty(), "missing range keeps FML unbounded default");
        for (String ordering : List.of("BEFORE","AFTER")) reject("active platform ordering " + ordering,root("contractprobe",dep("forge","[0,)",true,"BOTH",ordering)),SERVER);
        var inactive = project(root("contractprobe",dep("forge","[53,)",true,"CLIENT","AFTER")));
        check(deps(inactive,"dependencies","contractprobe").size()==1 && audit(inactive).getFirst().get("action").equals("DELEGATE_INACTIVE_SIDE"), "inactive side declaration and ordering retained");
        check(!sort(inactive).hasErrors(), "official FML ignores client requirement on dedicated server");
        var active = ForgePlatformDependencyResolver.resolve(root("contractprobe",dep("forge","[52,53)",true,"CLIENT","NONE")),CLIENT);
        check(deps(active,"dependencies","contractprobe").isEmpty(), "client model active platform projected");
        var serverInactive = ForgePlatformDependencyResolver.resolve(root("contractprobe",dep("forge","[53,)",true,"SERVER","BEFORE")),CLIENT);
        check(deps(serverInactive,"dependencies","contractprobe").size()==1, "server requirement retained inactive in client model");
        check(deps(project(root("contractprobe",dep("forge","[52,53)",false,"BOTH","NONE"))),"dependencies","contractprobe").isEmpty(), "optional platform verifies supplied reference capability");
        reject("optional platform range mismatch",root("contractprobe",dep("forge","[53,)",false,"BOTH","NONE")),SERVER);
        var badtype=platform("[0,)");badtype.put("type","incompatible");reject("unsupported platform type",root("contractprobe",badtype),SERVER);
        var badbool=platform("[0,)");badbool.put("mandatory","true");reject("wrong mandatory type",root("contractprobe",badbool),SERVER);
        var badside=platform("[0,)");badside.put("side","OTHER");reject("wrong side",root("contractprobe",badside),SERVER);
        var conflict=root("contractprobe",platform("[0,)"));conflict.put(ForgePlatformDependencyResolver.AUDIT,List.of());reject("audit collision",conflict,SERVER);
        reject("second platform clause failure",root("contractprobe",platform("[0,)"),platform("[53,)")),SERVER);
        for (var host : List.of(new ForgePlatformDependencyResolver.Host("1.20.1","21.1.256","4.0.45","DEDICATED_SERVER",21),new ForgePlatformDependencyResolver.Host("1.21.1","21.1.255","4.0.45","DEDICATED_SERVER",21),new ForgePlatformDependencyResolver.Host("1.21.1","21.1.256","4.0.44","DEDICATED_SERVER",21),new ForgePlatformDependencyResolver.Host("1.21.1","21.1.256","4.0.45","DEDICATED_SERVER",17),new ForgePlatformDependencyResolver.Host("1.21.1","21.1.256","4.0.45","OTHER",21))) reject("unsupported host " + host,root("contractprobe",platform("[0,)")),host);
        var missing=root("contractprobe",platform("[0,)"),dep("non_existing_mod","[1,)",true,"BOTH","NONE"));
        var missingProjected=project(missing);check(deps(missingProjected,"dependencies","contractprobe").equals(List.of(deps(missing,"dependencies","contractprobe").get(1))), "ordinary dependency fields exactly preserved");
        check(sort(missingProjected).hasErrors(), "real FML missing ordinary required still rejects");
        check(sort(project(root("contractprobe",platform("[0,)"),dep("minecraft","[1.20.1]",true,"BOTH","NONE")))).hasErrors(), "real FML mismatched Minecraft still rejects");
        check(sort(project(root("contractprobe",platform("[0,)"),dep("supportmod","[2,)",true,"BOTH","NONE"))),system("supportmod","1.0.0")).hasErrors(), "real FML low required installed version rejects");
        check(!sort(project(root("contractprobe",platform("[0,)"),dep("non_existing_mod","[1,)",false,"BOTH","NONE")))).hasErrors(), "real FML absent ordinary optional ignored");
        check(sort(project(root("contractprobe",platform("[0,)"),dep("supportmod","[2,)",false,"BOTH","NONE"))),system("supportmod","1.0.0")).hasErrors(), "real FML installed low optional rejects");
        var clientMissing=project(root("contractprobe",platform("[0,)"),dep("non_existing_mod","[1,)",true,"CLIENT","NONE")));
        check(!sort(clientMissing).hasErrors(), "real FML client ordinary dependency inactive on server");
        context(Dist.CLIENT);check(sort(clientMissing).hasErrors(), "real FML same client ordinary requirement active in client model");context(Dist.DEDICATED_SERVER);
        for(String ordering:List.of("AFTER","BEFORE")) {
            var sorted=sort(project(root("contractprobe",platform("[0,)"),dep("supportmod","[1,)",true,"BOTH",ordering))),system("supportmod","1.0.0"));
            var owner=sorted.getMods().stream().filter(m->m.getModId().equals("contractprobe")).findFirst().orElseThrow();
            var target=sorted.getMods().stream().filter(m->m.getModId().equals("supportmod")).findFirst().orElseThrow();
            check(!sorted.hasErrors() && (ordering.equals("AFTER") ? sorted.getDependencies(owner).contains(target) : sorted.getDependencies(target).contains(owner)), "official ordering graph preserved " + ordering);
        }
        var nativeModel=root("nativeprobe",dep("forge","[0,)",true,"BOTH","NONE"));
        check(ForgePlatformDependencyResolver.forFile(Path.of(args[1]),nativeModel)==nativeModel, "native physical input never projected even with Forge declaration");
        check(sort(nativeModel).hasErrors(), "native required forge remains real FML refusal");
        var realFnf = new ForgeModReader().read(JarContents.of(Path.of(args[2])),net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes.DEFAULT);
        var real = (ModFileInfo)realFnf.getModFileInfo();
        check(real.getMods().getFirst().getDependencies().size()==1 && real.getConfigList(ForgePlatformDependencyResolver.ORIGINAL,"fnf").size()==2, "original FnF real Reader carries projected deps and complete source audit");
        check(real.getConfigList(ForgePlatformDependencyResolver.AUDIT).getFirst().<String>getConfigElement("sourcePlatformDependency").orElseThrow().equals("VERIFIED"), "projection audit accessible through real FML config");
        System.out.println("PLATFORM DEPENDENCY CONTRACT PASS assertions="+assertions);
    }
}
