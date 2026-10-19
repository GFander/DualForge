package io.github.gfander.dualforge;

import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static io.github.gfander.dualforge.ForgeDualBusPlan.*;
import static io.github.gfander.dualforge.ForgeNetworkPlan.*;

/** A finite parent/type/codec/process proof. Source classes are read-only dependencies, not rewrite targets. */
final class ForgeDerivedPayloadPlan {
    record Proof(String owner,String parent,String context,String inherited,String type,String codec,String id,String kind,Set<String> classes) { }
    record Geometry(String parent,String context,String inherited,Map<String,List<FieldNode>> constructors,Set<String> methods) { }
    private ForgeDerivedPayloadPlan() { }
    static void fence(ClassNode n){
        require(n!=null,"CROSS_MODFILE_CONFLICT");RegistryBytecodeVerifier.verify(n);
        require(n.version==Opcodes.V21 && n.outerClass==null && n.nestHostClass==null && (n.nestMembers==null || n.nestMembers.isEmpty()) && n.visibleAnnotations==null && n.invisibleAnnotations==null,"UNKNOWN_PAYLOAD_SUPERCLASS");
        require(!n.name.startsWith("java/") && !n.name.startsWith("javax/") && !n.name.startsWith("net/minecraft") && !n.name.startsWith("net/neoforged/") && !n.name.startsWith("io/github/gfander/dualforge/"),"WRONG_MODFILE");
        Set<String> fields=new HashSet<>(),methods=new HashSet<>();for(var f:n.fields)require(fields.add(f.name) && f.value==null,"WRONG_FIELD_OWNER");
        for(var m:n.methods)require(methods.add(m.name+m.desc) && m.annotationDefault==null && (m.access&(Opcodes.ACC_NATIVE|Opcodes.ACC_SYNCHRONIZED))==0 && m.tryCatchBlocks.isEmpty(),"UNVERIFIED_INHERITED_METHOD");
    }
    static FieldNode field(ClassNode n,String name,String desc){return n.fields.stream().filter(f->f.name.equals(name) && f.desc.equals(desc)).findFirst().orElseThrow(()->new IllegalStateException("Dual bus rejected: WRONG_FIELD_OWNER "+n.name+"."+name+desc));}
    static String key(MethodNode m){return m.name+m.desc;}
    static int literal(Cursor c){var x=c.next();int op=x.getOpcode();if(op>=Opcodes.ICONST_M1 && op<=Opcodes.ICONST_5)return op-Opcodes.ICONST_0;if(x instanceof IntInsnNode v && (op==Opcodes.BIPUSH || op==Opcodes.SIPUSH))return v.operand;require(x instanceof LdcInsnNode l && l.cst instanceof Integer,"WRONG_SUPER_CONSTRUCTOR");return (Integer)((LdcInsnNode)x).cst;}
    static Geometry geometry(ClassNode n,Map<String,ClassNode> nodes){
        fence(n);require(n.access==(Opcodes.ACC_PUBLIC|Opcodes.ACC_SUPER) || n.access==(Opcodes.ACC_PUBLIC|Opcodes.ACC_FINAL|Opcodes.ACC_SUPER),"UNKNOWN_PAYLOAD_SUPERCLASS");require(n.interfaces.isEmpty(),"WRONG_INTERFACE_METHOD");
        ClassNode base=nodes.get(n.superName);require(base!=null,"UNKNOWN_PAYLOAD_SUPERCLASS "+n.superName);fence(base);
        require(base.access==(Opcodes.ACC_PUBLIC|Opcodes.ACC_ABSTRACT|Opcodes.ACC_SUPER) && base.superName.equals("java/lang/Object"),"UNKNOWN_PAYLOAD_SUPERCLASS "+base.name);require(base.interfaces.equals(List.of(PAYLOAD)),"WRONG_INTERFACE_METHOD");
        require(base.signature!=null && base.signature.startsWith("<") && base.signature.indexOf(':')>1,"UNKNOWN_PAYLOAD_SUPERCLASS generic binding");String formal=base.signature.substring(1,base.signature.indexOf(':'));require(!formal.isEmpty() && base.signature.equals("<"+formal+":L"+base.name+"<T"+formal+";>;>Ljava/lang/Object;L"+PAYLOAD+";") && ("L"+base.name+"<L"+n.name+";>;").equals(n.signature),"UNKNOWN_PAYLOAD_SUPERCLASS generic binding");
        List<MethodNode> process=base.methods.stream().filter(m->m.name.equals("process") && Type.getArgumentTypes(m.desc).length==1 && Type.getArgumentTypes(m.desc)[0].getSort()==Type.OBJECT && Type.getReturnType(m.desc).equals(Type.VOID_TYPE)).toList();require(process.size()==1,"UNKNOWN_PROCESS_METHOD");
        String context=Type.getArgumentTypes(process.getFirst().desc)[0].getInternalName();require(nodes.containsKey(context),"WRONG_PROCESS_CONTEXT");
        Set<String> expected=new HashSet<>();
        for(String[] abi:List.of(new String[]{"type","()L"+PT+";"},new String[]{"streamCodec","()L"+CODEC+";"},new String[]{"process","(L"+context+";)V"})){
            MethodNode method=method(base,abi[0],abi[1]);require(method.access==(Opcodes.ACC_PUBLIC|Opcodes.ACC_ABSTRACT) && method.instructions.size()==0,"WRONG_INTERFACE_METHOD");if(abi[0].equals("type"))require(("()L"+PT+"<T"+formal+";>;").equals(method.signature),"WRONG_TYPE_METHOD_RESOLUTION");if(abi[0].equals("streamCodec"))require(("()L"+CODEC+"<L"+BUFFER+";T"+formal+";>;").equals(method.signature),"WRONG_CODEC_BUFFER");expected.add(key(method));
        }
        Cursor init=new Cursor(method(base,"<init>","()V"));require(init.method.access==Opcodes.ACC_PUBLIC || init.method.access==Opcodes.ACC_PROTECTED,"WRONG_SUPER_CONSTRUCTOR");init.base();String inherited=null;
        if(!base.fields.isEmpty()){
            require(base.fields.size()==1,"WRONG_SUPER_CONSTRUCTOR");FieldNode value=base.fields.getFirst();require(value.access==(Opcodes.ACC_PRIVATE|Opcodes.ACC_FINAL) && value.desc.equals("I") && value.value==null,"WRONG_SUPER_CONSTRUCTOR");init.var(Opcodes.ALOAD,0);literal(init);init.field(Opcodes.PUTFIELD,base.name,value.name,"I");
            List<MethodNode> getters=base.methods.stream().filter(m->m.access==(Opcodes.ACC_PUBLIC|Opcodes.ACC_FINAL) && m.desc.equals("()I")).toList();require(getters.size()==1,"UNVERIFIED_INHERITED_METHOD");MethodNode getter=getters.getFirst();Cursor g=new Cursor(getter);g.var(Opcodes.ALOAD,0);g.field(Opcodes.GETFIELD,base.name,value.name,"I");g.op(Opcodes.IRETURN);g.end();require(n.methods.stream().noneMatch(m->m.name.equals(getter.name) && m.desc.equals(getter.desc)),"UNVERIFIED_INHERITED_METHOD override final");inherited=getter.name;expected.add(key(getter));
        }
        init.op(Opcodes.RETURN);init.end();expected.add(key(init.method));require(base.methods.stream().allMatch(m->expected.contains(key(m))) && base.methods.size()==expected.size(),"UNVERIFIED_INHERITED_METHOD");
        Map<String,List<FieldNode>> constructors=new HashMap<>();Set<String> verified=new HashSet<>();
        for(var m:n.methods)if(m.name.equals("<init>")){
            require(m.access==Opcodes.ACC_PUBLIC || m.access==Opcodes.ACC_PRIVATE,"WRONG_SUPER_CONSTRUCTOR");Cursor c=new Cursor(m);c.var(Opcodes.ALOAD,0);var call=c.next();require(call instanceof MethodInsnNode x && x.getOpcode()==Opcodes.INVOKESPECIAL && x.owner.equals(base.name) && x.name.equals("<init>") && x.desc.equals("()V") && !x.itf,"WRONG_SUPER_CONSTRUCTOR");
            List<FieldNode> assigned=new ArrayList<>();int slot=1;for(Type arg:Type.getArgumentTypes(m.desc)){
                c.var(Opcodes.ALOAD,0);c.var(arg.getOpcode(Opcodes.ILOAD),slot);slot+=arg.getSize();var op=c.next();require(op instanceof FieldInsnNode f && f.getOpcode()==Opcodes.PUTFIELD && f.owner.equals(n.name) && f.desc.equals(arg.getDescriptor()),"WRONG_FIELD_OWNER");FieldInsnNode f=(FieldInsnNode)op;FieldNode value=field(n,f.name,f.desc);require(value.access==(Opcodes.ACC_PRIVATE|Opcodes.ACC_FINAL) && assigned.stream().noneMatch(v->v.name.equals(value.name)),"WRONG_FIELD_OWNER");assigned.add(value);
            }
            require(assigned.size()==n.fields.stream().filter(f->(f.access&Opcodes.ACC_STATIC)==0).count(),"WRONG_SUPER_CONSTRUCTOR partial field initialization");c.op(Opcodes.RETURN);c.end();constructors.put(m.desc,List.copyOf(assigned));verified.add(key(m));
        }
        require(!constructors.isEmpty(),"WRONG_SUPER_CONSTRUCTOR");
        for(String name:List.of("type","streamCodec")){
            String desc="()L"+(name.equals("type")?PT:CODEC)+";";MethodNode m=method(n,name,desc);require(m.access==Opcodes.ACC_PUBLIC,"WRONG_TYPE_METHOD_RESOLUTION");require((name.equals("type")?"()L"+PT+"<L"+n.name+";>;":"()L"+CODEC+"<L"+BUFFER+";L"+n.name+";>;").equals(m.signature),"WRONG_TYPE_METHOD_RESOLUTION generic method binding");Cursor c=new Cursor(m);var op=c.next();require(op instanceof FieldInsnNode f && f.getOpcode()==Opcodes.GETSTATIC && f.owner.equals(n.name) && f.desc.equals(desc.substring(2)),"WRONG_TYPE_METHOD_RESOLUTION");FieldInsnNode f=(FieldInsnNode)op;require(field(n,f.name,f.desc).access==(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC|Opcodes.ACC_FINAL),"WRONG_TYPE_METHOD_RESOLUTION");c.op(Opcodes.ARETURN);c.end();verified.add(key(m));
        }
        List<MethodNode> concreteProcess=n.methods.stream().filter(m->m.name.equals("process")).toList();require(concreteProcess.size()==1,"UNKNOWN_PROCESS_METHOD");require(concreteProcess.getFirst().desc.equals("(L"+context+";)V"),"WRONG_PROCESS_CONTEXT");require(concreteProcess.getFirst().access==Opcodes.ACC_PUBLIC,"UNKNOWN_PROCESS_METHOD");
        return new Geometry(base.name,context,inherited,Map.copyOf(constructors),verified);
    }
    static Proof inspect(ClassNode n,String id,Map<String,ClassNode> nodes){
        Geometry g=geometry(n,nodes);var codec=ForgeRichCodecPlan.verify(n,id,nodes,g);Set<String> methods=new HashSet<>(g.methods);methods.addAll(codec.methods());methods.add("process(L"+g.context+";)V");require(n.methods.size()==methods.size() && n.methods.stream().allMatch(m->methods.contains(key(m))),"UNKNOWN_BUSINESS_SIDE_EFFECT unproved source method");
        Set<String> classes=new HashSet<>(codec.classes());classes.addAll(List.of(n.name,g.parent,g.context));return new Proof(n.name,g.parent,g.context,g.inherited,codec.type(),codec.codec(),codec.id(),codec.kind(),Set.copyOf(classes));
    }
    static void process(Proof proof,Map<String,ClassNode> nodes){
        MethodNode m=method(nodes.get(proof.owner),"process","(L"+proof.context+";)V");Cursor c=new Cursor(m);
        if(proof.inherited!=null){
            c.var(Opcodes.ALOAD,1);c.var(Opcodes.ALOAD,0);c.var(Opcodes.ALOAD,0);var op=c.next();require(op instanceof MethodInsnNode call && call.getOpcode()==Opcodes.INVOKEVIRTUAL && !call.itf && (call.owner.equals(proof.owner) || call.owner.equals(proof.parent)) && call.name.equals(proof.inherited) && call.desc.equals("()I"),"UNVERIFIED_INHERITED_METHOD");
            var call=c.next();require(call instanceof MethodInsnNode x && x.getOpcode()==Opcodes.INVOKEVIRTUAL && !x.itf && x.owner.equals(proof.context) && x.desc.equals("(Ljava/lang/Object;I)V") && nodes.get(proof.context).methods.stream().anyMatch(v->v.name.equals(x.name) && v.desc.equals(x.desc) && v.access==Opcodes.ACC_PUBLIC),"WRONG_PROCESS_CONTEXT");var remainder=c.peek();require(remainder.getOpcode()==Opcodes.RETURN,"UNKNOWN_BUSINESS_SIDE_EFFECT source="+proof.owner+".process next="+(remainder instanceof MethodInsnNode extra?extra.owner+"."+extra.name+extra.desc:Integer.toString(remainder.getOpcode())));c.op(Opcodes.RETURN);c.end();return;
        }
        // Read-only diagnostics for an unapproved business body, never a substitute implementation.
        ClassNode context=nodes.get(proof.context);ForgeRichCodecPlan.referenceRecord(context);
        for(var op:ForgeRegistryTransformationPlan.ops(m))if(op instanceof MethodInsnNode call){
            if(call.owner.equals(context.name) && context.methods.stream().anyMatch(v->v.name.equals(call.name) && v.desc.equals(call.desc) && Type.getArgumentTypes(v.desc).length==0))continue;
            String why=Type.getReturnType(call.desc).equals(Type.BOOLEAN_TYPE) && Arrays.stream(Type.getArgumentTypes(call.desc)).anyMatch(t->List.of("Lnet/minecraft/world/entity/player/Player;","Lnet/minecraft/server/level/ServerPlayer;").contains(t.getDescriptor()))?"UNVERIFIED_PERMISSION_CHECK":"UNKNOWN_PROCESS_METHOD";
            require(false,why+" source="+proof.owner+".process(L"+proof.context+";)V call="+call.owner+"."+call.name+call.desc+" derivedType=STATIC_VERIFIED codec=STATIC_VERIFIED originalProcess=RETAINED");
        }
        require(false,"UNKNOWN_PROCESS_METHOD "+proof.owner);
    }
    static void context(ClassNode n,String config,String server,String client,Map<String,ClassNode> nodes){
        fence(n);require(n.access==(Opcodes.ACC_PUBLIC|Opcodes.ACC_FINAL|Opcodes.ACC_SUPER) && n.superName.equals("java/lang/Object") && n.interfaces.isEmpty() && n.fields.size()==9 && n.methods.size()==2,"WRONG_PROCESS_CONTEXT");
        Cursor c=new Cursor(method(n,"<init>","(L"+ForgeNetworkClosurePlan.PLAYER+";L"+config+";L"+server+";L"+client+";)V"));require(c.method.access==Opcodes.ACC_PUBLIC,"WRONG_PROCESS_CONTEXT");c.base();Set<String> fields=new HashSet<>();String configField=null;int slot=1;
        for(String type:List.of(ForgeNetworkClosurePlan.PLAYER,config,server,client)){c.var(Opcodes.ALOAD,0);c.var(Opcodes.ALOAD,slot++);String name=c.ownField(Opcodes.PUTFIELD,n,"L"+type+";",true);require(fields.add(name),"WRONG_FIELD_OWNER");if(type.equals(config))configField=name;}c.op(Opcodes.RETURN);c.end();
        MethodNode record=n.methods.stream().filter(m->!m.name.equals("<init>")).findFirst().orElseThrow();require(record.access==Opcodes.ACC_PUBLIC && record.desc.equals("(Ljava/lang/Object;I)V"),"WRONG_PROCESS_CONTEXT");c=new Cursor(record);c.var(Opcodes.ALOAD,0);c.op(Opcodes.DUP);String count=c.ownField(Opcodes.GETFIELD,n,"I",false);require(fields.add(count),"WRONG_FIELD_OWNER");c.op(Opcodes.ICONST_1);c.op(Opcodes.IADD);c.field(Opcodes.PUTFIELD,n.name,count,"I");
        for(String[] f:List.of(new String[]{"1","Ljava/lang/Object;"},new String[]{"2","I"})){c.var(Opcodes.ALOAD,0);c.var(f[1].equals("I")?Opcodes.ILOAD:Opcodes.ALOAD,Integer.parseInt(f[0]));require(fields.add(c.ownField(Opcodes.PUTFIELD,n,f[1],false)),"WRONG_FIELD_OWNER");}
        c.var(Opcodes.ALOAD,0);c.var(Opcodes.ALOAD,0);c.field(Opcodes.GETFIELD,n.name,configField,"L"+config+";");var call=c.next();require(call instanceof MethodInsnNode m && m.getOpcode()==Opcodes.INVOKEINTERFACE && m.itf && m.owner.equals(config) && m.desc.equals("()Z") && nodes.get(config).methods.stream().anyMatch(v->v.name.equals(m.name) && v.desc.equals(m.desc) && v.access==(Opcodes.ACC_PUBLIC|Opcodes.ACC_ABSTRACT)),"WRONG_PROCESS_CONTEXT");require(fields.add(c.ownField(Opcodes.PUTFIELD,n,"Z",false)),"WRONG_FIELD_OWNER");
        c.var(Opcodes.ALOAD,0);c.call(Opcodes.INVOKESTATIC,"java/lang/Thread","currentThread","()Ljava/lang/Thread;");c.call(Opcodes.INVOKEVIRTUAL,"java/lang/Thread","getName","()Ljava/lang/String;");require(fields.add(c.ownField(Opcodes.PUTFIELD,n,"Ljava/lang/String;",false)) && fields.size()==n.fields.size(),"WRONG_FIELD_OWNER");c.op(Opcodes.RETURN);c.end();
    }
}
