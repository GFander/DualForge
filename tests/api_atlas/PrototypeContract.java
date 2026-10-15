import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

public class PrototypeContract {
    static Map<String,ClassNode> original;
    static JsonObject config;
    static Path source,target;
    static int assertions=0,negatives=0;
    static void check(boolean b,String message){if(!b)throw new AssertionError(message);assertions++;}
    static Map<String,ClassNode> copy(){Map<String,ClassNode> c=new TreeMap<>();original.forEach((n,v)->c.put(n,OfflinePrototype.node(OfflinePrototype.bytes(v))));return c;}
    static Map<String,String> hashes(Map<String,ClassNode> c)throws Exception{Map<String,String> m=new TreeMap<>();for(var e:c.entrySet())m.put(e.getKey(),OfflinePrototype.sha(OfflinePrototype.bytes(e.getValue())));return m;}
    static void reject(String label,Consumer<Map<String,ClassNode>> edit,Consumer<JsonObject> rules,boolean partial)throws Exception{
        var c=copy();edit.accept(c);var before=hashes(c);var r=config.deepCopy();rules.accept(r);
        boolean rejected=false;
        try{OfflinePrototype.transform(c,r,new OfflinePrototype.Oracle(source),new OfflinePrototype.Oracle(target),partial);}
        catch(Exception expected){rejected=true;System.out.println("REJECT "+label+": "+expected.getMessage());}
        check(rejected,label+" accepted");check(before.equals(hashes(c)),label+" polluted original");negatives++;
    }
    static MethodNode method(Map<String,ClassNode> c,String name){return c.get("research/fixture/AtlasProbe").methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();}
    static MethodInsnNode call(Map<String,ClassNode> c){return Arrays.stream(method(c,"create").instructions.toArray()).filter(n->n instanceof MethodInsnNode).map(n->(MethodInsnNode)n).findFirst().orElseThrow();}
    public static void main(String[] args)throws Exception{
        config=JsonParser.parseString(Files.readString(Path.of(args[1]))).getAsJsonObject();source=Path.of(args[2]);target=Path.of(args[3]);original=new TreeMap<>();
        try(JarFile jar=new JarFile(args[0])){for(var e:jar.stream().filter(e->e.getName().endsWith(".class")).toList()){ClassNode n=OfflinePrototype.node(jar.getInputStream(e).readAllBytes());original.put(n.name,n);}}
        var c=copy();var before=hashes(c);var good=OfflinePrototype.transform(c,config,new OfflinePrototype.Oracle(source),new OfflinePrototype.Oracle(target),false);
        check(good.get("status").equals("OFFLINE_TRANSFORMATION_PASS"),"positive status");check(before.equals(hashes(c)),"positive original bytes");
        @SuppressWarnings("unchecked") var nodes=(Map<String,ClassNode>)good.get("classes");
        var probe=nodes.get("research/fixture/AtlasProbe");var peer=nodes.get("research/fixture/Peer");
        String holder="Lnet/neoforged/neoforge/registries/DeferredHolder;";
        check(probe.fields.getFirst().desc.equals(holder),"field descriptor closure");
        check(peer.methods.stream().anyMatch(m->m.name.equals("call")&&m.desc.equals("()"+holder)),"cross-class return declaration");
        check(probe.fields.getFirst().signature.equals("Lnet/neoforged/neoforge/registries/DeferredHolder<Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/Item;>;"),"holder generic arity");
        check(Arrays.equals(OfflinePrototype.bytes(original.get("research/fixture/Untouched")),OfflinePrototype.bytes(nodes.get("research/fixture/Untouched"))),"unclaimed class");
        reject("source descriptor mismatch",x->call(x).desc=call(x).desc.replace("Ljava/lang/String;","Ljava/lang/Object;"),x->{},false);
        reject("source opcode mismatch",x->call(x).setOpcode(Opcodes.INVOKEVIRTUAL),x->{},false);
        reject("target member missing",x->{},r->r.getAsJsonArray("rules").get(0).getAsJsonObject().getAsJsonObject("target").addProperty("name","MISSING"),false);
        reject("return type not assignable",x->{},r->{for(var e:r.getAsJsonArray("classes")){var v=e.getAsJsonObject();if(v.get("source").getAsString().endsWith("/IForgeRegistry"))v.addProperty("target","java/lang/String");}},false);
        reject("unknown Registry type",x->{for(var n:method(x,"registry").instructions)if(n instanceof FieldInsnNode f)f.name="BLOCKS";},x->{},false);
        reject("mapping ambiguity",x->{},r->r.getAsJsonArray("classes").add(r.getAsJsonArray("classes").get(0).deepCopy()),false);
        reject("MethodHandle",x->{var m=method(x,"create");m.instructions.insert(new InsnNode(Opcodes.POP));m.instructions.insert(new LdcInsnNode(new Handle(Opcodes.H_INVOKESTATIC,"research/fixture/AtlasProbe","getToken","()Lnet/minecraftforge/registries/RegistryObject;",false)));m.maxStack+=1;},x->{},false);
        reject("invokedynamic",x->{var m=method(x,"create");m.instructions.insert(new InvokeDynamicInsnNode("unknown","()V",new Handle(Opcodes.H_INVOKESTATIC,"java/lang/invoke/LambdaMetafactory","metafactory","()V",false)));},x->{},false);
        reject("explicit JPMS",x->{ClassNode m=new ClassNode();m.name="module-info";m.version=Opcodes.V21;m.access=Opcodes.ACC_MODULE;m.module=new ModuleNode("probe",0,null);x.put(m.name,m);},x->{},false);
        reject("partial clone failure",x->{},x->{},true);
        reject("unknown dependency",x->x.get("research/fixture/AtlasProbe").fields.add(new FieldNode(Opcodes.ACC_PUBLIC,"unknown","Lnet/minecraftforge/unknown/Type;",null,null)),x->{},false);
        reject("missing runtime bus bridge",x->x.get("research/fixture/AtlasProbe").fields.add(new FieldNode(Opcodes.ACC_PUBLIC,"bus","Lnet/minecraftforge/eventbus/api/IEventBus;",null,null)),x->{},false);
        reject("unknown Holder generic",x->x.get("research/fixture/AtlasProbe").fields.getFirst().signature="Lnet/minecraftforge/registries/RegistryObject<Ljava/lang/String;>;",x->{},false);
        reject("unknown Registry generic",x->method(x,"create").signature="()Lnet/minecraftforge/registries/DeferredRegister<Ljava/lang/String;>;",x->{},false);
        var nativeNodes=copy();nativeNodes.get("research/fixture/AtlasProbe").visibleAnnotations.getFirst().desc="Lnet/neoforged/fml/common/Mod;";
        var nativeBefore=hashes(nativeNodes);var nativeResult=OfflinePrototype.transform(nativeNodes,config,new OfflinePrototype.Oracle(source),new OfflinePrototype.Oracle(target),false);
        check(nativeResult.get("status").equals("UNCHANGED_NON_FORGE"),"native selection");check(nativeBefore.equals(hashes(nativeNodes)),"native bytes");
        var broken=new TreeMap<>(nodes);
        var brokenPeer=OfflinePrototype.node(OfflinePrototype.bytes(peer));broken.put(peer.name,brokenPeer);
        for(var m:brokenPeer.methods)for(var n:m.instructions)if(n instanceof MethodInsnNode i&&i.name.equals("getToken"))i.desc="()Lnet/minecraftforge/registries/RegistryObject;";
        boolean closureRejected=false;try{OfflinePrototype.verify(broken,new OfflinePrototype.Oracle(target));}catch(Exception e){closureRejected=true;System.out.println("REJECT cross-class ABI closure: "+e.getMessage());}
        check(closureRejected,"inconsistent closure accepted");check(before.equals(hashes(c)),"closure failure polluted original");negatives++;
        System.out.println("PROTOTYPE_CONTRACT_PASS assertions="+assertions+" negatives="+negatives);
    }
}
