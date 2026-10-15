package io.github.gfander.dualforge;

import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static io.github.gfander.dualforge.ForgeRegistryTransformationPlan.*;

/** Typed builder and delayed callbacks; never invokes source methods while planning. */
final class ControlledCreativeTab {
    private static final String STACK="net/minecraft/world/item/ItemStack", LIKE="net/minecraft/world/level/ItemLike";
    private static final String BUILDER=TAB+"$Builder", OUTPUT=TAB+"$Output", PARAMS=TAB+"$ItemDisplayParameters", GENERATOR=TAB+"$DisplayItemsGenerator";
    private static final String DISPLAY="(L"+PARAMS+";L"+OUTPUT+";)V";
    private enum V { BUILDER, ICON, DISPLAY, TEXT, COMPONENT, TAB, OUTPUT, HOLDER, ITEM, ALLOCATION, STACK }
    private final Map<String,ClassNode> nodes;
    private final Map<String,Kind> kinds;
    private final ControlledItemSupplier links;
    private final Set<String> verified=new HashSet<>(),owners=new HashSet<>();
    ControlledCreativeTab(Map<String,ClassNode> nodes,Map<String,Kind> kinds,ControlledItemSupplier links){this.nodes=nodes;this.kinds=kinds;this.links=links;}
    Set<String> owners(){return Set.copyOf(owners);}
    boolean verified(String owner,MethodNode m){return verified.contains(key(owner,m.name,m.desc));}
    String supplier(ClassNode owner,InvokeDynamicInsnNode d){MethodNode m=lambda(owner,d,"get","()Ljava/util/function/Supplier;","()Ljava/lang/Object;","()L"+TAB+";");factory(owner,m);return m.name;}
    private MethodNode lambda(ClassNode creator,InvokeDynamicInsnNode d,String name,String call,String sam,String implementation) {
        require(d.bsm.equals(META)&&d.name.equals(name)&&d.desc.equals(call)&&d.bsmArgs.length==3
                && Type.getMethodType(sam).equals(d.bsmArgs[0])&&d.bsmArgs[1] instanceof Handle
                && Type.getMethodType(implementation).equals(d.bsmArgs[2]),"TAB_CALLBACK_BOOTSTRAP");
        Handle h=(Handle)d.bsmArgs[1];require(h.getTag()==Opcodes.H_INVOKESTATIC&&!h.isInterface()&&h.getDesc().equals(implementation)&&nodes.containsKey(h.getOwner()),"TAB_CALLBACK_OWNERSHIP");
        ClassNode target=nodes.get(h.getOwner());structure(target);MethodNode m=method(target,h.getName(),h.getDesc());
        int visibility=target==creator?Opcodes.ACC_PRIVATE:Opcodes.ACC_PUBLIC;
        require((m.access== (visibility|Opcodes.ACC_STATIC)||m.access==(visibility|Opcodes.ACC_STATIC|Opcodes.ACC_SYNTHETIC))&&m.signature==null,"TAB_CALLBACK_FLAGS");metadata(m,false);owners.add(target.name);return m;
    }
    private void factory(ClassNode n,MethodNode m) {
        require(m.desc.equals("()L"+TAB+";"),"TAB_SUPPLIER_TYPE");
        List<V> stack=new ArrayList<>();Set<String> attached=new HashSet<>();boolean done=false;var code=ops(m);
        for(int i=0;i<code.size();i++) {
            var op=code.get(i);require(!done,"TAB_AFTER_RETURN");int marker=marker(code,i,n.name);if(marker>0){i+=marker-1;continue;}
            if(op instanceof LdcInsnNode l){require(l.cst instanceof String,"TAB_LITERAL");stack.add(V.TEXT);}
            else if(op instanceof InvokeDynamicInsnNode d) {
                if(d.desc.equals("()Ljava/util/function/Supplier;")){MethodNode cb=lambda(n,d,"get",d.desc,"()Ljava/lang/Object;","()L"+STACK+";");Handle h=(Handle)d.bsmArgs[1];callback(nodes.get(h.getOwner()),cb,false);stack.add(V.ICON);}
                else {MethodNode cb=lambda(n,d,"accept","()L"+GENERATOR+";",DISPLAY,DISPLAY);Handle h=(Handle)d.bsmArgs[1];callback(nodes.get(h.getOwner()),cb,true);stack.add(V.DISPLAY);}
            } else if(op instanceof MethodInsnNode c) {
                if(exact(c,Opcodes.INVOKESTATIC,TAB,"builder","()L"+BUILDER+";",false)){require(stack.isEmpty(),"TAB_BUILDER_SOURCE");stack.add(V.BUILDER);}
                else if(exact(c,Opcodes.INVOKESTATIC,"net/minecraft/network/chat/Component","translatable","(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;",true)){pop(stack,V.TEXT);stack.add(V.COMPONENT);}
                else if(exact(c,Opcodes.INVOKEVIRTUAL,BUILDER,"icon","(Ljava/util/function/Supplier;)L"+BUILDER+";",false)){pop(stack,V.ICON);pop(stack,V.BUILDER);require(attached.add("icon"),"DUPLICATE_TAB_CALLBACK");stack.add(V.BUILDER);}
                else if(exact(c,Opcodes.INVOKEVIRTUAL,BUILDER,"displayItems","(L"+GENERATOR+";)L"+BUILDER+";",false)){pop(stack,V.DISPLAY);pop(stack,V.BUILDER);require(attached.add("display"),"DUPLICATE_TAB_CALLBACK");stack.add(V.BUILDER);}
                else if(exact(c,Opcodes.INVOKEVIRTUAL,BUILDER,"title","(Lnet/minecraft/network/chat/Component;)L"+BUILDER+";",false)){pop(stack,V.COMPONENT);pop(stack,V.BUILDER);require(attached.add("title"),"DUPLICATE_TAB_CALLBACK");stack.add(V.BUILDER);}
                else {require(exact(c,Opcodes.INVOKEVIRTUAL,BUILDER,"build","()L"+TAB+";",false)&&attached.equals(Set.of("icon","display","title")),"UNVERIFIED_TAB_API_OR_EAGER_CONSUMER");pop(stack,V.BUILDER);stack.add(V.TAB);}
                links.hostCall(n.name,c);
            } else {require(op.getOpcode()==Opcodes.ARETURN,"TAB_SUPPLIER_INSTRUCTION");pop(stack,V.TAB);require(stack.isEmpty(),"TAB_SUPPLIER_STACK");done=true;}
        }
        require(done,"TAB_MISSING_RETURN");verified.add(key(n.name,m.name,m.desc));
    }
    private void callback(ClassNode n,MethodNode m,boolean display) {
        if(verified(n.name,m))return;
        List<V> stack=new ArrayList<>();boolean done=false;int reads=0,outputs=0;var code=ops(m);
        for(int i=0;i<code.size();i++) {
            var op=code.get(i);require(!done,"TAB_CALLBACK_AFTER_RETURN");int marker=marker(code,i,n.name);if(marker>0){i+=marker-1;continue;}
            if(op instanceof VarInsnNode v){require(display&&v.getOpcode()==Opcodes.ALOAD&&v.var==1,"TAB_CALLBACK_LOCAL");stack.add(V.OUTPUT);}
            else if(op instanceof TypeInsnNode t) {
                if(t.getOpcode()==Opcodes.NEW){require(!display&&t.desc.equals(STACK),"TAB_CALLBACK_ALLOCATION");stack.add(V.ALLOCATION);}
                else {require(t.getOpcode()==Opcodes.CHECKCAST&&(t.desc.equals(ITEM)||t.desc.equals(LIKE))&&!stack.isEmpty()&&stack.getLast()==V.ITEM,"TAB_CALLBACK_CAST");}
            } else if(op.getOpcode()==Opcodes.DUP){require(!display&&!stack.isEmpty()&&stack.getLast()==V.ALLOCATION,"TAB_CALLBACK_DUP");stack.add(V.ALLOCATION);}
            else if(op instanceof FieldInsnNode f){require(f.getOpcode()==Opcodes.GETSTATIC&&f.desc.equals(H)&&holderKind(nodes,kinds,f.owner,f.name)==Kind.ITEM,"TAB_HOLDER_SOURCE");stack.add(V.HOLDER);reads++;}
            else if(op instanceof MethodInsnNode c) {
                if(call(c,Opcodes.INVOKEVIRTUAL,F+"RegistryObject","get","()Ljava/lang/Object;")){pop(stack,V.HOLDER);stack.add(V.ITEM);}
                else if(nodes.containsKey(c.owner)) {
                    require(c.getOpcode()==Opcodes.INVOKESTATIC&&!c.itf,"TAB_CONSUMER_CALL");MethodNode target=method(nodes.get(c.owner),c.name,c.desc);
                    validateAccessor(nodes,nodes.keySet(),kinds,c.owner,target,new HashSet<>());require(accessorKind(target)==Kind.ITEM,"TAB_ITEM_CONSUMER_TYPE");stack.add(c.desc.equals("()"+H)?V.HOLDER:V.ITEM);reads++;
                } else if(exact(c,Opcodes.INVOKESPECIAL,STACK,"<init>","(L"+LIKE+";)V",false)) {
                    require(!display,"TAB_CALLBACK_CONSTRUCTOR");pop(stack,V.ITEM);pop(stack,V.ALLOCATION);pop(stack,V.ALLOCATION);stack.add(V.STACK);links.hostCall(n.name,c);
                } else {require(display&&exact(c,Opcodes.INVOKEINTERFACE,OUTPUT,"accept","(L"+LIKE+";)V",true),"UNVERIFIED_TAB_CALLBACK_API");pop(stack,V.ITEM);pop(stack,V.OUTPUT);outputs++;links.hostCall(n.name,c);}
            } else {require(op.getOpcode()==(display?Opcodes.RETURN:Opcodes.ARETURN),"TAB_CALLBACK_INSTRUCTION");if(!display)pop(stack,V.STACK);require(stack.isEmpty(),"TAB_CALLBACK_STACK");done=true;}
        }
        require(done&&reads>0&&(!display||outputs>0),"TAB_CALLBACK_CLOSURE");verified.add(key(n.name,m.name,m.desc));
    }
    private int marker(List<AbstractInsnNode> c,int i,String owner) {
        if(i+2<c.size()&&c.get(i) instanceof FieldInsnNode f&&f.getOpcode()==Opcodes.GETSTATIC&&f.owner.equals("java/lang/System")&&f.name.equals("out")&&f.desc.equals("Ljava/io/PrintStream;")&&c.get(i+1) instanceof LdcInsnNode l&&l.cst instanceof String&&call(c.get(i+2),Opcodes.INVOKEVIRTUAL,"java/io/PrintStream","println","(Ljava/lang/String;)V")){links.hostCall(owner,(MethodInsnNode)c.get(i+2));return 3;}return 0;
    }
    private static boolean exact(MethodInsnNode c,int op,String owner,String name,String desc,boolean itf){return c.getOpcode()==op&&c.owner.equals(owner)&&c.name.equals(name)&&c.desc.equals(desc)&&c.itf==itf;}
    private static void pop(List<V> stack,V value){require(!stack.isEmpty()&&stack.removeLast()==value,"TAB_DATAFLOW "+value);}
}
