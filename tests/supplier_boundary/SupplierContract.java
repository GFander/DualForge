package io.github.gfander.dualforge;
import java.nio.file.*;import java.util.*;import java.util.function.Consumer;import java.util.zip.*;
import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
public final class SupplierContract {
 static int assertions,negatives;static Map<String,ClassNode> owned;static PinnedHostOracle oracle;
 static byte[] bytes(ClassNode n){ClassWriter w=new ClassWriter(0);n.accept(w);return w.toByteArray();}
 public static void main(String[] args)throws Exception {
  owned=new TreeMap<>();try(ZipFile z=new ZipFile(args[0])){for(var e:z.stream().toList())if(e.getName().endsWith(".class")){ClassNode n=ForgeRegistryTransformationPlan.read(z.getInputStream(e).readAllBytes());owned.put(n.name,n);}}
  oracle=new PinnedHostOracle(Path.of(args[1]));int count=0;
  for(ClassNode n:owned.values())for(MethodNode m:n.methods)if(m.name.startsWith("lambda$")&&m.desc.equals("()Lnet/minecraft/world/item/Item;")){var before=bytes(n);new ControlledItemSupplier(owned,oracle).verify(n.name,m);if(!Arrays.equals(before,bytes(n)))throw new AssertionError("Mutation");assertions++;count++;}
  System.out.println("SUPPLIER_INDIVIDUAL_PASS count="+count);
  ClassNode root=owned.values().stream().filter(n->n.methods.stream().anyMatch(m->m.name.startsWith("lambda$")&&m.desc.equals("()Lnet/minecraft/world/item/Item;"))).findFirst().orElseThrow();
  MethodNode base=root.methods.stream().filter(m->m.name.startsWith("lambda$")&&m.desc.equals("()Lnet/minecraft/world/item/Item;")).findFirst().orElseThrow();
  reject(root,base,"forge-api",m->m.instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC,"net/minecraftforge/Unknown","run","()V",false)),"UNMAPPED_FORGE_API");
  reject(root,base,"missing-field",m->m.instructions.insert(new FieldInsnNode(Opcodes.GETSTATIC,"net/minecraft/world/item/Item","missing","I")),"MEMBER_MISSING");
  reject(root,base,"wrong-field-desc",m->m.instructions.insert(new FieldInsnNode(Opcodes.GETSTATIC,"java/lang/System","out","I")),"MEMBER_MISSING");
  reject(root,base,"missing-factory",m->m.instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC,"absent/Data","make","()V",false)),"HOST_TYPE_MISSING");
  reject(root,base,"reflection",m->m.instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC,"java/lang/Class","forName","(Ljava/lang/String;)Ljava/lang/Class;",false)),"DYNAMIC_LINKAGE");
  reject(root,base,"null-return",m->{m.instructions.clear();m.instructions.add(new InsnNode(Opcodes.ACONST_NULL));m.instructions.add(new InsnNode(Opcodes.ARETURN));},"SUPPLIER_RETURN_TYPE");
  reject(root,base,"wrong-return",m->{m.instructions.clear();m.instructions.add(new LdcInsnNode("wrong"));m.instructions.add(new InsnNode(Opcodes.ARETURN));},"SUPPLIER_TYPE_VERIFICATION");
  reject(root,base,"control-flow",m->{LabelNode l=new LabelNode();m.instructions.insert(new JumpInsnNode(Opcodes.GOTO,l));m.instructions.add(l);},"BUSINESS_CONTROL_FLOW");
  reject(root,base,"exception-table",m->{LabelNode l=new LabelNode();m.instructions.insert(l);m.tryCatchBlocks.add(new TryCatchBlockNode(l,l,l,"java/lang/Exception"));},"BUSINESS_CONTROL_FLOW");
  reject(root,base,"handle",m->m.instructions.insert(new LdcInsnNode(new Handle(Opcodes.H_INVOKESTATIC,"java/lang/System","gc","()V",false))),"BUSINESS_HANDLE");
  reject(root,base,"missing-ctor",m->{for(var i:m.instructions)if(i instanceof MethodInsnNode c&&c.name.equals("<init>"))c.desc="(Ljava/lang/String;)V";},"MEMBER_MISSING");
  reject(root,base,"protected-receiver",m->{InsnList code=new InsnList();code.add(new LdcInsnNode("foreign-receiver"));code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,"java/lang/Object","clone","()Ljava/lang/Object;",false));code.add(new InsnNode(Opcodes.POP));m.instructions.insert(code);},"MEMBER_INACCESSIBLE");
  String custom=owned.values().stream().filter(n->"net/minecraft/world/item/Item".equals(n.superName)).map(n->n.name).findFirst().orElseThrow();
  MethodNode customFactory=root.methods.stream().filter(m->java.util.Arrays.stream(m.instructions.toArray()).anyMatch(i->i instanceof TypeInsnNode t&&t.getOpcode()==Opcodes.NEW&&t.desc.equals(custom))).findFirst().orElseThrow();
  dependency(root,customFactory,"private-subclass-ctor",custom,n->n.methods.stream().filter(m->m.name.equals("<init>")).forEach(m->m.access=Opcodes.ACC_PRIVATE),"MEMBER_INACCESSIBLE");
  dependency(root,customFactory,"missing-subclass-ctor",custom,n->n.methods.removeIf(m->m.name.equals("<init>")),"MEMBER_MISSING");
  dependency(root,customFactory,"abstract-subclass",custom,n->n.access|=Opcodes.ACC_ABSTRACT,"ABSTRACT_ALLOCATION");
  dependency(root,customFactory,"unknown-generic-type",custom,n->n.fields.getFirst().signature="Lnet/minecraftforge/Unknown;","UNMAPPED_FORGE_API");
  String data=owned.values().stream().filter(n->!n.name.equals(root.name)&&n.fields.stream().anyMatch(f->f.desc.equals("Lnet/minecraft/world/food/FoodProperties;"))).map(n->n.name).findFirst().orElseThrow();
  MethodNode foodFactory=root.methods.stream().filter(m->java.util.Arrays.stream(m.instructions.toArray()).anyMatch(i->i instanceof FieldInsnNode f&&f.owner.equals(data))).findFirst().orElseThrow();
  dependency(root,foodFactory,"missing-static-data",data,null,"HOST_TYPE_MISSING");
  dependency(root,foodFactory,"inaccessible-static-data",data,n->n.fields.forEach(f->f.access=(f.access&~Opcodes.ACC_PUBLIC)|Opcodes.ACC_PRIVATE),"MEMBER_INACCESSIBLE");
  dependency(root,foodFactory,"wrong-static-data-descriptor",data,n->n.fields.forEach(f->f.desc="Ljava/lang/Object;"),"MEMBER_MISSING");
  System.out.println("SUPPLIER CONTRACT PASS assertions="+assertions+" negatives="+negatives);
 }
 static void dependency(ClassNode root,MethodNode method,String label,String name,Consumer<ClassNode> mutation,String reason) {
  Map<String,ClassNode> nodes=new TreeMap<>(owned);if(mutation==null)nodes.remove(name);else {ClassNode clone=ForgeRegistryTransformationPlan.read(bytes(nodes.get(name)));mutation.accept(clone);nodes.put(name,clone);}
  Map<String,byte[]> before=new TreeMap<>();nodes.forEach((n,c)->before.put(n,bytes(c)));
  try{new ControlledItemSupplier(nodes,oracle).verify(root.name,method);throw new AssertionError("Accepted "+label);}catch(IllegalStateException e){if(!e.toString().contains(reason))throw new AssertionError(label+" wrong reason "+e,e);}
  nodes.forEach((n,c)->{if(!Arrays.equals(before.get(n),bytes(c)))throw new AssertionError("Mutation "+label);});assertions++;negatives++;System.out.println("REJECT "+label+" "+reason+" stage=dependency-validation outputTargets=not-published originalUnchanged=true");
 }
 static void reject(ClassNode root,MethodNode original,String label,Consumer<MethodNode> mutate,String reason) {
  ClassNode clone=ForgeRegistryTransformationPlan.read(bytes(root));MethodNode m=clone.methods.stream().filter(x->x.name.equals(original.name)).findFirst().orElseThrow();mutate.accept(m);Map<String,ClassNode> nodes=new TreeMap<>(owned);nodes.put(clone.name,clone);byte[] before=bytes(clone);
  try{new ControlledItemSupplier(nodes,oracle).verify(clone.name,m);throw new AssertionError("Accepted "+label);}catch(IllegalStateException e){if(!e.toString().contains(reason))throw new AssertionError(label+" wrong reason "+e,e);}
  if(!Arrays.equals(before,bytes(clone)))throw new AssertionError("Mutation "+label);assertions++;negatives++;System.out.println("REJECT "+label+" "+reason+" originalUnchanged=true");
 }
}
