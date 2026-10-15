package io.github.gfander.dualforge;

import cpw.mods.jarhandling.JarContents;
import cpw.mods.modlauncher.api.ITransformer;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.*;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.readers.JarModsDotTomlModFileReader;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

public final class HelperRegistryContract {
    private static Path original, output;
    private static int assertions, negatives;
    private static final String E="example/helperprobe/HelperProbe", H="example/helperprobe/ItemHelper", C="example/helperprobe/Consumer", U="example/helperprobe/Unused";
    private static final String F=ForgeRegistryTransformationPlan.F, R=ForgeRegistryTransformationPlan.R, HOLDER=ForgeRegistryTransformationPlan.H;
    public static void main(String[] args)throws Exception {
        original=Path.of(args[0]);output=Path.of(args[1]);Files.createDirectories(output);
        var v=FMLLoader.class.getDeclaredField("versionInfo");v.setAccessible(true);v.set(null,new VersionInfo("21.1.256","4.0.45","1.21.1","20240808.144430"));
        var d=FMLLoader.class.getDeclaredField("dist");d.setAccessible(true);d.set(null,Dist.DEDICATED_SERVER);
        byte[] jarBefore=Files.readAllBytes(original);ModFile file=scan(original,false);var transformer=new DualForgeEntryTransformer();
        check(transformer.targets().equals(Set.of(ITransformer.Target.targetClass(E.replace('/','.')),ITransformer.Target.targetClass(H.replace('/','.')),ITransformer.Target.targetClass(C.replace('/','.')))),"actual FML scan publishes exact entry/helper/consumer targets");
        Map<String,byte[]> changed=new HashMap<>();
        for(String name:List.of(E,H,C,U)) {
            ClassNode n=read(Files.readAllBytes(file.findResource(name+".class")));byte[] before=bytes(n);ClassNode after=transformer.transform(n,null);
            check(Arrays.equals(before,bytes(n)),"source ClassNode immutable "+name);changed.put(name,bytes(after));Files.write(output.resolve(name.substring(name.lastIndexOf('/')+1)+".before.class"),before);Files.write(output.resolve(name.substring(name.lastIndexOf('/')+1)+".after.class"),bytes(after));
            if(name.equals(U))check(Arrays.equals(before,bytes(after)),"unreferenced same-ABI class unchanged");
        }
        check(new String(changed.get(H),java.nio.charset.StandardCharsets.ISO_8859_1).contains("ForgeRegistryBridge"),"actual bridge call emitted");
        ClassNode helperAfter=read(changed.get(H)),helperBefore=read(Files.readAllBytes(file.findResource(H+".class")));
        for(MethodNode m:helperBefore.methods)if(m.name.startsWith("lambda$") || m.name.equals("makeBeta"))check(methodBytes(m).equals(methodBytes(method(helperAfter,m.name))),"Supplier business and bootstrap implementation unchanged "+m.name);
        check(method(read(changed.get(C)),"receipt").desc.equals("()Lnet/neoforged/neoforge/registries/DeferredHolder;"),"cross-class return descriptor closed");
        check(method(read(changed.get(C)),"receipt").signature.contains("Item;Lnet/minecraft/world/item/Item;"),"Holder two-parameter generic signature closed");
        negative("duplicate-id",H,n->{for(var i:method(n,"<clinit>").instructions)if(i instanceof LdcInsnNode l && "beta_item".equals(l.cst))l.cst="alpha_item";},"DUPLICATE_OR_INVALID_REGISTRY_ID");
        negative("wrong-registry",H,n->{for(var i:method(n,"<clinit>").instructions)if(i instanceof FieldInsnNode f && f.owner.equals(F+"ForgeRegistries"))f.name="BLOCKS";},"UNKNOWN_REGISTRY_FIELD");
        negative("wrong-namespace",H,n->{for(var i:method(n,"<clinit>").instructions)if(i instanceof LdcInsnNode l && "helperprobe".equals(l.cst))l.cst="foreign";},"CREATE_REGISTRY_NAMESPACE");
        negative("unknown-bootstrap",H,n->{indy(n).bsm=new Handle(Opcodes.H_INVOKESTATIC,"foreign/Bootstrap","make","()Ljava/lang/invoke/CallSite;",false);},"UNKNOWN_LAMBDA_BOOTSTRAP");
        negative("captured-lambda",H,n->indy(n).desc="(Ljava/lang/Object;)Ljava/util/function/Supplier;","UNKNOWN_LAMBDA_BOOTSTRAP");
        negative("foreign-impl-handle",H,n->indy(n).bsmArgs[1]=new Handle(Opcodes.H_INVOKESTATIC,"foreign/Helper","make","()Lnet/minecraft/world/item/Item;",false),"SUPPLIER_CAPTURE_OR_FOREIGN_HANDLE");
        negative("extra-forge-api",H,n->{method(n,"<clinit>").instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC,"net/minecraftforge/Unknown","call","()V",false));},"UNKNOWN_DEFERRED_REGISTER_DESCRIPTOR");
        negative("wrong-register-overload",H,n->{for(var i:method(n,"<clinit>").instructions)if(i instanceof MethodInsnNode m && m.name.equals("register"))m.desc="(Ljava/lang/String;Ljava/util/function/Function;)"+HOLDER;},"UNKNOWN_DEFERRED_REGISTER_DESCRIPTOR");
        negative("wrong-registry-field-type",H,n->n.fields.getFirst().signature="L"+F+"DeferredRegister<Lnet/minecraft/world/level/block/Block;>;","ITEM_FIELD_DESCRIPTOR");
        negative("holder-field-descriptor",H,n->n.fields.get(1).desc="Ljava/lang/Object;","ITEM_FIELD_DESCRIPTOR");
        negative("wrong-helper-bus",H,n->method(n,"attach").desc="(Lnet/neoforged/bus/api/IEventBus;)V","MISSING_METHOD_CLOSURE");
        negative("wrong-helper-local",H,n->{for(var i:method(n,"attach").instructions)if(i instanceof VarInsnNode x && x.getOpcode()==Opcodes.ALOAD)x.var=1;},"ATTACHMENT_RECEIVER_BUS");
        negative("invalid-conditional",H,n->{for(var i:method(n,"attach").instructions)if(i instanceof JumpInsnNode j)j.setOpcode(Opcodes.IFEQ);},"ATTACHMENT_CONDITION");
        negative("unknown-frame",H,n->{for(var i:method(n,"attach").instructions)if(i instanceof FrameNode f){f.type=Opcodes.F_SAME1;f.stack=List.of("java/lang/Object");}},"FRAME_NOT_VERIFIED");
        negative("supplier-business",H,n->{method(n,"makeBeta").instructions.insert(new InsnNode(Opcodes.NOP));},"SUPPLIER_BUSINESS_UNSUPPORTED");
        negative("foreign-helper-entry",E,n->{for(var i:method(n,"<init>").instructions)if(i instanceof MethodInsnNode m && m.owner.equals(H))m.owner="foreign/Helper";},"HELPER_OUTSIDE_MODFILE");
        negative("consumer-descriptor",C,n->{for(var i:method(n,"alpha").instructions)if(i instanceof MethodInsnNode m && m.owner.equals(H))m.desc="()Ljava/lang/Object;";},"MISSING_METHOD_CLOSURE");
        negative("consumer-wrong-cast",C,n->{for(var i:method(n,"alpha").instructions)if(i instanceof TypeInsnNode t)t.desc="java/lang/String";},"CONSUMER_RETURN");
        negative("consumer-extra-api",C,n->method(n,"alpha").instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC,"java/lang/System","nanoTime","()J",false)),"CONSUMER_METHOD_CLOSURE");
        negative("consumer-handle",C,n->{method(n,"alpha").instructions.insert(new LdcInsnNode(new Handle(Opcodes.H_GETSTATIC,H,"ALPHA",HOLDER,false)));},"CONSUMER_RETURN");
        negative("consumer-extra-field",C,n->n.fields.add(new FieldNode(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"escape",HOLDER,null,null)),"UNVERIFIED_CONSUMER_FIELD");
        negative("consumer-generic",C,n->method(n,"receipt").signature="()Ljava/lang/Object;","CONSUMER_GENERIC_SIGNATURE");
        negative("constructor-side-effect",E,n->method(n,"<init>").instructions.insertBefore(method(n,"<init>").instructions.getLast(),new MethodInsnNode(Opcodes.INVOKESTATIC,"java/lang/System","gc","()V",false)),"unverified constructor effect");
        negative("duplicate-helper-target",E,n->{InsnList call=new InsnList();call.add(new VarInsnNode(Opcodes.ALOAD,1));call.add(new MethodInsnNode(Opcodes.INVOKESTATIC,H,"attach","("+ControlledForgeContextProfile.BUS+")V",false));method(n,"<init>").instructions.insertBefore(lastReal(method(n,"<init>")),call);},"DUPLICATE_TARGET");
        negative("insufficient-max-stack",H,n->method(n,"<clinit>").maxStack=0,"BYTECODE_FRAME_VERIFICATION");
        // A post-plan changed ClassNode must fail closed without edits, even if the new node could be valid in another plan.
        scan(original,false);transformer=new DualForgeEntryTransformer();transformer.targets();ClassNode altered=read(Files.readAllBytes(file.findResource(H+".class")));altered.sourceFile="changed.java";byte[] prior=bytes(altered);
        try{transformer.transform(altered,null);throw new AssertionError("Changed discovery input accepted");}catch(IllegalStateException expected){check(causes(expected).contains("TRANSFORMATION_INPUT_CHANGED"),"other-transformer input change rejected");}check(Arrays.equals(prior,bytes(altered)),"changed input remains unpolluted");negatives++;
        Path blocked=output.resolve("dump-blocker");Files.writeString(blocked,"blocked");System.setProperty("dualforge.dump.profileDir",blocked.toString());ClassNode unchanged=read(Files.readAllBytes(file.findResource(H+".class")));prior=bytes(unchanged);
        try{transformer.transform(unchanged,null);throw new AssertionError("Dump failure swallowed");}catch(IllegalStateException expected){check(Arrays.equals(prior,bytes(unchanged)),"late failure leaves original ClassNode unchanged");}System.clearProperty("dualforge.dump.profileDir");negatives++;
        Path nativeJar=variant("native",null,n->{},true);scan(nativeJar,true);var nativeTransformer=new DualForgeEntryTransformer();check(nativeTransformer.targets().isEmpty(),"native ModFile not claimed");check(Arrays.equals(prior,bytes(nativeTransformer.transform(unchanged,null))),"native path not rewritten");
        advanced();
        check(Arrays.equals(jarBefore,Files.readAllBytes(original)),"frozen Forge JAR unchanged");
        System.out.println("HELPER REGISTRY CONTRACT PASS assertions="+assertions+" negatives="+negatives);
    }
    private static Map<String,ClassNode> allNodes()throws Exception {Map<String,ClassNode> nodes=new TreeMap<>();try(ZipFile z=new ZipFile(original.toFile())){for(var e:z.stream().toList())if(e.getName().endsWith(".class")){ClassNode n=read(z.getInputStream(e).readAllBytes());nodes.put(n.name,n);}}return nodes;}
    private static Path pack(String label,Map<String,ClassNode> nodes,String metadata)throws Exception {
        Path p=output.resolve(label+".jar");try(ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(p))){out.putNextEntry(new ZipEntry("META-INF/mods.toml"));out.write(metadata.getBytes(java.nio.charset.StandardCharsets.UTF_8));out.closeEntry();for(ClassNode n:nodes.values()){out.putNextEntry(new ZipEntry(n.name+".class"));out.write(bytes(n));out.closeEntry();}}return p;
    }
    private static void refused(String label,Path jar,String reason)throws Exception {byte[] before=Files.readAllBytes(jar);var t=new DualForgeEntryTransformer();try{scan(jar,false);t.targets();throw new AssertionError("Accepted "+label);}catch(IllegalStateException expected){check(causes(expected).contains(reason),"precise group reject "+label+" "+causes(expected));}var f=DualForgeEntryTransformer.class.getDeclaredField("helperPlans");f.setAccessible(true);check(((Map<?,?>)f.get(t)).isEmpty() && Arrays.equals(before,Files.readAllBytes(jar)),"group atomicity "+label);negatives++;}
    private static void advanced()throws Exception {
        String metadata;try(ZipFile z=new ZipFile(original.toFile())){metadata=new String(z.getInputStream(z.getEntry("META-INF/mods.toml")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}
        Map<String,ClassNode> two=allNodes();ClassNode second=new ClassNode();two.get(H).accept(new ClassRemapper(second,new SimpleRemapper(H,H+"Second")));
        for(var i:method(second,"<clinit>").instructions)if(i instanceof LdcInsnNode l && l.cst instanceof String x && x.endsWith("_item"))l.cst="second_"+x;
        two.put(second.name,second);InsnList call=new InsnList();call.add(new VarInsnNode(Opcodes.ALOAD,1));call.add(new MethodInsnNode(Opcodes.INVOKESTATIC,second.name,"attach","("+ControlledForgeContextProfile.BUS+")V",false));method(two.get(E),"<init>").instructions.insertBefore(lastReal(method(two.get(E),"<init>")),call);
        Path jar=pack("two-helpers",two,metadata);scan(jar,false);var t=new DualForgeEntryTransformer();check(t.targets().size()==4,"two distinct helpers preplanned without identity whitelist");
        Map<String,String> names=Map.of(E,"changed/names/Entry",H,"changed/names/Declarations",C,"changed/names/Readers",U,"changed/names/Ordinary");Map<String,ClassNode> renamed=new TreeMap<>();
        for(ClassNode n:allNodes().values()){ClassNode q=new ClassNode();n.accept(new ClassRemapper(q,new SimpleRemapper(names)));if(q.visibleAnnotations!=null)for(var a:q.visibleAnnotations)if(a.desc.equals("Lnet/minecraftforge/fml/common/Mod;"))a.values=List.of("value","anotherhelper");for(MethodNode m:q.methods)for(var i:m.instructions)if(i instanceof LdcInsnNode l && "helperprobe".equals(l.cst))l.cst="anotherhelper";renamed.put(q.name,q);}
        Path renamedJar=pack("changed-identities",renamed,metadata.replace("helperprobe","anotherhelper"));scan(renamedJar,false);t=new DualForgeEntryTransformer();check(t.targets().size()==3,"different modId package entry and helper names accepted from metadata");
        Map<String,ClassNode> missing=allNodes();missing.remove(H);refused("missing-helper",pack("missing-helper",missing,metadata),"HELPER_OUTSIDE_MODFILE");
        Map<String,ClassNode> consumer=allNodes();MethodNode getter=method(consumer.get(H),"alphaHolder");getter.instructions.clear();getter.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,C,"receipt","()"+HOLDER,false));getter.instructions.add(new InsnNode(Opcodes.ARETURN));consumer.remove(C);refused("missing-consumer",pack("missing-consumer",consumer,metadata),"CONSUMER_METHOD_CLOSURE");
        Map<String,ClassNode> owners=allNodes();ClassNode secondEntry=new ClassNode();owners.get(E).accept(new ClassRemapper(secondEntry,new SimpleRemapper(E,E+"Second")));for(var a:secondEntry.visibleAnnotations)if(a.desc.equals("Lnet/minecraftforge/fml/common/Mod;"))a.values=List.of("value","secondowner");owners.put(secondEntry.name,secondEntry);
        refused("two-metadata-owners",pack("two-metadata-owners",owners,metadata+"\n[[mods]]\nmodId=\"secondowner\"\nversion=\"1\"\n"),"Context entry requires one same-file Forge entry and metadata owner");
        Map<String,ClassNode> module=allNodes();ClassNode mi=new ClassNode();mi.version=Opcodes.V21;mi.access=Opcodes.ACC_MODULE;mi.name="module-info";mi.module=new ModuleNode("helperprobe",0,null);mi.module.requires=List.of(new ModuleRequireNode("java.base",Opcodes.ACC_MANDATED,null));module.put(mi.name,mi);refused("explicit-module",pack("explicit-module",module,metadata),"EXPLICIT_JPMS_HELPER_UNSUPPORTED");
        ModFile owned=scan(original,false);Path nativeJar=variant("same-name-native",null,n->{},true);ModFile foreign=scan(nativeJar,true);LoadingModList.of(List.of(),List.of(owned,foreign),List.of(),List.of(),Map.of());t=new DualForgeEntryTransformer();try{t.targets();throw new AssertionError("Cross-file target conflict accepted");}catch(IllegalStateException expected){check(causes(expected).contains("TRANSFORMATION_RULE_CONFLICT"),"foreign ModFile same-name class conflict");negatives++;}
        scan(original,false);t=new DualForgeEntryTransformer();t.targets();ClassNode source=allNodes().get(H);byte[] before=bytes(source);var context=(cpw.mods.modlauncher.api.ITransformerVotingContext)java.lang.reflect.Proxy.newProxyInstance(HelperRegistryContract.class.getClassLoader(),new Class<?>[]{cpw.mods.modlauncher.api.ITransformerVotingContext.class},(proxy,m,args)->m.getName().equals("getInitialClassSha256")?new byte[32]:null);
        try{t.transform(source,context);throw new AssertionError("Wrong original SHA accepted");}catch(IllegalStateException expected){check(causes(expected).contains("INPUT_JAR_SHA_CHANGED") && Arrays.equals(before,bytes(source)),"actual voting-context original SHA is enforced atomically");negatives++;}
    }
    private static void negative(String label,String owner,Consumer<ClassNode> mutate,String reason)throws Exception {
        Path jar=variant(label,owner,mutate,false);byte[] prior=Files.readAllBytes(jar);var t=new DualForgeEntryTransformer();
        try{scan(jar,false);t.targets();throw new AssertionError("Accepted "+label);}catch(IllegalStateException expected){check(causes(expected).contains(reason),"precise reject "+label+" "+causes(expected));}
        var f=DualForgeEntryTransformer.class.getDeclaredField("helperPlans");f.setAccessible(true);check(((Map<?,?>)f.get(t)).isEmpty(),"failed group publishes no plan "+label);check(Arrays.equals(prior,Files.readAllBytes(jar)),"source JAR immutable "+label);negatives++;
    }
    private static Path variant(String label,String owner,Consumer<ClassNode> mutate,boolean nativeMetadata)throws Exception {
        Path p=output.resolve(label+".jar");try(ZipFile in=new ZipFile(original.toFile());ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(p))){for(var e:in.stream().toList()){byte[] b=in.getInputStream(e).readAllBytes();String name=e.getName();if(name.equals(owner+".class")){ClassNode n=read(b);mutate.accept(n);b=bytes(n);}if(nativeMetadata && name.equals("META-INF/mods.toml")){name="META-INF/neoforge.mods.toml";b=new String(b,java.nio.charset.StandardCharsets.UTF_8).replace("helperprobe","nativeowner").getBytes(java.nio.charset.StandardCharsets.UTF_8);}out.putNextEntry(new ZipEntry(name));out.write(b);out.closeEntry();}}return p;
    }
    private static ModFile scan(Path jar,boolean nativeMetadata)throws Exception{var reader=nativeMetadata?new JarModsDotTomlModFileReader():new ForgeModReader();ModFile f=(ModFile)reader.read(JarContents.of(jar),ModFileDiscoveryAttributes.DEFAULT);if(f==null)throw new IllegalStateException("READER_DECLINED");f.setScanResult(f.compileContent(),null);LoadingModList.of(List.of(),List.of(f),List.of(),List.of(),Map.of());return f;}
    private static String causes(Throwable t){String s="";while(t!=null){s+=t.getMessage()+";";t=t.getCause();}return s;}
    private static InvokeDynamicInsnNode indy(ClassNode n){return Arrays.stream(method(n,"<clinit>").instructions.toArray()).filter(i->i instanceof InvokeDynamicInsnNode).map(i->(InvokeDynamicInsnNode)i).findFirst().orElseThrow();}
    private static List<String> methodBytes(MethodNode m){List<String> code=new ArrayList<>();for(var i:m.instructions){if(i.getOpcode()<0)continue;String s=""+i.getOpcode();if(i instanceof MethodInsnNode c)s+=c.owner+c.name+c.desc+c.itf;if(i instanceof LdcInsnNode l)s+=l.cst;if(i instanceof TypeInsnNode t)s+=t.desc;if(i instanceof FieldInsnNode f)s+=f.owner+f.name+f.desc;code.add(s);}return code;}
    private static MethodNode method(ClassNode n,String name){return n.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();}
    private static ClassNode read(byte[] b){return ForgeRegistryTransformationPlan.read(b);}
    private static byte[] bytes(ClassNode n){return ForgeRegistryTransformationPlan.bytes(n);}
    private static AbstractInsnNode lastReal(MethodNode m){AbstractInsnNode i=m.instructions.getLast();while(i.getOpcode()<0)i=i.getPrevious();return i;}
    private static void check(boolean b,String label){if(!b)throw new AssertionError(label);assertions++;System.out.println("PASS "+label);}
}
