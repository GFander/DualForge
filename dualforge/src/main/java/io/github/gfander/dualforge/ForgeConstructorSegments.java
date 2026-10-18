package io.github.gfander.dualforge;

import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static io.github.gfander.dualforge.ForgeDualBusPlan.*;

/** Finite, stack-balanced original initializer segments. No source code is executed while planning. */
final class ForgeConstructorSegments {
    record LocaleSegment(String supplier, String translator) {
        Set<String> classes(){return Set.of(supplier,translator);}
    }
    private static final String SUP="Ljava/util/function/Supplier;", LOC="Ljava/util/Locale;";
    private ForgeConstructorSegments() { }

    static LocaleSegment locale(ForgeDualBusPlan.Cursor c,Map<String,ClassNode> nodes) {
        if(!(c.peek() instanceof TypeInsnNode t) || t.getOpcode()!=Opcodes.NEW)return null;
        String supplier=c.allocation(nodes);ClassNode s=nodes.get(supplier);
        require(s!=null,"CROSS_MODFILE_INITIALIZER");
        c.op(Opcodes.DUP);c.call(Opcodes.INVOKESPECIAL,supplier,"<init>","()V");
        var op=c.next();require(op instanceof MethodInsnNode,"WRONG_LOCALE_INITIALIZATION_ORDER");
        MethodInsnNode setter=(MethodInsnNode)op;
        require(setter.getOpcode()==Opcodes.INVOKESTATIC && !setter.itf && setter.desc.equals("("+SUP+")V")
            && !setter.owner.equals(supplier) && nodes.containsKey(setter.owner),"WRONG_TRANSLATOR_OWNER");
        verifySupplier(s);verifyTranslator(nodes.get(setter.owner),setter.name);
        require(!(c.peek() instanceof TypeInsnNode),"DUPLICATE_TRANSLATOR_INITIALIZATION");
        return new LocaleSegment(supplier,setter.owner);
    }
    private static ForgeDualBusPlan.Cursor cursor(ClassNode n,String name,String desc){return new ForgeDualBusPlan.Cursor(method(n,name,desc));}
    private static void end(ForgeDualBusPlan.Cursor c,int opcode){c.op(opcode);c.end();}
    private static void plain(ClassNode n,List<String> interfaces) {
        RegistryBytecodeVerifier.verify(n);
        require(n.version==Opcodes.V21 && n.superName.equals("java/lang/Object") && n.interfaces.equals(interfaces)
            && (n.access==(Opcodes.ACC_PUBLIC|Opcodes.ACC_SUPER) || n.access==(Opcodes.ACC_PUBLIC|Opcodes.ACC_SUPER|Opcodes.ACC_FINAL))
            && n.outerClass==null && n.nestHostClass==null && (n.nestMembers==null || n.nestMembers.isEmpty())
            && n.name.indexOf('/')>0 && !n.name.startsWith("net/minecraft") && !n.name.startsWith("java/")
            && !n.name.startsWith("net/neoforged/") && !n.name.startsWith("javax/") && !n.name.startsWith("io/github/gfander/dualforge/"),"UNVERIFIED_LOCALE_SUPPLIER");
        Set<String> seen=new HashSet<>();for(var m:n.methods)require(seen.add(m.name+m.desc) && m.tryCatchBlocks.isEmpty()
            && (m.access&(Opcodes.ACC_NATIVE|Opcodes.ACC_ABSTRACT|Opcodes.ACC_SYNCHRONIZED))==0,"UNKNOWN_INITIALIZER_SIDE_EFFECT");
        require(n.visibleAnnotations==null && n.invisibleAnnotations==null,"UNKNOWN_INITIALIZER_SIDE_EFFECT");
    }
    static void verifySupplier(ClassNode n) {
        plain(n,List.of("java/util/function/Supplier"));
        require(n.methods.size()==3,"UNVERIFIED_LOCALE_SUPPLIER");
        var c=cursor(n,"<init>","()V");require(c.method.access==Opcodes.ACC_PUBLIC,"UNVERIFIED_LOCALE_SUPPLIER");c.base();counter(c,n);end(c,Opcodes.RETURN);
        c=cursor(n,"get","()Ljava/lang/Object;");require(c.method.access==(Opcodes.ACC_PUBLIC|Opcodes.ACC_BRIDGE|Opcodes.ACC_SYNTHETIC),"UNVERIFIED_LOCALE_SUPPLIER");
        c.var(Opcodes.ALOAD,0);c.call(Opcodes.INVOKEVIRTUAL,n.name,"get","()"+LOC);end(c,Opcodes.ARETURN);
        c=cursor(n,"get","()"+LOC);require(c.method.access==Opcodes.ACC_PUBLIC,"UNVERIFIED_LOCALE_SUPPLIER");
        if(n.fields.stream().allMatch(f->f.desc.equals("I"))) {c.field(Opcodes.GETSTATIC,"java/util/Locale","ROOT",LOC);end(c,Opcodes.ARETURN);return;}
        require(n.fields.size()==2 && n.fields.stream().allMatch(f->f.access==Opcodes.ACC_PRIVATE),"UNVERIFIED_LOCALE_SUPPLIER");
        String cached=uniqueField(n,LOC),code=uniqueField(n,"Ljava/lang/String;");
        String mc="net/minecraft/client/Minecraft",lm="net/minecraft/client/resources/language/LanguageManager";
        c.call(Opcodes.INVOKESTATIC,mc,"getInstance","()L"+mc+";");c.var(Opcodes.ASTORE,1);c.var(Opcodes.ALOAD,1);LabelNode present=c.jump(Opcodes.IFNONNULL);
        c.call(Opcodes.INVOKESTATIC,"java/util/Locale","getDefault","()"+LOC);c.op(Opcodes.ARETURN);target(c,present);
        c.var(Opcodes.ALOAD,1);c.call(Opcodes.INVOKEVIRTUAL,mc,"getLanguageManager","()L"+lm+";");c.var(Opcodes.ASTORE,2);
        c.var(Opcodes.ALOAD,2);c.call(Opcodes.INVOKEVIRTUAL,lm,"getSelected","()Ljava/lang/String;");c.var(Opcodes.ASTORE,3);
        c.var(Opcodes.ALOAD,0);c.field(Opcodes.GETFIELD,n.name,cached,LOC);LabelNode refresh=c.jump(Opcodes.IFNULL);
        c.var(Opcodes.ALOAD,3);c.var(Opcodes.ALOAD,0);c.field(Opcodes.GETFIELD,n.name,code,"Ljava/lang/String;");
        c.call(Opcodes.INVOKEVIRTUAL,"java/lang/String","equals","(Ljava/lang/Object;)Z");LabelNode done=c.jump(Opcodes.IFNE);target(c,refresh);
        c.var(Opcodes.ALOAD,0);c.var(Opcodes.ALOAD,3);c.field(Opcodes.PUTFIELD,n.name,code,"Ljava/lang/String;");c.var(Opcodes.ALOAD,3);c.literal("_");c.op(Opcodes.ICONST_2);
        c.call(Opcodes.INVOKEVIRTUAL,"java/lang/String","split","(Ljava/lang/String;I)[Ljava/lang/String;");c.var(Opcodes.ASTORE,4);c.var(Opcodes.ALOAD,4);c.op(Opcodes.ARRAYLENGTH);c.op(Opcodes.ICONST_1);LabelNode country=c.jump(Opcodes.IF_ICMPNE);
        c.var(Opcodes.ALOAD,0);c.var(Opcodes.ALOAD,3);c.call(Opcodes.INVOKESTATIC,"java/util/Locale","of","(Ljava/lang/String;)"+LOC);c.field(Opcodes.PUTFIELD,n.name,cached,LOC);LabelNode done2=c.jump(Opcodes.GOTO);target(c,country);
        c.var(Opcodes.ALOAD,0);c.var(Opcodes.ALOAD,4);c.op(Opcodes.ICONST_0);c.op(Opcodes.AALOAD);c.var(Opcodes.ALOAD,4);c.op(Opcodes.ICONST_1);c.op(Opcodes.AALOAD);
        c.call(Opcodes.INVOKESTATIC,"java/util/Locale","of","(Ljava/lang/String;Ljava/lang/String;)"+LOC);c.field(Opcodes.PUTFIELD,n.name,cached,LOC);target(c,done);target(c,done2);
        c.var(Opcodes.ALOAD,0);c.field(Opcodes.GETFIELD,n.name,cached,LOC);end(c,Opcodes.ARETURN);
    }
    // Optional exact once-per-call diagnostic counter; no method calls, initializers or foreign state.
    private static void counter(ForgeDualBusPlan.Cursor c,ClassNode n) {
        var counters=n.fields.stream().filter(f->f.desc.equals("I")).toList();require(counters.size()<=1,"UNKNOWN_INITIALIZER_SIDE_EFFECT");
        if(counters.isEmpty())return;
        var f=counters.getFirst();require(f.access==(Opcodes.ACC_PRIVATE|Opcodes.ACC_STATIC) && f.value==null,"UNKNOWN_INITIALIZER_SIDE_EFFECT");
        c.field(Opcodes.GETSTATIC,n.name,f.name,"I");c.op(Opcodes.ICONST_1);c.op(Opcodes.IADD);c.field(Opcodes.PUTSTATIC,n.name,f.name,"I");
    }
    private static String uniqueField(ClassNode n,String desc){var fields=n.fields.stream().filter(f->f.desc.equals(desc)).toList();require(fields.size()==1,"UNVERIFIED_LOCALE_SUPPLIER");return fields.getFirst().name;}
    private static void target(ForgeDualBusPlan.Cursor c,LabelNode label){AbstractInsnNode op=label;do{op=op.getNext();}while(op!=null && op.getOpcode()<0);require(op==c.peek(),"WRONG_CONSTRUCTOR_DATAFLOW");}
    static void verifyTranslator(ClassNode n,String setter) {
        plain(n,List.of());require(n.fields.size()>=1 && n.fields.size()<=2,"UNKNOWN_INITIALIZER_SIDE_EFFECT");var values=n.fields.stream().filter(f->f.desc.equals(SUP)).toList();require(values.size()==1,"UNKNOWN_INITIALIZER_SIDE_EFFECT");var field=values.getFirst();
        require(field.access==(Opcodes.ACC_PRIVATE|Opcodes.ACC_STATIC) && field.desc.equals(SUP) && field.value==null,"UNKNOWN_INITIALIZER_SIDE_EFFECT");
        var c=cursor(n,setter,"("+SUP+")V");require(c.method.access==(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC),"WRONG_TRANSLATOR_OWNER");
        counter(c,n);c.var(Opcodes.ALOAD,0);c.field(Opcodes.PUTSTATIC,n.name,field.name,SUP);end(c,Opcodes.RETURN);
        c=cursor(n,"<clinit>","()V");require(c.method.access==Opcodes.ACC_STATIC,"UNKNOWN_INITIALIZER_SIDE_EFFECT");
        var op=c.next();require(op instanceof InvokeDynamicInsnNode,"UNSUPPORTED_BOOTSTRAP");var d=(InvokeDynamicInsnNode)op;
        require(d.bsm.equals(ForgeRegistryTransformationPlan.META) && d.name.equals("get") && d.desc.equals("()"+SUP) && d.bsmArgs.length==3
            && d.bsmArgs[0].equals(Type.getMethodType("()Ljava/lang/Object;")) && d.bsmArgs[1] instanceof Handle
            && d.bsmArgs[2].equals(Type.getMethodType("()"+LOC)),"UNSUPPORTED_BOOTSTRAP");
        Handle h=(Handle)d.bsmArgs[1];require(h.getTag()==Opcodes.H_INVOKESTATIC && !h.isInterface() && h.getOwner().equals(n.name) && h.getDesc().equals("()"+LOC),"WRONG_TRANSLATOR_OWNER");
        c.field(Opcodes.PUTSTATIC,n.name,field.name,SUP);end(c,Opcodes.RETURN);
        c=cursor(n,h.getName(),h.getDesc());require(c.method.access==(Opcodes.ACC_PRIVATE|Opcodes.ACC_STATIC|Opcodes.ACC_SYNTHETIC),"UNKNOWN_INITIALIZER_SIDE_EFFECT");
        c.field(Opcodes.GETSTATIC,"java/util/Locale","ROOT",LOC);end(c,Opcodes.ARETURN);
        require(n.methods.size()==7,"UNKNOWN_INITIALIZER_SIDE_EFFECT");
        for(var m:n.methods) {
            if(m.name.equals(setter) || m.name.equals("<clinit>") || m.name.equals(h.getName()))continue;
            c=new ForgeDualBusPlan.Cursor(m);
            if(m.name.equals("<init>")){require(m.access==Opcodes.ACC_PRIVATE && m.desc.equals("()V"),"UNKNOWN_INITIALIZER_SIDE_EFFECT");c.base();end(c,Opcodes.RETURN);continue;}
            require((m.access & ~Opcodes.ACC_VARARGS)==(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC),"UNKNOWN_INITIALIZER_SIDE_EFFECT");
            if(m.desc.equals("(Ljava/lang/String;)Ljava/lang/String;") && c.peek() instanceof FieldInsnNode) {
                c.field(Opcodes.GETSTATIC,n.name,field.name,SUP);c.call(Opcodes.INVOKEINTERFACE,"java/util/function/Supplier","get","()Ljava/lang/Object;");c.type(Opcodes.CHECKCAST,"java/util/Locale");c.var(Opcodes.ASTORE,1);c.var(Opcodes.ALOAD,0);c.var(Opcodes.ALOAD,1);
                c.call(Opcodes.INVOKEVIRTUAL,"java/lang/String","toLowerCase","(Ljava/util/Locale;)Ljava/lang/String;");end(c,Opcodes.ARETURN);
            } else {
                c.var(Opcodes.ALOAD,0);
                if(m.desc.equals("(Ljava/lang/String;)Ljava/lang/String;")){c.op(Opcodes.ICONST_0);c.type(Opcodes.ANEWARRAY,"java/lang/Object");}
                else {require(m.desc.equals("(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"),"UNKNOWN_INITIALIZER_SIDE_EFFECT");c.var(Opcodes.ALOAD,1);}
                c.call(Opcodes.INVOKESTATIC,"net/minecraft/client/resources/language/I18n","get","(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;");end(c,Opcodes.ARETURN);
            }
        }
    }
}
