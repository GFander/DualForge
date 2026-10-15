package io.github.gfander.dualforge;
import cpw.mods.jarhandling.JarContents;import java.nio.file.*;import java.util.*;import java.util.zip.*;
import net.neoforged.api.distmarker.Dist;import net.neoforged.fml.loading.*;import net.neoforged.fml.loading.moddiscovery.ModFile;import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import org.objectweb.asm.util.*;
public final class PlanContract {
 static String text(MethodNode m){Textifier t=new Textifier();m.accept(new TraceMethodVisitor(t));return t.text.toString();}
 public static void main(String[] args)throws Exception {
  var v=FMLLoader.class.getDeclaredField("versionInfo");v.setAccessible(true);v.set(null,new VersionInfo("21.1.256","4.0.45","1.21.1","20240808.144430"));var d=FMLLoader.class.getDeclaredField("dist");d.setAccessible(true);d.set(null,Dist.DEDICATED_SERVER);
  Path evidence=Path.of(args[2]).getParent();Files.createDirectories(evidence.resolve("before"));Files.createDirectories(evidence.resolve("after"));
  Path jar=Path.of(args[0]);byte[] before=Files.readAllBytes(jar);ModFile f=(ModFile)new ForgeModReader().read(JarContents.of(jar),ModFileDiscoveryAttributes.DEFAULT);f.setScanResult(f.compileContent(),null);LoadingModList.of(List.of(),List.of(f),List.of(),List.of(),Map.of());
  String entry=DualForgeLanguageLoader.forgeEntrypoints(f.getModInfos().getFirst(),f.getScanResult()).getFirst().replace('.','/');var plan=ForgeRegistryTransformationPlan.create(f.getModInfos().getFirst(),entry,new PinnedHostOracle(Path.of(args[1])));int methods=0;Set<String> targets=new TreeSet<>(plan.targets());
  if(targets.size()!=3)throw new AssertionError("Wrong targets "+targets);
  try(ZipFile z=new ZipFile(jar.toFile())) {for(var e:z.stream().toList())if(e.getName().endsWith(".class")) {
   ClassNode n=ForgeRegistryTransformationPlan.read(z.getInputStream(e).readAllBytes());byte[] prior=ForgeRegistryTransformationPlan.bytes(n);String label=n.name.replace('/','_');Files.write(evidence.resolve("before").resolve(label+".class"),prior);
   if(!targets.contains(n.name)){Files.write(evidence.resolve("after").resolve(label+".class"),prior);System.out.println("FORWARD_DEPENDENCY_UNCHANGED "+n.name);continue;}
   ClassNode after=plan.transform(n,null);Files.write(evidence.resolve("after").resolve(label+".class"),ForgeRegistryTransformationPlan.bytes(after));if(!Arrays.equals(prior,ForgeRegistryTransformationPlan.bytes(n)))throw new AssertionError("Original modified");
   for(MethodNode m:n.methods)if(m.desc.equals("()Lnet/minecraft/world/item/Item;") && (m.access&org.objectweb.asm.Opcodes.ACC_PRIVATE)!=0){MethodNode a=after.methods.stream().filter(q->q.name.equals(m.name)).findFirst().orElseThrow();if(!text(m).equals(text(a)))throw new AssertionError("Business changed "+m.name);Files.writeString(evidence.resolve(label+"-"+m.name+".normalized.txt"),text(m));methods++;System.out.println("BUSINESS_NORMALIZED_IDENTICAL "+m.name);}
  }}
  if(methods!=2 || !Arrays.equals(before,Files.readAllBytes(jar)))throw new AssertionError("Incomplete preservation proof");
  // A valid first declaration and a failing later Supplier must return no plan.
  Path bad=Path.of(args[2]);Map<String,ClassNode> changed=new TreeMap<>();String toml;
  try(ZipFile z=new ZipFile(jar.toFile())){toml=new String(z.getInputStream(z.getEntry("META-INF/mods.toml")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);for(var e:z.stream().toList())if(e.getName().endsWith(".class")){ClassNode n=ForgeRegistryTransformationPlan.read(z.getInputStream(e).readAllBytes());changed.put(n.name,n);}}
  ClassNode helper=changed.values().stream().filter(n->n.methods.stream().filter(m->m.desc.equals("()Lnet/minecraft/world/item/Item;")&&(m.access&Opcodes.ACC_PRIVATE)!=0).count()==2).findFirst().orElseThrow();
  MethodNode later=helper.methods.stream().filter(m->m.desc.equals("()Lnet/minecraft/world/item/Item;")&&(m.access&Opcodes.ACC_PRIVATE)!=0).toList().get(1);
  later.instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC,"net/minecraftforge/Unknown","run","()V",false));
  try(ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(bad))){out.putNextEntry(new ZipEntry("META-INF/mods.toml"));out.write(toml.getBytes(java.nio.charset.StandardCharsets.UTF_8));out.closeEntry();for(ClassNode n:changed.values()){out.putNextEntry(new ZipEntry(n.name+".class"));out.write(ForgeRegistryTransformationPlan.bytes(n));out.closeEntry();}}
  byte[] badBefore=Files.readAllBytes(bad);ModFile bf=(ModFile)new ForgeModReader().read(JarContents.of(bad),ModFileDiscoveryAttributes.DEFAULT);bf.setScanResult(bf.compileContent(),null);LoadingModList.of(List.of(),List.of(bf),List.of(),List.of(),Map.of());boolean returned=false;
  try{ForgeRegistryTransformationPlan.create(bf.getModInfos().getFirst(),entry,new PinnedHostOracle(Path.of(args[1])));returned=true;}catch(IllegalStateException expected){if(!expected.getMessage().startsWith("Registry helper rejected: UNMAPPED_FORGE_API"))throw expected;}
  if(returned || !Arrays.equals(badBefore,Files.readAllBytes(bad)) || !Arrays.equals(before,Files.readAllBytes(jar)))throw new AssertionError("Late Supplier failure was not atomic");
  System.out.println("REJECT later-supplier stage=plan-validation reason=UNMAPPED_FORGE_API outputTargets=none originalUnchanged=true");
  System.out.println("SUPPLIER PLAN CONTRACT PASS supplierMethods="+methods+" targets="+targets+" negativePlans=1");
 }
}
