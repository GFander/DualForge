package io.github.gfander.dualforge;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;
import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

/** Read-only links/types for unchanged supplier business. Not a Java security sandbox. */
final class ControlledItemSupplier {
    private final Map<String,ClassNode> owned, host=new TreeMap<>();
    private final Function<String,byte[]> oracle;
    private final Set<String> visited=new HashSet<>(), active=new HashSet<>(), classes=new HashSet<>();
    private final Map<String,String> abi=new TreeMap<>();
    ControlledItemSupplier(Map<String,ClassNode> owned,Function<String,byte[]> oracle){this.owned=owned;this.oracle=oracle;}
    Map<String,String> hostFingerprints(){return Map.copyOf(abi);}
    Set<String> methods(){return Set.copyOf(visited);}

    static byte[] hostResource(String name) {
        try {
            if(name.startsWith("java/") || name.startsWith("javax/")) {
                try(InputStream in=ClassLoader.getSystemResourceAsStream(name+".class")){return in==null?null:in.readAllBytes();}
            }
            String mod=name.startsWith("net/minecraft/")?"minecraft":name.startsWith("net/neoforged/")?"neoforge":null;
            if(mod==null || LoadingModList.get()==null)return null;
            byte[] result=null;
            for(var file:LoadingModList.get().getModFiles())if(file.getMods().stream().anyMatch(m->m.getModId().equals(mod))) {
                var p=file.getFile().findResource(name+".class");
                if(Files.isRegularFile(p)) {byte[] b=Files.readAllBytes(p);require(result==null || Arrays.equals(result,b),"HOST_ABI_CONFLICT "+name);result=b;}
            }
            return result;
        }catch(IOException failure){throw new IllegalStateException("Registry helper rejected: SUPPLIER_HOST_READ "+name,failure);}
    }
    private ClassNode node(String name) {
        require(name!=null && !name.startsWith("net/minecraftforge/"),"UNMAPPED_FORGE_API "+name);
        if(owned.containsKey(name)){require(!name.startsWith("java/") && !name.startsWith("javax/") && !name.startsWith("net/minecraft/") && !name.startsWith("net/neoforged/"),"BUSINESS_CLASS_OWNER_CONFLICT "+name);return owned.get(name);}
        if(host.containsKey(name))return host.get(name);
        byte[] bytes=oracle.apply(name);require(bytes!=null,"HOST_TYPE_MISSING "+name);
        ClassNode n=new ClassNode();new ClassReader(bytes).accept(n,ClassReader.SKIP_CODE|ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
        require(n.name.equals(name),"HOST_TYPE_IDENTITY "+name);host.put(name,n);abi.put(name,ForgeRegistryTransformationPlan.digest(bytes));return n;
    }
    private void signature(String s,boolean typeSignature) {
        if(s==null)return;
        require(!s.contains("Lnet/minecraftforge/"),"UNMAPPED_FORGE_API signature");
        try {
            class Links extends org.objectweb.asm.signature.SignatureVisitor {
                String current;Links(){super(Opcodes.ASM9);}
                @Override public void visitClassType(String name){current=name;node(name);}
                @Override public void visitInnerClassType(String name){current+="$"+name;node(current);}
                @Override public org.objectweb.asm.signature.SignatureVisitor visitTypeArgument(char wildcard){return new Links();}
            }
            var reader=new org.objectweb.asm.signature.SignatureReader(s);if(typeSignature)reader.acceptType(new Links());else reader.accept(new Links());
        }
        catch(IllegalArgumentException e){throw new IllegalStateException("Registry helper rejected: BUSINESS_SIGNATURE",e);}
    }
    private void annotations(List<? extends AnnotationNode> list) {
        if(list==null)return;
        for(var a:list) {
            if(a.desc.equals("Lorg/jetbrains/annotations/Nullable;")){require(a.values==null || a.values.isEmpty(),"BUSINESS_ANNOTATION_VALUES");continue;}
            type(Type.getType(a.desc));require(a.values==null || a.values.isEmpty(),"BUSINESS_ANNOTATION_VALUES");
        }
    }
    private void type(Type t) {
        if(t.getSort()==Type.METHOD){for(Type a:t.getArgumentTypes())type(a);type(t.getReturnType());}
        else if(t.getSort()==Type.ARRAY)type(t.getElementType());
        else if(t.getSort()==Type.OBJECT)node(t.getInternalName());
    }
    private boolean assign(Type expected,Type actual) {
        if(expected.equals(actual))return true;
        if(actual.equals(BasicInterpreter.NULL_TYPE))return expected.getSort()==Type.OBJECT || expected.getSort()==Type.ARRAY;
        if(expected.getSort()==Type.ARRAY || actual.getSort()==Type.ARRAY) {
            if(actual.getSort()==Type.ARRAY && expected.equals(Type.getObjectType("java/lang/Object")))return true;
            return expected.getSort()==Type.ARRAY && actual.getSort()==Type.ARRAY && expected.getDimensions()==actual.getDimensions()
                    && assign(expected.getElementType(),actual.getElementType());
        }
        if(expected.getSort()!=Type.OBJECT || actual.getSort()!=Type.OBJECT)return false;
        return inherits(actual.getInternalName(),expected.getInternalName(),new HashSet<>());
    }
    private boolean inherits(String actual,String expected,Set<String> seen) {
        if(actual.equals(expected))return true;
        require(seen.add(actual),"CYCLIC_TYPE "+actual);ClassNode n=node(actual);
        if(n.superName!=null && inherits(n.superName,expected,new HashSet<>(seen)))return true;
        for(String i:n.interfaces)if(inherits(i,expected,new HashSet<>(seen)))return true;
        return false;
    }
    private record Member(String owner,int access,String descriptor,MethodNode method){}
    private Member member(String owner,String name,String desc,boolean method,Set<String> seen) {
        require(seen.add(owner),"CYCLIC_MEMBER_HIERARCHY "+owner);ClassNode n=node(owner);
        if(method)for(MethodNode m:n.methods)if(m.name.equals(name)&&m.desc.equals(desc))return new Member(owner,m.access,m.desc,m);
        if(!method)for(FieldNode f:n.fields)if(f.name.equals(name)&&f.desc.equals(desc))return new Member(owner,f.access,f.desc,null);
        if(method && name.equals("<init>"))return null;
        if(n.superName!=null){Member found=member(n.superName,name,desc,method,new HashSet<>(seen));if(found!=null)return found;}
        Member result=null;
        for(String i:n.interfaces){Member found=member(i,name,desc,method,new HashSet<>(seen));if(found!=null){require(result==null || result.owner.equals(found.owner),"AMBIGUOUS_MEMBER "+owner+"."+name);result=found;}}
        return result;
    }
    private static String pack(String name){int p=name.lastIndexOf('/');return p<0?"":name.substring(0,p);}
    private void accessible(String caller,String symbolicOwner,Member m,boolean isStatic) {
        require(m!=null,"MEMBER_MISSING "+symbolicOwner);
        ClassNode target=node(symbolicOwner);
        require((target.access&Opcodes.ACC_PUBLIC)!=0 || owned.containsKey(symbolicOwner)&&pack(caller).equals(pack(symbolicOwner)),"CLASS_INACCESSIBLE "+symbolicOwner);
        require(((m.access&Opcodes.ACC_STATIC)!=0)==isStatic,"MEMBER_STATIC_MISMATCH "+m.owner);
        boolean same=caller.equals(m.owner),samePackage=owned.containsKey(m.owner)&&pack(caller).equals(pack(m.owner));
        // Cross-package protected access needs an additional receiver proof; outside this contract.
        require((m.access&Opcodes.ACC_PUBLIC)!=0 || same || (m.access&Opcodes.ACC_PRIVATE)==0 && samePackage,"MEMBER_INACCESSIBLE "+m.owner);
    }
    void verify(String owner,MethodNode method) {validate(owner,method,true);}
    private void initialize(String owner) {
        if(!owned.containsKey(owner) || !classes.add(owner))return;
        ClassNode n=node(owner);require(n.module==null && n.version==Opcodes.V21 && (n.access&(Opcodes.ACC_INTERFACE|Opcodes.ACC_ABSTRACT))==0,"BUSINESS_CLASS_STRUCTURE "+owner);
        signature(n.signature,false);
        require(n.visibleAnnotations==null || n.visibleAnnotations.isEmpty(),"BUSINESS_CLASS_ANNOTATION "+owner);
        type(Type.getObjectType(n.superName));for(String i:n.interfaces)type(Type.getObjectType(i));
        annotations(n.invisibleAnnotations);annotations(n.visibleTypeAnnotations);annotations(n.invisibleTypeAnnotations);
        for(FieldNode f:n.fields){type(Type.getType(f.desc));signature(f.signature,true);annotations(f.visibleAnnotations);annotations(f.invisibleAnnotations);annotations(f.visibleTypeAnnotations);annotations(f.invisibleTypeAnnotations);}
        // A dependency class is left byte-identical; all of its executable bodies must link.
        for(MethodNode m:n.methods) {
            if(!m.name.startsWith("<") && (m.access&Opcodes.ACC_PRIVATE)==0) {
                Member parent=member(n.superName,m.name,m.desc,true,new HashSet<>());
                if(parent!=null && (parent.access&Opcodes.ACC_PRIVATE)==0)require((parent.access&Opcodes.ACC_FINAL)==0 && ((parent.access&Opcodes.ACC_STATIC)!=0)==((m.access&Opcodes.ACC_STATIC)!=0)
                        && ((parent.access&Opcodes.ACC_PUBLIC)==0 || (m.access&Opcodes.ACC_PUBLIC)!=0),"INCOMPATIBLE_OVERRIDE "+owner+"."+m.name);
            }
            validate(owner,m,false);
        }
    }
    private void validate(String owner,MethodNode m,boolean supplier) {
        String key=owner+"."+m.name+m.desc;
        if(visited.contains(key))return;
        require(active.add(key),"CYCLIC_BUSINESS_METHOD "+key);
        require((m.access&(Opcodes.ACC_ABSTRACT|Opcodes.ACC_NATIVE|Opcodes.ACC_SYNCHRONIZED))==0,"BUSINESS_METHOD_FLAGS "+key);
        require(m.tryCatchBlocks.isEmpty() && (m.exceptions==null || m.exceptions.isEmpty()),"BUSINESS_CONTROL_FLOW "+key);
        annotations(m.visibleAnnotations);annotations(m.invisibleAnnotations);annotations(m.visibleTypeAnnotations);annotations(m.invisibleTypeAnnotations);annotations(m.visibleLocalVariableAnnotations);annotations(m.invisibleLocalVariableAnnotations);
        if(m.visibleParameterAnnotations!=null)for(var a:m.visibleParameterAnnotations)annotations(a);
        if(m.invisibleParameterAnnotations!=null)for(var a:m.invisibleParameterAnnotations)annotations(a);
        signature(m.signature,false);
        type(Type.getMethodType(m.desc));
        if(m.localVariables!=null)for(var v:m.localVariables){type(Type.getType(v.desc));signature(v.signature,true);}
        for(var i:m.instructions) {
            int op=i.getOpcode();
            require(!(i instanceof JumpInsnNode || i instanceof TableSwitchInsnNode || i instanceof LookupSwitchInsnNode || i instanceof FrameNode)
                    && op!=Opcodes.ATHROW && op!=Opcodes.MONITORENTER && op!=Opcodes.MONITOREXIT && op!=Opcodes.RET,"BUSINESS_CONTROL_FLOW "+key);
            require(op!=Opcodes.NOP,"SUPPLIER_BUSINESS_UNSUPPORTED");
            require(!(i instanceof InvokeDynamicInsnNode),"BUSINESS_BOOTSTRAP "+key);
            if(i instanceof LdcInsnNode l){require(!(l.cst instanceof Handle || l.cst instanceof ConstantDynamic),"BUSINESS_HANDLE "+key);if(l.cst instanceof Type t)type(t);}
            require(!(i instanceof MultiANewArrayInsnNode) && op!=Opcodes.NEWARRAY,"BUSINESS_ARRAY "+key);
            if(i instanceof TypeInsnNode t){
                require(op==Opcodes.NEW || op==Opcodes.CHECKCAST || op==Opcodes.INSTANCEOF,"BUSINESS_TYPE_INSTRUCTION "+key);
                ClassNode n=node(t.desc);if(op==Opcodes.NEW){require((n.access&(Opcodes.ACC_ABSTRACT|Opcodes.ACC_INTERFACE))==0,"ABSTRACT_ALLOCATION "+t.desc);initialize(t.desc);}
            }
            if(i instanceof FieldInsnNode f) {
                type(Type.getType(f.desc));Member ref=member(f.owner,f.name,f.desc,false,new HashSet<>());
                accessible(owner,f.owner,ref,op==Opcodes.GETSTATIC || op==Opcodes.PUTSTATIC);
                require((ref.access&Opcodes.ACC_FINAL)==0 || (op!=Opcodes.PUTSTATIC && op!=Opcodes.PUTFIELD) || owner.equals(ref.owner) && m.name.equals(op==Opcodes.PUTSTATIC?"<clinit>":"<init>"),"FINAL_FIELD_WRITE "+key);
                if(op==Opcodes.GETSTATIC || op==Opcodes.PUTSTATIC)initialize(f.owner);
            }
            if(i instanceof MethodInsnNode c) {
                require(!c.owner.startsWith("java/lang/reflect/") && !c.owner.startsWith("java/lang/invoke/") && !c.owner.equals("java/lang/Class") && !c.owner.equals("java/lang/ClassLoader")
                        && !c.owner.equals("java/util/ServiceLoader") && !(c.owner.equals("java/lang/System") && (c.name.startsWith("load")||c.name.equals("exit"))),"DYNAMIC_LINKAGE "+key);
                type(Type.getMethodType(c.desc));Member ref=member(c.owner,c.name,c.desc,true,new HashSet<>());
                accessible(owner,c.owner,ref,op==Opcodes.INVOKESTATIC);
                boolean itf=(node(c.owner).access&Opcodes.ACC_INTERFACE)!=0;
                require(c.itf==itf && (op!=Opcodes.INVOKEINTERFACE || itf) && (op!=Opcodes.INVOKEVIRTUAL || !itf),"METHOD_OPCODE "+c.owner);
                require(op!=Opcodes.INVOKESPECIAL || c.name.equals("<init>") || owner.equals(ref.owner) && (ref.access&Opcodes.ACC_PRIVATE)!=0,"BUSINESS_SPECIAL_CALL "+key);
                require(!c.name.equals("<init>") || op==Opcodes.INVOKESPECIAL,"CONSTRUCTOR_OPCODE "+key);
                if(owned.containsKey(ref.owner)){if(op==Opcodes.INVOKESTATIC)initialize(ref.owner);validate(ref.owner,ref.method,false);}
            }
        }
        try {
            var verifier=new SimpleVerifier(Opcodes.ASM9,null,null,null,false) {
                @Override protected boolean isInterface(Type t){return t.getSort()==Type.OBJECT && (node(t.getInternalName()).access&Opcodes.ACC_INTERFACE)!=0;}
                @Override protected Type getSuperClass(Type t){if(t.getSort()==Type.ARRAY)return Type.getObjectType("java/lang/Object");String s=node(t.getInternalName()).superName;return s==null?null:Type.getObjectType(s);}
                @Override protected boolean isAssignableFrom(Type a,Type b){return assign(a,b);}
                @Override protected Class<?> getClass(Type t){throw new IllegalStateException("Reflective type lookup forbidden: "+t);}
            };
            Frame<BasicValue>[] frames=new Analyzer<BasicValue>(verifier).analyze(owner,m);
            if(supplier)for(int j=0;j<m.instructions.size();j++)if(m.instructions.get(j).getOpcode()==Opcodes.ARETURN) {
                var f=frames[j];require(f!=null && f.getStackSize()==1,"SUPPLIER_RETURN_STACK "+key);Type t=f.getStack(0).getType();
                require(t!=null && !t.equals(BasicInterpreter.NULL_TYPE) && assign(Type.getObjectType(ForgeRegistryTransformationPlan.ITEM),t),"SUPPLIER_RETURN_TYPE "+key);
            }
        active.remove(key);visited.add(key);
        }catch(AnalyzerException failure){throw new IllegalStateException("Registry helper rejected: SUPPLIER_TYPE_VERIFICATION "+key,failure);}
    }
    private static void require(boolean valid,String reason){if(!valid)throw new IllegalStateException("Registry helper rejected: "+reason);}
}
