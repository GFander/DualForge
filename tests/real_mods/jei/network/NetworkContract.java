package io.github.gfander.dualforge;

import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Bytecode planning tests only; native owner/lifecycle tests live in the separate observer. */
public final class NetworkContract {
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
            id=which==0?"netalpha":"netbeta";entry="org/example/"+id+"/Entry";router="org/example/"+id+"/Subscriptions";config="org/example/"+id+"/Settings";
            String network="org/example/"+id+"/Network",up="org/example/"+id+"/Up",down="org/example/"+id+"/Down";
            ForgeDualBusPlan p=plan(source);check(p.targets().equals(source.keySet()) && p.targets().size()==9,"nine-class exact composition");
            check(p.network!=null && p.network.version==3 && p.network.payloads.size()==2,"complete network definition");
            check(p.network.payloads.get(0).direction().equals("SERVERBOUND") && p.network.payloads.get(1).direction().equals("CLIENTBOUND"),"ordered independent directions");
            for(var row:source.entrySet()) {
                ClassNode original=node(row.getKey());byte[] before=ForgeDualBusPlan.bytes(original);
                ClassNode mapped=p.transform(original,HexFormat.of().parseHex(ForgeDualBusPlan.digest(row.getValue())));
                check(Arrays.equals(before,ForgeDualBusPlan.bytes(original)),"clone only");
                Path target=output.resolve(row.getKey()+".class");Files.createDirectories(target.getParent());Files.write(target,ForgeDualBusPlan.bytes(mapped));
                try{p.transform(original,new byte[32]);throw new AssertionError("SHA approved");}catch(IllegalStateException e){check(e.getMessage().contains("INPUT_SHA_CHANGED"),"SHA guard");}
                if(original.methods.getFirst().instructions.size()==0)continue;
                original.methods.getFirst().instructions.insertBefore(original.methods.getFirst().instructions.getFirst(),new InsnNode(Opcodes.NOP));
                try{p.transform(original,null);throw new AssertionError("partial accepted");}catch(IllegalStateException e){check(e.getMessage().contains("PARTIAL_TRANSFORM_REJECTED"),"normalized input guard");}
            }
            reject("UNKNOWN_NETWORK_BUILDER",changed(network,n->rename(n,"named","unknown")),"UNKNOWN_CONSTRUCTOR_EFFECT");
            reject("WRONG_CHANNEL_OWNER",changed(entry,n->{for(var op:ctor(n).instructions)if(op instanceof LdcInsnNode l && id.equals(l.cst))l.cst="foreign";}),"WRONG_CHANNEL_OWNER");
            reject("DUPLICATE_PAYLOAD_ID",changed(down,n->{for(var m:n.methods)for(var op:m.instructions)if(op instanceof LdcInsnNode l && "down".equals(l.cst))l.cst="up";}),"DUPLICATE_PAYLOAD_ID");
            reject("WRONG_PAYLOAD_DIRECTION",changed(network,n->rename(n,"serverbound","clientbound")),"UNKNOWN_CONSTRUCTOR_EFFECT");
            reject("WRONG_PROTOCOL_VERSION",changed(entry,n->{for(var op:ctor(n).instructions.toArray())if(op.getOpcode()==Opcodes.ICONST_3)ctor(n).instructions.set(op,new InsnNode(Opcodes.ICONST_M1));}),"WRONG_PROTOCOL_VERSION");
            reject("UNSUPPORTED_CODEC",changed(up,n->rename(n,"writeVarInt","writeInt")),"UNKNOWN_CONSTRUCTOR_EFFECT");
            reject("UNVERIFIED_HANDLER",changed(network,n->{for(var m:n.methods)if(m.name.equals("up"))m.instructions.insertBefore(m.instructions.getLast(),new InsnNode(Opcodes.NOP));}),"UNKNOWN_CONSTRUCTOR_EFFECT");
            reject("WRONG_HANDLER_THREAD",changed(network,n->rename(n,"addMain","add")),"UNKNOWN_CONSTRUCTOR_EFFECT");
            reject("UNSUPPORTED_BOOTSTRAP",changed(network,n->{for(var op:ctor(n).instructions)if(op instanceof InvokeDynamicInsnNode d)d.bsm=new Handle(Opcodes.H_INVOKESTATIC,"other/Bootstrap","call",d.bsm.getDesc(),false);}),"UNSUPPORTED_BOOTSTRAP");
            reject("UNVERIFIED_CONNECTION_OPTIONALITY",changed(network,n->rename(n,"optional","optionalServer")),"UNKNOWN_CONSTRUCTOR_EFFECT");
            reject("UNVERIFIED_NETWORK_RESULT",changed(network,n->rename(n,"build","simpleChannel")),"UNKNOWN_CONSTRUCTOR_EFFECT");
            reject("UNKNOWN_NETWORK_SIDE_EFFECT",changed(network,n->n.fields.add(new FieldNode(Opcodes.ACC_PRIVATE,"unknown","I",null,null))),"UNKNOWN_NETWORK_SIDE_EFFECT");
            Map<String,byte[]> missing=new TreeMap<>(source);missing.remove(up);reject("CROSS_MODFILE_PAYLOAD",missing,"CROSS_MODFILE_PAYLOAD");
            reject("PARTIAL_NETWORK_PLAN",changed(network,n->{ctor(n).instructions.insertBefore(ctor(n).instructions.getLast(),new InsnNode(Opcodes.NOP));}),"UNKNOWN_CONSTRUCTOR_EFFECT");
            Map<String,byte[]> extra=new TreeMap<>(source);ClassNode extraNode=node(up);extraNode.name="unclaimed/Extra";extra.put("unclaimed/Extra",ForgeDualBusPlan.bytes(extraNode));reject("UNCLAIMED_CLASS_MODIFIED",extra,"UNCLAIMED_CLASS_MODIFIED");
            ClassNode unknown=node(up);unknown.name="unclaimed/Extra";try{p.transform(unknown,null);throw new AssertionError("unclaimed approved");}catch(IllegalStateException e){check(e.getMessage().contains("UNCLAIMED_CLASS_MODIFIED"),"unclaimed transform");}
            try{p.rejectForeignClasses(Set.of(up));throw new AssertionError("foreign approved");}catch(IllegalStateException e){check(e.getMessage().contains("CROSS_MODFILE_CONFLICT"),"foreign ModFile conflict");}
            String key="net/neoforged/neoforge/network/registration/PayloadRegistrar";byte[] saved=host.remove(key);
            try{plan(source);throw new AssertionError("missing host ABI approved");}catch(IllegalStateException e){check(e.getMessage().contains("UNVERIFIED_HOST_NETWORK_ABI"),"host ABI guard");}finally{host.put(key,saved);}
        }
        System.out.println("NETWORK_PLAN_PASS assertions="+assertions+" scope=STATIC_ABI");
    }
    static void rename(ClassNode n,String from,String to){boolean changed=false;for(var m:n.methods)for(var op:m.instructions)if(op instanceof MethodInsnNode c && c.name.equals(from)){c.name=to;changed=true;}check(changed,"mutation exists "+from);}
}
