package io.github.gfander.dualforge;

import cpw.mods.jarhandling.JarContents;
import cpw.mods.modlauncher.api.ITransformer;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.readers.JarModsDotTomlModFileReader;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Actual FML scan ownership, per-call mapping, unchanged suppliers and whole-target atomicity. */
public final class BlockRegistryBytecodeTest {
    private static final String FORGE = "net/minecraftforge/registries/";
    private static final String NEO = "net/neoforged/neoforge/registries/";
    private static final String BLOCK = "net/minecraft/world/level/block/Block";
    private static final String BLOCK_ITEM = "net/minecraft/world/item/BlockItem";
    private static final String ITEM = "net/minecraft/world/item/Item";
    private static final String ORIGINAL_BUS = "(Lnet/minecraftforge/eventbus/api/IEventBus;)V";
    private static final String MAPPED_BUS = "(Lnet/neoforged/bus/api/IEventBus;)V";
    private static final String ITEM_REGISTRATION = "(Ljava/lang/String;Ljava/util/function/Supplier;)L" + FORGE + "RegistryObject;";
    private static final String MAIN_ENTRY = "com/example/blockregistrycheck/BlockRegistryProbe";
    private static final String MATRIX_ENTRY = "com/example/blockmatrix/RenamedBlockProbe";

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        System.out.println("PASS " + label);
    }

    private static ClassNode read(byte[] value) {
        ClassNode node = new ClassNode();
        new ClassReader(value).accept(node, 0);
        return node;
    }

    private static byte[] bytes(ClassNode node) {
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static MethodNode method(ClassNode node, String name) {
        return node.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();
    }

    private static MethodNode method(ClassNode node, Handle handle) {
        return node.methods.stream().filter(m -> m.name.equals(handle.getName())
                && m.desc.equals(handle.getDesc())).findFirst().orElseThrow();
    }

    private static List<InvokeDynamicInsnNode> suppliers(ClassNode node) {
        List<InvokeDynamicInsnNode> result = new ArrayList<>();
        for (AbstractInsnNode insn : method(node, "<clinit>").instructions) {
            if (insn instanceof InvokeDynamicInsnNode indy) result.add(indy);
        }
        return result;
    }

    private static AbstractInsnNode previousOpcode(AbstractInsnNode node) {
        AbstractInsnNode result = node.getPrevious();
        while (result != null && result.getOpcode() < 0) result = result.getPrevious();
        return result;
    }

    private static ModFile scan(Path jar, boolean nativeMetadata) throws Exception {
        ModFile file = (ModFile) (nativeMetadata
                ? new JarModsDotTomlModFileReader().read(JarContents.of(jar), ModFileDiscoveryAttributes.DEFAULT)
                : new ForgeModReader().read(JarContents.of(jar), ModFileDiscoveryAttributes.DEFAULT));
        check(file != null, "actual " + (nativeMetadata ? "native" : "Forge") + " Reader accepted " + jar.getFileName());
        file.setScanResult(file.compileContent(), null);
        LoadingModList.of(List.of(), List.of(file), List.of(), List.of(), Map.of());
        return file;
    }

    private static String exceptionText(Throwable failure) {
        StringWriter output = new StringWriter();
        failure.printStackTrace(new PrintWriter(output));
        return output.toString();
    }

    private static void reject(DualForgeEntryTransformer transformer, ClassNode source,
                               Consumer<ClassNode> mutation, Path output, String label) throws Exception {
        ClassNode bad = read(bytes(source));
        mutation.accept(bad);
        byte[] before = bytes(bad);
        Files.write(output.resolve(label + ".before.class"), before);
        IllegalStateException failure = null;
        try { transformer.transform(bad, null); }
        catch (IllegalStateException ex) { failure = ex; }
        check(failure != null && failure.getMessage().contains("Controlled Forge transformation rejected")
                && exceptionText(failure).contains("006B unsupported Block profile:"), label + " explicit controlled refusal");
        Files.writeString(output.resolve(label + ".reason.log"), exceptionText(failure));
        byte[] after = bytes(bad);
        Files.write(output.resolve(label + ".after.class"), after);
        check(Arrays.equals(before, after), label + " whole original ClassNode unchanged");
    }

    /** Use the real compiler-produced bootstrap and implementation; rebase only class ownership. */
    private static void replaceSupplier(ClassNode target, ClassNode forms, String factory, boolean captured) {
        InvokeDynamicInsnNode original = suppliers(target).getFirst();
        Handle oldHandle = (Handle) original.bsmArgs[1];
        MethodNode factoryMethod = method(forms, factory);
        InvokeDynamicInsnNode real = null;
        for (AbstractInsnNode insn : factoryMethod.instructions) {
            if (insn instanceof InvokeDynamicInsnNode indy) real = indy;
        }
        if (real == null) throw new AssertionError("missing actual compiler supplier form " + factory);
        Handle implementation = (Handle) real.bsmArgs[1];
        MethodNode copy = new MethodNode();
        method(forms, implementation).accept(copy);
        // MethodNode.accept(MethodVisitor) copies the body; the declaration metadata is explicit here.
        MethodNode actual = method(forms, implementation);
        copy.access = actual.access;
        copy.name = actual.name;
        copy.desc = actual.desc;
        copy.signature = actual.signature;
        copy.exceptions = new ArrayList<>(actual.exceptions);
        target.methods.removeIf(m -> m.name.equals(oldHandle.getName()) && m.desc.equals(oldHandle.getDesc()));
        target.methods.add(copy);
        original.name = real.name;
        original.desc = real.desc;
        original.bsm = real.bsm;
        original.bsmArgs = real.bsmArgs.clone();
        original.bsmArgs[1] = new Handle(implementation.getTag(), target.name,
                implementation.getName(), implementation.getDesc(), implementation.isInterface());
        if (captured) method(target, "<clinit>").instructions.insertBefore(original, new LdcInsnNode("captured marker"));
    }

    private static void dumpFailure(DualForgeEntryTransformer transformer, ClassNode source, Path output) throws Exception {
        Path blocked = output.resolve("dump-parent-is-a-file");
        Files.writeString(blocked, "This intentionally blocks Files.createDirectories; this is a test-only expected dump failure.\n");
        String previous = System.getProperty("dualforge.dump.profileDir");
        ClassNode input = read(bytes(source));
        byte[] before = bytes(input);
        Files.write(output.resolve("dump-io-failure.before.class"), before);
        IllegalStateException failure = null;
        try {
            System.setProperty("dualforge.dump.profileDir", blocked.toString());
            try { transformer.transform(input, null); }
            catch (IllegalStateException ex) { failure = ex; }
        } finally {
            if (previous == null) System.clearProperty("dualforge.dump.profileDir");
            else System.setProperty("dualforge.dump.profileDir", previous);
        }
        boolean ioCause = false;
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof IOException) ioCause = true;
        }
        check(failure != null && ioCause && failure.getMessage().contains("Controlled Forge transformation rejected"),
                "expected test-only dump IO failure after rewrite is explicit");
        Files.writeString(output.resolve("dump-io-failure.reason.log"), exceptionText(failure));
        Files.write(output.resolve("dump-io-failure.after.class"), bytes(input));
        check(Arrays.equals(before, bytes(input)), "failure after validated edits never exposes partial ClassNode mutation");
    }

    private static ClassNode positive(DualForgeEntryTransformer transformer, ClassNode source, Path output, String label) throws Exception {
        byte[] before = bytes(source); Files.write(output.resolve(label + ".before.class"), before);
        ClassNode mapped = transformer.transform(source, null);
        check(Arrays.equals(before, bytes(source)), label + " caller unchanged after successful mapping");
        check(method(mapped, "<init>").desc.equals(MAPPED_BUS), "original single-bus ABI preserved");
        check(suppliers(source).size() == 2, "exact two actual supplier declarations");
        for (int i = 0; i < 2; i++) {
            var old = suppliers(source).get(i); var now = suppliers(mapped).get(i);
            check(old.name.equals(now.name) && old.desc.equals(now.desc) && old.bsm.equals(now.bsm) && Arrays.deepEquals(old.bsmArgs,now.bsmArgs), "Bootstrap and original lambda implementation handle unchanged " + i);
        }
        ClassNode restored = read(bytes(mapped));
        int registers = 0, holders = 0, creates = 0, declares = 0, attaches = 0, gets = 0;
        for (int i = 0; i < source.fields.size(); i++) {
            FieldNode old = source.fields.get(i), now = restored.fields.get(i);
            check(old.name.equals(now.name), "field identity preserved");
            if (old.desc.equals("L" + FORGE + "DeferredRegister;")) {
                registers++; String type = old.signature.contains("<L" + BLOCK + ";>") ? BLOCK : ITEM;
                check(now.desc.equals("L" + NEO + "DeferredRegister;") && now.signature.equals("L" + NEO + "DeferredRegister<L" + type + ";>;"), "typed register field mapped");
            } else {
                holders++; boolean block = old.signature.contains("<L" + BLOCK + ";>");
                check(now.desc.equals("L" + NEO + "DeferredHolder;") && now.signature.equals("L" + NEO + "DeferredHolder<L" + (block ? BLOCK : ITEM) + ";L" + (block ? BLOCK : BLOCK_ITEM) + ";>;"), "typed holder field mapped");
            }
            now.desc = old.desc; now.signature = old.signature;
        }
        for (int m = 0; m < source.methods.size(); m++) {
            MethodNode oldMethod = source.methods.get(m), nowMethod = restored.methods.get(m);
            if (oldMethod.name.equals("<init>")) nowMethod.desc = oldMethod.desc;
            var oldOps = oldMethod.instructions.toArray(); var newOps = nowMethod.instructions.toArray();
            check(oldOps.length == newOps.length, "supplier/business instruction layout unchanged " + oldMethod.name);
            for (int i = 0; i < oldOps.length; i++) {
                if (oldOps[i] instanceof FieldInsnNode old && newOps[i] instanceof FieldInsnNode now) {
                    if (old.owner.equals(FORGE + "ForgeRegistries")) {
                        check(now.owner.equals("net/minecraft/core/registries/BuiltInRegistries") && now.name.equals(old.name.equals("BLOCKS") ? "BLOCK" : "ITEM") && now.desc.equals("Lnet/minecraft/core/DefaultedRegistry;"), "actual Registry source mapped");
                        now.owner=old.owner;now.name=old.name;now.desc=old.desc;
                    } else if (old.owner.equals(source.name)) {
                        FieldNode target=mapped.fields.stream().filter(f->f.name.equals(old.name)).findFirst().orElseThrow();
                        check(now.owner.equals(old.owner) && now.name.equals(old.name) && now.desc.equals(target.desc), "all owned field references coherent"); now.desc=old.desc;
                    }
                } else if (oldOps[i] instanceof MethodInsnNode old && newOps[i] instanceof MethodInsnNode now) {
                    if (old.owner.equals(FORGE+"DeferredRegister")) {
                        String expected;
                        if (old.name.equals("create")) { creates++;expected="(Lnet/minecraft/core/Registry;Ljava/lang/String;)L"+NEO+"DeferredRegister;"; }
                        else if (old.desc.equals(ORIGINAL_BUS)) { attaches++;expected=MAPPED_BUS; }
                        else { declares++;expected="(Ljava/lang/String;Ljava/util/function/Supplier;)L"+NEO+"DeferredHolder;"; }
                        check(now.owner.equals(NEO+"DeferredRegister") && now.desc.equals(expected) && now.name.equals(old.name), "exact Registry call-site mapping");now.owner=old.owner;now.desc=old.desc;
                    } else if (old.owner.equals(FORGE+"RegistryObject")) {
                        gets++;check(now.owner.equals(NEO+"DeferredHolder") && now.name.equals("get") && now.desc.equals("()Ljava/lang/Object;"), "precise Holder.get including BlockItem dependency");now.owner=old.owner;
                    }
                }
            }
        }
        check(registers==2 && holders==2 && creates==2 && declares==2 && attaches==2 && gets==3, "two registries and all three holder uses accounted");
        check(Arrays.equals(before,bytes(restored)), "full canonical bytes equal after reversing only authorized edits");
        java.io.StringWriter verify = new java.io.StringWriter();
        org.objectweb.asm.util.CheckClassAdapter.verify(new ClassReader(bytes(mapped)),false,new java.io.PrintWriter(verify));
        check(verify.toString().isEmpty(), "ASM data-flow verification passes");
        Files.write(output.resolve(label+".after.class"),bytes(mapped));return mapped;
    }
    private static void duplicate(ClassNode n, int role) {
        MethodNode init=method(n,"<clinit>");InvokeDynamicInsnNode indy=suppliers(n).get(role);
        FieldInsnNode receiver=(FieldInsnNode)previousOpcode(previousOpcode(indy));
        LdcInsnNode id=(LdcInsnNode)previousOpcode(indy);
        InsnList extra=new InsnList();extra.add(new FieldInsnNode(receiver.getOpcode(),receiver.owner,receiver.name,receiver.desc));extra.add(new LdcInsnNode(id.cst));
        extra.add(new InvokeDynamicInsnNode(indy.name,indy.desc,indy.bsm,indy.bsmArgs.clone()));extra.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,FORGE+"DeferredRegister","register",ITEM_REGISTRATION,false));extra.add(new InsnNode(Opcodes.POP));
        AbstractInsnNode last=init.instructions.getLast();while(last.getOpcode()<0)last=last.getPrevious();init.instructions.insertBefore(last,extra);
    }
    private static void registry(ClassNode n, String old, String replacement) {
        for (AbstractInsnNode i:method(n,"<clinit>").instructions) if(i instanceof FieldInsnNode f && f.owner.equals(FORGE+"ForgeRegistries") && f.name.equals(old))f.name=replacement;
    }
    public static void main(String[] args) throws Exception {
        Path output=Path.of(args[3]);Files.createDirectories(output);
        ModFile file=scan(Path.of(args[0]),false);DualForgeEntryTransformer transformer=new DualForgeEntryTransformer();
        check(transformer.targets().equals(Set.of(ITransformer.Target.targetClass(MAIN_ENTRY.replace('/','.')))),"only actual owned entry targeted");
        ClassNode source=read(Files.readAllBytes(file.findResource(MAIN_ENTRY+".class")));
        ClassNode mapped=positive(transformer,source,output,"entry");
        if(!args[4].equals("-"))check(Arrays.equals(bytes(mapped),bytes(read(Files.readAllBytes(Path.of(args[4]).resolve("BlockRegistryProbe.class"))))),"actual runtime dump equals controlled mapping");
        ClassNode forms=read(Files.readAllBytes(file.findResource("com/example/blockregistrycheck/SupplierForms.class")));
        reject(transformer,source,n->registry(n,"BLOCKS","ITEMS"),output,"wrong-block-registry");
        reject(transformer,source,n->registry(n,"ITEMS","BLOCKS"),output,"wrong-item-registry");
        reject(transformer,source,n->registry(n,"ITEMS","ENTITY_TYPES"),output,"unsupported-registry");
        reject(transformer,source,n->{for(AbstractInsnNode i:method(n,"<clinit>").instructions)if(i instanceof LdcInsnNode l && l.cst.equals("blockregistryprobe"))l.cst="other_owner";},output,"wrong-register-owner");
        reject(transformer,source,n->duplicate(n,0),output,"duplicate-block-id");
        reject(transformer,source,n->duplicate(n,1),output,"duplicate-item-id");
        reject(transformer,source,n->{var handle=(Handle)suppliers(n).get(1).bsmArgs[1];for(AbstractInsnNode i:method(n,handle).instructions)if(i instanceof FieldInsnNode f && f.owner.equals(n.name))f.name="UNKNOWN_BLOCK";},output,"unknown-block-holder");
        reject(transformer,source,n->n.fields.stream().filter(f->f.signature.contains("RegistryObject<L"+BLOCK+";>")).findFirst().orElseThrow().signature="L"+FORGE+"RegistryObject<L"+ITEM+";>;",output,"wrong-holder-type");
        reject(transformer,source,n->{var indy=suppliers(n).get(1);var h=(Handle)indy.bsmArgs[1];indy.bsmArgs[1]=new Handle(h.getTag(),h.getOwner(),h.getName(),"()L"+ITEM+";",false);},output,"wrong-supplier-return");
        reject(transformer,source,n->replaceSupplier(n,forms,"captured",true),output,"captured-supplier");
        reject(transformer,source,n->replaceSupplier(n,forms,"alternate",false),output,"alternate-bootstrap");
        reject(transformer,source,n->{var i=suppliers(n).getFirst();i.bsm=new Handle(Opcodes.H_INVOKESTATIC,"unknown/Factory","get",i.bsm.getDesc(),false);},output,"unknown-bootstrap");
        reject(transformer,source,n->{var init=method(n,"<clinit>");InsnList extra=new InsnList();extra.add(new FieldInsnNode(Opcodes.GETSTATIC,n.name,"ITEMS","L"+FORGE+"DeferredRegister;"));extra.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,FORGE+"DeferredRegister","getEntries","()Ljava/util/Collection;",false));extra.add(new InsnNode(Opcodes.POP));AbstractInsnNode last=init.instructions.getLast();while(last.getOpcode()<0)last=last.getPrevious();init.instructions.insertBefore(last,extra);},output,"extra-forge-api");
        reject(transformer,source,n->{for(AbstractInsnNode i:method(n,"<init>").instructions)if(i instanceof MethodInsnNode call && call.owner.equals(FORGE+"DeferredRegister"))((VarInsnNode)previousOpcode(call)).var=0;},output,"wrong-modbus");
        reject(transformer,source,n->{MethodNode ctor=method(n,"<init>");for(AbstractInsnNode i:ctor.instructions.toArray())if(i instanceof MethodInsnNode call && call.owner.equals(FORGE+"DeferredRegister")){AbstractInsnNode load=previousOpcode(call),field=previousOpcode(load);ctor.instructions.remove(field);ctor.instructions.remove(load);ctor.instructions.remove(call);break;}},output,"missing-attachment");
        reject(transformer,source,n->{for(AbstractInsnNode i:method(n,"itemValue").instructions)if(i instanceof MethodInsnNode c)c.desc="()Ljava/lang/String;";},output,"late-getter-failure");
        reject(transformer,source,n->n.visibleAnnotations.getFirst().values=List.of("value","unknown_owner"),output,"unrecognized-entry-owner");
        dumpFailure(transformer,source,output);
        ClassNode ordinary=read(bytes(source));ordinary.name="unclaimed/Ordinary";byte[] ordinaryBefore=bytes(ordinary);
        check(Arrays.equals(ordinaryBefore,bytes(transformer.transform(ordinary,null))),"unclaimed same-ABI class unchanged");
        Files.write(output.resolve("unclaimed.before.class"),ordinaryBefore);Files.write(output.resolve("unclaimed.after.class"),bytes(ordinary));
        // An additional actually scanned Forge @Mod entry must refuse the whole target set.
        Path extraJar=output.resolve("extra-entry.jar");
        try(java.util.jar.JarFile jar=new java.util.jar.JarFile(args[0]);java.util.jar.JarOutputStream out=new java.util.jar.JarOutputStream(Files.newOutputStream(extraJar))) {
            var entries=jar.entries();while(entries.hasMoreElements()){var e=entries.nextElement();out.putNextEntry(new java.util.jar.JarEntry(e.getName()));out.write(jar.getInputStream(e).readAllBytes());out.closeEntry();}
            ClassNode extra=new ClassNode();extra.version=source.version;extra.access=Opcodes.ACC_PUBLIC;extra.name="extra/Entry";extra.superName="java/lang/Object";
            var annotation=new org.objectweb.asm.tree.AnnotationNode("Lnet/minecraftforge/fml/common/Mod;");annotation.values=List.of("value","extra_entry");extra.visibleAnnotations=List.of(annotation);
            out.putNextEntry(new java.util.jar.JarEntry("extra/Entry.class"));out.write(bytes(extra));out.closeEntry();
        }
        scan(extraJar,false);boolean rejected=false;try{transformer.targets();}catch(IllegalStateException e){rejected=e.getMessage().contains("one same-file Forge @Mod");Files.writeString(output.resolve("extra-entry.reason.log"),exceptionText(e));}
        check(rejected,"actual extra scanned entry rejected before transformation");check(Arrays.equals(bytes(source),Files.readAllBytes(output.resolve("entry.before.class"))),"extra-entry rejection does not edit source");
        ModFile matrix=scan(Path.of(args[2]),false);DualForgeEntryTransformer mt=new DualForgeEntryTransformer();
        check(mt.targets().equals(Set.of(ITransformer.Target.targetClass(MATRIX_ENTRY.replace('/','.')))),"independent renamed modId fields classes derived from scan");
        positive(mt,read(Files.readAllBytes(matrix.findResource(MATRIX_ENTRY+".class"))),output,"renamed");
        scan(Path.of(args[1]),true);check(transformer.targets().isEmpty(),"native metadata path not claimed");
        byte[] before=bytes(source);check(Arrays.equals(before,bytes(transformer.transform(read(before),null))),"native same bytes remain unchanged");
        Files.write(output.resolve("native.before.class"),before);Files.write(output.resolve("native.after.class"),bytes(source));
        System.out.println("PASS controlled Block pair scope and atomicity");
    }
}
