package io.github.gfander.dualforge;

import java.nio.file.*;
import java.util.*;
import cpw.mods.jarhandling.JarContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.*;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.objectweb.asm.tree.*;

/** Defines no source classes. A complete read-only plan is not a game execution. */
public final class FnFGlobalAdmission {
 public static void main(String[] args)throws Exception{
  Path source=Path.of(args[0]),out=Path.of(args[2]);Files.createDirectories(out);byte[] original=Files.readAllBytes(source);
  if(!ForgeRegistryTransformationPlan.digest(original).equals("bd415b04529c4ffc31e14757488f71dab6917e4560c61e236c3c4d0de6557958"))throw new AssertionError("Wrong original FnF input");
  var v=FMLLoader.class.getDeclaredField("versionInfo");v.setAccessible(true);v.set(null,new VersionInfo("21.1.256","4.0.45","1.21.1","20240808.144430"));var d=FMLLoader.class.getDeclaredField("dist");d.setAccessible(true);d.set(null,Dist.DEDICATED_SERVER);
  var f=(ModFile)new ForgeModReader().read(JarContents.of(source),ModFileDiscoveryAttributes.DEFAULT);f.setScanResult(f.compileContent(),null);LoadingModList.of(List.of(),List.of(f),List.of(),List.of(),Map.of());
  String entry=DualForgeLanguageLoader.forgeEntrypoints(f.getModInfos().getFirst(),f.getScanResult()).getFirst().replace('.','/');ClassNode n=ForgeRegistryTransformationPlan.read(Files.readAllBytes(f.findResource(entry+".class")));
  if(ForgeEntryProfile.select(n,false)!=ForgeEntryProfile.Kind.CONTEXT||!ForgeRegistryTransformationPlan.isCandidate(n))throw new AssertionError("Normal composed candidate not selected");
  var plan=ForgeRegistryTransformationPlan.create(f.getModInfos().getFirst(),entry,new PinnedHostOracle(Path.of(args[1])));
  if(plan.global==null||plan.global.listeners()!=0||plan.targets().size()!=3||!plan.targets().contains(entry))throw new AssertionError("Unexpected complete FnF plan");
  for(String name:new TreeSet<>(plan.targets())){
   byte[] raw=Files.readAllBytes(f.findResource(name+".class"));ClassNode input=ForgeRegistryTransformationPlan.read(raw);byte[] before=ForgeRegistryTransformationPlan.bytes(input);
   var changed=plan.transform(input,java.security.MessageDigest.getInstance("SHA-256").digest(raw));RegistryBytecodeVerifier.verify(changed);
   if(!Arrays.equals(before,ForgeRegistryTransformationPlan.bytes(input)))throw new AssertionError("Source ClassNode changed");
   Files.write(out.resolve(name.substring(name.lastIndexOf('/')+1)+".before.class"),raw);Files.write(out.resolve(name.substring(name.lastIndexOf('/')+1)+".after.class"),ForgeRegistryTransformationPlan.bytes(changed));
   for(MethodNode m:changed.methods)for(var op:m.instructions){if(op instanceof FieldInsnNode field&&field.owner.equals(ControlledForgeGlobalBus.SOURCE))throw new AssertionError("Unmapped global source");if(op instanceof MethodInsnNode c&&(c.owner.startsWith(ForgeRegistryTransformationPlan.F)||c.desc.contains("RegistryObject")))throw new AssertionError("Unclosed Registry reference");}
   System.out.println("FNF_TARGET_VERIFIED="+name+" originalSHA256="+ForgeRegistryTransformationPlan.digest(raw)+" outputSHA256="+ForgeRegistryTransformationPlan.digest(ForgeRegistryTransformationPlan.bytes(changed)));
  }
  if(!Arrays.equals(original,Files.readAllBytes(source)))throw new AssertionError("FnF input changed");
  System.out.println("FNF_CONTRACT_VERSION=global-v1");System.out.println("FNF_PLAN_ADMISSION=ACCEPTED targets=3 candidate=COMPOSED_CONTEXT globalListeners=0");System.out.println("FNF READ_ONLY COMPLETE PLAN PASS; FNF_RUNTIME=NOT RUN; source classes never defined or initialized");
 }
}
