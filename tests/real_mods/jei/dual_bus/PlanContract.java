package io.github.gfander.dualforge;

import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Bytecode planning tests only; native owner/lifecycle tests live in the separate observer. */
public final class PlanContract {
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
    static MethodNode ctor(ClassNode n){return n.methods.stream().filter(m->m.name.equals("<init>")).findFirst().orElseThrow();}
    static MethodNode registration(ClassNode n){return n.methods.stream().filter(m->!m.name.equals("<init>") && m.desc.equals(ForgeDualBusPlan.PAIR) && m.name.equals("register")).findFirst().orElseThrow();}
    public static void main(String[] args)throws Exception {
        for(Path jar:Files.walk(Path.of(args[2])).filter(p->p.toString().endsWith(".jar")).toList())try(JarFile zip=new JarFile(jar.toFile())){
            for(var e:zip.stream().filter(e->e.getName().endsWith(".class")&&!e.getName().startsWith("META-INF/")).toList()) {
                String name=e.getName().substring(0,e.getName().length()-6);if(name.startsWith("net/neoforged/"))host.put(name,zip.getInputStream(e).readAllBytes());
            }
        }
        try(JarFile fnf=new JarFile(args[4])) {
            ClassNode old=ForgeDualBusPlan.read(fnf.getInputStream(fnf.getJarEntry("net/natevg/fnf/FoodNFuel.class")).readAllBytes());
            check(!ForgeDualBusPlan.isCandidate(old),"existing FnF Global+Registry profile is not claimed");
            check(ForgeEntryProfile.select(old,false)==ForgeEntryProfile.Kind.CONTEXT,"existing FnF context routing retained");
        }
        Path output=Path.of(args[3]);Files.createDirectories(output);
        for(int which=0;which<2;which++) {
            source=new TreeMap<>();try(JarFile jar=new JarFile(args[which])){for(var e:jar.stream().filter(e->e.getName().endsWith(".class")).toList())source.put(e.getName().replace(".class",""),jar.getInputStream(e).readAllBytes());}
            id=which==0?"busalpha":"busbeta";entry="org/example/"+id+"/Entry";router="org/example/"+id+"/Subscriptions";config="org/example/"+id+"/Settings";
            ForgeDualBusPlan p=plan(source);check(p.targets().equals(source.keySet()),"exact owned closure");check(p.listeners.size()==3,"three explicit original Consumers");
            check(p.listeners.get(2).canceled()==(which==1),"receiveCanceled belongs to exact callsite");
            check(ForgeEntryProfile.select(node(entry),false)==ForgeEntryProfile.Kind.DUAL_BUS,"separate profile");
            for(var row:source.entrySet()) {
                ClassNode original=node(row.getKey());byte[] before=ForgeDualBusPlan.bytes(original);
                ClassNode mapped=p.transform(original,HexFormat.of().parseHex(ForgeDualBusPlan.digest(row.getValue())));
                check(Arrays.equals(before,ForgeDualBusPlan.bytes(original)),"clone-only transformation");
                Path target=output.resolve(row.getKey()+".class");Files.createDirectories(target.getParent());Files.write(target,ForgeDualBusPlan.bytes(mapped));
                try{p.transform(original,new byte[32]);throw new AssertionError("SHA approved");}catch(IllegalStateException e){check(e.getMessage().contains("INPUT_SHA_CHANGED"),"SHA refusal");}
                original.methods.getFirst().instructions.insertBefore(original.methods.getFirst().instructions.getFirst(),new InsnNode(Opcodes.NOP));
                try{p.transform(original,null);throw new AssertionError("partial approved");}catch(IllegalStateException e){check(e.getMessage().contains("PARTIAL_TRANSFORM_REJECTED"),"partial input refusal");}
            }
            try{p.rejectForeignClasses(Set.of(router));throw new AssertionError("foreign approved");}catch(IllegalStateException e){check(e.getMessage().contains("CROSS_MODFILE_CONFLICT"),"cross file conflict");}
            ClassNode unknown=node(entry);unknown.name="other/Unclaimed";try{p.transform(unknown,null);throw new AssertionError("unclaimed approved");}catch(IllegalStateException e){check(e.getMessage().contains("UNCLAIMED_CLASS_MODIFIED"),"unclaimed refusal");}
            reject("UNKNOWN_CONSTRUCTOR_EFFECT",changed(entry,n->ctor(n).instructions.insertBefore(ctor(n).instructions.getLast(),new InsnNode(Opcodes.NOP))),"UNKNOWN_CONSTRUCTOR_EFFECT");
            reject("UNKNOWN_BUS_ORIGIN",changed(entry,n->{for(var m:n.methods)for(var op:m.instructions)if(op instanceof FieldInsnNode f&&f.name.equals("EVENT_BUS"))f.owner="custom/Global";}),"UNKNOWN_BUS_ORIGIN");
            reject("CUSTOM_BUS_UNSUPPORTED",changed(entry,n->{for(var m:n.methods)for(var op:m.instructions)if(op instanceof FieldInsnNode f&&f.name.equals("EVENT_BUS"))f.owner="net/minecraftforge/eventbus/api/BusBuilder";}),"UNKNOWN_BUS_ORIGIN");
            reject("FOREIGN_MOD_BUS_DATAFLOW",changed(entry,n->{for(var op:ctor(n).instructions)if(op instanceof VarInsnNode v&&v.getOpcode()==Opcodes.ALOAD&&v.var==2){v.var=1;break;}}),"UNKNOWN_CONSTRUCTOR_EFFECT");
            reject("WRONG_EVENT_DOMAIN",changed(router,n->{for(var m:n.methods)if(!m.name.equals("<init>"))for(var op:m.instructions)if(op instanceof FieldInsnNode f&&f.desc.equals(ForgeDualBusPlan.B))f.name=f.name.equals("game")?"mod":"game";}),"WRONG_EVENT_DOMAIN");
            reject("UNVERIFIED_PRIORITY",changed(router,n->{for(var op:registration(n).instructions)if(op instanceof FieldInsnNode f&&f.name.equals("NORMAL"))f.name="HIGH";}),"UNKNOWN_BUS_ORIGIN");
            reject("UNVERIFIED_RECEIVE_CANCELED",changed(router,n->{for(var op:registration(n).instructions)if(op.getOpcode()==Opcodes.ICONST_0){registration(n).instructions.set(op,new InsnNode(Opcodes.ICONST_2));break;}}),"UNVERIFIED_RECEIVE_CANCELED");
            reject("WRONG_EVENT_CLASS",changed(entry,n->{for(var op:ctor(n).instructions)if(op instanceof LdcInsnNode l&&l.cst instanceof Type t&&t.getInternalName().endsWith("FMLCommonSetupEvent"))l.cst=Type.getType(String.class);}),"WRONG_EVENT_CLASS");
            reject("WRONG_LISTENER_OWNER",changed(entry,n->{for(var op:ctor(n).instructions)if(op instanceof InvokeDynamicInsnNode d&&d.name.equals("accept")){Handle h=(Handle)d.bsmArgs[1];d.bsmArgs[1]=new Handle(h.getTag(),router,h.getName(),h.getDesc(),false);break;}}),"WRONG_LISTENER_OWNER");
            reject("WRONG_CONSUMER_TYPE",changed(entry,n->{for(var op:ctor(n).instructions)if(op instanceof InvokeDynamicInsnNode d&&d.name.equals("accept")){d.desc="()Ljava/util/function/Consumer;";break;}}),"UNSUPPORTED_BOOTSTRAP");
            reject("UNSUPPORTED_BOOTSTRAP",changed(entry,n->{for(var op:ctor(n).instructions)if(op instanceof InvokeDynamicInsnNode d&&d.name.equals("accept")){d.bsm=new Handle(Opcodes.H_INVOKESTATIC,"unknown/Bootstrap","call",d.bsm.getDesc(),false);break;}}),"UNSUPPORTED_BOOTSTRAP");
            reject("DUPLICATE_LISTENER",changed(entry,n->{String field=null;for(var op:ctor(n).instructions)if(op instanceof FieldInsnNode f&&f.getOpcode()==Opcodes.PUTFIELD&&f.desc.equals("Ljava/util/function/Consumer;")){if(field==null)field=f.name;else{f.name=field;break;}}}),"DUPLICATE_LISTENER");
            reject("WRONG_REGISTRATION_ORDER",changed(entry,n->{for(var op:ctor(n).instructions)if(op instanceof FieldInsnNode f&&f.getOpcode()==Opcodes.GETFIELD&&f.desc.equals("Ljava/util/function/Consumer;")){f.name="deathListener";break;}}),"UNKNOWN_BUS_ORIGIN");
            reject("CANCELED_NONCANCELLABLE_EVENT",changed(entry,n->{for(var op:ctor(n).instructions)if(op instanceof MethodInsnNode call&&call.owner.equals(router)&&call.name.equals("register")){call.name="registerCanceled";break;}}),"UNVERIFIED_RECEIVE_CANCELED");
            reject("NONPUBLIC_ROUTER",changed(router,n->registration(n).access=Opcodes.ACC_PRIVATE),"WRONG_LISTENER_OWNER");
            reject("UNSUPPORTED_EVENT_SEMANTICS",changed(entry,n->{for(var m:n.methods)for(var op:m.instructions)if(op instanceof MethodInsnNode call&&call.name.equals("setCanceled"))call.name="unsupportedResult";}),"UNSUPPORTED_EVENT_SEMANTICS");
            reject("CONFIG_SPEC_UNSUPPORTED",changed(config,n->{for(var m:n.methods)for(var op:m.instructions)if(op instanceof MethodInsnNode call&&call.name.equals("define"))call.name="defineUnknown";}),"UNKNOWN_CONSTRUCTOR_EFFECT");
            reject("CONFIG_WRONG_TYPE",changed(config,n->{for(var m:n.methods)for(var op:m.instructions)if(op instanceof FieldInsnNode f&&f.owner.equals(ForgeDualBusPlan.TYPE))f.name="UNKNOWN";}),"CONFIG_SPEC_UNSUPPORTED");
            reject("CONFIG_OWNER_MISMATCH",changed(config,n->{n.methods.stream().filter(m->m.name.equals("register")).findFirst().orElseThrow().access=Opcodes.ACC_PRIVATE|Opcodes.ACC_STATIC;}),"CONFIG_OWNER_MISMATCH");
            reject("DUPLICATE_CONFIG_REGISTRATION",changed(config,n->{MethodNode m=n.methods.stream().filter(v->v.name.equals("register")).findFirst().orElseThrow();m.instructions.insertBefore(m.instructions.getLast(),new InsnNode(Opcodes.NOP));}),"UNKNOWN_CONSTRUCTOR_EFFECT");
            Map<String,byte[]> missing=new TreeMap<>(source);missing.remove(config);reject("WRONG_MODFILE",missing,"UNKNOWN_CONSTRUCTOR_EFFECT");
            String saved=id;id="wrong_owner";reject("WRONG_MOD_CONTAINER",source,"WRONG_MOD_CONTAINER");id=saved;
        }
        System.out.println("DUAL_BUS_PLAN_PASS assertions="+assertions+" scope=BYTECODE_MODEL_ONLY");
    }
}
