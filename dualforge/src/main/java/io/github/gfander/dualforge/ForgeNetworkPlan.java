package io.github.gfander.dualforge;

import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static io.github.gfander.dualforge.ForgeDualBusPlan.*;

/** Closed play/addMain definition contract. Connection queries, sending and context access are unapproved. */
final class ForgeNetworkPlan {
    static final String NET=F+"network/", BUILD=NET+"ChannelBuilder", FLOW=NET+"payload/PayloadFlow";
    static final String CONNECTION=NET+"payload/PayloadConnection", PROTOCOL=NET+"payload/PayloadProtocol", CHANNEL=NET+"Channel";
    static final String CONTEXT=F+"event/network/CustomPayloadEvent$Context", BI="Ljava/util/function/BiConsumer;";
    static final String RL="net/minecraft/resources/ResourceLocation", BUFFER="net/minecraft/network/RegistryFriendlyByteBuf";
    static final String PAYLOAD="net/minecraft/network/protocol/common/custom/CustomPacketPayload", PT=PAYLOAD+"$Type", CODEC="net/minecraft/network/codec/StreamCodec";
    static final String BRIDGE="io/github/gfander/dualforge/ForgeNetworkBridge";
    static final String ADD="(L"+PT+";L"+CODEC+";"+BI+")L"+FLOW+";";
    static final Map<String,String> SOURCE_TYPES=Map.of(BUILD,"java/lang/Object",FLOW,"java/lang/Object",CONNECTION,"java/lang/Object",PROTOCOL,"java/lang/Object",CHANNEL,"java/lang/Object",CONTEXT,"java/lang/Object");
    record Payload(String owner,String typeField,String codecField,String getter,String handlerField,String handlerMethod,String id,String direction) { }
    final String owner,channelId,channelField,entryField;final int version;final List<Payload> payloads;final ForgeNetworkClosurePlan closure;
    ForgeNetworkPlan(String owner,String channelId,String field,int version,List<Payload> payloads,String entryField,ForgeNetworkClosurePlan closure){this.closure=closure;this.owner=owner;this.channelId=channelId;this.entryField=entryField;channelField=field;this.version=version;this.payloads=List.copyOf(payloads);}
    Set<String> classes(){return closure!=null?closure.classes:Set.of(owner,payloads.get(0).owner,payloads.get(1).owner);}

