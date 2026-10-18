package io.github.gfander.dualforge;

import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Bytecode planning tests only; native owner/lifecycle tests live in the separate observer. */
public final class ClosureContract {
    static int assertions;
    static Map<String,byte[]> source;
    static String entry,router,config,id;
    static final Map<String,byte[]> host=new HashMap<>();
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);assertions++;}
    static ClassNode node(String name){return ForgeDualBusPlan.read(source.get(name));}
    static Map<String,byte[]> changed(String name,Consumer<ClassNode> mutation){Map<String,byte[]> copy=new TreeMap<>(source);ClassNode n=node(name);mutation.accept(n);copy.put(name,ForgeDualBusPlan.bytes(n));return copy;}
    static ForgeDualBusPlan plan(Map<String,byte[]> raw){return ForgeDualBusPlan.validate(id,id,entry,raw,host::get);}
    static void reject(String label,Map<String,byte[]> raw,String reason){Map<String,String> before=new TreeMap<>();raw.forEach((k,v)->before.put(k,ForgeDualBusPlan.digest(v)));
        try{plan(raw);throw new AssertionError("Unexpected approval "+label);}catch(IllegalStateException e){check(e.getMessage().contains(reason),label+" actual="+e);System.out.println("NEGATIVE "+label+" => "+e.getMessage());}
        check(raw.entrySet().stream().allMatch(e->before.get(e.getKey()).equals(ForgeDualBusPlan.digest(e.getValue()))),"reject never mutates source "+label);
    }
    static void duplicateCall(MethodNode method,String owner,String name,int inputs) {
        var ops=ForgeRegistryTransformationPlan.ops(method);
        for(int at=0;at<ops.size();at++)if(ops.get(at) instanceof MethodInsnNode call && call.owner.equals(owner) && call.name.equals(name)) {
            InsnList extra=new InsnList();for(int i=at-inputs;i<=at;i++)extra.add(ops.get(i).clone(new HashMap<>()));
            method.instructions.insert(call,extra);return;
        }
        throw new AssertionError("Missing duplicate source call");
    }
    static MethodNode ctor(ClassNode n){return n.methods.stream().filter(m->m.name.equals("<init>")).findFirst().orElseThrow();}
    static MethodNode registration(ClassNode n){return n.methods.stream().filter(m->!m.name.equals("<init>") && m.desc.equals(ForgeDualBusPlan.PAIR) && m.name.equals("register")).findFirst().orElseThrow();}
    public static void main(String[] args)throws Exception {
        for(Path jar:Files.walk(Path.of(args[2])).filter(p->p.toString().endsWith(".jar")).toList())try(JarFile zip=new JarFile(jar.toFile())){
            for(var e:zip.stream().filter(e->e.getName().startsWith("net/neoforged/") && e.getName().endsWith(".class")).toList())host.put(e.getName().replace(".class",""),zip.getInputStream(e).readAllBytes());
        }
        Path output=Path.of(args[3]);Files.createDirectories(output);
        for(int which=0;which<2;which++) {
            source=new TreeMap<>();try(JarFile jar=new JarFile(args[which])){for(var e:jar.stream().filter(e->e.getName().endsWith(".class")).toList())source.put(e.getName().replace(".class",""),jar.getInputStream(e).readAllBytes());}
            id=which==0?"flowalpha":"flowbeta";entry="org/example/"+id+"/Entry";router="org/example/"+id+"/Subscriptions";config="org/example/"+id+"/Settings";
            ForgeDualBusPlan p=plan(source);check(p.targets().equals(source.keySet()) && p.targets().size()==20,"twenty-class exact composition");
            check(p.network.closure!=null && p.network.payloads.size()==6,"finite six payload closure");
            check(new HashSet<>(p.network.closure.codecKinds.values()).size()==3,"three verified codec forms");
            String net="org/example/"+id+"/Network",global="org/example/"+id+"/Globals",up="org/example/"+id+"/UpTuple",unit="org/example/"+id+"/UpUnit",down="org/example/"+id+"/DownTuple";
            for(var row:source.entrySet()) {
                ClassNode original=node(row.getKey());byte[] before=ForgeDualBusPlan.bytes(original);
                ClassNode mapped=p.transform(original,HexFormat.of().parseHex(ForgeDualBusPlan.digest(row.getValue())));
                check(Arrays.equals(before,ForgeDualBusPlan.bytes(original)),"clone only");
                Path target=output.resolve(row.getKey()+".class");Files.createDirectories(target.getParent());Files.write(target,ForgeDualBusPlan.bytes(mapped));
                try{p.transform(original,new byte[32]);throw new AssertionError("SHA accepted");}catch(IllegalStateException e){check(e.getMessage().contains("INPUT_SHA_CHANGED"),"SHA guard");}
            }
            reject("GLOBAL_CONNECTION_OWNER_MISMATCH",changed(global,n->{for(var m:n.methods)if(m.name.equals("set"))m.access=Opcodes.ACC_PRIVATE|Opcodes.ACC_STATIC;}),"GLOBAL_CONNECTION_OWNER_MISMATCH");
            reject("GLOBAL_CONNECTION_WRONG_ORDER",changed(net,n->{for(var op:ctor(n).instructions)if(op instanceof FieldInsnNode f && f.name.equals("server") && f.getOpcode()==Opcodes.GETFIELD){f.name="client";break;}}),"UNKNOWN_BUS_ORIGIN");
            reject("DUPLICATE_GLOBAL_CONNECTION_WRITE",changed(global,n->{MethodNode m=n.methods.stream().filter(x->x.name.equals("set")).findFirst().orElseThrow();var ops=ForgeRegistryTransformationPlan.ops(m);InsnList again=new InsnList();again.add(ops.get(0).clone(new HashMap<>()));again.add(ops.get(1).clone(new HashMap<>()));m.instructions.insert(ops.get(1),again);}),"UNKNOWN_CONSTRUCTOR_EFFECT");
            reject("UNKNOWN_STATIC_SIDE_EFFECT",changed(global,n->{var m=n.methods.stream().filter(x->x.name.equals("<clinit>")).findFirst().orElseThrow();InsnList effect=new InsnList();effect.add(new LdcInsnNode("010e.unverified"));effect.add(new LdcInsnNode("effect"));effect.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"java/lang/System","setProperty","(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",false));effect.add(new InsnNode(Opcodes.POP));m.instructions.insertBefore(m.instructions.getLast(),effect);}),"UNKNOWN_CONSTRUCTOR_EFFECT");
            reject("WRONG_NETWORK_HANDLER_IDENTITY",changed("org/example/"+id+"/ToServer",n->{for(var op:ctor(n).instructions.toArray())if(op instanceof VarInsnNode v && v.getOpcode()==Opcodes.ALOAD && v.var==1){ctor(n).instructions.set(op,new InsnNode(Opcodes.ACONST_NULL));break;}}),"UNKNOWN_CONSTRUCTOR_EFFECT");
            reject("UNKNOWN_CONTEXT_METHOD",changed(net,n->rename(n,"getSender","unknownContext")),"UNKNOWN_CONSTRUCTOR_EFFECT");
            reject("CONTEXT_SENDER_UNVERIFIED",changed(net,n->rename(n,"getSender","getPlayerUnchecked")),"UNKNOWN_CONSTRUCTOR_EFFECT");
            reject("CONTEXT_THREAD_UNVERIFIED",changed(net,n->rename(n,"enqueueWork","runImmediately")),"UNKNOWN_CONSTRUCTOR_EFFECT");
            reject("CONTEXT_ENQUEUE_UNVERIFIED",changed(net,n->{for(var m:n.methods)for(var op:m.instructions)if(op instanceof MethodInsnNode call && call.name.equals("enqueueWork"))call.desc="(Ljava/lang/Runnable;)Ljava/lang/Object;";}),"UNKNOWN_CONSTRUCTOR_EFFECT");
            reject("CONTEXT_HANDLED_STATE_UNVERIFIED",changed(net,n->{for(var m:n.methods)if(m.name.equals("uptuple"))for(var op:m.instructions.toArray())if(op.getOpcode()==Opcodes.ICONST_1)m.instructions.set(op,new InsnNode(Opcodes.ICONST_0));}),"UNKNOWN_CONSTRUCTOR_EFFECT");
            reject("UNKNOWN_PAYLOAD_TYPE",changed(up,n->n.interfaces.clear()),"PARTIAL_NETWORK_PLAN");
            reject("DUPLICATE_PAYLOAD_ID",changed(down,n->{for(var m:n.methods)for(var op:m.instructions)if(op instanceof LdcInsnNode l && "downtuple".equals(l.cst))l.cst="uptuple";}),"DUPLICATE_PAYLOAD_ID");
            reject("WRONG_PAYLOAD_DIRECTION",changed(net,n->rename(n,"clientbound","serverbound")),"UNKNOWN_CONSTRUCTOR_EFFECT");
            reject("WRONG_CODEC_OWNER",changed(up,n->{for(var m:n.methods)for(var op:m.instructions)if(op instanceof FieldInsnNode f && f.owner.equals(ForgeCodecPlan.BYTE_CODECS) && f.name.equals("VAR_INT"))f.owner="foreign/Codec";}),"WRONG_CODEC_OWNER");
            reject("WRONG_CODEC_DESCRIPTOR",changed(up,n->{for(var f:n.fields)if(f.name.equals("STREAM_CODEC"))f.signature=f.signature.replace("RegistryFriendlyByteBuf","FriendlyByteBuf");}),"WRONG_CODEC_DESCRIPTOR");
            reject("UNSUPPORTED_COMPOSITE_CODEC",changed(up,n->{for(var m:n.methods)for(var op:m.instructions)if(op instanceof FieldInsnNode f && f.name.equals("BOOL"))f.name="VAR_INT";}),"UNKNOWN_BUS_ORIGIN");
            reject("UNSUPPORTED_UNIT_CODEC",changed(unit,n->rename(n,"unit","unknownUnit")),"UNSUPPORTED_COMPOSITE_CODEC");
            reject("UNSUPPORTED_BOOTSTRAP",changed(up,n->{for(var m:n.methods)for(var op:m.instructions)if(op instanceof InvokeDynamicInsnNode d)d.bsm=new Handle(Opcodes.H_INVOKESTATIC,"other/Bootstrap","call",d.bsm.getDesc(),false);}),"UNSUPPORTED_BOOTSTRAP");
            reject("UNVERIFIED_HANDLER_CAPTURE",changed(net,n->{for(var op:ctor(n).instructions)if(op instanceof InvokeDynamicInsnNode d && d.name.equals("accept")){Handle h=(Handle)d.bsmArgs[1];d.bsmArgs[1]=new Handle(h.getTag(),global,h.getName(),h.getDesc(),false);break;}}),"UNVERIFIED_HANDLER_CAPTURE");
            reject("WRONG_CHANNEL_OWNER",changed(entry,n->{for(var op:ctor(n).instructions)if(op instanceof LdcInsnNode l && id.equals(l.cst))l.cst="foreign";}),"WRONG_CHANNEL_OWNER");
            for(String operation:List.of("unknownChannel","isRemotePresent","send"))reject(operation.equals("isRemotePresent")?"UNVERIFIED_REMOTE_PRESENCE":operation.equals("send")?"UNVERIFIED_SEND_OPERATION":"UNKNOWN_CHANNEL_OPERATION",changed(net,n->extraOperation(n,operation)),operation.equals("isRemotePresent")?"UNVERIFIED_REMOTE_PRESENCE":operation.equals("send")?"UNVERIFIED_SEND_OPERATION":"UNKNOWN_CHANNEL_OPERATION");
            reject("UNVERIFIED_PERMISSION_RESULT",changed(net,n->{MethodNode body=n.methods.stream().filter(m->m.name.startsWith("lambda$uptuple")).findFirst().orElseThrow();InsnList effect=new InsnList();effect.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"unverified/Permission","allowed","()Z",false));effect.add(new InsnNode(Opcodes.POP));body.instructions.insertBefore(body.instructions.getLast(),effect);}),"UNVERIFIED_PERMISSION_RESULT");
            try{p.rejectForeignClasses(Set.of(up));throw new AssertionError("foreign accepted");}catch(IllegalStateException e){check(e.getMessage().contains("CROSS_MODFILE_CONFLICT"),"cross-file target collision");}
            for(List<String> omitted:List.of(List.of("UpWords","DownWords"),List.of("UpWords","DownWords","UpTuple","DownTuple"))) {
                Map<String,byte[]> variable=new TreeMap<>(source);ClassNode reduced=node(net);
                Set<String> lower=new HashSet<>();for(String leaf:omitted){lower.add(leaf.toLowerCase(Locale.ROOT));variable.remove("org/example/"+id+"/"+leaf);}
                reduced.fields.removeIf(f->lower.stream().anyMatch(f.name::startsWith));reduced.methods.removeIf(m->lower.contains(m.name) || lower.stream().anyMatch(v->m.name.startsWith("lambda$"+v+"$")));
                MethodNode init=ctor(reduced);var ops=ForgeRegistryTransformationPlan.ops(init);
                for(int at=0;at<ops.size();at++)if(ops.get(at) instanceof InvokeDynamicInsnNode dyn && dyn.name.equals("accept") && lower.contains(((Handle)dyn.bsmArgs[1]).getName()))for(int j=at-2;j<=at+1;j++)init.instructions.remove(ops.get(j));
                ops=ForgeRegistryTransformationPlan.ops(init);
                for(int at=0;at<ops.size();at++)if(ops.get(at) instanceof FieldInsnNode f && f.getOpcode()==Opcodes.GETSTATIC && f.name.equals("TYPE") && omitted.stream().anyMatch(v->f.owner.endsWith("/"+v)))for(int j=at;j<=at+4;j++)init.instructions.remove(ops.get(j));
                variable.put(net,ForgeDualBusPlan.bytes(reduced));var reducedPlan=plan(variable);check(reducedPlan.network.payloads.size()==6-omitted.size(),"verified variable payload count");check(reducedPlan.targets().size()==20-omitted.size(),"exact reduced targets, no partial payload publication");System.out.println("VARIABLE_PAYLOAD_PLAN_PASS count="+reducedPlan.network.payloads.size()+" scope=STATIC_ABI");
            }
            String old=id;id="wrong_owner";reject("WRONG_MOD_CONTAINER",source,"WRONG_MOD_CONTAINER");id=old;
            reject("PARTIAL_NETWORK_PLAN",changed(net,n->ctor(n).instructions.insertBefore(ctor(n).instructions.getLast(),new InsnNode(Opcodes.NOP))),"UNKNOWN_CONSTRUCTOR_EFFECT");
            for(var row:source.entrySet()){ClassNode altered=node(row.getKey());if(altered.methods.getFirst().instructions.size()==0)continue;altered.methods.getFirst().instructions.insert(new InsnNode(Opcodes.NOP));try{p.transform(altered,null);throw new AssertionError("partial accepted");}catch(IllegalStateException e){check(e.getMessage().contains("PARTIAL_TRANSFORM_REJECTED"),"normalized input guard");}}
            ClassNode unknown=node(up);unknown.name="unclaimed/Type";try{p.transform(unknown,null);throw new AssertionError("unclaimed accepted");}catch(IllegalStateException e){check(e.getMessage().contains("UNCLAIMED_CLASS_MODIFIED"),"unclaimed output guard");}
        }
        System.out.println("NETWORK_CLOSURE_PLAN_PASS assertions="+assertions+" scope=STATIC_ABI");
    }
    static void rename(ClassNode n,String from,String to){boolean found=false;for(var m:n.methods)for(var op:m.instructions)if(op instanceof MethodInsnNode call && call.name.equals(from)){call.name=to;found=true;}check(found,"mutation exists "+from);}
    static void extraOperation(ClassNode n,String name){
        String channel=ForgeNetworkPlan.CHANNEL,connection="net/minecraft/network/Connection";MethodNode m=new MethodNode(Opcodes.ACC_PRIVATE,"unverifiedOperation",name.equals("send")?"(Ljava/lang/Object;L"+connection+";)V":name.equals("isRemotePresent")?"(L"+connection+";)Z":"()V",null,null);
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));m.instructions.add(new FieldInsnNode(Opcodes.GETFIELD,n.name,"channel","L"+channel+";"));
        if(name.equals("send")){m.instructions.add(new VarInsnNode(Opcodes.ALOAD,1));m.instructions.add(new VarInsnNode(Opcodes.ALOAD,2));m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,channel,name,"(Ljava/lang/Object;L"+connection+";)V",false));m.instructions.add(new InsnNode(Opcodes.RETURN));}
        else if(name.equals("isRemotePresent")){m.instructions.add(new VarInsnNode(Opcodes.ALOAD,1));m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,channel,name,"(L"+connection+";)Z",false));m.instructions.add(new InsnNode(Opcodes.IRETURN));}
        else{m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,channel,name,"()V",false));m.instructions.add(new InsnNode(Opcodes.RETURN));}m.maxStack=3;m.maxLocals=3;n.methods.add(m);
    }
}
