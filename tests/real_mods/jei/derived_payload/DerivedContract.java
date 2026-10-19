package io.github.gfander.dualforge;
import java.nio.file.*;import java.util.*;import java.util.jar.JarFile;import java.util.function.Consumer;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
public final class DerivedContract {
 static int assertions;static Map<String,byte[]> source;static String id,entry,net,packet,base,rich,record,helper,context;static final Map<String,byte[]> host=new HashMap<>();
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);assertions++;}
 static Map<String,byte[]> load(Path path)throws Exception{Map<String,byte[]> out=new TreeMap<>();try(JarFile jar=new JarFile(path.toFile())){for(var e:jar.stream().filter(x->x.getName().endsWith(".class")).toList())out.put(e.getName().replace(".class",""),jar.getInputStream(e).readAllBytes());}return out;}
 static ForgeDualBusPlan plan(Map<String,byte[]> raw){return ForgeDualBusPlan.validate(id,id,entry,raw,host::get);}
 static ClassNode node(String name){return ForgeDualBusPlan.read(source.get(name));}
 static Map<String,byte[]> changed(String name,Consumer<ClassNode> action){Map<String,byte[]> raw=new TreeMap<>(source);ClassNode n=node(name);action.accept(n);raw.put(name,ForgeDualBusPlan.bytes(n));return raw;}
 static void reject(String label,Map<String,byte[]> raw,String expected){Map<String,String> before=new TreeMap<>();raw.forEach((k,v)->before.put(k,ForgeDualBusPlan.digest(v)));try{plan(raw);throw new AssertionError("accepted "+label);}catch(IllegalStateException e){check(e.getMessage().contains(expected),label+" got="+e);System.out.println("NEGATIVE "+label+" => "+e.getMessage());}check(raw.entrySet().stream().allMatch(e->ForgeDualBusPlan.digest(e.getValue()).equals(before.get(e.getKey()))),"rejected inputs untouched");}
 static MethodNode ctor(ClassNode n){return n.methods.stream().filter(m->m.name.equals("<init>")).findFirst().orElseThrow();}
 static MethodNode process(ClassNode n){return n.methods.stream().filter(m->m.name.equals("process")).findFirst().orElseThrow();}
 static void rename(ClassNode n,String from,String to){boolean exists=false;for(var m:n.methods)for(var op:m.instructions)if(op instanceof MethodInsnNode call && call.name.equals(from)){call.name=to;exists=true;}check(exists,"mutation exists "+from);}
 static void append(MethodNode m,String owner,String name,String desc){m.instructions.insertBefore(ForgeRegistryTransformationPlan.ops(m).getLast(),new MethodInsnNode(Opcodes.INVOKESTATIC,owner,name,desc,false));m.maxStack+=4;}
 public static void main(String[] args)throws Exception{
  for(Path jar:Files.walk(Path.of(args[2])).filter(p->p.toString().endsWith(".jar")).toList())try(JarFile zip=new JarFile(jar.toFile())){for(var e:zip.stream().filter(e->e.getName().startsWith("net/neoforged/") && e.getName().endsWith(".class")).toList())host.put(e.getName().replace(".class",""),zip.getInputStream(e).readAllBytes());}
  Path output=Path.of(args[3]);Files.createDirectories(output);
  for(int which=0;which<2;which++){
   source=load(Path.of(args[which]));id=which==0?"treealpha":"treebeta";String pkg="org/example/"+id+"/";entry=pkg+"Entry";net=pkg+"Network";packet=pkg+"UpTuple";rich=pkg+"UpWords";base=pkg+"BasePacket";record=pkg+"TransferData";helper=pkg+"EnumCodec";context=pkg+"BusinessContext";
   ForgeDualBusPlan p=plan(source);check(source.size()==24 && p.targets().size()==8,"eight exact transform targets and sixteen read-only dependencies");check(p.network.closure.readOnly.size()==16,"source dependencies kept raw");check(p.network.payloads.size()==6,"six derived definitions");
   for(var row:source.entrySet()){
    ClassNode input=node(row.getKey());byte[] before=ForgeDualBusPlan.bytes(input);
    if(!p.targets().contains(row.getKey())){try{p.transform(input,null);throw new AssertionError("read-only dependency rewritten");}catch(IllegalStateException e){check(e.getMessage().contains("UNCLAIMED_CLASS_MODIFIED"),"read-only not claimed");}continue;}
    ClassNode mapped=p.transform(input,HexFormat.of().parseHex(ForgeDualBusPlan.digest(row.getValue())));check(Arrays.equals(before,ForgeDualBusPlan.bytes(input)),"clone only");Path target=output.resolve(row.getKey()+".class");Files.createDirectories(target.getParent());Files.write(target,ForgeDualBusPlan.bytes(mapped));
    try{p.transform(input,new byte[32]);throw new AssertionError("SHA accepted");}catch(IllegalStateException e){check(e.getMessage().contains("INPUT_SHA_CHANGED"),"source SHA");}
   }
   reject("UNKNOWN_PAYLOAD_SUPERCLASS",changed(packet,n->n.superName="foreign/UnprovedBase"),"UNKNOWN_PAYLOAD_SUPERCLASS");
   reject("UNVERIFIED_INHERITED_METHOD",changed(base,n->{MethodNode m=new MethodNode(Opcodes.ACC_PUBLIC,"unprovedInherited","()V",null,null);m.instructions.add(new InsnNode(Opcodes.RETURN));m.maxLocals=1;n.methods.add(m);}),"UNVERIFIED_INHERITED_METHOD");
   reject("WRONG_SUPER_CONSTRUCTOR",changed(packet,n->{for(var op:ctor(n).instructions)if(op instanceof MethodInsnNode c && c.name.equals("<init>")){c.owner="java/lang/Object";break;}}),"WRONG_SUPER_CONSTRUCTOR");
   reject("WRONG_INTERFACE_METHOD",changed(base,n->n.interfaces=List.of("unverified/Interface")),"WRONG_INTERFACE_METHOD");
   reject("WRONG_TYPE_METHOD_RESOLUTION",changed(packet,n->{for(var m:n.methods)if(m.name.equals("type"))for(var op:m.instructions)if(op instanceof FieldInsnNode f)f.owner=pkg+"DownTuple";}),"WRONG_TYPE_METHOD_RESOLUTION");
   reject("WRONG_METHOD_GENERIC_BINDING",changed(packet,n->{for(var m:n.methods)if(m.name.equals("streamCodec"))m.signature=m.signature.replace("RegistryFriendlyByteBuf","FriendlyByteBuf");}),"WRONG_TYPE_METHOD_RESOLUTION");
   reject("WRONG_ENUM_GENERIC_BINDING",changed(helper,n->{for(var m:n.methods)if(m.name.equals("decode") && !m.desc.startsWith("(Ljava/lang/Object;"))m.signature=null;}),"UNSUPPORTED_ENUM_CODEC");
   reject("WRONG_FIELD_OWNER",changed(packet,n->{for(var op:ctor(n).instructions)if(op instanceof FieldInsnNode f)f.owner=base;}),"WRONG_FIELD_OWNER");
   reject("UNKNOWN_STATIC_INITIALIZER",changed(packet,n->append(n.methods.stream().filter(m->m.name.equals("<clinit>")).findFirst().orElseThrow(),"java/lang/System","gc","()V")),"UNKNOWN_CONSTRUCTOR_EFFECT");
   reject("UNKNOWN_BUSINESS_SIDE_EFFECT",changed(packet,n->{MethodNode m=new MethodNode(Opcodes.ACC_PUBLIC,"unprovedBusiness","()V",null,null);m.instructions.add(new InsnNode(Opcodes.RETURN));m.maxLocals=1;n.methods.add(m);}),"UNKNOWN_BUSINESS_SIDE_EFFECT");
   reject("FORGE_ABI_IN_READONLY_DEPENDENCY",changed(rich,n->{for(var f:n.fields)if(f.name.equals("label"))f.signature="Lnet/minecraftforge/unverified/Value;";}),"UNKNOWN_BUSINESS_SIDE_EFFECT read-only dependency contains Forge ABI");
   reject("UNSUPPORTED_ITEMSTACK_CODEC",changed(rich,n->{for(var m:n.methods)for(var op:m.instructions)if(op instanceof FieldInsnNode f && f.owner.equals(ForgeRichCodecPlan.STACK) && f.name.equals("STREAM_CODEC"))f.name="OPTIONAL_STREAM_CODEC";}),"UNSUPPORTED_ITEMSTACK_CODEC");
   reject("UNSUPPORTED_ENUM_CODEC",changed(helper,n->n.interfaces.clear()),"UNSUPPORTED_ENUM_CODEC");
   reject("UNSUPPORTED_LIST_CODEC",changed(rich,n->rename(n,"list","notList")),"UNSUPPORTED_LIST_CODEC");
   reject("UNSUPPORTED_RECORD_CODEC",changed(record,n->{MethodNode m=new MethodNode(Opcodes.ACC_PUBLIC,"unprovedRecordAction","()V",null,null);m.instructions.add(new InsnNode(Opcodes.RETURN));m.maxLocals=1;n.methods.add(m);}),"UNSUPPORTED_RECORD_CODEC");
   reject("UNSUPPORTED_COMPOSITE_CODEC",changed(rich,n->rename(n,"composite","unprovedComposite")),"UNSUPPORTED_COMPOSITE_CODEC");
   reject("WRONG_CODEC_BOOTSTRAP",changed(rich,n->{for(var m:n.methods)for(var op:m.instructions)if(op instanceof InvokeDynamicInsnNode d)d.bsm=new Handle(Opcodes.H_INVOKESTATIC,"unverified/Bootstrap","call",d.bsm.getDesc(),false);}),"WRONG_CODEC_BOOTSTRAP");
   reject("WRONG_CODEC_BUFFER",changed(rich,n->{for(var f:n.fields)if(f.name.equals("STREAM_CODEC"))f.signature=f.signature.replace("RegistryFriendlyByteBuf","FriendlyByteBuf");}),"WRONG_CODEC_BUFFER");
   reject("WRONG_CODEC_OWNER",changed(rich,n->{for(var m:n.methods)for(var op:m.instructions)if(op instanceof FieldInsnNode f && f.owner.equals(ForgeRichCodecPlan.STACK))f.owner="unverified/Codec";}),"WRONG_CODEC_OWNER");
   reject("UNKNOWN_PROCESS_METHOD",changed(packet,n->process(n).name="unverifiedProcess"),"UNKNOWN_PROCESS_METHOD");
   reject("WRONG_PROCESS_CONTEXT",changed(packet,n->process(n).desc="(L"+pkg+"ISettings;)V"),"WRONG_PROCESS_CONTEXT");
   reject("WRONG_HANDLER_CAPTURE",changed(net,n->{for(var op:ctor(n).instructions)if(op instanceof InvokeDynamicInsnNode d && d.name.equals("accept")){Handle h=(Handle)d.bsmArgs[1];d.bsmArgs[1]=new Handle(h.getTag(),base,h.getName(),h.getDesc(),false);break;}}),"UNVERIFIED_HANDLER_CAPTURE");
   reject("WRONG_HANDLER_THREAD",changed(net,n->rename(n,"enqueueWork","runUnverified")),"UNKNOWN_CONSTRUCTOR_EFFECT");
   reject("UNVERIFIED_PLAYER_IDENTITY",changed(net,n->rename(n,"getSender","getUncheckedPlayer")),"UNKNOWN_CONSTRUCTOR_EFFECT");
   reject("UNVERIFIED_INVENTORY_MUTATION",changed(packet,n->append(process(n),"unverified/Inventory","mutate","()V")),"UNKNOWN_BUSINESS_SIDE_EFFECT");
   reject("UNKNOWN_RECIPE_TRANSFER",changed(packet,n->append(process(n),"unverified/RecipeTransfer","apply","()V")),"UNKNOWN_BUSINESS_SIDE_EFFECT");
   reject("DUPLICATE_PAYLOAD_ID",changed(pkg+"DownTuple",n->{for(var m:n.methods)for(var op:m.instructions)if(op instanceof LdcInsnNode l && l.cst.equals("downtuple"))l.cst="uptuple";}),"DUPLICATE_PAYLOAD_ID");
   reject("WRONG_PAYLOAD_DIRECTION",changed(net,n->rename(n,"clientbound","serverbound")),"UNKNOWN_CONSTRUCTOR_EFFECT");
   reject("PARTIAL_NETWORK_PLAN",changed(net,n->ctor(n).instructions.insertBefore(ctor(n).instructions.getLast(),new InsnNode(Opcodes.NOP))),"UNKNOWN_CONSTRUCTOR_EFFECT");
   try{p.rejectForeignClasses(Set.of(base));throw new AssertionError("read-only owner conflict accepted");}catch(IllegalStateException e){check(e.getMessage().contains("CROSS_MODFILE_CONFLICT"),"read-only dependencies also own their names");System.out.println("NEGATIVE CROSS_MODFILE_CONFLICT => "+e.getMessage());}
   ClassNode unknown=node(packet);unknown.name="unclaimed/Packet";try{p.transform(unknown,null);throw new AssertionError("unclaimed accepted");}catch(IllegalStateException e){check(e.getMessage().contains("UNCLAIMED_CLASS_MODIFIED"),"unclaimed guard");System.out.println("NEGATIVE UNCLAIMED_CLASS_MODIFIED => "+e.getMessage());}
   var alteredParent=changed(base,n->{for(var op:ctor(n).instructions)if(op instanceof IntInsnNode i && i.operand==17)i.operand=18;});check(!plan(alteredParent).fingerprint.equals(p.fingerprint),"whole-source fingerprint includes unclaimed parent bytes");System.out.println("READ_ONLY_SHA_BINDING_PASS source parent remains untransformed but contributes to complete-plan fingerprint");
   for(String name:p.targets()){ClassNode altered=node(name);altered.methods.getFirst().instructions.insert(new InsnNode(Opcodes.NOP));try{p.transform(altered,null);throw new AssertionError("partial accepted");}catch(IllegalStateException e){check(e.getMessage().contains("PARTIAL_TRANSFORM_REJECTED"),"normalized target guard");}}
   System.out.println("NEGATIVE INPUT_SHA_CHANGED => each of 8 targets refused wrong SHA; entire read-only source is fingerprinted");
   System.out.println("DERIVED_POSITIVE_PASS owner="+id+" targets="+p.targets().size()+" raw="+p.network.closure.readOnly.size());
  }
  Map<String,byte[]> original=load(Path.of(args[5]));Map<String,ClassNode> nodes=new TreeMap<>();original.forEach((k,v)->nodes.put(k,ForgeDualBusPlan.read(v)));var proof=ForgeDerivedPayloadPlan.inspect(nodes.get("mezz/jei/common/network/packets/PacketDeletePlayerItem"),"jei",nodes);check(proof.kind().equals("DERIVED_COMPOSITE_1"),"original ItemStack source codec is structurally proved");try{ForgeDerivedPayloadPlan.process(proof,nodes);throw new AssertionError("permission approved");}catch(IllegalStateException e){check(e.getMessage().contains("UNVERIFIED_PERMISSION_CHECK"),"original permission is refused");System.out.println("NEGATIVE UNVERIFIED_PERMISSION_CHECK => "+e.getMessage());System.out.println("ORIGINAL_PROCESS_REFUSAL "+e.getMessage());}
  System.out.println("DERIVED_PLAN_PASS assertions="+assertions+" scope=STATIC_ABI; REAL_CONNECTION=NOT_RUN");
 }
}
