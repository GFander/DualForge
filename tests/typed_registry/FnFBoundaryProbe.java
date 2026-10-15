package io.github.gfander.dualforge;
import java.nio.file.*;import java.util.*;import cpw.mods.jarhandling.JarContents;import net.neoforged.api.distmarker.Dist;import net.neoforged.fml.loading.*;import net.neoforged.fml.loading.moddiscovery.ModFile;import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
/** Actual fixed-release read-only plan; no source class is defined or initialized. */
public final class FnFBoundaryProbe {
 public static void main(String[] args)throws Exception {
  Path jar=Path.of(args[0]);byte[] before=Files.readAllBytes(jar);if(!ForgeRegistryTransformationPlan.digest(before).equals("bd415b04529c4ffc31e14757488f71dab6917e4560c61e236c3c4d0de6557958"))throw new AssertionError("Wrong original input");
  var v=FMLLoader.class.getDeclaredField("versionInfo");v.setAccessible(true);v.set(null,new VersionInfo("21.1.256","4.0.45","1.21.1","20240808.144430"));var d=FMLLoader.class.getDeclaredField("dist");d.setAccessible(true);d.set(null,Dist.DEDICATED_SERVER);
  var file=(ModFile)new ForgeModReader().read(JarContents.of(jar),ModFileDiscoveryAttributes.DEFAULT);file.setScanResult(file.compileContent(),null);LoadingModList.of(List.of(),List.of(file),List.of(),List.of(),Map.of());String entry=DualForgeLanguageLoader.forgeEntrypoints(file.getModInfos().getFirst(),file.getScanResult()).getFirst().replace('.','/');var node=ForgeRegistryTransformationPlan.read(Files.readAllBytes(file.findResource(entry+".class")));if(ForgeRegistryTransformationPlan.isCandidate(node))throw new AssertionError("Global Bus candidate boundary removed");
  try{ForgeRegistryTransformationPlan.create(file.getModInfos().getFirst(),entry,new PinnedHostOracle(Path.of(args[1])));throw new AssertionError("Unexpected full admission");}catch(IllegalStateException e){System.out.println("FNF_OBSERVED="+e.getMessage());if(args.length>2&&!e.getMessage().equals(args[2]))throw new AssertionError("Wrong precise refusal",e);}
  if(!Arrays.equals(before,Files.readAllBytes(jar)))throw new AssertionError("Input changed");System.out.println("FNF_RUNTIME=NOT RUN; normalCandidate=false; originalUnchanged=true");
 }
}