    static ForgeNetworkPlan entry(Cursor c,ClassNode entry,String id,Map<String,ClassNode> nodes,String entryConfig,String config) {
        if(c.remaining()<2 || !(c.code.get(c.pos) instanceof VarInsnNode) || !(c.code.get(c.pos+1) instanceof TypeInsnNode))return null;
        c.var(Opcodes.ALOAD,0);String network=c.allocation(nodes);c.op(Opcodes.DUP);
        boolean extended=c.peek() instanceof VarInsnNode;if(extended){c.var(Opcodes.ALOAD,0);c.field(Opcodes.GETFIELD,entry.name,entryConfig,"L"+config+";");}
        String namespace=c.string(),path=c.string();require(namespace.equals(id) && valid(path),"WRONG_CHANNEL_OWNER");
        c.call(Opcodes.INVOKESTATIC,RL,"fromNamespaceAndPath","(Ljava/lang/String;Ljava/lang/String;)L"+RL+";");
        int version=integer(c);require(version>=0,"WRONG_PROTOCOL_VERSION");
        c.call(Opcodes.INVOKESPECIAL,network,"<init>",(extended?"(L"+config+";":"(")+"L"+RL+";I)V");String field=c.ownField(Opcodes.PUTFIELD,entry,"L"+network+";",true);
        return extended?ForgeNetworkClosurePlan.validate(nodes.get(network),namespace+":"+path,version,id,nodes,field,config,entryConfig):validate(nodes.get(network),namespace+":"+path,version,id,nodes,field);
    }
    static ForgeNetworkPlan validate(ClassNode n,String channelId,int version,String id,Map<String,ClassNode> nodes,String entryField) {
        plain(n,List.of());require(n.methods.size()==3 && n.fields.size()==9,"UNKNOWN_NETWORK_SIDE_EFFECT");
        Cursor c=new Cursor(method(n,"<init>","(L"+RL+";I)V"));require(c.method.access==Opcodes.ACC_PUBLIC,"UNKNOWN_NETWORK_BUILDER");c.base();
        List<Handle> handles=new ArrayList<>();List<String> fields=new ArrayList<>(),owners=new ArrayList<>();
        for(int i=0;i<2;i++) {
            c.var(Opcodes.ALOAD,0);c.var(Opcodes.ALOAD,0);var op=c.next();require(op instanceof InvokeDynamicInsnNode,"UNVERIFIED_HANDLER");var d=(InvokeDynamicInsnNode)op;
            require(d.bsm.equals(ForgeRegistryTransformationPlan.META) && d.name.equals("accept") && d.desc.equals("(L"+n.name+";)"+BI)
                && d.bsmArgs.length==3 && d.bsmArgs[0].equals(Type.getMethodType("(Ljava/lang/Object;Ljava/lang/Object;)V")) && d.bsmArgs[1] instanceof Handle,"UNSUPPORTED_BOOTSTRAP");
            var h=(Handle)d.bsmArgs[1];Type[] args=Type.getArgumentTypes(h.getDesc());
            require(h.getTag()==Opcodes.H_INVOKEVIRTUAL && !h.isInterface() && h.getOwner().equals(n.name) && args.length==2 && args[0].getSort()==Type.OBJECT
                && args[1].equals(Type.getObjectType(CONTEXT)) && Type.getReturnType(h.getDesc()).equals(Type.VOID_TYPE) && d.bsmArgs[2].equals(Type.getMethodType(h.getDesc())),"UNVERIFIED_HANDLER");
            handles.add(h);owners.add(args[0].getInternalName());fields.add(c.ownField(Opcodes.PUTFIELD,n,BI,true));
        }
        require(!fields.get(0).equals(fields.get(1)) && !owners.get(0).equals(owners.get(1)),"DUPLICATE_PAYLOAD_ID");
        c.var(Opcodes.ALOAD,0);c.var(Opcodes.ALOAD,1);c.call(Opcodes.INVOKESTATIC,BUILD,"named","(L"+RL+";)L"+BUILD+";");
        c.var(Opcodes.ILOAD,2);c.call(Opcodes.INVOKEVIRTUAL,BUILD,"networkProtocolVersion","(I)L"+BUILD+";");
        c.call(Opcodes.INVOKEVIRTUAL,BUILD,"optional","()L"+BUILD+";");
        c.call(Opcodes.INVOKEVIRTUAL,BUILD,"payloadChannel","()L"+CONNECTION+";");c.call(Opcodes.INVOKEINTERFACE,CONNECTION,"play","()L"+PROTOCOL+";");
        List<Payload> payloads=new ArrayList<>();Set<String> ids=new HashSet<>(),mutableFields=new HashSet<>();
        for(int i=0;i<2;i++) {
            c.call(Opcodes.INVOKEINTERFACE,i==0?PROTOCOL:FLOW,i==0?"serverbound":"clientbound","()Ljava/lang/Object;");c.type(Opcodes.CHECKCAST,FLOW);
            ClassNode packet=nodes.get(owners.get(i));require(packet!=null,"CROSS_MODFILE_PAYLOAD");
            Payload p=packet(packet,id,fields.get(i),handles.get(i).getName(),i==0?"SERVERBOUND":"CLIENTBOUND");require(ids.add(p.id),"DUPLICATE_PAYLOAD_ID");
            c.field(Opcodes.GETSTATIC,p.owner,p.typeField,"L"+PT+";");c.field(Opcodes.GETSTATIC,p.owner,p.codecField,"L"+CODEC+";");
            c.var(Opcodes.ALOAD,0);c.field(Opcodes.GETFIELD,n.name,p.handlerField,BI);c.call(Opcodes.INVOKEINTERFACE,FLOW,"addMain",ADD);
            callback(n,handles.get(i),p,mutableFields);payloads.add(p);
        }
        c.call(Opcodes.INVOKEINTERFACE,FLOW,"build","()L"+CHANNEL+";");String field=c.ownField(Opcodes.PUTFIELD,n,"L"+CHANNEL+";",true);c.op(Opcodes.RETURN);c.end();
        require(mutableFields.size()==6 && n.fields.stream().filter(f->f.access==Opcodes.ACC_PRIVATE).allMatch(f->mutableFields.contains(f.name)),"UNKNOWN_NETWORK_SIDE_EFFECT");
        return new ForgeNetworkPlan(n.name,channelId,field,version,payloads,entryField,null);
    }
    static boolean valid(String path){return path!=null && !path.isEmpty() && path.matches("[a-z0-9/._-]+");}
    private static int integer(Cursor c){var op=c.next();if(op.getOpcode()>=Opcodes.ICONST_M1 && op.getOpcode()<=Opcodes.ICONST_5)return op.getOpcode()-Opcodes.ICONST_0;
        if(op instanceof IntInsnNode v && (v.getOpcode()==Opcodes.BIPUSH || v.getOpcode()==Opcodes.SIPUSH))return v.operand;
        require(op instanceof LdcInsnNode l && l.cst instanceof Integer,"WRONG_PROTOCOL_VERSION");return (Integer)((LdcInsnNode)op).cst;}
    static void plain(ClassNode n,List<String> interfaces){require(n!=null,"CROSS_MODFILE_PAYLOAD");RegistryBytecodeVerifier.verify(n);
        require(n.version==Opcodes.V21 && n.access==(Opcodes.ACC_PUBLIC|Opcodes.ACC_FINAL|Opcodes.ACC_SUPER) && n.superName.equals("java/lang/Object") && n.interfaces.equals(interfaces)
            && n.outerClass==null && n.nestHostClass==null && (n.nestMembers==null || n.nestMembers.isEmpty()) && n.visibleAnnotations==null && n.invisibleAnnotations==null
            && !n.name.startsWith("java/") && !n.name.startsWith("javax/") && !n.name.startsWith("net/minecraft") && !n.name.startsWith("net/neoforged/") && !n.name.startsWith("io/github/gfander/dualforge/"),"PARTIAL_NETWORK_PLAN");
        Set<String> fields=new HashSet<>(),methods=new HashSet<>();for(var f:n.fields)require(fields.add(f.name),"UNKNOWN_NETWORK_SIDE_EFFECT");
        for(var m:n.methods)require(methods.add(m.name+m.desc) && m.tryCatchBlocks.isEmpty() && m.annotationDefault==null && (m.access&(Opcodes.ACC_NATIVE|Opcodes.ACC_ABSTRACT|Opcodes.ACC_SYNCHRONIZED))==0,"UNKNOWN_NETWORK_SIDE_EFFECT");
    }
    private static Payload packet(ClassNode n,String id,String handlerField,String handlerMethod,String direction) {
        plain(n,List.of(PAYLOAD));require(n.fields.size()==3 && n.methods.size()==6,"UNSUPPORTED_CODEC");
        Cursor c=new Cursor(method(n,"<init>","(I)V"));require(c.method.access==Opcodes.ACC_PUBLIC,"UNSUPPORTED_CODEC");c.base();c.var(Opcodes.ALOAD,0);c.var(Opcodes.ILOAD,1);
        String number=c.ownField(Opcodes.PUTFIELD,n,"I",true);c.op(Opcodes.RETURN);c.end();
        var getters=n.methods.stream().filter(m->m.access==Opcodes.ACC_PUBLIC && m.desc.equals("()I")).toList();require(getters.size()==1,"UNSUPPORTED_CODEC");String getter=getters.getFirst().name;
        c=new Cursor(getters.getFirst());c.var(Opcodes.ALOAD,0);c.field(Opcodes.GETFIELD,n.name,number,"I");c.op(Opcodes.IRETURN);c.end();
        c=new Cursor(method(n,"<clinit>","()V"));require(c.method.access==Opcodes.ACC_STATIC,"UNSUPPORTED_CODEC");c.type(Opcodes.NEW,PT);c.op(Opcodes.DUP);
        String namespace=c.string(),path=c.string();require(namespace.equals(id) && valid(path),"CROSS_MODFILE_PAYLOAD");
        c.call(Opcodes.INVOKESTATIC,RL,"fromNamespaceAndPath","(Ljava/lang/String;Ljava/lang/String;)L"+RL+";");c.call(Opcodes.INVOKESPECIAL,PT,"<init>","(L"+RL+";)V");
        String type=staticField(c,n,"L"+PT+";");
        Handle encode=codecLambda(c,n,"encode","StreamEncoder","(Ljava/lang/Object;Ljava/lang/Object;)V","(L"+BUFFER+";L"+n.name+";)V");
        Handle decode=codecLambda(c,n,"decode","StreamDecoder","(Ljava/lang/Object;)Ljava/lang/Object;","(L"+BUFFER+";)L"+n.name+";");
        var factory=c.next();require(factory instanceof MethodInsnNode f && f.getOpcode()==Opcodes.INVOKESTATIC && f.itf && f.owner.equals(CODEC) && f.name.equals("of") && f.desc.equals("(Lnet/minecraft/network/codec/StreamEncoder;Lnet/minecraft/network/codec/StreamDecoder;)L"+CODEC+";"),"UNSUPPORTED_CODEC");String codec=staticField(c,n,"L"+CODEC+";");c.op(Opcodes.RETURN);c.end();
        c=new Cursor(method(n,encode.getName(),encode.getDesc()));c.var(Opcodes.ALOAD,0);c.var(Opcodes.ALOAD,1);c.field(Opcodes.GETFIELD,n.name,number,"I");
        c.call(Opcodes.INVOKEVIRTUAL,BUFFER,"writeVarInt","(I)Lnet/minecraft/network/FriendlyByteBuf;");c.op(Opcodes.POP);c.op(Opcodes.RETURN);c.end();
        c=new Cursor(method(n,decode.getName(),decode.getDesc()));c.type(Opcodes.NEW,n.name);c.op(Opcodes.DUP);c.var(Opcodes.ALOAD,0);c.call(Opcodes.INVOKEVIRTUAL,BUFFER,"readVarInt","()I");
        c.call(Opcodes.INVOKESPECIAL,n.name,"<init>","(I)V");c.op(Opcodes.ARETURN);c.end();
        c=new Cursor(method(n,"type","()L"+PT+";"));require(c.method.access==Opcodes.ACC_PUBLIC,"UNSUPPORTED_CODEC");c.field(Opcodes.GETSTATIC,n.name,type,"L"+PT+";");c.op(Opcodes.ARETURN);c.end();
        return new Payload(n.name,type,codec,getter,handlerField,handlerMethod,namespace+":"+path,direction);
    }
    static String staticField(Cursor c,ClassNode n,String desc){var op=c.next();require(op instanceof FieldInsnNode f && f.getOpcode()==Opcodes.PUTSTATIC && f.owner.equals(n.name) && f.desc.equals(desc),"UNSUPPORTED_CODEC");
        String name=((FieldInsnNode)op).name;require(n.fields.stream().anyMatch(f->f.name.equals(name) && f.desc.equals(desc) && f.access==(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC|Opcodes.ACC_FINAL) && f.value==null),"UNSUPPORTED_CODEC");return name;}
    private static Handle codecLambda(Cursor c,ClassNode n,String name,String target,String erased,String typed){var op=c.next();require(op instanceof InvokeDynamicInsnNode,"UNSUPPORTED_BOOTSTRAP");var d=(InvokeDynamicInsnNode)op;
        require(d.bsm.equals(ForgeRegistryTransformationPlan.META) && d.name.equals(name) && d.desc.equals("()Lnet/minecraft/network/codec/"+target+";") && d.bsmArgs.length==3
            && d.bsmArgs[0].equals(Type.getMethodType(erased)) && d.bsmArgs[1] instanceof Handle && d.bsmArgs[2].equals(Type.getMethodType(typed)),"UNSUPPORTED_BOOTSTRAP");
        Handle h=(Handle)d.bsmArgs[1];require(h.getTag()==Opcodes.H_INVOKESTATIC && !h.isInterface() && h.getOwner().equals(n.name) && h.getDesc().equals(typed)
            && method(n,h.getName(),h.getDesc()).access==(Opcodes.ACC_PRIVATE|Opcodes.ACC_STATIC|Opcodes.ACC_SYNTHETIC),"UNSUPPORTED_CODEC");return h;}
    private static void callback(ClassNode n,Handle h,Payload p,Set<String> fields){Cursor c=new Cursor(method(n,h.getName(),h.getDesc()));require(c.method.access==Opcodes.ACC_PRIVATE,"UNVERIFIED_HANDLER");
        c.var(Opcodes.ALOAD,0);c.op(Opcodes.DUP);String counter=c.ownField(Opcodes.GETFIELD,n,"I",false);c.op(Opcodes.ICONST_1);c.op(Opcodes.IADD);c.field(Opcodes.PUTFIELD,n.name,counter,"I");require(fields.add(counter),"UNVERIFIED_HANDLER");
        c.var(Opcodes.ALOAD,0);c.var(Opcodes.ALOAD,1);c.call(Opcodes.INVOKEVIRTUAL,p.owner,p.getter,"()I");require(fields.add(c.ownField(Opcodes.PUTFIELD,n,"I",false)),"UNVERIFIED_HANDLER");
        c.var(Opcodes.ALOAD,0);c.call(Opcodes.INVOKESTATIC,"java/lang/Thread","currentThread","()Ljava/lang/Thread;");c.call(Opcodes.INVOKEVIRTUAL,"java/lang/Thread","getName","()Ljava/lang/String;");
        require(fields.add(c.ownField(Opcodes.PUTFIELD,n,"Ljava/lang/String;",false)),"UNVERIFIED_HANDLER");c.op(Opcodes.RETURN);c.end();
    }
    void rewrite(ClassNode n){if(closure!=null)closure.rewrite(n,this);if(!n.name.equals(owner))return;
        for(var f:n.fields)if(f.desc.equals("L"+CHANNEL+";"))f.signature=null;
        n.innerClasses.removeIf(i->SOURCE_TYPES.containsKey(i.name));
        for(var m:n.methods)for(var op:m.instructions.toArray())if(op instanceof MethodInsnNode call && (call.owner.equals(BUILD)||call.owner.equals(CONNECTION)||call.owner.equals(PROTOCOL)||call.owner.equals(FLOW))) {
            String name=call.name,desc="(Ljava/lang/Object;)Ljava/lang/Object;";
            if(name.equals("named")){m.instructions.insertBefore(op,new VarInsnNode(Opcodes.ALOAD,0));desc="(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";}
            else if(name.equals("networkProtocolVersion")){name="version";desc="(Ljava/lang/Object;I)Ljava/lang/Object;";}
            else if(name.equals("addMain") || name.equals("add"))desc="(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/util/function/BiConsumer;)Ljava/lang/Object;";
            m.instructions.set(op,new MethodInsnNode(Opcodes.INVOKESTATIC,BRIDGE,name,desc,false));m.maxStack+=2;
        }
    }
}
