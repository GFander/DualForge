import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.signature.*;
import org.objectweb.asm.tree.*;

/** Offline ClassFile reader. No input classes are defined or initialized. */
public final class Atlas {
    static final Gson JSON = new GsonBuilder().serializeNulls().disableHtmlEscaping().create();
    static Map<String,Object> obj(Object... pairs) {
        Map<String,Object> m = new TreeMap<>();
        for (int i=0;i<pairs.length;i+=2) m.put((String)pairs[i],pairs[i+1]);
        return m;
    }
    static String access(int a) {
        return (a&1)!=0 ? "PUBLIC_API" : (a&4)!=0 ? "PROTECTED_API" : (a&2)!=0 ? "PRIVATE_INTERNAL" : "PACKAGE_PRIVATE";
    }
    static Object constant(Object v) {
        if (v instanceof Float f) return obj("floatBits",Integer.toUnsignedString(Float.floatToRawIntBits(f),16));
        if (v instanceof Double d) return obj("doubleBits",Long.toUnsignedString(Double.doubleToRawLongBits(d),16));
        if (v instanceof Type t) return obj("type",t.getDescriptor());
        if (v instanceof Handle h) return obj("handle",obj("tag",h.getTag(),"owner",h.getOwner(),"name",h.getName(),"descriptor",h.getDesc(),"interface",h.isInterface()));
        if (v instanceof ConstantDynamic c) {
            List<Object> args = new ArrayList<>();
            for(int i=0;i<c.getBootstrapMethodArgumentCount();i++) args.add(constant(c.getBootstrapMethodArgument(i)));
            return obj("constantDynamic",obj("name",c.getName(),"descriptor",c.getDescriptor(),"bootstrap",constant(c.getBootstrapMethod()),"arguments",args));
        }
        if (v instanceof AnnotationNode a) return annotation(a);
        if (v instanceof List<?> list) return list.stream().map(Atlas::constant).toList();
        if (v instanceof String[] arr) return obj("enumDescriptor",arr[0],"value",arr[1]);
        if (v!=null && v.getClass().isArray()) {
            List<Object> values=new ArrayList<>();
            for(int i=0;i<java.lang.reflect.Array.getLength(v);i++)values.add(constant(java.lang.reflect.Array.get(v,i)));
            return values;
        }
        return v;
    }
    static Object annotation(AnnotationNode a) {
        Map<String,Object> values = new TreeMap<>();
        if(a.values!=null) for(int i=0;i<a.values.size();i+=2) values.put((String)a.values.get(i),constant(a.values.get(i+1)));
        Map<String,Object> m=obj("descriptor",a.desc,"values",values);
        if(a instanceof TypeAnnotationNode t) { m.put("typeRef",t.typeRef);m.put("typePath",String.valueOf(t.typePath)); }
        return m;
    }
    static List<Object> annotations(List<? extends AnnotationNode> visible,List<? extends AnnotationNode> invisible) {
        List<Object> result=new ArrayList<>();
        if(visible!=null) for(var a:visible) result.add(obj("visible",true,"annotation",annotation(a)));
        if(invisible!=null) for(var a:invisible) result.add(obj("visible",false,"annotation",annotation(a)));
        result.sort(Comparator.comparing(JSON::toJson)); return result;
    }
    static Object module(ModuleNode m) {
        if(m==null)return null;
        return obj("name",m.name,"access",m.access,"version",m.version,"mainClass",m.mainClass,
            "packages",m.packages,"uses",m.uses,
            "requires",m.requires==null?List.of():m.requires.stream().map(r->obj("module",r.module,"access",r.access,"version",r.version)).toList(),
            "exports",m.exports==null?List.of():m.exports.stream().map(e->obj("package",e.packaze,"access",e.access,"modules",e.modules)).toList(),
            "opens",m.opens==null?List.of():m.opens.stream().map(e->obj("package",e.packaze,"access",e.access,"modules",e.modules)).toList(),
            "provides",m.provides==null?List.of():m.provides.stream().map(p->obj("service",p.service,"providers",p.providers)).toList());
    }
    static Map<String,Object> abi(ClassNode c,String artifact,String member) {
        List<Object> fields=new ArrayList<>(),methods=new ArrayList<>(),records=new ArrayList<>();
        c.fields.stream().sorted(Comparator.comparing((FieldNode f)->f.name).thenComparing(f->f.desc)).forEach(f->fields.add(obj(
            "owner",c.name,"name",f.name,"descriptor",f.desc,"signature",f.signature,"access",f.access,"visibility",access(f.access),
            "synthetic",(f.access&4096)!=0,"value",constant(f.value),"annotations",annotations(f.visibleAnnotations,f.invisibleAnnotations),
            "typeAnnotations",annotations(f.visibleTypeAnnotations,f.invisibleTypeAnnotations))));
        c.methods.stream().sorted(Comparator.comparing((MethodNode m)->m.name).thenComparing(m->m.desc)).forEach(m->methods.add(obj(
            "owner",c.name,"name",m.name,"descriptor",m.desc,"signature",m.signature,"access",m.access,"visibility",access(m.access),
            "synthetic",(m.access&4096)!=0,"bridge",(m.access&64)!=0,"exceptions",m.exceptions,"annotationDefault",constant(m.annotationDefault),
            "annotations",annotations(m.visibleAnnotations,m.invisibleAnnotations),"typeAnnotations",annotations(m.visibleTypeAnnotations,m.invisibleTypeAnnotations),
            "visibleParameterAnnotations",parameterAnnotations(m.visibleParameterAnnotations),"invisibleParameterAnnotations",parameterAnnotations(m.invisibleParameterAnnotations))));
        if(c.recordComponents!=null)for(var r:c.recordComponents)records.add(obj("name",r.name,"descriptor",r.descriptor,"signature",r.signature,
            "annotations",annotations(r.visibleAnnotations,r.invisibleAnnotations),"typeAnnotations",annotations(r.visibleTypeAnnotations,r.invisibleTypeAnnotations)));
        return obj("name",c.name,"artifact",artifact,"member",member,"access",c.access,"visibility",access(c.access),"classVersion",c.version,
            "synthetic",(c.access&4096)!=0,"superclass",c.superName,"interfaces",c.interfaces,"signature",c.signature,
            "annotations",annotations(c.visibleAnnotations,c.invisibleAnnotations),"typeAnnotations",annotations(c.visibleTypeAnnotations,c.invisibleTypeAnnotations),
            "nestHost",c.nestHostClass,"nestMembers",c.nestMembers,"recordComponents",records,"permittedSubclasses",c.permittedSubclasses,
            "outerClass",c.outerClass,"outerMethod",c.outerMethod,"outerMethodDescriptor",c.outerMethodDesc,
            "innerClasses",c.innerClasses.stream().map(i->obj("name",i.name,"outerName",i.outerName,"innerName",i.innerName,"access",i.access)).toList(),
            "module",module(c.module),"fields",fields,"methods",methods);
    }
    static Object parameterAnnotations(List<AnnotationNode>[] a) {
        return a==null?null:Arrays.stream(a).map(x->annotations(x,null)).toList();
    }
    static final class Usage {
        final List<Object> refs=new ArrayList<>();
        final ClassNode c; final String artifact;
        String method="<class>";int index=-1;
        Usage(ClassNode c,String artifact){this.c=c;this.artifact=artifact;}
        void ref(String kind,String owner,String name,String descriptor,int opcode,String context){
            if(owner==null)return;
            refs.add(obj("class",c.name,"method",method,"instructionIndex",index,"kind",kind,"owner",owner,"name",name,
                "descriptor",descriptor,"opcode",opcode,"context",context,"artifact",artifact));
        }
        void type(Type t,String why){
            switch(t.getSort()){
                case Type.OBJECT -> ref("CLASS",t.getInternalName(),null,null,-1,why);
                case Type.ARRAY -> type(t.getElementType(),why);
                case Type.METHOD -> { for(Type a:t.getArgumentTypes())type(a,why);type(t.getReturnType(),why); }
            }
        }
        void desc(String d,String why){if(d!=null)type(Type.getType(d),why);}
        void signature(String s,boolean type){
            if(s==null)return;
            class Sig extends SignatureVisitor {
                String owner;
                Sig(){super(Opcodes.ASM9);}
                @Override public void visitClassType(String name){owner=name;ref("CLASS",name,null,null,-1,"genericSignature");}
                @Override public void visitInnerClassType(String name){owner+="$"+name;ref("CLASS",owner,null,null,-1,"genericSignature");}
                @Override public SignatureVisitor visitTypeArgument(char w){return new Sig();}
                @Override public SignatureVisitor visitArrayType(){return new Sig();}
                @Override public SignatureVisitor visitClassBound(){return new Sig();}
                @Override public SignatureVisitor visitInterfaceBound(){return new Sig();}
                @Override public SignatureVisitor visitSuperclass(){return new Sig();}
                @Override public SignatureVisitor visitInterface(){return new Sig();}
                @Override public SignatureVisitor visitParameterType(){return new Sig();}
                @Override public SignatureVisitor visitReturnType(){return new Sig();}
                @Override public SignatureVisitor visitExceptionType(){return new Sig();}
            }
            SignatureVisitor v=new Sig();
            if(type)new SignatureReader(s).acceptType(v);else new SignatureReader(s).accept(v);
        }
        void value(Object v,String why){
            if(v instanceof Type t)type(t,why);
            else if(v instanceof Handle h){ref(h.getTag()<=4?"FIELD":"METHOD",h.getOwner(),h.getName(),h.getDesc(),h.getTag(),why+":handle");desc(h.getDesc(),why);}
            else if(v instanceof ConstantDynamic d){desc(d.getDescriptor(),why);value(d.getBootstrapMethod(),"constantDynamicBootstrap");
                for(int i=0;i<d.getBootstrapMethodArgumentCount();i++)value(d.getBootstrapMethodArgument(i),"constantDynamicArgument");}
            else if(v instanceof AnnotationNode a){desc(a.desc,"annotation");if(a.values!=null)for(int i=1;i<a.values.size();i+=2)value(a.values.get(i),"annotationValue");}
            else if(v instanceof String[] a){desc(a[0],"annotationEnum");}
            else if(v instanceof List<?> list)for(var x:list)value(x,why);
        }
        void ann(List<? extends AnnotationNode> a){if(a!=null)for(var x:a)value(x,"annotation");}
        void scan(){
            ref("CLASS",c.superName,null,null,-1,"superclass");for(String i:c.interfaces)ref("CLASS",i,null,null,-1,"interface");
            signature(c.signature,false);ann(c.visibleAnnotations);ann(c.invisibleAnnotations);ann(c.visibleTypeAnnotations);ann(c.invisibleTypeAnnotations);
            if(c.nestHostClass!=null)ref("CLASS",c.nestHostClass,null,null,-1,"nestHost");
            if(c.nestMembers!=null)for(String n:c.nestMembers)ref("CLASS",n,null,null,-1,"nestMember");
            if(c.permittedSubclasses!=null)for(String n:c.permittedSubclasses)ref("CLASS",n,null,null,-1,"permittedSubclass");
            ref("CLASS",c.outerClass,null,null,-1,"outerClass");desc(c.outerMethodDesc,"outerMethod");
            for(var i:c.innerClasses){ref("CLASS",i.name,null,null,-1,"innerClass");ref("CLASS",i.outerName,null,null,-1,"innerOuterClass");}
            for(var f:c.fields){method="<field>"+f.name;desc(f.desc,"fieldDeclaration");signature(f.signature,true);ann(f.visibleAnnotations);ann(f.invisibleAnnotations);ann(f.visibleTypeAnnotations);ann(f.invisibleTypeAnnotations);value(f.value,"fieldConstant");}
            if(c.recordComponents!=null)for(var r:c.recordComponents){method="<record>"+r.name;desc(r.descriptor,"record");signature(r.signature,true);ann(r.visibleAnnotations);ann(r.invisibleAnnotations);}
            for(var m:c.methods){method=m.name+m.desc;index=-1;desc(m.desc,"methodDeclaration");signature(m.signature,false);ann(m.visibleAnnotations);ann(m.invisibleAnnotations);ann(m.visibleTypeAnnotations);ann(m.invisibleTypeAnnotations);value(m.annotationDefault,"annotationDefault");
                if(m.visibleParameterAnnotations!=null)for(var a:m.visibleParameterAnnotations)ann(a);
                if(m.invisibleParameterAnnotations!=null)for(var a:m.invisibleParameterAnnotations)ann(a);
                ann(m.visibleLocalVariableAnnotations);ann(m.invisibleLocalVariableAnnotations);
                for(String x:m.exceptions)ref("CLASS",x,null,null,-1,"throws");
                for(var t:m.tryCatchBlocks){ref("CLASS",t.type,null,null,-1,"catch");ann(t.visibleTypeAnnotations);ann(t.invisibleTypeAnnotations);}
                if(m.localVariables!=null)for(var v:m.localVariables){desc(v.desc,"localVariable");signature(v.signature,true);}
                for(var n:m.instructions){index++;ann(n.visibleTypeAnnotations);ann(n.invisibleTypeAnnotations);
                    if(n instanceof MethodInsnNode x){ref("METHOD",x.owner,x.name,x.desc,x.getOpcode(),"instruction");desc(x.desc,"instructionDescriptor");}
                    else if(n instanceof FieldInsnNode x){ref("FIELD",x.owner,x.name,x.desc,x.getOpcode(),"instruction");desc(x.desc,"instructionDescriptor");}
                    else if(n instanceof TypeInsnNode x){if(x.desc.startsWith("["))desc(x.desc,"typeInstruction");else ref("CLASS",x.desc,null,null,x.getOpcode(),"typeInstruction");}
                    else if(n instanceof InvokeDynamicInsnNode x){ref("INVOKEDYNAMIC",c.name,x.name,x.desc,x.getOpcode(),"dynamicCallSite");desc(x.desc,"invokedynamicDescriptor");value(x.bsm,"bootstrap");for(Object a:x.bsmArgs)value(a,"bootstrapArgument");}
                    else if(n instanceof LdcInsnNode x){value(x.cst,"ldc");if(x.cst instanceof String s)ref("STRING",s,null,null,x.getOpcode(),"potentialReflectionConstant; not a proven class reference");}
                    else if(n instanceof MultiANewArrayInsnNode x)desc(x.desc,"multiANewArray");
                    else if(n instanceof FrameNode x){if(x.local!=null)for(var v:x.local)if(v instanceof String s){if(s.startsWith("["))desc(s,"frameLocal");else ref("CLASS",s,null,null,-1,"frameLocal");}if(x.stack!=null)for(var v:x.stack)if(v instanceof String s){if(s.startsWith("["))desc(s,"frameStack");else ref("CLASS",s,null,null,-1,"frameStack");}}
                }
            }
        }
    }
    public static void main(String[] args)throws Exception{
        if(args.length!=3)throw new IllegalArgumentException("scan|usage inputs.tsv output.jsonl.gz");
        boolean usage=args[0].equals("usage");if(!usage&&!args[0].equals("scan"))throw new IllegalArgumentException("mode");
        try(var output=new PrintWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(Path.of(args[2]))),java.nio.charset.StandardCharsets.UTF_8))){
            for(String row:Files.readAllLines(Path.of(args[1]))){
                String[] cols=row.split("\t",2);String artifact=cols[0];Path path=Path.of(cols[1]);
                try(JarFile jar=new JarFile(path.toFile())){
                    boolean mr="true".equalsIgnoreCase(jar.getManifest()==null?null:jar.getManifest().getMainAttributes().getValue("Multi-Release"));
                    for(String name:jar.stream().map(JarEntry::getName).filter(n->n.endsWith(".class")).sorted().toList()){
                        int release=0;if(name.startsWith("META-INF/versions/")){release=Integer.parseInt(name.split("/")[2]);}
                        byte[] bytes=jar.getInputStream(jar.getJarEntry(name)).readAllBytes();
                        ClassNode c=new ClassNode();new ClassReader(bytes).accept(c,usage?0:ClassReader.SKIP_CODE);
                        var record=abi(c,artifact,name);record.put("multiRelease",mr);record.put("release",release);record.put("java21Eligible",release==0||(mr&&release<=21));
                        record.put("classSHA256",HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)));
                        if(usage){Usage u=new Usage(c,artifact);u.scan();record.put("references",u.refs);}
                        output.println(JSON.toJson(record));
                    }
                }
            }
        }
    }
}
