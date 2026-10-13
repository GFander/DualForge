package com.example.forgecompat;

import cpw.mods.jarhandling.JarContents;
import cpw.mods.modlauncher.api.ITransformer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.readers.JarModsDotTomlModFileReader;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

public final class RegistryBytecodeTest {
    static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
        System.out.println("PASS " + name);
    }
    static ClassNode read(byte[] b) { ClassNode n=new ClassNode(); new ClassReader(b).accept(n,0); return n; }
    static byte[] bytes(ClassNode n) { ClassWriter w=new ClassWriter(0); n.accept(w); return w.toByteArray(); }
    static MethodNode method(ClassNode n,String name) {return n.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();}
    static void reject(ForgeProbeConstructorTransformer t,ClassNode source,Consumer<ClassNode> edit,Path out,String label) throws Exception {
        ClassNode bad=read(bytes(source));edit.accept(bad);byte[] before=bytes(bad);
        Files.write(out.resolve(label+".before.class"),before);
        boolean rejected=false;
        try {t.transform(bad,null);} catch(IllegalStateException ex) {rejected=ex.getCause().getMessage().contains("005 unsupported Item profile");}
        check(rejected,label+" explicitly rejected");
        check(Arrays.equals(before,bytes(bad)),label+" original unchanged");
        Files.write(out.resolve(label+".after.class"),bytes(bad));
    }
    public static void main(String[] args) throws Exception {
        Path out=Path.of(args[3]);Files.createDirectories(out);
        ModFile file=(ModFile)new ForgeModReader().read(JarContents.of(Path.of(args[0])),ModFileDiscoveryAttributes.DEFAULT);
        file.setScanResult(file.compileContent(),null);
        LoadingModList.of(List.of(),List.of(file),List.of(),List.of(),Map.of());
        var t=new ForgeProbeConstructorTransformer();
        check(t.targets().equals(Set.of(ITransformer.Target.targetClass("com.example.registryprobe.RegistryProbe"))),"only scan-owned registry entry targeted");
        ClassNode before=read(Files.readAllBytes(file.findResource("com/example/registryprobe/RegistryProbe.class")));
        byte[] original=bytes(before);ClassNode after=t.transform(before,null);
        check(Arrays.equals(original,bytes(before)),"successful transform preserves original");
        Files.write(out.resolve("entry.before.class"),original);Files.write(out.resolve("entry.after.class"),bytes(after));
        check(method(after,"<init>").desc.equals("(Lnet/neoforged/bus/api/IEventBus;)V"),"same single-argument constructor ABI mapped");
        check(after.fields.stream().anyMatch(f->f.desc.equals("Lnet/neoforged/neoforge/registries/DeferredHolder;")),"holder field type mapped without cast");
        check(Arrays.equals(supplierBytes(before),supplierBytes(after)),"supplier instructions metadata and bootstrap implementation unchanged");
        ClassNode restored=read(bytes(after));
        method(restored,"<init>").desc=method(before,"<init>").desc;
        for(int i=0;i<before.fields.size();i++) {restored.fields.get(i).desc=before.fields.get(i).desc;restored.fields.get(i).signature=before.fields.get(i).signature;}
        // Pair every instruction with the original; only the documented member owner/descriptors may differ.
        for(int m=0;m<before.methods.size();m++) {
            var old=before.methods.get(m).instructions.toArray();var now=restored.methods.get(m).instructions.toArray();
            check(old.length==now.length,"no added/removed instructions in "+before.methods.get(m).name);
            for(int i=0;i<old.length;i++) {
                if(now[i] instanceof FieldInsnNode f && old[i] instanceof FieldInsnNode o) {
                    if(!f.owner.equals(o.owner)||!f.desc.equals(o.desc)||!f.name.equals(o.name)) {
                        check(o.owner.equals(before.name)||o.owner.equals("net/minecraftforge/registries/ForgeRegistries"),"authorized field site");
                        f.owner=o.owner;f.name=o.name;f.desc=o.desc;
                    }
                } else if(now[i] instanceof MethodInsnNode c && old[i] instanceof MethodInsnNode o) {
                    if(!c.owner.equals(o.owner)||!c.desc.equals(o.desc)) {
                        check(o.owner.equals("net/minecraftforge/registries/DeferredRegister")||o.owner.equals("net/minecraftforge/registries/RegistryObject"),"authorized method site");
                        c.owner=o.owner;c.desc=o.desc;
                    }
                }
            }
        }
        check(Arrays.equals(original,bytes(restored)),"only authorized constructor field signature and member edits");
        reject(t,before,n->n.fields.getFirst().desc="Ljava/lang/Object;",out,"wrong-field");
        reject(t,before,n->{
            MethodNode m=method(n,"<clinit>");var ops=new java.util.ArrayList<AbstractInsnNode>();
            for(var i:m.instructions)if(i.getOpcode()>=0)ops.add(i);
            InsnList extra=new InsnList();for(int i=4;i<=7;i++)extra.add(ops.get(i).clone(new HashMap<>()));
            extra.add(new InsnNode(Opcodes.POP));m.instructions.insertBefore(ops.getLast(),extra);
        },out,"extra-registration");
        reject(t,before,n->{for(var i:method(n,"<clinit>").instructions)if(i instanceof InvokeDynamicInsnNode d)d.bsm=new Handle(Opcodes.H_INVOKESTATIC,"unknown/Factory","make",d.bsm.getDesc(),false);},out,"unknown-bootstrap");
        reject(t,before,n->{for(var i:method(n,"<clinit>").instructions)if(i instanceof InvokeDynamicInsnNode d){Handle h=(Handle)d.bsmArgs[1];d.bsmArgs[1]=new Handle(h.getTag(),"unowned/Other",h.getName(),h.getDesc(),false);}},out,"unowned-lambda");
        reject(t,before,n->{for(var i:method(n,"<init>").instructions)if(i instanceof VarInsnNode v && v.var==1)v.var=0;},out,"wrong-bus-argument");
        ClassNode ordinary=read(original);ordinary.name="unclaimed/Ordinary";
        for(var m:ordinary.methods)for(var i:m.instructions) {
            if(i instanceof FieldInsnNode f && f.owner.equals(before.name))f.owner=ordinary.name;
            if(i instanceof InvokeDynamicInsnNode d){Handle h=(Handle)d.bsmArgs[1];d.bsmArgs[1]=new Handle(h.getTag(),ordinary.name,h.getName(),h.getDesc(),false);}
        }
        byte[] plain=bytes(ordinary);
        check(Arrays.equals(plain,bytes(t.transform(ordinary,null))),"unclaimed same-shaped class unchanged");
        Files.write(out.resolve("ordinary.before.class"),plain);Files.write(out.resolve("ordinary.after.class"),bytes(ordinary));
        if(!args[2].equals("-"))check(Arrays.equals(bytes(after),bytes(read(Files.readAllBytes(Path.of(args[2]).resolve("RegistryProbe.class"))))),"runtime dump matches exact controlled transform");
        ModFile nativeFile=(ModFile)new JarModsDotTomlModFileReader().read(JarContents.of(Path.of(args[1])),ModFileDiscoveryAttributes.DEFAULT);
        nativeFile.setScanResult(nativeFile.compileContent(),null);
        LoadingModList.of(List.of(),List.of(nativeFile),List.of(),List.of(),Map.of());
        check(t.targets().isEmpty(),"native metadata with identical Forge-shaped entry not claimed");
        check(Arrays.equals(original,bytes(t.transform(read(original),null))),"native-path entry unchanged");
        Files.write(out.resolve("native.before.class"),original);Files.write(out.resolve("native.after.class"),bytes(t.transform(read(original),null)));
        System.out.println("PASS registry bytecode scope and atomicity");
    }
    static byte[] supplierBytes(ClassNode n) {
        ClassNode c=new ClassNode();c.version=n.version;c.access=Opcodes.ACC_PUBLIC;c.name="SupplierOnly";c.superName="java/lang/Object";
        n.methods.stream().filter(m->m.name.startsWith("lambda$")).forEach(c.methods::add);return bytes(c);
    }
}
