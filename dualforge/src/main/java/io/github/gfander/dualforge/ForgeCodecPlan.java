package io.github.gfander.dualforge;

import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static io.github.gfander.dualforge.ForgeDualBusPlan.*;
import static io.github.gfander.dualforge.ForgeNetworkPlan.*;

/** Finite tuple(VAR_INT,BOOL), STRING_UTF8 list/map and singleton unit data/codec proofs. */
final class ForgeCodecPlan {
    static final String BYTE_CODECS="net/minecraft/network/codec/ByteBufCodecs", FUNCTION="Ljava/util/function/Function;";
    record Proof(String type,String codec,String id,String kind) { }
    private ForgeCodecPlan() { }
    static Proof verify(ClassNode n,String id) {
        ForgeNetworkPlan.plain(n,List.of(PAYLOAD));
        Cursor c=new Cursor(method(n,"<clinit>","()V"));require(c.method.access==Opcodes.ACC_STATIC,"WRONG_CODEC_DESCRIPTOR");
        c.type(Opcodes.NEW,PT);c.op(Opcodes.DUP);String namespace=c.string(),path=c.string();require(namespace.equals(id) && ForgeNetworkPlan.valid(path),"WRONG_CODEC_OWNER");
        c.call(Opcodes.INVOKESTATIC,RL,"fromNamespaceAndPath","(Ljava/lang/String;Ljava/lang/String;)L"+RL+";");c.call(Opcodes.INVOKESPECIAL,PT,"<init>","(L"+RL+";)V");
        String type=ForgeNetworkPlan.staticField(c,n,"L"+PT+";"),kind;
        if(c.peek() instanceof FieldInsnNode f)require(f.getOpcode()==Opcodes.GETSTATIC && f.owner.equals(BYTE_CODECS),"WRONG_CODEC_OWNER");
        if(c.peek() instanceof FieldInsnNode f && f.owner.equals(BYTE_CODECS) && f.name.equals("VAR_INT")) {
            kind="INT_BOOL_TUPLE";require(n.fields.size()==4 && n.methods.size()==5,"UNSUPPORTED_COMPOSITE_CODEC");
            c.field(Opcodes.GETSTATIC,BYTE_CODECS,"VAR_INT","L"+CODEC+";");Handle number=lambda(c,n,"java/util/function/Function","(Ljava/lang/Object;)Ljava/lang/Object;","(L"+n.name+";)Ljava/lang/Integer;",Opcodes.H_INVOKEVIRTUAL,"()I");
            c.field(Opcodes.GETSTATIC,BYTE_CODECS,"BOOL","L"+CODEC+";");Handle flag=lambda(c,n,"java/util/function/Function","(Ljava/lang/Object;)Ljava/lang/Object;","(L"+n.name+";)Ljava/lang/Boolean;",Opcodes.H_INVOKEVIRTUAL,"()Z");
            Handle ctor=lambda(c,n,"java/util/function/BiFunction","(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;","(Ljava/lang/Integer;Ljava/lang/Boolean;)L"+n.name+";",Opcodes.H_NEWINVOKESPECIAL,"(IZ)V");require(ctor.getName().equals("<init>"),"WRONG_CODEC_DESCRIPTOR");
            itf(c,"composite","(L"+CODEC+";"+FUNCTION+"L"+CODEC+";"+FUNCTION+"Ljava/util/function/BiFunction;)L"+CODEC+";");
            Cursor init=new Cursor(method(n,"<init>","(IZ)V"));require(init.method.access==Opcodes.ACC_PUBLIC,"WRONG_CODEC_DESCRIPTOR");init.base();init.var(Opcodes.ALOAD,0);init.var(Opcodes.ILOAD,1);String a=init.ownField(Opcodes.PUTFIELD,n,"I",true);init.var(Opcodes.ALOAD,0);init.var(Opcodes.ILOAD,2);String b=init.ownField(Opcodes.PUTFIELD,n,"Z",true);init.op(Opcodes.RETURN);init.end();getter(n,number,a,"I");getter(n,flag,b,"Z");
        } else if(c.peek() instanceof FieldInsnNode f && f.owner.equals(BYTE_CODECS) && f.name.equals("STRING_UTF8")) {
            kind="UTF8_LIST";require(n.fields.size()==3 && n.methods.size()==4,"UNSUPPORTED_COMPOSITE_CODEC");
            c.field(Opcodes.GETSTATIC,BYTE_CODECS,"STRING_UTF8","L"+CODEC+";");
            var list=c.next();require(list instanceof MethodInsnNode m && m.getOpcode()==Opcodes.INVOKESTATIC && m.itf && m.owner.equals(BYTE_CODECS) && m.name.equals("list") && m.desc.equals("()L"+CODEC+"$CodecOperation;"),"UNSUPPORTED_COMPOSITE_CODEC");
            c.call(Opcodes.INVOKEINTERFACE,CODEC,"apply","(L"+CODEC+"$CodecOperation;)L"+CODEC+";");
            Handle read=lambda(c,n,"java/util/function/Function","(Ljava/lang/Object;)Ljava/lang/Object;","(L"+n.name+";)Ljava/util/List;",Opcodes.H_INVOKEVIRTUAL,"()Ljava/util/List;");
            Handle ctor=lambda(c,n,"java/util/function/Function","(Ljava/lang/Object;)Ljava/lang/Object;","(Ljava/util/List;)L"+n.name+";",Opcodes.H_NEWINVOKESPECIAL,"(Ljava/util/List;)V");require(ctor.getName().equals("<init>"),"WRONG_CODEC_DESCRIPTOR");
            itf(c,"composite","(L"+CODEC+";"+FUNCTION+FUNCTION+")L"+CODEC+";");
            Cursor init=new Cursor(method(n,"<init>","(Ljava/util/List;)V"));require(init.method.access==Opcodes.ACC_PUBLIC,"WRONG_CODEC_DESCRIPTOR");init.base();init.var(Opcodes.ALOAD,0);init.var(Opcodes.ALOAD,1);String value=init.ownField(Opcodes.PUTFIELD,n,"Ljava/util/List;",true);init.op(Opcodes.RETURN);init.end();getter(n,read,value,"Ljava/util/List;");
        } else {
            kind="SINGLETON_UNIT";require(n.fields.size()==3 && n.methods.size()==3,"UNSUPPORTED_UNIT_CODEC");c.type(Opcodes.NEW,n.name);c.op(Opcodes.DUP);c.call(Opcodes.INVOKESPECIAL,n.name,"<init>","()V");String instance=ForgeNetworkPlan.staticField(c,n,"L"+n.name+";");c.field(Opcodes.GETSTATIC,n.name,instance,"L"+n.name+";");itf(c,"unit","(Ljava/lang/Object;)L"+CODEC+";");
            Cursor init=new Cursor(method(n,"<init>","()V"));require(init.method.access==Opcodes.ACC_PRIVATE,"UNSUPPORTED_UNIT_CODEC");init.base();init.op(Opcodes.RETURN);init.end();
        }
        String codec=ForgeNetworkPlan.staticField(c,n,"L"+CODEC+";");c.op(Opcodes.RETURN);c.end();
        c=new Cursor(method(n,"type","()L"+PT+";"));require(c.method.access==Opcodes.ACC_PUBLIC,"UNKNOWN_PAYLOAD_TYPE");c.field(Opcodes.GETSTATIC,n.name,type,"L"+PT+";");c.op(Opcodes.ARETURN);c.end();
        require(n.fields.stream().filter(f->f.name.equals(type)).allMatch(f->("L"+PT+"<L"+n.name+";>;").equals(f.signature)),"WRONG_CODEC_DESCRIPTOR");
        require(n.fields.stream().filter(f->f.name.equals(codec)).allMatch(f->("L"+CODEC+"<L"+BUFFER+";L"+n.name+";>;").equals(f.signature)),"WRONG_CODEC_DESCRIPTOR");
        return new Proof(type,codec,namespace+":"+path,kind);
    }
    private static Handle lambda(Cursor c,ClassNode n,String target,String erased,String instantiated,int tag,String desc) {
        var op=c.next();require(op instanceof InvokeDynamicInsnNode,"UNSUPPORTED_BOOTSTRAP");var d=(InvokeDynamicInsnNode)op;
        require(d.bsm.equals(ForgeRegistryTransformationPlan.META) && d.name.equals("apply") && d.desc.equals("()L"+target+";") && d.bsmArgs.length==3 && d.bsmArgs[0].equals(Type.getMethodType(erased)) && d.bsmArgs[1] instanceof Handle && d.bsmArgs[2].equals(Type.getMethodType(instantiated)),"UNSUPPORTED_BOOTSTRAP");
        Handle h=(Handle)d.bsmArgs[1];require(!h.isInterface() && h.getTag()==tag && h.getOwner().equals(n.name) && h.getDesc().equals(desc),"WRONG_CODEC_OWNER");return h;
    }
    private static void getter(ClassNode n,Handle h,String field,String desc){Cursor c=new Cursor(method(n,h.getName(),h.getDesc()));require(c.method.access==Opcodes.ACC_PUBLIC,"WRONG_CODEC_DESCRIPTOR");c.var(Opcodes.ALOAD,0);c.field(Opcodes.GETFIELD,n.name,field,desc);c.op(desc.startsWith("L")?Opcodes.ARETURN:Opcodes.IRETURN);c.end();}
    private static void itf(Cursor c,String name,String desc){var op=c.next();require(op instanceof MethodInsnNode m && m.getOpcode()==Opcodes.INVOKESTATIC && m.itf && m.owner.equals(CODEC) && m.name.equals(name) && m.desc.equals(desc),"UNSUPPORTED_COMPOSITE_CODEC");}
}
