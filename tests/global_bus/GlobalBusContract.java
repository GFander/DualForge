package io.github.gfander.dualforge;

import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.*;
import cpw.mods.jarhandling.JarContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.*;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.*;

public final class GlobalBusContract {
    static final String E="org/example/combinedbus/EntryPoint",T="org/example/combinedbus/Shelves",I="org/example/combinedbus/Items",C="org/example/combinedbus/Consumer";
    static Path original,out;static PinnedHostOracle oracle;static String metadata;static int assertions,negatives;
    static void check(boolean valid,String label){if(!valid)throw new AssertionError(label);assertions++;System.out.println("PASS "+label);}
    static ClassNode read(byte[] b){return ForgeRegistryTransformationPlan.read(b);}static byte[] bytes(ClassNode n){return ForgeRegistryTransformationPlan.bytes(n);}
    static MethodNode method(ClassNode n,String name){return n.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();}
    static Map<String,ClassNode> nodes()throws Exception{Map<String,ClassNode> n=new TreeMap<>();try(ZipFile z=new ZipFile(original.toFile())){for(var e:z.stream().toList())if(e.getName().endsWith(".class")){var c=read(z.getInputStream(e).readAllBytes());n.put(c.name,c);}}return n;}
    static ModFile scan(Path jar)throws Exception{var f=(ModFile)new ForgeModReader().read(JarContents.of(jar),ModFileDiscoveryAttributes.DEFAULT);if(f==null)throw new IllegalStateException("SOURCE_ADMISSION_DECLINED");f.setScanResult(f.compileContent(),null);LoadingModList.of(List.of(),List.of(f),List.of(),List.of(),Map.of());return f;}
    static ForgeRegistryTransformationPlan plan(Path jar,String entry)throws Exception{var f=scan(jar);return ForgeRegistryTransformationPlan.create(f.getModInfos().getFirst(),entry,oracle);}
    static String business(MethodNode m){Textifier t=new Textifier();m.instructions.accept(new TraceMethodVisitor(t));return t.text.toString();}
    static FieldInsnNode global(ClassNode n){return Arrays.stream(method(n,"<init>").instructions.toArray()).filter(i->i instanceof FieldInsnNode f&&f.owner.equals(ControlledForgeGlobalBus.SOURCE)).map(i->(FieldInsnNode)i).findFirst().orElseThrow();}
    static AbstractInsnNode next(AbstractInsnNode i){do{i=i.getNext();}while(i!=null&&i.getOpcode()<0);return i;}
    static MethodInsnNode register(ClassNode n){return (MethodInsnNode)next(next(global(n)));}
    static void beforeReturn(MethodNode m,AbstractInsnNode op){m.instructions.insertBefore(Arrays.stream(m.instructions.toArray()).filter(i->i.getOpcode()==Opcodes.RETURN).findFirst().orElseThrow(),op);}
    static void reject(String label,String owner,Consumer<ClassNode> mutation,String expected)throws Exception{
        var ns=nodes();mutation.accept(ns.get(owner));Map<String,byte[]> before=new TreeMap<>();for(var n:ns.values())before.put(n.name,bytes(n));
        Path jar=out.resolve("test"+label.replace("-", "")+".jar");try(ZipOutputStream z=new ZipOutputStream(Files.newOutputStream(jar))){z.putNextEntry(new ZipEntry("META-INF/mods.toml"));z.write(metadata.getBytes(java.nio.charset.StandardCharsets.UTF_8));z.closeEntry();for(var n:ns.values()){z.putNextEntry(new ZipEntry(n.name+".class"));z.write(bytes(n));z.closeEntry();}}
        byte[] input=Files.readAllBytes(jar);ForgeRegistryTransformationPlan published=null;
        try{published=plan(jar,E);throw new AssertionError("Accepted "+label);}catch(IllegalStateException failure){check(failure.getMessage().contains(expected),label+" exact rejection category "+failure.getMessage());}
        check(published==null&&Arrays.equals(input,Files.readAllBytes(jar)),label+" entire plan rejected / source JAR unchanged");
        for(var n:ns.values())check(Arrays.equals(before.get(n.name),bytes(n)),label+" source ClassNode unchanged "+n.name);
        negatives++;System.out.println("REJECT "+label+" reason="+expected+" publishedTargets=none");
    }
    public static void main(String[] args)throws Exception{
        original=Path.of(args[0]);oracle=new PinnedHostOracle(Path.of(args[1]));out=Path.of(args[2]);Files.createDirectories(out);
        try(ZipFile z=new ZipFile(original.toFile())){metadata=new String(z.getInputStream(z.getEntry("META-INF/mods.toml")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}
        var v=FMLLoader.class.getDeclaredField("versionInfo");v.setAccessible(true);v.set(null,new VersionInfo("21.1.256","4.0.45","1.21.1","20240808.144430"));var d=FMLLoader.class.getDeclaredField("dist");d.setAccessible(true);d.set(null,Dist.DEDICATED_SERVER);
        byte[] input=Files.readAllBytes(original);var p=plan(original,E);check(p.targets().equals(Set.of(E,T,I,C))&&p.global!=null&&p.global.listeners()==1,"single complete typed/global closure");
        check(ForgeRegistryTransformationPlan.isCandidate(nodes().get(E)),"normal candidate includes proven global source");
        for(var n:nodes().values()){
            byte[] before=bytes(n);ClassNode after=p.transform(n,null);check(Arrays.equals(before,bytes(n)),"successful clone preserves "+n.name);RegistryBytecodeVerifier.verify(after);Files.write(out.resolve(n.name.substring(n.name.lastIndexOf('/')+1)+".before.class"),before);Files.write(out.resolve(n.name.substring(n.name.lastIndexOf('/')+1)+".after.class"),bytes(after));
            if(n.name.equals(E)){
                check(business(method(n,"started")).equals(business(method(after,"started"))),"listener business instructions unchanged");
                check(method(after,"started").desc.equals("(L"+ControlledForgeGlobalBus.HOST_EVENT+";)V")&&method(after,"started").visibleAnnotations.getFirst().desc.equals(ControlledForgeGlobalBus.HOST_ANNOTATION),"exact event/annotation mapping");
                check(Arrays.stream(method(after,"<init>").instructions.toArray()).filter(i->i instanceof MethodInsnNode c&&c.owner.equals(ControlledForgeGlobalBus.BRIDGE)).count()==1,"one bridge call in same cloned constructor");
                check(Arrays.stream(method(after,"<init>").instructions.toArray()).noneMatch(i->i instanceof FieldInsnNode f&&f.owner.equals(ControlledForgeGlobalBus.SOURCE)),"no unresolved source global field");
            }
        }
        for(String variant:List.of("quiet","signal")){
            String entry="org/example/"+variant+"bus/EntryPoint";var a=plan(Path.of(args[variant.equals("quiet")?3:4]),entry);check(a.targets().equals(Set.of(entry))&&a.global.listeners()==(variant.equals("quiet")?0:1),"standalone "+variant+" exact plan");
        }
        reject("wrong-field-name",E,n->global(n).name="WRONG","unverified constructor effect");
        reject("wrong-bus-owner",E,n->global(n).owner="foreign/Global","unverified constructor effect");
        reject("mod-bus-as-global",E,n->method(n,"<init>").instructions.set(global(n),new VarInsnNode(Opcodes.ALOAD,1)),"unverified constructor effect");
        reject("wrong-object",E,n->((VarInsnNode)next(global(n))).var=1,"unverified constructor effect");
        reject("null-object",E,n->method(n,"<init>").instructions.set(next(global(n)),new InsnNode(Opcodes.ACONST_NULL)),"unverified constructor effect");
        reject("wrong-opcode",E,n->register(n).setOpcode(Opcodes.INVOKEVIRTUAL),"unverified constructor effect");
        reject("wrong-interface-bit",E,n->register(n).itf=false,"unverified constructor effect");
        reject("unsupported-post",E,n->register(n).name="post","unverified constructor effect");
        reject("wrong-descriptor",E,n->register(n).desc="(Ljava/lang/Object;)Z","BYTECODE_FRAME_VERIFICATION");
        reject("duplicate-global",E,n->{InsnList ops=new InsnList();ops.add(new FieldInsnNode(Opcodes.GETSTATIC,ControlledForgeGlobalBus.SOURCE,"EVENT_BUS",ControlledForgeContextProfile.BUS));ops.add(new VarInsnNode(Opcodes.ALOAD,0));ops.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,"net/minecraftforge/eventbus/api/IEventBus","register","(Ljava/lang/Object;)V",true));method(n,"<init>").instructions.insertBefore(Arrays.stream(method(n,"<init>").instructions.toArray()).filter(i->i.getOpcode()==Opcodes.RETURN).findFirst().orElseThrow(),ops);},"duplicate global bus call");
        reject("static-listener",E,n->{var m=method(n,"started");m.access|=Opcodes.ACC_STATIC;},"LISTENER_CONTRACT");
        reject("private-listener",E,n->method(n,"started").access=Opcodes.ACC_PRIVATE,"LISTENER_CONTRACT");
        reject("inherited-listener",E,n->n.superName="foreign/Parent","class structure");
        reject("unknown-event",E,n->method(n,"started").desc="(Lnet/minecraftforge/event/entity/living/LivingEvent;)V","LISTENER_CONTRACT");
        reject("unknown-annotation",E,n->method(n,"started").visibleAnnotations.getFirst().desc="Lforeign/Listen;","LISTENER_CONTRACT");
        reject("explicit-priority",E,n->method(n,"started").visibleAnnotations.getFirst().values=List.of("priority",new String[]{"Lnet/minecraftforge/eventbus/api/EventPriority;","HIGH"}),"LISTENER_CONTRACT");
        reject("receive-cancelled",E,n->method(n,"started").visibleAnnotations.getFirst().values=List.of("receiveCanceled",true),"LISTENER_CONTRACT");
        reject("second-listener",E,n->{var source=method(n,"started");MethodNode m=new MethodNode(source.access,"another",source.desc,source.signature,null);source.accept(m);n.methods.add(m);},"LISTENER_CONTRACT");
        reject("handler-business-post",E,n->beforeReturn(method(n,"started"),new MethodInsnNode(Opcodes.INVOKESTATIC,"java/lang/System","gc","()V",false)),"LISTENER_BUSINESS");
        reject("handler-handle",E,n->{InsnList c=new InsnList();c.add(new LdcInsnNode(new Handle(Opcodes.H_INVOKESTATIC,"foreign/Owner","call","()V",false)));c.add(new InsnNode(Opcodes.POP));method(n,"started").instructions.insert(c);},"LISTENER_BUSINESS");
        reject("constructor-indy",E,n->{InsnList c=new InsnList();c.add(new InvokeDynamicInsnNode("get","()Ljava/lang/Object;",new Handle(Opcodes.H_INVOKESTATIC,"foreign/Bootstrap","call","()Ljava/lang/invoke/CallSite;",false)));c.add(new InsnNode(Opcodes.POP));method(n,"<init>").instructions.insertBefore(global(n),c);},"unverified constructor effect");
        reject("unsafe-timing-extra-method",E,n->{MethodNode m=new MethodNode(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"later","(Ljava/lang/Object;)V",null,null);m.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC,ControlledForgeGlobalBus.SOURCE,"EVENT_BUS",ControlledForgeContextProfile.BUS));m.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));m.instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,"net/minecraftforge/eventbus/api/IEventBus","register","(Ljava/lang/Object;)V",true));m.instructions.add(new InsnNode(Opcodes.RETURN));m.maxStack=2;m.maxLocals=1;n.methods.add(m);},"LISTENER_CONTRACT");
        reject("wrong-entry-owner",E,n->n.visibleAnnotations.getFirst().values=List.of("value","foreign"),"SOURCE_ADMISSION_DECLINED");
        reject("profile-conflict",E,n->n.fields.add(new FieldNode(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"other",ForgeRegistryTransformationPlan.R,null,null)),"unverified entry field");
        reject("global-frame",E,n->method(n,"<init>").maxStack=0,"BYTECODE_FRAME_VERIFICATION");
        reject("incomplete-consumer",C,n->method(n,"first").name="missing","MISSING_METHOD_CLOSURE");
        reject("tab-bootstrap-conflict",T,n->{for(var op:method(n,"make").instructions)if(op instanceof InvokeDynamicInsnNode i){i.bsm=new Handle(Opcodes.H_INVOKESTATIC,"foreign/B","make","()V",false);break;}},"TAB_CALLBACK_BOOTSTRAP");
        reject("typed-registry-conflict",T,n->{for(var op:method(n,"<clinit>").instructions)if(op instanceof FieldInsnNode f&&f.name.equals("CREATIVE_MODE_TAB"))f.name="BLOCK";},"WRONG_TYPED_REGISTRY_KEY");
        reject("unknown-type-annotation",E,n->{n.visibleTypeAnnotations=new ArrayList<>();n.visibleTypeAnnotations.add(new TypeAnnotationNode(TypeReference.newSuperTypeReference(-1).getValue(),null,"Lforeign/Unknown;"));},"ENTRY_ANNOTATION");
        var file=scan(original);var ns=nodes();Set<String> names=new HashSet<>(ns.keySet());names.add("net/neoforged/neoforge/event/server/ServerStartedEvent");
        Map<String,byte[]> raw=new TreeMap<>();for(var node:ns.values())raw.put(node.name,bytes(node));
        var scope=new ControlledForgeContextProfile.Scope("catalogbus",names,raw::get);
        try{ControlledForgeGlobalBus.validate(ns.get(E),scope,p.helperMethods,oracle);throw new AssertionError("Platform shadow accepted");}catch(IllegalStateException failure){check(failure.getMessage().equals("Global bus rejected: PLATFORM_CLASS_OWNER"),"platform shadow source rejected before publication");negatives++;}
        ClassNode conflict=read(bytes(ns.get(E)));conflict.fields.add(new FieldNode(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"other",ForgeRegistryTransformationPlan.R,null,null));
        try{ForgeEntryProfile.select(conflict,false);throw new AssertionError("Other Profile overlap accepted");}catch(IllegalStateException failure){check(failure.getMessage().startsWith("AMBIGUOUS_ENTRY"),"other Profile overlap fails classification");negatives++;}
        check(Arrays.equals(input,Files.readAllBytes(original)),"original Forge fixture immutable");
        System.out.println("GLOBAL BUS CONTRACT PASS assertions="+assertions+" negatives="+negatives);
    }
}
