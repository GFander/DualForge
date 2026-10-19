package io.github.gfander.dualforge;

import cpw.mods.jarhandling.JarContents;
import java.nio.file.*;
import java.util.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.*;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Actual Reader/scan/planner refusal model, never a GAME layer or mod constructor. */
public final class ClosureRegressionBoundaryContract {
    private static int assertions;
    public static void main(String[] args) throws Exception {
        var version = FMLLoader.class.getDeclaredField("versionInfo"); version.setAccessible(true);
        version.set(null, new VersionInfo("21.1.256", "4.0.45", "1.21.1", "20240808.144430"));
        var dist = FMLLoader.class.getDeclaredField("dist"); dist.setAccessible(true);
        for (Dist side : List.of(Dist.CLIENT, Dist.DEDICATED_SERVER)) {
            dist.set(null, side);
            var path = Path.of(args[0]);
            ModFile file = (ModFile) new ForgeModReader().read(JarContents.of(path), ModFileDiscoveryAttributes.DEFAULT);
            check(file != null, "actual Forge Reader");
            file.setScanResult(file.compileContent(), null);
            var info = file.getModInfos().getFirst();
            check(file.getModInfos().size() == 1 && info.getModId().equals("jei"), "single owner");
            check(info.getVersion().toString().equals("19.27.0.340"), "exact release version");
            check(info.getDependencies().stream().noneMatch(d -> d.getModId().equals("forge")), "verified platform projection");
            check(info.getDependencies().stream().anyMatch(d -> d.getModId().equals("minecraft")
                && d.getVersionRange().toString().equals("[1.21,1.21.1)")), "MC declaration retained");
            String entry = DualForgeLanguageLoader.forgeEntrypoints(info, file.getScanResult()).getFirst().replace('.', '/');
            byte[] original = Files.readAllBytes(file.findResource(entry + ".class"));
            ClassNode node = new ClassNode(); new ClassReader(original).accept(node, 0);
            check(ForgeEntryProfile.select(node, false) == ForgeEntryProfile.Kind.DUAL_BUS, "candidate routing only");
            check(!ForgeClientEventPlan.isCandidate(node), "009C profile does not claim JEI");
            check(ForgeRegistryTransformationPlan.isCandidate(node), "global candidate routes to combined validator");
            ModFile nativeHost=(ModFile)new net.neoforged.fml.loading.moddiscovery.readers.JarModsDotTomlModFileReader().read(JarContents.of(Path.of(args[1])),ModFileDiscoveryAttributes.DEFAULT);
            nativeHost.setScanResult(nativeHost.compileContent(),null);
            ModFile positive=(ModFile)new ForgeModReader().read(JarContents.of(Path.of(args[2])),ModFileDiscoveryAttributes.DEFAULT);
            check(positive!=null,"independent positive Reader");positive.setScanResult(positive.compileContent(),null);
            LoadingModList.of(List.of(),List.of(nativeHost,positive,file),List.of(),List.of(),Map.of());
            check(ForgeDualBusPlan.create(positive.getModInfos().getFirst(),"org/example/flowalpha/Entry").targets().size()==20,"earlier complete plan is valid before later rejection");
            LoadingModList.of(List.of(), List.of(nativeHost,positive,file), List.of(), List.of(), Map.of());
            var transformer = new DualForgeEntryTransformer();
            try {
                transformer.targets(); throw new AssertionError("Unexpected JEI approval");
            } catch (IllegalStateException expected) {
                check(expected.getMessage().contains("UNVERIFIED_NETWORK_INITIALIZATION") && expected.getMessage().contains("combined=REJECTED_NO_TARGETS") && expected.getMessage().contains("UNVERIFIED_PERMISSION_CHECK") && expected.getMessage().contains("globalConnection=STATIC_VERIFIED"), "precise final closure refusal");
            }
            for (String field : List.of("contextEntries", "helperPlans", "eventInitializerPlans", "clientEventPlans", "dualBusPlans")) {
                var state = DualForgeEntryTransformer.class.getDeclaredField(field); state.setAccessible(true);
                check(((Map<?,?>) state.get(transformer)).isEmpty(), "no partially published " + field);
            }
            check(Arrays.equals(original, Files.readAllBytes(file.findResource(entry + ".class"))), "original input unchanged");
            System.out.println("JEI_BOUNDARY_SIDE_PASS " + side);
        }
        System.out.println("NETWORK CLOSURE BOUNDARY CONTRACT PASS assertions=" + assertions
            + "; MODEL_TEST; LIVE_JEI_NEO_CONSTRUCTION=NOT_RUN");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        assertions++;
    }
}
