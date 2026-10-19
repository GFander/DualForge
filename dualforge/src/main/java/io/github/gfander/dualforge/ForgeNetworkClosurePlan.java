package io.github.gfander.dualforge;

import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static io.github.gfander.dualforge.ForgeDualBusPlan.*;
import static io.github.gfander.dualforge.ForgeNetworkPlan.*;

/** Closed source-object/add/context/multi-codec extension; no sending or presence operations. */
final class ForgeNetworkClosurePlan {
    static final String PLAYER="net/minecraft/server/level/ServerPlayer", ANCHOR="dualforge$networkState";
    record Connection(String owner,String api,String parent) { }
    record Global(String owner,String setter,String value,String counter,String featureOwner,String featureField) { }
    final String config,configField,entryConfig,business;final Connection server,client;final String serverField,clientField;
    final Global global;final Map<String,String> codecKinds;final Set<String> classes,readOnly;
    private ForgeNetworkClosurePlan(String config,String configField,String entryConfig,String business,Connection server,Connection client,String sf,String cf,Global global,Map<String,String> kinds,Set<String> classes,Set<String> readOnly){
        this.config=config;this.configField=configField;this.entryConfig=entryConfig;this.business=business;this.server=server;this.client=client;serverField=sf;clientField=cf;this.global=global;codecKinds=Map.copyOf(kinds);this.classes=Set.copyOf(classes);this.readOnly=Set.copyOf(readOnly);
    }
    static ForgeNetworkPlan validate(ClassNode n,String channelId,int version,String id,Map<String,ClassNode> nodes,String entryField,String config,String entryConfig){
        plain(n,List.of());Cursor c=new Cursor(method(n,"<init>","(L"+config+";L"+RL+";I)V"));require(c.method.access==Opcodes.ACC_PUBLIC,"WRONG_NETWORK_HANDLER_IDENTITY");c.base();c.var(Opcodes.ALOAD,0);c.var(Opcodes.ALOAD,1);String configField=c.ownField(Opcodes.PUTFIELD,n,"L"+config+";",true);
        Connection server=connection(c,n,nodes);String sf=stored(c,n,"L"+server.api+";");
        c.var(Opcodes.ALOAD,0);c.field(Opcodes.GETFIELD,n.name,sf,"L"+server.api+";");MethodInsnNode write=c.ownedCall(nodes);
        require(write.getOpcode()==Opcodes.INVOKESTATIC && write.desc.equals("(L"+server.api+";)V"),"GLOBAL_CONNECTION_WRONG_ORDER");Global global=global(nodes.get(write.owner),write.name,server.api,nodes);
        Connection client=connection(c,n,nodes);String cf=stored(c,n,"L"+client.api+";");require(!server.owner.equals(client.owner) && !sf.equals(cf),"WRONG_NETWORK_HANDLER_IDENTITY");
        List<Handle> handlers=new ArrayList<>();List<String> fields=new ArrayList<>(),packets=new ArrayList<>();
        while(c.remaining()>2 && c.code.get(c.pos+2) instanceof InvokeDynamicInsnNode){
            require(handlers.size()<16,"PARTIAL_NETWORK_PLAN");c.var(Opcodes.ALOAD,0);c.var(Opcodes.ALOAD,0);var op=c.next();require(op instanceof InvokeDynamicInsnNode,"UNVERIFIED_HANDLER_CAPTURE");var d=(InvokeDynamicInsnNode)op;
            require(d.bsm.equals(ForgeRegistryTransformationPlan.META) && d.name.equals("accept") && d.desc.equals("(L"+n.name+";)"+BI) && d.bsmArgs.length==3 && d.bsmArgs[0].equals(Type.getMethodType("(Ljava/lang/Object;Ljava/lang/Object;)V")) && d.bsmArgs[1] instanceof Handle,"UNSUPPORTED_BOOTSTRAP");
            Handle h=(Handle)d.bsmArgs[1];Type[] params=Type.getArgumentTypes(h.getDesc());require(h.getTag()==Opcodes.H_INVOKEVIRTUAL && !h.isInterface() && h.getOwner().equals(n.name) && params.length==2 && params[0].getSort()==Type.OBJECT && params[1].equals(Type.getObjectType(CONTEXT)) && Type.getReturnType(h.getDesc()).equals(Type.VOID_TYPE) && d.bsmArgs[2].equals(Type.getMethodType(h.getDesc())),"UNVERIFIED_HANDLER_CAPTURE");
            handlers.add(h);packets.add(params[0].getInternalName());fields.add(c.ownField(Opcodes.PUTFIELD,n,BI,true));
        }
        int count=handlers.size();require(count>=2 && new HashSet<>(fields).size()==count && new HashSet<>(packets).size()==count,"DUPLICATE_PAYLOAD_ID");
        c.var(Opcodes.ALOAD,0);c.var(Opcodes.ALOAD,2);c.call(Opcodes.INVOKESTATIC,BUILD,"named","(L"+RL+";)L"+BUILD+";");c.var(Opcodes.ILOAD,3);c.call(Opcodes.INVOKEVIRTUAL,BUILD,"networkProtocolVersion","(I)L"+BUILD+";");c.call(Opcodes.INVOKEVIRTUAL,BUILD,"optional","()L"+BUILD+";");c.call(Opcodes.INVOKEVIRTUAL,BUILD,"payloadChannel","()L"+CONNECTION+";");c.call(Opcodes.INVOKEINTERFACE,CONNECTION,"play","()L"+PROTOCOL+";");
        Map<String,ForgeDerivedPayloadPlan.Proof> derived=new HashMap<>();List<Payload> defs=new ArrayList<>();Map<String,String> kinds=new HashMap<>();Set<String> ids=new HashSet<>(),used=new HashSet<>(List.of(configField,sf,cf));String direction="SERVERBOUND",business=null;boolean switched=false;
        c.call(Opcodes.INVOKEINTERFACE,PROTOCOL,"serverbound","()Ljava/lang/Object;");c.type(Opcodes.CHECKCAST,FLOW);
        for(int i=0;i<count;i++){
            if(c.peek() instanceof MethodInsnNode call && call.name.equals("clientbound")){require(i>0 && !switched,"WRONG_PAYLOAD_DIRECTION");c.call(Opcodes.INVOKEINTERFACE,FLOW,"clientbound","()Ljava/lang/Object;");c.type(Opcodes.CHECKCAST,FLOW);direction="CLIENTBOUND";switched=true;}
            String packet=packets.get(i);ClassNode pn=nodes.get(packet);require(pn!=null,"CROSS_MODFILE_CONFLICT");ForgeCodecPlan.Proof proof;if(!pn.superName.equals("java/lang/Object")){var complete=ForgeDerivedPayloadPlan.inspect(pn,id,nodes);ForgeDerivedPayloadPlan.process(complete,nodes);derived.put(packet,complete);proof=new ForgeCodecPlan.Proof(complete.type(),complete.codec(),complete.id(),complete.kind());}else proof=ForgeCodecPlan.verify(pn,id);require(ids.add(proof.id()),"DUPLICATE_PAYLOAD_ID");kinds.put(packet,proof.kind());
            c.field(Opcodes.GETSTATIC,packet,proof.type(),"L"+PT+";");c.field(Opcodes.GETSTATIC,packet,proof.codec(),"L"+CODEC+";");c.var(Opcodes.ALOAD,0);c.field(Opcodes.GETFIELD,n.name,fields.get(i),BI);c.call(Opcodes.INVOKEINTERFACE,FLOW,"add",ADD);
            String actual=handler(n,handlers.get(i),packet,config,configField,server.api,sf,client.api,cf,used,derived.get(packet));if(business==null)business=actual;require(business.equals(actual),"UNVERIFIED_HANDLER_CAPTURE");
            used.add(fields.get(i));defs.add(new Payload(packet,proof.type(),proof.codec(),"",fields.get(i),handlers.get(i).getName(),proof.id(),direction));
        }
        require(switched,"WRONG_PAYLOAD_DIRECTION");c.call(Opcodes.INVOKEINTERFACE,FLOW,"build","()L"+CHANNEL+";");String channel=c.ownField(Opcodes.PUTFIELD,n,"L"+CHANNEL+";",true);used.add(channel);c.op(Opcodes.RETURN);c.end();
        for(var m:n.methods)for(var op:ForgeRegistryTransformationPlan.ops(m))if(op instanceof MethodInsnNode call && call.owner.equals(CHANNEL))require(false,call.name.equals("isRemotePresent")?"UNVERIFIED_REMOTE_PRESENCE":List.of("send","reply","toVanillaPacket").contains(call.name)?"UNVERIFIED_SEND_OPERATION":"UNKNOWN_CHANNEL_OPERATION");
        require(n.methods.size()==1+2*count && n.fields.size()==4+5*count && n.fields.stream().allMatch(f->used.contains(f.name)),"UNKNOWN_STATIC_SIDE_EFFECT");
        require(derived.isEmpty() || derived.size()==count,"PARTIAL_NETWORK_PLAN mixed callback profiles");
        if(derived.isEmpty())business(nodes.get(business),config,server.api,client.api);else ForgeDerivedPayloadPlan.context(nodes.get(business),config,server.api,client.api,nodes);
        Set<String> classes=new HashSet<>(packets);classes.addAll(List.of(n.name,server.owner,server.api,client.owner,client.api,global.owner,global.featureOwner,business));
        Set<String> readOnly=new HashSet<>();if(!derived.isEmpty()){derived.values().forEach(p->classes.addAll(p.classes()));readOnly.addAll(classes);readOnly.removeAll(List.of(n.name,global.owner));for(String source:readOnly)require(!new String(ForgeDualBusPlan.bytes(nodes.get(source)),java.nio.charset.StandardCharsets.ISO_8859_1).contains(ForgeDualBusPlan.F),"UNKNOWN_BUSINESS_SIDE_EFFECT read-only dependency contains Forge ABI source="+source);}
        var closure=new ForgeNetworkClosurePlan(config,configField,entryConfig,business,server,client,sf,cf,global,kinds,classes,readOnly);
        return new ForgeNetworkPlan(n.name,channelId,channel,version,defs,entryField,closure);
    }
    private static Connection connection(Cursor c,ClassNode network,Map<String,ClassNode> nodes){
        c.var(Opcodes.ALOAD,0);String type=c.allocation(nodes);c.op(Opcodes.DUP);c.var(Opcodes.ALOAD,0);c.call(Opcodes.INVOKESPECIAL,type,"<init>","(L"+network.name+";)V");
        ClassNode n=nodes.get(type);require(n!=null && n.interfaces.size()==1,"WRONG_NETWORK_HANDLER_IDENTITY");String api=n.interfaces.getFirst();plain(n,List.of(api));require(n.fields.size()==1 && n.methods.size()==2,"UNKNOWN_CHANNEL_OPERATION");
        Cursor init=new Cursor(method(n,"<init>","(L"+network.name+";)V"));require(init.method.access==Opcodes.ACC_PUBLIC,"WRONG_NETWORK_HANDLER_IDENTITY");init.base();init.var(Opcodes.ALOAD,0);init.var(Opcodes.ALOAD,1);String parent=init.ownField(Opcodes.PUTFIELD,n,"L"+network.name+";",true);init.op(Opcodes.RETURN);init.end();
        var getters=n.methods.stream().filter(m->!m.name.equals("<init>")).toList();require(getters.size()==1,"UNKNOWN_CHANNEL_OPERATION");var read=getters.getFirst();require(read.access==Opcodes.ACC_PUBLIC && read.desc.equals("()L"+network.name+";"),"UNVERIFIED_SEND_OPERATION");Cursor g=new Cursor(read);g.var(Opcodes.ALOAD,0);g.field(Opcodes.GETFIELD,n.name,parent,"L"+network.name+";");g.op(Opcodes.ARETURN);g.end();
        ClassNode iface=nodes.get(api);require(iface!=null && iface.version==Opcodes.V21 && iface.access==(Opcodes.ACC_PUBLIC|Opcodes.ACC_INTERFACE|Opcodes.ACC_ABSTRACT) && iface.fields.isEmpty() && iface.interfaces.isEmpty() && iface.methods.size()==1 && iface.visibleAnnotations==null && iface.methods.getFirst().name.equals(read.name) && iface.methods.getFirst().desc.equals(read.desc) && iface.methods.getFirst().access==(Opcodes.ACC_PUBLIC|Opcodes.ACC_ABSTRACT),"UNKNOWN_CHANNEL_OPERATION");
        return new Connection(type,api,parent);
    }
    private static String stored(Cursor c,ClassNode n,String desc){return c.ownField(Opcodes.PUTFIELD,n,desc,true);}
    static Global global(ClassNode n,String setter,String api,Map<String,ClassNode> nodes){
        plain(n,List.of());Cursor c=new Cursor(method(n,setter,"(L"+api+";)V"));require(c.method.access==(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC),"GLOBAL_CONNECTION_OWNER_MISMATCH");c.var(Opcodes.ALOAD,0);var op=c.next();require(op instanceof FieldInsnNode f && f.getOpcode()==Opcodes.PUTSTATIC && f.owner.equals(n.name) && f.desc.equals("L"+api+";"),"GLOBAL_CONNECTION_OWNER_MISMATCH");String value=((FieldInsnNode)op).name;
        require(n.fields.stream().anyMatch(f->f.name.equals(value) && f.desc.equals("L"+api+";") && f.access==(Opcodes.ACC_PRIVATE|Opcodes.ACC_STATIC) && f.value==null),"GLOBAL_CONNECTION_OWNER_MISMATCH");
        String counter=null;if(c.peek() instanceof FieldInsnNode f && f.getOpcode()==Opcodes.GETSTATIC){counter=f.name;require(f.owner.equals(n.name) && f.desc.equals("I") && n.fields.stream().anyMatch(v->v.name.equals(f.name) && v.access==(Opcodes.ACC_PRIVATE|Opcodes.ACC_STATIC) && v.desc.equals("I")),"UNKNOWN_STATIC_SIDE_EFFECT");c.next();c.op(Opcodes.ICONST_1);c.op(Opcodes.IADD);c.field(Opcodes.PUTSTATIC,n.name,counter,"I");}c.op(Opcodes.RETURN);c.end();
        c=new Cursor(method(n,"<clinit>","()V"));String features=c.allocation(nodes);c.op(Opcodes.DUP);c.call(Opcodes.INVOKESPECIAL,features,"<init>","()V");var put=c.next();require(put instanceof FieldInsnNode f && f.getOpcode()==Opcodes.PUTSTATIC && f.owner.equals(n.name) && f.desc.equals("L"+features+";") && n.fields.stream().anyMatch(v->v.name.equals(f.name) && v.access==(Opcodes.ACC_PRIVATE|Opcodes.ACC_STATIC|Opcodes.ACC_FINAL)),"UNKNOWN_STATIC_SIDE_EFFECT");String ff=((FieldInsnNode)put).name;c.op(Opcodes.RETURN);c.end();require(nodes.get(features)!=null && nodes.get(features).interfaces.isEmpty(),"UNKNOWN_STATIC_SIDE_EFFECT");features(nodes.get(features));
        require(n.fields.size()==(counter==null?2:3) && n.methods.size()==4,"UNKNOWN_STATIC_SIDE_EFFECT");
        for(var m:n.methods)if(!m.name.equals(setter) && !m.name.equals("<clinit>")){
            c=new Cursor(m);if(m.name.equals("<init>")){require(m.access==Opcodes.ACC_PRIVATE,"UNKNOWN_STATIC_SIDE_EFFECT");c.base();c.op(Opcodes.RETURN);}else{require(m.access==(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC) && m.desc.equals("()L"+api+";"),"UNKNOWN_STATIC_SIDE_EFFECT");c.field(Opcodes.GETSTATIC,n.name,value,"L"+api+";");c.op(Opcodes.ARETURN);}c.end();
        }
        return new Global(n.name,setter,value,counter,features,ff);
    }
    static void features(ClassNode n){
        require(n!=null && n.version==Opcodes.V21 && n.superName.equals("java/lang/Object") && n.fields.size()==1 && n.methods.size()==3 && n.visibleAnnotations==null && n.invisibleAnnotations==null && n.outerClass==null && n.nestHostClass==null && (n.nestMembers==null || n.nestMembers.isEmpty()) && (n.access==(Opcodes.ACC_PUBLIC|Opcodes.ACC_SUPER) || n.access==(Opcodes.ACC_PUBLIC|Opcodes.ACC_SUPER|Opcodes.ACC_FINAL)),"UNKNOWN_STATIC_SIDE_EFFECT");RegistryBytecodeVerifier.verify(n);FieldNode f=n.fields.getFirst();require(f.desc.equals("Z") && f.access==Opcodes.ACC_PRIVATE,"UNKNOWN_STATIC_SIDE_EFFECT");
        Cursor c=new Cursor(method(n,"<init>","()V"));require(c.method.access==Opcodes.ACC_PUBLIC,"UNKNOWN_STATIC_SIDE_EFFECT");c.base();c.var(Opcodes.ALOAD,0);c.op(Opcodes.ICONST_1);c.field(Opcodes.PUTFIELD,n.name,f.name,"Z");c.op(Opcodes.RETURN);c.end();
        for(var m:n.methods)if(!m.name.equals("<init>")){require(m.access==Opcodes.ACC_PUBLIC,"UNKNOWN_STATIC_SIDE_EFFECT");c=new Cursor(m);c.var(Opcodes.ALOAD,0);if(m.desc.equals("()Z")){c.field(Opcodes.GETFIELD,n.name,f.name,"Z");c.op(Opcodes.IRETURN);}else{require(m.desc.equals("()V"),"UNKNOWN_STATIC_SIDE_EFFECT");c.op(Opcodes.ICONST_0);c.field(Opcodes.PUTFIELD,n.name,f.name,"Z");c.op(Opcodes.RETURN);}c.end();}
    }
    private static void business(ClassNode n,String config,String server,String client){plain(n,List.of());require(n.fields.size()==4 && n.methods.size()==1,"UNVERIFIED_HANDLER_CAPTURE");Cursor c=new Cursor(method(n,"<init>","(L"+PLAYER+";L"+config+";L"+server+";L"+client+";)V"));require(c.method.access==Opcodes.ACC_PUBLIC,"UNVERIFIED_HANDLER_CAPTURE");c.base();int slot=1;Set<String> seen=new HashSet<>();for(String type:List.of(PLAYER,config,server,client)){c.var(Opcodes.ALOAD,0);c.var(Opcodes.ALOAD,slot++);require(seen.add(c.ownField(Opcodes.PUTFIELD,n,"L"+type+";",true)),"UNVERIFIED_HANDLER_CAPTURE");}c.op(Opcodes.RETURN);c.end();}
    private static String handler(ClassNode n,Handle h,String packet,String config,String cfg,String server,String sf,String client,String cf,Set<String> fields,ForgeDerivedPayloadPlan.Proof derived){
        Cursor c=new Cursor(method(n,h.getName(),h.getDesc()));require(c.method.access==Opcodes.ACC_PRIVATE,"UNVERIFIED_HANDLER_CAPTURE");var alloc=c.next();require(alloc instanceof TypeInsnNode t && t.getOpcode()==Opcodes.NEW,"UNVERIFIED_HANDLER_CAPTURE");String context=((TypeInsnNode)alloc).desc;require(derived==null || context.equals(derived.context()),"WRONG_PROCESS_CONTEXT");c.op(Opcodes.DUP);c.var(Opcodes.ALOAD,2);c.call(Opcodes.INVOKEVIRTUAL,CONTEXT,"getSender","()L"+PLAYER+";");
        for(String[] part:List.of(new String[]{cfg,config},new String[]{sf,server},new String[]{cf,client})){c.var(Opcodes.ALOAD,0);c.field(Opcodes.GETFIELD,n.name,part[0],"L"+part[1]+";");}
        c.call(Opcodes.INVOKESPECIAL,context,"<init>","(L"+PLAYER+";L"+config+";L"+server+";L"+client+";)V");c.var(Opcodes.ASTORE,3);c.var(Opcodes.ALOAD,2);c.op(Opcodes.ICONST_1);c.call(Opcodes.INVOKEVIRTUAL,CONTEXT,"setPacketHandled","(Z)V");
        c.var(Opcodes.ALOAD,2);c.var(Opcodes.ALOAD,0);c.var(Opcodes.ALOAD,1);c.var(Opcodes.ALOAD,3);var op=c.next();require(op instanceof InvokeDynamicInsnNode,"CONTEXT_ENQUEUE_UNVERIFIED");var d=(InvokeDynamicInsnNode)op;
        require(d.bsm.equals(ForgeRegistryTransformationPlan.META) && d.name.equals("run") && d.desc.equals("(L"+n.name+";L"+packet+";L"+context+";)Ljava/lang/Runnable;") && d.bsmArgs.length==3 && d.bsmArgs[0].equals(Type.getMethodType("()V")) && d.bsmArgs[2].equals(Type.getMethodType("()V")) && d.bsmArgs[1] instanceof Handle,"UNSUPPORTED_BOOTSTRAP");Handle body=(Handle)d.bsmArgs[1];require(body.getOwner().equals(n.name) && body.getTag()==Opcodes.H_INVOKEVIRTUAL && !body.isInterface() && body.getDesc().equals("(L"+packet+";L"+context+";)V"),"UNVERIFIED_HANDLER_CAPTURE");
        c.call(Opcodes.INVOKEVIRTUAL,CONTEXT,"enqueueWork","(Ljava/lang/Runnable;)Ljava/util/concurrent/CompletableFuture;");c.op(Opcodes.POP);c.op(Opcodes.RETURN);c.end();
        c=new Cursor(method(n,body.getName(),body.getDesc()));require(c.method.access==(Opcodes.ACC_PRIVATE|Opcodes.ACC_SYNTHETIC),"UNVERIFIED_HANDLER_CAPTURE");if(derived!=null){c.var(Opcodes.ALOAD,1);c.var(Opcodes.ALOAD,2);c.call(Opcodes.INVOKEVIRTUAL,packet,"process","(L"+context+";)V");}c.var(Opcodes.ALOAD,0);c.op(Opcodes.DUP);String counter=c.ownField(Opcodes.GETFIELD,n,"I",false);require(fields.add(counter),"UNVERIFIED_HANDLER_CAPTURE");c.op(Opcodes.ICONST_1);c.op(Opcodes.IADD);c.field(Opcodes.PUTFIELD,n.name,counter,"I");
        for(String[] value:List.of(new String[]{"1","Ljava/lang/Object;"},new String[]{"2","L"+context+";"})){c.var(Opcodes.ALOAD,0);c.var(Opcodes.ALOAD,Integer.parseInt(value[0]));require(fields.add(c.ownField(Opcodes.PUTFIELD,n,value[1],false)),"UNVERIFIED_HANDLER_CAPTURE");}
        c.var(Opcodes.ALOAD,0);c.call(Opcodes.INVOKESTATIC,"java/lang/Thread","currentThread","()Ljava/lang/Thread;");c.call(Opcodes.INVOKEVIRTUAL,"java/lang/Thread","getName","()Ljava/lang/String;");require(fields.add(c.ownField(Opcodes.PUTFIELD,n,"Ljava/lang/String;",false)),"UNVERIFIED_HANDLER_CAPTURE");if(c.peek() instanceof MethodInsnNode call && Type.getReturnType(call.desc).equals(Type.BOOLEAN_TYPE))require(false,"UNVERIFIED_PERMISSION_RESULT "+call.owner+"."+call.name+call.desc);
        c.op(Opcodes.RETURN);c.end();return context;
    }
    void rewrite(ClassNode n,ForgeNetworkPlan plan){
        if(n.name.equals(global.owner)){
            MethodNode setter=method(n,global.setter,"(L"+server.api+";)V");InsnList guard=new InsnList();guard.add(new VarInsnNode(Opcodes.ALOAD,0));guard.add(new MethodInsnNode(Opcodes.INVOKESTATIC,ForgeNetworkPlan.BRIDGE,"globalWrite","(Ljava/lang/Object;)V",false));setter.instructions.insert(guard);setter.maxStack+=2;
        }
        if(!n.name.equals(plan.owner))return;
        n.fields.add(new FieldNode(Opcodes.ACC_PRIVATE|Opcodes.ACC_SYNTHETIC,ANCHOR,"Ljava/lang/Object;",null,null));
        MethodNode ctor=method(n,"<init>","(L"+config+";L"+RL+";I)V");
        InsnList begin=new InsnList();begin.add(new VarInsnNode(Opcodes.ALOAD,0));begin.add(new VarInsnNode(Opcodes.ALOAD,0));begin.add(new MethodInsnNode(Opcodes.INVOKESTATIC,ForgeNetworkPlan.BRIDGE,"begin","(Ljava/lang/Object;)Ljava/lang/Object;",false));begin.add(new FieldInsnNode(Opcodes.PUTFIELD,n.name,ANCHOR,"Ljava/lang/Object;"));LabelNode start=new LabelNode();begin.add(start);ctor.instructions.insert(ForgeRegistryTransformationPlan.ops(ctor).get(1),begin);
        for(var op:ctor.instructions.toArray())if(op instanceof MethodInsnNode call && call.owner.equals(global.owner) && call.name.equals(global.setter)){InsnList after=new InsnList();after.add(new VarInsnNode(Opcodes.ALOAD,0));after.add(new MethodInsnNode(Opcodes.INVOKESTATIC,ForgeNetworkPlan.BRIDGE,"globalAssigned","(Ljava/lang/Object;)V",false));ctor.instructions.insert(op,after);}
        LabelNode end=new LabelNode(),handler=new LabelNode();ctor.instructions.add(end);ctor.instructions.add(handler);ctor.instructions.add(new FrameNode(Opcodes.F_FULL,4,new Object[]{n.name,config,RL,Opcodes.INTEGER},1,new Object[]{"java/lang/Throwable"}));ctor.instructions.add(new VarInsnNode(Opcodes.ASTORE,4));ctor.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));ctor.instructions.add(new VarInsnNode(Opcodes.ALOAD,4));ctor.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,ForgeNetworkPlan.BRIDGE,"constructorFailure","(Ljava/lang/Object;Ljava/lang/Throwable;)V",false));ctor.instructions.add(new VarInsnNode(Opcodes.ALOAD,4));ctor.instructions.add(new InsnNode(Opcodes.ATHROW));ctor.tryCatchBlocks.add(new TryCatchBlockNode(start,end,handler,"java/lang/Throwable"));ctor.maxLocals=Math.max(ctor.maxLocals,5);ctor.maxStack+=6;
        for(var m:n.methods)for(var op:m.instructions.toArray())if(op instanceof MethodInsnNode call && call.owner.equals(CONTEXT)){
            String name=call.name,desc;String bridge="io/github/gfander/dualforge/ForgeSourceContextBridge";
            if(name.equals("getSender")){desc="(Ljava/lang/Object;)Ljava/lang/Object;";m.instructions.insert(op,new TypeInsnNode(Opcodes.CHECKCAST,PLAYER));}
            else if(name.equals("setPacketHandled"))desc="(Ljava/lang/Object;Z)V";
            else {require(name.equals("enqueueWork"),"UNKNOWN_CONTEXT_METHOD");desc="(Ljava/lang/Object;Ljava/lang/Runnable;)Ljava/util/concurrent/CompletableFuture;";}
            m.instructions.set(op,new MethodInsnNode(Opcodes.INVOKESTATIC,bridge,name,desc,false));
        }
    }

}
