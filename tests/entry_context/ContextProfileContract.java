package io.github.gfander.dualforge;

import cpw.mods.jarhandling.JarContents;
import cpw.mods.modlauncher.api.ITransformer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.VersionInfo;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.readers.JarModsDotTomlModFileReader;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** Official Reader/scan ownership plus exact source-ABI boundary and rejection atomicity. */
public final class ContextProfileContract {
    private static Path output;
    private static int assertions, negatives;
    private static final String ENTRY = "com/example/context/ContextProbe";
    private static final String SOURCE = ControlledForgeContextProfile.SOURCE;
    private static final String BRIDGE = ControlledForgeContextProfile.BRIDGE;

    public static void main(String[] args) throws Exception {
        output = Path.of(args[8]); Files.createDirectories(output);
        // Explicit offline discovery host; no GAME layer or synthetic runtime container.
        var version = FMLLoader.class.getDeclaredField("versionInfo"); version.setAccessible(true);
        version.set(null, new VersionInfo("21.1.256", "4.0.45", "1.21.1", "20240808.144430"));
        var dist = FMLLoader.class.getDeclaredField("dist"); dist.setAccessible(true); dist.set(null, Dist.DEDICATED_SERVER);
        for (String key : List.of("2a.rewrite", "0g.apiRewrite", "dump.profileDir")) System.clearProperty("dualforge." + key);
        ModFile file = scan(Path.of(args[0]), false);
        ClassNode source = read(Files.readAllBytes(file.findResource(ENTRY + ".class")));
        check(ForgeEntryProfile.select(source, false) == ForgeEntryProfile.Kind.CONTEXT, "non-final no-arg context classification");
        DualForgeEntryTransformer transformer = new DualForgeEntryTransformer();
        check(transformer.targets().equals(Set.of(ITransformer.Target.targetClass(ENTRY.replace('/', '.')))), "only actual scan-owned entry targeted");
        byte[] before = bytes(source); ClassNode changed = transformer.transform(source, null);
        check(Arrays.equals(before, bytes(source)), "successful transform leaves caller unchanged");
        check(method(changed, "<init>").desc.equals("()V"), "native host no-arg ABI retained");
        reverseAndCompare(source, changed, "fixture");
        Files.write(output.resolve("positive.before.class"), before); Files.write(output.resolve("positive.after.class"), bytes(changed));
        reject(transformer, source, "two-context-sources", n -> {
            InsnList code = new InsnList(); code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,SOURCE,"get","()L"+SOURCE+";",false));
            code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,SOURCE,"getModEventBus","()"+ControlledForgeContextProfile.BUS,false)); code.add(new InsnNode(Opcodes.POP)); ctor(n).instructions.insertBefore(ctor(n).instructions.getLast(),code);
        });
        reject(transformer, source, "extra-constructor", n -> { MethodNode m=new MethodNode(Opcodes.ACC_PRIVATE,"<init>","(I)V",null,null);m.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));m.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V",false));m.instructions.add(new InsnNode(Opcodes.RETURN));m.maxStack=1;m.maxLocals=2;n.methods.add(m); });
        reject(transformer, source, "static-getter-shape", n -> contextCall(n,"getModEventBus").setOpcode(Opcodes.INVOKESTATIC));
        reject(transformer, source, "wrong-context-return", n -> contextCall(n,"get").desc="()Ljava/lang/Object;");
        reject(transformer, source, "foreign-context-source", n -> contextCall(n,"get").owner="foreign/Context");
        reject(transformer, source, "unsafe-constructor-effect", n -> {InsnList code=new InsnList();code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"java/lang/System","nanoTime","()J",false));code.add(new InsnNode(Opcodes.POP2));ctor(n).instructions.insertBefore(ctor(n).instructions.getLast(),code);});
        reject(transformer, source, "foreign-metadata-owner", n -> n.visibleAnnotations.getFirst().values.set(1,"foreignowner"));
        reject(transformer, source, "native-annotation", n -> n.visibleAnnotations.getFirst().desc="Lnet/neoforged/fml/common/Mod;");
        reject(transformer, source, "forged-second-annotation", n -> n.visibleAnnotations.add(new AnnotationNode("Lnet/neoforged/fml/common/Mod;")));
        reject(transformer, source, "unverified-method-handle", n -> {InsnList code=new InsnList();code.add(new LdcInsnNode(new Handle(Opcodes.H_INVOKESTATIC,"foreign/Context","get","()Ljava/lang/Object;",false)));code.add(new InsnNode(Opcodes.POP));ctor(n).instructions.insertBefore(ctor(n).instructions.getLast(),code);});
        reject(transformer, source, "unverified-invokedynamic", n -> {InsnList code=new InsnList();code.add(new InvokeDynamicInsnNode("run","()Ljava/lang/Runnable;",new Handle(Opcodes.H_INVOKESTATIC,"foreign/Bootstrap","make","()Ljava/lang/invoke/CallSite;",false)));code.add(new InsnNode(Opcodes.POP));ctor(n).instructions.insertBefore(ctor(n).instructions.getLast(),code);});
        reject(transformer, source, "wrong-bus-local", n -> {for(var i:ctor(n).instructions)if(i instanceof VarInsnNode v && v.getOpcode()==Opcodes.ASTORE)v.var=2;ctor(n).maxLocals=3;});
        reject(transformer, source, "insufficient-stack", n -> ctor(n).maxStack=0);
        reject(transformer, source, "invalid-local-capacity", n -> ctor(n).maxLocals=0);
        reject(transformer, source, "unverified-static-initializer", n -> {MethodNode m=new MethodNode(Opcodes.ACC_STATIC,"<clinit>","()V",null,null);m.instructions.add(new InsnNode(Opcodes.RETURN));n.methods.add(m);});
        reject(transformer, source, "extra-entry-method", n -> n.methods.add(new MethodNode(Opcodes.ACC_PUBLIC,"unknown","()V",null,null)));
        reject(transformer, source, "context-escapes-to-field", n -> n.fields.getFirst().desc="L"+SOURCE+";");
        Path blocker=output.resolve("dump-blocker");Files.writeString(blocker,"blocked");System.setProperty("dualforge.dump.profileDir",blocker.toString());
        reject(transformer,source,"dump-failure-after-mapping",n -> {});System.clearProperty("dualforge.dump.profileDir");
        System.setProperty("dualforge.0g.apiRewrite","false");var disabledApi=new DualForgeEntryTransformer();disabledApi.targets();reject(disabledApi,source,"api-disabled",n -> {});System.clearProperty("dualforge.0g.apiRewrite");
        System.setProperty("dualforge.2a.rewrite","false");var disabled=new DualForgeEntryTransformer();disabled.targets();check(Arrays.equals(before,bytes(disabled.transform(source,null))),"explicit rewrite false preserves source");System.clearProperty("dualforge.2a.rewrite");
        ClassNode ordinary=read(before);ordinary.name="unclaimed/Ordinary";ordinary.visibleAnnotations=null;
        check(Arrays.equals(bytes(ordinary),bytes(transformer.transform(ordinary,null))),"unclaimed same-shape class unchanged");
        ClassNode ambiguous=read(before);ambiguous.fields.add(new FieldNode(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"unverified","Lnet/minecraftforge/registries/DeferredRegister;",null,null));
        selectionReject(ambiguous,"AMBIGUOUS_ENTRY");
        ClassNode unknown=read(before);ctor(unknown).instructions.clear();ctor(unknown).instructions.add(new VarInsnNode(Opcodes.ALOAD,0));ctor(unknown).instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V",false));ctor(unknown).instructions.add(new InsnNode(Opcodes.RETURN));
        selectionReject(unknown,"UNSUPPORTED_ENTRY");
        for (var test : List.of(Map.entry(args[2],ForgeEntryProfile.Kind.NORMAL_ITEM),Map.entry(args[3],ForgeEntryProfile.Kind.LEGACY_ITEM),Map.entry(args[4],ForgeEntryProfile.Kind.BLOCK))) {
            ModFile old=scan(Path.of(test.getKey()),false);String name=DualForgeLanguageLoader.forgeEntrypoints(old.getModInfos().getFirst(),old.getScanResult()).getFirst().replace('.','/');ClassNode node=read(Files.readAllBytes(old.findResource(name+".class")));
            check(ForgeEntryProfile.select(node,false)==test.getValue(),"old profile independently selected "+test.getValue());
            if(test.getValue()==ForgeEntryProfile.Kind.NORMAL_ITEM){node.access &= ~Opcodes.ACC_FINAL;byte[] prior=bytes(node);try{ControlledForgeItemProfile.validateNormal(node,old.getModInfos().getFirst().getModId());throw new AssertionError("Non-final old Normal accepted");}catch(IllegalStateException expected){check(expected.getMessage().contains("class structure"),"old class structure remains strict");}check(ForgeEntryProfile.select(node,false)==ForgeEntryProfile.Kind.NORMAL_ITEM && Arrays.equals(prior,bytes(node)),"malformed old profile cannot fall through to Context");}
        }
        ModFile probe=scan(Path.of(args[5]),false);String probeEntry=DualForgeLanguageLoader.forgeEntrypoints(probe.getModInfos().getFirst(),probe.getScanResult()).getFirst().replace('.','/');check(ForgeEntryProfile.select(read(Files.readAllBytes(probe.findResource(probeEntry+".class"))),true)==ForgeEntryProfile.Kind.EVENT,"controlled Event profile independently selected");
        scan(Path.of(args[6]),true);var nativeTransformer=new DualForgeEntryTransformer();check(nativeTransformer.targets().isEmpty() && Arrays.equals(before,bytes(nativeTransformer.transform(source,null))),"real native metadata not claimed or rewritten");
        try{ForgeConstructorContext.get();throw new AssertionError("Static caller acquired context");}catch(IllegalStateException expected){check(expected.getMessage().contains("outside entry constructor"),"escaped static context acquisition rejected");}
        byte[] fnfBefore=Files.readAllBytes(Path.of(args[1]));ModFile fnf=scan(Path.of(args[1]),false);String name=DualForgeLanguageLoader.forgeEntrypoints(fnf.getModInfos().getFirst(),fnf.getScanResult()).getFirst().replace('.','/');ClassNode entry=read(Files.readAllBytes(fnf.findResource(name+".class")));
        var owned=scope(fnf);var stats=ControlledForgeContextProfile.validate(entry,owned);check(stats.helpers()==2 && stats.pendingGlobalCalls()==1 && stats.receipts()==0,"real FnF helper boundaries accounted without adapting Registry");
        var realTransformer=new DualForgeEntryTransformer();check(realTransformer.targets().equals(Set.of(ITransformer.Target.targetClass(name.replace('/','.')))),"FnF target is entry only, no helper conversion");ClassNode actual=realTransformer.transform(entry,null);reverseAndCompare(entry,actual,"original-fnf");
        String helper="net/natevg/fnf/item/ItemRegistries";Set<String> missing=new java.util.HashSet<>(owned.classes());missing.remove(helper);
        profileReject(entry,new ControlledForgeContextProfile.Scope("fnf",missing,owned.read()),"helper-outside-owner");
        Set<String> missingSecond=new java.util.HashSet<>(owned.classes());missingSecond.remove("net/natevg/fnf/item/FnFCreativeModeTabs");
        profileReject(entry,new ControlledForgeContextProfile.Scope("fnf",missingSecond,owned.read()),"second-helper-failure-after-first-validation");
        ClassNode changedHelper=read(owned.read().apply(helper));method(changedHelper,"register").desc="(Lnet/neoforged/bus/api/IEventBus;)V";
        profileReject(entry,new ControlledForgeContextProfile.Scope("fnf",owned.classes(),owner -> owner.equals(helper)?bytes(changedHelper):owned.read().apply(owner)),"helper-descriptor-mismatch");
        check(Arrays.equals(fnfBefore,Files.readAllBytes(Path.of(args[1]))),"original FnF JAR never modified");
        // Real Reader rechecks candidate ownership and explicit module policy, not just a manually registered target.
        scan(Path.of(args[0]),false);Path ambiguousJar=jarVariant(Path.of(args[0]),ambiguous,null,"ambiguous");
        try{scan(ambiguousJar,false);new DualForgeEntryTransformer().targets();throw new AssertionError("Ambiguous entry accepted");}catch(IllegalStateException expected){check(expected.getMessage().contains("AMBIGUOUS_ENTRY"),"actual scan rejects ambiguous entry");}
        Path unsupportedJar=jarVariant(Path.of(args[0]),unknown,null,"unsupported");
        try{scan(unsupportedJar,false);new DualForgeEntryTransformer().targets();throw new AssertionError("Unknown entry accepted");}catch(IllegalStateException expected){check(expected.getMessage().contains("UNSUPPORTED_ENTRY"),"actual scan rejects unknown entry");}
        ClassNode module=new ClassNode();module.version=Opcodes.V21;module.access=Opcodes.ACC_MODULE;module.name="module-info";module.module=new ModuleNode("mismatched.explicit",0,null);module.module.requires=List.of(new ModuleRequireNode("java.base",Opcodes.ACC_MANDATED,null));
        Path explicit=jarVariant(Path.of(args[0]),source,module,"explicit");byte[] explicitBefore=Files.readAllBytes(explicit);
        try{scan(explicit,false);throw new AssertionError("JPMS silently remapped");}catch(IllegalStateException expected){check(expected.getMessage().contains("unsupported explicit Forge module identity"),"explicit JPMS mismatch rejected");}
        check(Arrays.equals(explicitBefore,Files.readAllBytes(explicit)),"JPMS refusal leaves JAR unchanged");
        System.out.println("CONTEXT PROFILE CONTRACT PASS assertions="+assertions+" negatives="+negatives);
    }

    private static ControlledForgeContextProfile.Scope scope(ModFile file){return new ControlledForgeContextProfile.Scope(file.getModInfos().getFirst().getModId(),file.getScanResult().getClasses().stream().map(c -> c.clazz().getInternalName()).collect(java.util.stream.Collectors.toSet()),owner -> {try{return Files.readAllBytes(file.findResource(owner+".class"));}catch(Exception e){throw new RuntimeException(e);}});}
    private static ModFile scan(Path jar,boolean nativeMetadata)throws Exception{ModFile f=(ModFile)(nativeMetadata?new JarModsDotTomlModFileReader().read(JarContents.of(jar),ModFileDiscoveryAttributes.DEFAULT):new ForgeModReader().read(JarContents.of(jar),ModFileDiscoveryAttributes.DEFAULT));check(f!=null,"actual Reader accepts "+jar.getFileName());f.setScanResult(f.compileContent(),null);LoadingModList.of(List.of(),List.of(f),List.of(),List.of(),Map.of());return f;}
    private static void reverseAndCompare(ClassNode before,ClassNode changed,String label)throws Exception{ClassNode restored=read(bytes(changed));int mapped=0;for(MethodNode m:restored.methods)for(var i:m.instructions)if(i instanceof MethodInsnNode call && call.owner.equals(BRIDGE)){mapped++;call.owner=SOURCE;if(call.name.equals("get"))call.desc="()L"+SOURCE+";";}check(mapped==2 && Arrays.equals(bytes(before),bytes(restored)),"only two Context calls changed; locals/frames/helper APIs preserved "+label);Files.write(output.resolve(label+".before.class"),bytes(before));Files.write(output.resolve(label+".after.class"),bytes(changed));}
    private static void reject(DualForgeEntryTransformer t,ClassNode source,String label,Consumer<ClassNode> mutate)throws Exception{ClassNode n=read(bytes(source));mutate.accept(n);byte[] before=bytes(n);Files.write(output.resolve(label+".before.class"),before);try{t.transform(n,null);throw new AssertionError("Accepted "+label);}catch(IllegalStateException expected){check(expected.getMessage().contains("Controlled Forge transformation rejected"),"explicit Context reject "+label);}check(Arrays.equals(before,bytes(n)),"caller unpolluted on failure "+label);Files.write(output.resolve(label+".caller-after.class"),bytes(n));negatives++;}
    private static void profileReject(ClassNode n,ControlledForgeContextProfile.Scope scope,String label){byte[] before=bytes(n);try{ControlledForgeContextProfile.rewrite(n,scope);throw new AssertionError("Accepted "+label);}catch(IllegalStateException expected){check(expected.getMessage().contains("Unsupported Forge Context entry"),"explicit helper refusal "+label);}check(Arrays.equals(before,bytes(n)),"plan validates before mutation "+label);negatives++;}
    private static void selectionReject(ClassNode n,String label){try{ForgeEntryProfile.select(n,false);throw new AssertionError("Accepted "+label);}catch(IllegalStateException expected){check(expected.getMessage().contains(label),"explicit classification "+label);}}
    private static Path jarVariant(Path original,ClassNode entry,ClassNode module,String label)throws Exception{Path p=output.resolve(label+".jar");try(JarFile old=new JarFile(original.toFile());ZipOutputStream z=new ZipOutputStream(Files.newOutputStream(p))){z.putNextEntry(new ZipEntry("META-INF/mods.toml"));z.write(old.getInputStream(old.getJarEntry("META-INF/mods.toml")).readAllBytes());z.closeEntry();z.putNextEntry(new ZipEntry(entry.name+".class"));z.write(bytes(entry));z.closeEntry();if(module!=null){z.putNextEntry(new ZipEntry("module-info.class"));z.write(bytes(module));z.closeEntry();}}return p;}
    private static ClassNode read(byte[] b){ClassNode n=new ClassNode();new ClassReader(b).accept(n,0);return n;}
    private static byte[] bytes(ClassNode n){ClassWriter w=new ClassWriter(0);n.accept(w);return w.toByteArray();}
    private static MethodNode method(ClassNode n,String name){return n.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();}
    private static MethodNode ctor(ClassNode n){return method(n,"<init>");}
    private static MethodInsnNode contextCall(ClassNode n,String name){return Arrays.stream(ctor(n).instructions.toArray()).filter(i -> i instanceof MethodInsnNode m && m.owner.equals(SOURCE)&&m.name.equals(name)).map(i -> (MethodInsnNode)i).findFirst().orElseThrow();}
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);assertions++;System.out.println("PASS "+label);}
}
