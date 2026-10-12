package com.example.forgecompat;

import cpw.mods.jarhandling.JarContents;
import cpw.mods.modlauncher.api.ITransformer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

public final class EventBytecodeTest {
    private static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
        System.out.println("PASS " + name);
    }
    private static ClassNode read(byte[] bytes) {
        ClassNode n = new ClassNode(); new ClassReader(bytes).accept(n, 0); return n;
    }
    private static byte[] bytes(ClassNode n) {
        ClassWriter w = new ClassWriter(0); n.accept(w); return w.toByteArray();
    }
    private static ModFile file(Path p) throws Exception {
        ModFile f = (ModFile)new ForgeModReader().read(JarContents.of(p), ModFileDiscoveryAttributes.DEFAULT);
        f.setScanResult(f.compileContent(), null);
        LoadingModList.of(List.of(), List.of(f), List.of(), List.of(), Map.of()); return f;
    }
    private static MethodNode listener(ClassNode n) { return n.methods.stream().filter(m -> !m.name.equals("<init>")).findFirst().orElseThrow(); }
    private static byte[] methodBytes(ClassNode n) {
        ClassNode m = new ClassNode(); m.version=n.version; m.access=Opcodes.ACC_PUBLIC; m.name="MethodOnly"; m.superName="java/lang/Object";
        MethodNode source=listener(n), copy=new MethodNode(source.access,source.name,source.desc,source.signature,source.exceptions.toArray(String[]::new));
        source.accept(copy); copy.visibleAnnotations=null; m.methods.add(copy); return bytes(m);
    }
    public static void main(String[] args) throws Exception {
        Path out=Path.of(args[3]); Files.createDirectories(out);
        ModFile f=file(Path.of(args[0])); var t=new ForgeProbeConstructorTransformer();
        check(t.targets().equals(Set.of(ITransformer.Target.targetClass("com.example.dispatchcheck.DispatchProbe"), ITransformer.Target.targetClass("com.example.dispatchcheck.DispatchEvent"))),"exact scan-derived entry and Event targets");
        ClassNode before=read(Files.readAllBytes(f.findResource("com/example/dispatchcheck/DispatchProbe.class")));
        check(before.methods.getFirst().desc.equals("(Lnet/minecraftforge/eventbus/api/IEventBus;)V"),"native Forge constructor");
        ClassNode after=t.transform(before,null);
        check(after!=before && before.methods.getFirst().desc.equals("(Lnet/minecraftforge/eventbus/api/IEventBus;)V"),"successful copy leaves original untouched");
        check(Arrays.equals(methodBytes(before),methodBytes(after)),"listener instructions and code metadata byte-identical after annotation normalization");
        Files.write(out.resolve("listener.before.class"),methodBytes(before)); Files.write(out.resolve("listener.after.class"),methodBytes(after));
        Files.write(out.resolve("entry.before.class"),bytes(before)); Files.write(out.resolve("entry.after.class"),bytes(after));
        check(after.methods.getFirst().desc.equals("(Lnet/neoforged/bus/api/IEventBus;)V"),"Neo constructor");
        check(listener(after).visibleAnnotations.getFirst().desc.equals("Lnet/neoforged/bus/api/SubscribeEvent;"),"Neo annotation");
        // Invert exactly the authorized constructor/member/annotation changes; no other semantic edits.
        ClassNode restored=read(bytes(after)); restored.methods.getFirst().desc=before.methods.getFirst().desc;
        listener(restored).visibleAnnotations.getFirst().desc=listener(before).visibleAnnotations.getFirst().desc;
        int fields=0,calls=0;
        for (var insn:restored.methods.getFirst().instructions) {
            if(insn instanceof FieldInsnNode field && field.owner.equals("net/neoforged/neoforge/common/NeoForge")) { field.owner="net/minecraftforge/common/MinecraftForge";field.desc="Lnet/minecraftforge/eventbus/api/IEventBus;";fields++; }
            if(insn instanceof MethodInsnNode call && call.owner.equals("com/example/forgecompat/ForgeEmptyRegistration")) {call.setOpcode(Opcodes.INVOKEINTERFACE);call.owner="net/minecraftforge/eventbus/api/IEventBus";call.desc="(Ljava/lang/Object;)V";call.itf=true;calls++;}
        }
        check(fields==1 && calls==1 && Arrays.equals(bytes(before),bytes(restored)),"entry changed only authorized structures");
        ClassNode event=read(Files.readAllBytes(f.findResource("com/example/dispatchcheck/DispatchEvent.class"))), translated=t.transform(event,null);
        check(translated.superName.equals("net/neoforged/bus/api/Event"),"Neo Event superclass");
        Files.write(out.resolve("event.before.class"),bytes(event)); Files.write(out.resolve("event.after.class"),bytes(translated));
        translated.superName=event.superName; for(var insn:translated.methods.getFirst().instructions) if(insn instanceof MethodInsnNode m)m.owner=event.superName;
        check(Arrays.equals(bytes(event),bytes(translated)),"Event changed only superclass and super invocation");
        // Actual runtime dump must be the same canonical output of the production transformer.
        if (!args[2].equals("-")) {
            check(Arrays.equals(bytes(after),bytes(read(Files.readAllBytes(Path.of(args[2]).resolve("DispatchProbe.class"))))),"runtime entry dump equals controlled transform");
        }
        f=file(Path.of(args[1]));t.targets();
        ClassNode unclaimed=read(Files.readAllBytes(f.findResource("com/example/dispatchcontrol/UnclaimedMarker.class"))); byte[] same=bytes(unclaimed);
        check(Arrays.equals(same,bytes(t.transform(unclaimed,null))),"unclaimed marker listener unchanged");
        ClassNode bad=read(Files.readAllBytes(f.findResource("com/example/dispatchcontrol/RejectedProbe.class")));byte[] original=bytes(bad);
        Files.write(out.resolve("rejected.before.class"),original);
        boolean rejected=false;
        try { t.transform(bad,null); } catch(IllegalStateException e) { rejected=e.getCause().getMessage().contains("unsupported listener"); System.out.println("EXPECTED REJECTION " + e); }
        check(rejected,"extra call explicitly rejected");check(Arrays.equals(original,bytes(bad)),"rejected original node byte-identical and no partial edits");
        Files.write(out.resolve("rejected.after.class"),bytes(bad));
        System.out.println("PASS controlled dispatch bytecode and atomicity");
    }
}
