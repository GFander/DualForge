package io.github.gfander.dualforge;

import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static io.github.gfander.dualforge.ForgeDualBusPlan.*;
import static io.github.gfander.dualforge.ForgeNetworkPlan.*;

/** Read-only source-prefix proof. It never creates a partial transform plan or executes the setter. */
final class ForgeNetworkBoundaryProof {
    private ForgeNetworkBoundaryProof() { }
    static void reject(ClassNode n,Map<String,ClassNode> nodes,String id){
        List<MethodNode> constructors=n.methods.stream().filter(m->m.name.equals("<init>") && Type.getArgumentTypes(m.desc).length==3).toList();
        require(constructors.size()==1,"UNVERIFIED_NETWORK_INITIALIZATION UNKNOWN_STATIC_SIDE_EFFECT constructor closure");
        MethodNode ctor=constructors.getFirst();Type[] args=Type.getArgumentTypes(ctor.desc);
        require(args[0].getSort()==Type.OBJECT && args[1].equals(Type.getObjectType(RL)) && args[2].equals(Type.INT_TYPE),"UNVERIFIED_NETWORK_INITIALIZATION WRONG_NETWORK_HANDLER_IDENTITY");
        RegistryBytecodeVerifier.verify(n);logger(n);
        Cursor c=new Cursor(ctor);c.base();c.var(Opcodes.ALOAD,0);c.var(Opcodes.ALOAD,1);c.ownField(Opcodes.PUTFIELD,n,args[0].getDescriptor(),true);
        String server=connection(c,n,nodes);String serverApi=nextFieldDescriptor(c);String sf=c.ownField(Opcodes.PUTFIELD,n,serverApi,true);
        c.var(Opcodes.ALOAD,0);c.field(Opcodes.GETFIELD,n.name,sf,serverApi);MethodInsnNode write=c.ownedCall(nodes);require(write.getOpcode()==Opcodes.INVOKESTATIC && write.desc.equals("("+serverApi+")V"),"UNVERIFIED_NETWORK_INITIALIZATION GLOBAL_CONNECTION_WRONG_ORDER");
        global(nodes.get(write.owner),write.name,serverApi,nodes);
        String client=connection(c,n,nodes);String clientApi=nextFieldDescriptor(c);c.ownField(Opcodes.PUTFIELD,n,clientApi,true);require(!server.equals(client),"UNVERIFIED_NETWORK_INITIALIZATION WRONG_NETWORK_HANDLER_IDENTITY");
        c.var(Opcodes.ALOAD,0);c.var(Opcodes.ALOAD,2);c.call(Opcodes.INVOKESTATIC,BUILD,"named","(L"+RL+";)L"+BUILD+";");c.var(Opcodes.ILOAD,3);c.call(Opcodes.INVOKEVIRTUAL,BUILD,"networkProtocolVersion","(I)L"+BUILD+";");c.call(Opcodes.INVOKEVIRTUAL,BUILD,"optional","()L"+BUILD+";");c.call(Opcodes.INVOKEVIRTUAL,BUILD,"payloadChannel","()L"+CONNECTION+";");c.call(Opcodes.INVOKEINTERFACE,CONNECTION,"play","()L"+PROTOCOL+";");c.call(Opcodes.INVOKEINTERFACE,PROTOCOL,"serverbound","()Ljava/lang/Object;");c.type(Opcodes.CHECKCAST,FLOW);
        var first=c.next();require(first instanceof FieldInsnNode f && f.getOpcode()==Opcodes.GETSTATIC && f.desc.equals("L"+PT+";"),"UNVERIFIED_NETWORK_INITIALIZATION UNKNOWN_PAYLOAD_TYPE");String packet=((FieldInsnNode)first).owner;ClassNode payload=nodes.get(packet);require(payload!=null,"UNVERIFIED_NETWORK_INITIALIZATION CROSS_MODFILE_CONFLICT");
        try{var proof=ForgeDerivedPayloadPlan.inspect(payload,id,nodes);ForgeDerivedPayloadPlan.process(proof,nodes);}
        catch(IllegalStateException failure){String message=failure.getMessage();throw new IllegalStateException("Dual bus rejected: UNVERIFIED_NETWORK_INITIALIZATION "+message.replaceFirst("^Dual bus rejected: ","")+" globalConnection=STATIC_VERIFIED originalSetter=RETAINED sourceClinit=STATIC_VERIFIED context=FINITE_ADD_PROFILE_ONLY combined=REJECTED_NO_TARGETS",failure);}
        require(false,"UNVERIFIED_NETWORK_INITIALIZATION PARTIAL_NETWORK_PLAN all payloads and source wrappers must be proved combined=REJECTED_NO_TARGETS");
    }
    private static String nextFieldDescriptor(Cursor c){var op=c.peek();require(op instanceof FieldInsnNode,"UNVERIFIED_NETWORK_INITIALIZATION WRONG_NETWORK_HANDLER_IDENTITY");return ((FieldInsnNode)op).desc;}
    private static String connection(Cursor c,ClassNode network,Map<String,ClassNode> nodes){
        c.var(Opcodes.ALOAD,0);String target=c.allocation(nodes);c.op(Opcodes.DUP);c.var(Opcodes.ALOAD,0);c.call(Opcodes.INVOKESPECIAL,target,"<init>","(L"+network.name+";)V");ClassNode own=nodes.get(target);require(own!=null && own.interfaces.size()==1 && own.fields.stream().filter(f->f.access==(Opcodes.ACC_PRIVATE|Opcodes.ACC_FINAL)).count()==1,"UNVERIFIED_NETWORK_INITIALIZATION WRONG_NETWORK_HANDLER_IDENTITY");
        Cursor init=new Cursor(method(own,"<init>","(L"+network.name+";)V"));init.base();init.var(Opcodes.ALOAD,0);init.var(Opcodes.ALOAD,1);init.ownField(Opcodes.PUTFIELD,own,"L"+network.name+";",true);init.op(Opcodes.RETURN);init.end();
        for(var m:own.methods)if(m.name.equals("<clinit>")){Cursor staticInit=new Cursor(m);while(staticInit.peek().getOpcode()!=Opcodes.RETURN){int literal=staticInit.next().getOpcode();require(literal==Opcodes.ACONST_NULL || literal==Opcodes.ICONST_0,"UNVERIFIED_NETWORK_INITIALIZATION UNKNOWN_STATIC_SIDE_EFFECT connection clinit");var op=staticInit.next();require(op instanceof FieldInsnNode f && f.getOpcode()==Opcodes.PUTSTATIC && f.owner.equals(own.name) && ((literal==Opcodes.ACONST_NULL && f.desc.startsWith("L")) || (literal==Opcodes.ICONST_0 && f.desc.equals("Z"))) && own.fields.stream().anyMatch(v->v.name.equals(f.name) && v.access==(Opcodes.ACC_PRIVATE|Opcodes.ACC_STATIC)),"UNVERIFIED_NETWORK_INITIALIZATION UNKNOWN_STATIC_SIDE_EFFECT connection cache");}staticInit.op(Opcodes.RETURN);staticInit.end();}
        return target;
    }
    private static void global(ClassNode n,String setter,String descriptor,Map<String,ClassNode> nodes){
        require(n!=null && n.nestHostClass==null && (n.nestMembers==null || n.nestMembers.isEmpty()),"UNVERIFIED_NETWORK_INITIALIZATION GLOBAL_CONNECTION_OWNER_MISMATCH");Cursor c=new Cursor(method(n,setter,"("+descriptor+")V"));require(c.method.access==(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC),"UNVERIFIED_NETWORK_INITIALIZATION GLOBAL_CONNECTION_OWNER_MISMATCH");c.var(Opcodes.ALOAD,0);
        var op=c.next();require(op instanceof FieldInsnNode f && f.getOpcode()==Opcodes.PUTSTATIC && f.owner.equals(n.name) && f.desc.equals(descriptor) && n.fields.stream().anyMatch(v->v.name.equals(f.name) && v.access==(Opcodes.ACC_PRIVATE|Opcodes.ACC_STATIC) && v.value==null),"UNVERIFIED_NETWORK_INITIALIZATION GLOBAL_CONNECTION_OWNER_MISMATCH");FieldInsnNode field=(FieldInsnNode)op;c.op(Opcodes.RETURN);c.end();
        long writes=n.methods.stream().flatMap(m->ForgeRegistryTransformationPlan.ops(m).stream()).filter(i->i instanceof FieldInsnNode f && f.getOpcode()==Opcodes.PUTSTATIC && f.owner.equals(n.name) && f.name.equals(field.name)).count();require(writes==1,"UNVERIFIED_NETWORK_INITIALIZATION DUPLICATE_GLOBAL_CONNECTION_WRITE");
        c=new Cursor(method(n,"<clinit>","()V"));String feature=c.allocation(nodes);c.op(Opcodes.DUP);c.call(Opcodes.INVOKESPECIAL,feature,"<init>","()V");var put=c.next();require(put instanceof FieldInsnNode f && f.getOpcode()==Opcodes.PUTSTATIC && f.owner.equals(n.name) && f.desc.equals("L"+feature+";") && n.fields.stream().anyMatch(v->v.name.equals(f.name) && v.access==(Opcodes.ACC_PRIVATE|Opcodes.ACC_STATIC|Opcodes.ACC_FINAL)),"UNVERIFIED_NETWORK_INITIALIZATION UNKNOWN_STATIC_SIDE_EFFECT");c.op(Opcodes.RETURN);c.end();ClassNode value=nodes.get(feature);ForgeNetworkClosurePlan.features(value);
        for(String name:value.interfaces){ClassNode api=nodes.get(name);require(api!=null && api.access==(Opcodes.ACC_PUBLIC|Opcodes.ACC_INTERFACE|Opcodes.ACC_ABSTRACT) && api.version==Opcodes.V21 && api.interfaces.isEmpty() && api.fields.isEmpty() && !api.methods.isEmpty() && api.methods.size()<=2 && api.methods.stream().allMatch(m->m.access==(Opcodes.ACC_PUBLIC|Opcodes.ACC_ABSTRACT) && m.instructions.size()==0 && value.methods.stream().anyMatch(own->own.name.equals(m.name) && own.desc.equals(m.desc))),"UNVERIFIED_NETWORK_INITIALIZATION UNKNOWN_STATIC_SIDE_EFFECT feature interface");}
    }
    private static void logger(ClassNode n){Cursor c=new Cursor(method(n,"<clinit>","()V"));c.call(Opcodes.INVOKESTATIC,"org/apache/logging/log4j/LogManager","getLogger","()Lorg/apache/logging/log4j/Logger;");var op=c.next();require(op instanceof FieldInsnNode f && f.getOpcode()==Opcodes.PUTSTATIC && f.owner.equals(n.name) && f.desc.equals("Lorg/apache/logging/log4j/Logger;") && n.fields.stream().anyMatch(v->v.name.equals(f.name) && v.access==(Opcodes.ACC_PRIVATE|Opcodes.ACC_STATIC|Opcodes.ACC_FINAL)),"UNVERIFIED_NETWORK_INITIALIZATION UNKNOWN_STATIC_SIDE_EFFECT");c.op(Opcodes.RETURN);c.end();}
}
