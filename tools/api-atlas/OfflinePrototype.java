import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.jar.*;
import java.util.zip.ZipFile;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import org.objectweb.asm.util.CheckClassAdapter;

/** Explicit offline experiments only. Not a transformer service or runtime shim. */
public final class OfflinePrototype {
    record Ref(String owner,String name,String descriptor,int opcode) {
        static Ref read(JsonObject r){return new Ref(r.get("owner").getAsString(),r.get("name").getAsString(),r.get("descriptor").getAsString(),r.get("opcode").getAsInt());}
        String key(){return owner+"."+name+descriptor+"@"+opcode;}
    }
    record Rewrite(String id,String kind,Ref source,Ref target){}
    static void require(boolean b,String reason){if(!b)throw new IllegalArgumentException(reason);}
    static String sha(byte[] b)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
    static byte[] bytes(ClassNode c){ClassWriter w=new ClassWriter(0);c.accept(w);return w.toByteArray();}
    static ClassNode node(byte[] b){ClassNode c=new ClassNode();new ClassReader(b).accept(c,0);return c;}

    /** Lazy, exact provider lookup. Unknown hierarchy never defaults to Object. */
    static final class Oracle {
        final Map<String,String[]> locations=new HashMap<>();
        final Map<String,ClassNode> cache=new HashMap<>();
        Oracle(Path tsv)throws IOException{
            for(String line:Files.readAllLines(tsv)){
                String[] c=line.split("\t",3);require(c.length==3,"oracle row");
                require(locations.put(c[0],new String[]{c[1],c[2]})==null,"ambiguous oracle provider: "+c[0]);
            }
        }
        ClassNode get(String name){
            if(cache.containsKey(name))return cache.get(name);
            byte[] b;
            try{
                String[] loc=locations.get(name);
                if(loc!=null){try(ZipFile z=new ZipFile(loc[0])){b=z.getInputStream(z.getEntry(loc[1])).readAllBytes();}}
                else if(name.startsWith("java/")||name.startsWith("javax/")||name.startsWith("jdk/")){
                    try(InputStream in=ClassLoader.getSystemResourceAsStream(name+".class")){require(in!=null,"unknown JDK class: "+name);b=in.readAllBytes();}
                }else throw new IllegalArgumentException("unknown hierarchy: "+name);
            }catch(IOException e){throw new UncheckedIOException(e);}
            ClassNode c=node(b);require(c.name.equals(name),"oracle internal name mismatch");cache.put(name,c);return c;
        }
        Object member(String kind,String owner,String name,String desc,Set<String> seen){
            if(!seen.add(owner))return null;
            ClassNode c=get(owner);
            if(kind.equals("FIELD")){
                for(var f:c.fields)if(f.name.equals(name)&&f.desc.equals(desc))return f;
                for(String i:c.interfaces){Object found=member(kind,i,name,desc,new HashSet<>(seen));if(found!=null)return found;}
            }else{
                for(var m:c.methods)if(m.name.equals(name)&&m.desc.equals(desc))return m;
                if(name.equals("<init>")||name.equals("<clinit>"))return null;
            }
            if(c.superName!=null){Object found=member(kind,c.superName,name,desc,new HashSet<>(seen));if(found!=null)return found;}
            if(kind.equals("METHOD")){
                List<Object> found=new ArrayList<>();
                for(String i:c.interfaces){Object m=member(kind,i,name,desc,new HashSet<>(seen));if(m!=null&&!found.contains(m))found.add(m);}
                require(found.size()<=1,"ambiguous interface resolution: "+owner+"."+name+desc);
                if(!found.isEmpty())return found.getFirst();
            }
            return null;
        }
        int memberFlags(String kind,Ref r){
            Object m=member(kind,r.owner,r.name,r.descriptor,new HashSet<>());
            require(m!=null,"missing "+kind+" "+r.key());
            return m instanceof MethodNode n?n.access:((FieldNode)m).access;
        }
        void validate(String kind,Ref r,boolean external){
            ClassNode c=get(r.owner);int flags=memberFlags(kind,r);
            require(!external||((flags&Opcodes.ACC_PUBLIC)!=0&&(c.access&Opcodes.ACC_PUBLIC)!=0),"inaccessible API "+r.key());
            boolean stat=(flags&Opcodes.ACC_STATIC)!=0;
            if(kind.equals("FIELD"))require(stat==(r.opcode==Opcodes.GETSTATIC||r.opcode==Opcodes.PUTSTATIC),"field opcode mismatch");
            else{
                require(stat==(r.opcode==Opcodes.INVOKESTATIC),"method opcode mismatch");
                require(r.opcode!=Opcodes.INVOKEINTERFACE||(c.access&Opcodes.ACC_INTERFACE)!=0,"interface opcode mismatch");
                require(r.opcode!=Opcodes.INVOKEVIRTUAL||(c.access&Opcodes.ACC_INTERFACE)==0,"virtual opcode mismatch");
            }
        }
        boolean assign(Type to,Type from){
            if(to.equals(from))return true;
            if(from.getSort()==Type.OBJECT&&from.getInternalName().equals("null"))return to.getSort()==Type.OBJECT||to.getSort()==Type.ARRAY;
            if(from.getSort()==Type.ARRAY){
                if(to.getSort()==Type.OBJECT)return Set.of("java/lang/Object","java/lang/Cloneable","java/io/Serializable").contains(to.getInternalName());
                return to.getSort()==Type.ARRAY&&assign(Type.getType(to.getDescriptor().substring(1)),Type.getType(from.getDescriptor().substring(1)));
            }
            if(to.getSort()!=Type.OBJECT||from.getSort()!=Type.OBJECT)return false;
            Set<String> seen=new HashSet<>();ArrayDeque<String> todo=new ArrayDeque<>();todo.add(from.getInternalName());
            while(!todo.isEmpty()){
                String next=todo.remove();if(!seen.add(next))continue;
                if(next.equals(to.getInternalName()))return true;
                ClassNode c=get(next);if(c.superName!=null)todo.add(c.superName);todo.addAll(c.interfaces);
            }
            return false;
        }
    }
    static final class Verifier extends SimpleVerifier {
        final Oracle oracle;
        Verifier(ClassNode c,Oracle o){
            super(Opcodes.ASM9,Type.getObjectType(c.name),c.superName==null?null:Type.getObjectType(c.superName),
                c.interfaces.stream().map(Type::getObjectType).toList(),(c.access&Opcodes.ACC_INTERFACE)!=0);oracle=o;
        }
        @Override protected boolean isInterface(Type t){return t.getSort()!=Type.ARRAY&&(oracle.get(t.getInternalName()).access&Opcodes.ACC_INTERFACE)!=0;}
        @Override protected Type getSuperClass(Type t){if(t.getSort()==Type.ARRAY)return Type.getObjectType("java/lang/Object");String n=oracle.get(t.getInternalName()).superName;return n==null?null:Type.getObjectType(n);}
        @Override protected boolean isAssignableFrom(Type a,Type b){return oracle.assign(a,b);}
        @Override protected Class<?> getClass(Type t){throw new IllegalStateException("reflection is forbidden for target hierarchy: "+t);}
    }
    static final class Plan {
        final Map<String,String> classes=new TreeMap<>();
        final Map<String,Rewrite> fields=new HashMap<>(),methods=new HashMap<>();
        final Map<String,String> signatures=new TreeMap<>();
        final Set<String> used=new TreeSet<>();
        Plan(JsonObject json,Oracle source,Oracle target){
            require(json.get("schemaVersion").getAsInt()==1,"schema version");
            require(json.get("approval").getAsString().equals("OFFLINE_STATIC_EXPERIMENT"),"candidate is not approved for offline experiment");
            require(!json.get("productionEnabled").getAsBoolean()&&!json.get("runtimeVerified").getAsBoolean(),"production/runtime approval forbidden");
            for(JsonElement el:json.getAsJsonArray("classes")){
                var r=el.getAsJsonObject();String a=r.get("source").getAsString(),b=r.get("target").getAsString();
                require(a.startsWith("net/minecraftforge/"),"only explicit Forge source types may be mapped");
                require(classes.put(a,b)==null,"AMBIGUOUS_MAPPING "+a);source.get(a);target.get(b);
            }
            for(JsonElement el:json.getAsJsonArray("signatures")){
                var r=el.getAsJsonObject();require(signatures.put(r.get("source").getAsString(),r.get("target").getAsString())==null,"ambiguous generic signature rule");
            }
            for(JsonElement el:json.getAsJsonArray("rules")){
                var r=el.getAsJsonObject();String strategy=r.get("strategy").getAsString();
                require(!strategy.equals("RUNTIME_SHIM_REQUIRED"),"RUNTIME_SHIM_REQUIRED: "+r.get("id"));
                if(r.get("kind").getAsString().equals("CLASS"))continue;
                Rewrite rule=new Rewrite(r.get("id").getAsString(),r.get("kind").getAsString(),Ref.read(r.getAsJsonObject("source")),Ref.read(r.getAsJsonObject("target")));
                require(rule.source.opcode==rule.target.opcode,"invocation kind change needs separate stack contract");
                source.validate(rule.kind,rule.source,true);target.validate(rule.kind,rule.target,true);
                var map=rule.kind.equals("FIELD")?fields:methods;
                require(map.put(rule.source.key(),rule)==null,"AMBIGUOUS_MAPPING "+rule.source.key());
            }
        }
        String type(String name){
            if(name.startsWith("net/minecraftforge/")&&!classes.containsKey(name))
                throw new IllegalArgumentException(name.contains("eventbus")||name.contains("/fml/javafmlmod/")?"RUNTIME_SHIM_REQUIRED: "+name:"unverified Forge type: "+name);
            return classes.getOrDefault(name,name);
        }
        void checkType(Type t){
            if(t.getSort()==Type.OBJECT)type(t.getInternalName());
            else if(t.getSort()==Type.ARRAY)checkType(t.getElementType());
            else if(t.getSort()==Type.METHOD){for(Type a:t.getArgumentTypes())checkType(a);checkType(t.getReturnType());}
        }
        void descriptor(String d){if(d!=null)checkType(Type.getType(d));}
        boolean claimed(ClassNode c){
            Atlas.Usage usage=new Atlas.Usage(c,"offline-input");usage.scan();
            return usage.refs.stream().anyMatch(r->((Map<?,?>)r).get("owner").toString().startsWith("net/minecraftforge/"));
        }
        void original(ClassNode c,Set<String> owned){
            if(c.superName!=null)type(c.superName);for(String i:c.interfaces)type(i);
            for(var f:c.fields)descriptor(f.desc);
            for(var m:c.methods){descriptor(m.desc);
                for(var n:m.instructions){
                    if(n instanceof InvokeDynamicInsnNode)throw new IllegalArgumentException("unhandled invokedynamic");
                    if(n instanceof LdcInsnNode l&&(l.cst instanceof Handle||l.cst instanceof ConstantDynamic))throw new IllegalArgumentException("unsupported MethodHandle/ConstantDynamic");
                    if(n instanceof TypeInsnNode t){if(t.desc.startsWith("["))descriptor(t.desc);else type(t.desc);}
                    if(n instanceof MultiANewArrayInsnNode t)descriptor(t.desc);
                    if(n instanceof MethodInsnNode x){
                        descriptor(x.desc);Ref ref=new Ref(x.owner,x.name,x.desc,x.getOpcode());
                        if(x.owner.startsWith("net/minecraftforge/")){Rewrite r=methods.get(ref.key());require(r!=null,"unverified Forge method/opcode: "+ref.key());used.add(r.id);}
                    }else if(n instanceof FieldInsnNode x){
                        descriptor(x.desc);Ref ref=new Ref(x.owner,x.name,x.desc,x.getOpcode());
                        if(x.owner.startsWith("net/minecraftforge/")){Rewrite r=fields.get(ref.key());require(r!=null,"unknown Registry/Forge field: "+ref.key());used.add(r.id);}
                    }else if(n instanceof LdcInsnNode l&&l.cst instanceof Type t)checkType(t);
                }
            }
        }
        Remapper remapper(){return new Remapper(Opcodes.ASM9){
            @Override public String map(String name){return type(name);}
            @Override public String mapSignature(String s,boolean typeSignature){
                if(s==null)return null;
                String value=s;
                for(String registry:List.of("DeferredRegister","IForgeRegistry")){
                    String prefix="Lnet/minecraftforge/registries/"+registry+"<";
                    String supported=prefix+"Lnet/minecraft/world/item/Item;>;";
                    require(!value.replace(supported,"").contains(prefix),"unknown Registry generic type: "+registry);
                }
                for(var e:signatures.entrySet())value=value.replace(e.getKey(),e.getValue());
                require(!value.contains("Lnet/minecraftforge/registries/RegistryObject<"),"unknown Registry/Holder generic type");
                return super.mapSignature(value,typeSignature);
            }
        };}
        ClassNode cloneAndRewrite(ClassNode input){
            ClassNode copy=new ClassNode();Remapper mapper=remapper();
            input.accept(new ClassRemapper(Opcodes.ASM9,copy,mapper){
                @Override protected MethodVisitor createMethodRemapper(MethodVisitor mv){return new MethodRemapper(Opcodes.ASM9,mv,mapper){
                    @Override public void visitFieldInsn(int op,String owner,String name,String desc){
                        Rewrite r=fields.get(new Ref(owner,name,desc,op).key());
                        if(r==null)super.visitFieldInsn(op,owner,name,desc);else{var t=r.target;mv.visitFieldInsn(t.opcode,t.owner,t.name,t.descriptor);}
                    }
                    @Override public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf){
                        Rewrite r=methods.get(new Ref(owner,name,desc,op).key());
                        if(r==null)super.visitMethodInsn(op,owner,name,desc,itf);else{var t=r.target;mv.visitMethodInsn(t.opcode,t.owner,t.name,t.descriptor,itf);}
                    }
                };}
            });return copy;
        }
    }
    static void verify(Map<String,ClassNode> nodes,Oracle oracle)throws Exception{
        oracle.cache.putAll(nodes);
        for(var c:nodes.values()){
            c.accept(new CheckClassAdapter(new ClassWriter(0),true));
            for(var m:c.methods){
                if((m.access&(Opcodes.ACC_ABSTRACT|Opcodes.ACC_NATIVE))==0)new Analyzer<BasicValue>(new Verifier(c,oracle)).analyze(c.name,m);
                for(var n:m.instructions){
                    if(n instanceof MethodInsnNode x)oracle.validate("METHOD",new Ref(x.owner,x.name,x.desc,x.getOpcode()),!nodes.containsKey(x.owner));
                    if(n instanceof FieldInsnNode x)oracle.validate("FIELD",new Ref(x.owner,x.name,x.desc,x.getOpcode()),!nodes.containsKey(x.owner));
                }
            }
        }
    }
    static Map<String,Object> transform(Map<String,ClassNode> original,JsonObject config,Oracle source,Oracle target,boolean injectedFailure)throws Exception{
        Map<String,String> before=new TreeMap<>();for(var e:original.entrySet())before.put(e.getKey(),sha(bytes(e.getValue())));
        try{
            require(!original.containsKey("module-info"),"explicit JPMS unsupported by offline closure");
            boolean forge=false,nativeMod=false;
            for(var c:original.values())if(c.visibleAnnotations!=null)for(var a:c.visibleAnnotations){forge|=a.desc.equals("Lnet/minecraftforge/fml/common/Mod;");nativeMod|=a.desc.equals("Lnet/neoforged/fml/common/Mod;");}
            if(!forge)return Atlas.obj("status","UNCHANGED_NON_FORGE","classes",original,"usedRules",List.of());
            require(!nativeMod,"mixed Forge/native ownership");
            Plan plan=new Plan(config,source,target);
            for(var c:original.values())if(plan.claimed(c))plan.original(c,original.keySet());
            verify(original,source);
            Map<String,ClassNode> clones=new TreeMap<>();
            for(var c:original.values()){
                clones.put(c.name,plan.claimed(c)?plan.cloneAndRewrite(c):c);
                require(!injectedFailure,"injected partial-plan failure after clone");
            }
            verify(clones,target);
            return Atlas.obj("status","OFFLINE_TRANSFORMATION_PASS","classes",clones,"usedRules",plan.used);
        }finally{
            for(var e:original.entrySet())require(before.get(e.getKey()).equals(sha(bytes(e.getValue()))),"original ClassNode was modified");
        }
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args)throws Exception{
        require(args.length==6,"input.jar offline-rules.json source-oracle.tsv target-oracle.tsv output-dir report.json");
        Path input=Path.of(args[0]),output=Path.of(args[4]),report=Path.of(args[5]);
        require(output.toAbsolutePath().normalize().toString().contains("/out/"),"offline output must be inside a dedicated out directory");
        require(!Files.exists(output),"choose a new output directory");byte[] jarBefore=Files.readAllBytes(input);
        Map<String,ClassNode> originals=new TreeMap<>();Map<String,byte[]> raw=new TreeMap<>();boolean metadata;
        try(JarFile jar=new JarFile(input.toFile())){
            metadata=jar.getJarEntry("META-INF/mods.toml")!=null;
            for(String n:jar.stream().map(JarEntry::getName).filter(n->n.endsWith(".class")).sorted().toList()){
                require(!n.startsWith("/")&&!Arrays.asList(n.split("/")).contains(".."),"unsafe input path");
                byte[] b=jar.getInputStream(jar.getJarEntry(n)).readAllBytes();ClassNode c=node(b);
                require(n.equals(c.name+".class"),"multi-release/foreign class path not supported by prototype");
                require(originals.put(c.name,c)==null,"duplicate class");raw.put(c.name,b);
            }
        }
        Map<String,Object> receipt=Atlas.obj("inputSHA256",sha(jarBefore),"runtimeCompatibility","NOT VERIFIED", "productionEnabled",false);
        try{
            JsonObject rules=JsonParser.parseString(Files.readString(Path.of(args[1]))).getAsJsonObject();
            require(metadata||originals.values().stream().noneMatch(c->c.visibleAnnotations!=null&&c.visibleAnnotations.stream().anyMatch(a->a.desc.equals("Lnet/minecraftforge/fml/common/Mod;"))),"missing Forge metadata");
            var result=transform(originals,rules,new Oracle(Path.of(args[2])),new Oracle(Path.of(args[3])),false);
            var transformed=(Map<String,ClassNode>)result.remove("classes");
            Map<String,String> hashes=new TreeMap<>();Map<String,byte[]> prepared=new TreeMap<>();
            for(var e:transformed.entrySet()){
                boolean unchanged=Arrays.equals(bytes(originals.get(e.getKey())),bytes(e.getValue()));
                byte[] b=unchanged?raw.get(e.getKey()):bytes(e.getValue());
                prepared.put(e.getKey(),b);hashes.put(e.getKey(),sha(b));
            }
            // No files are published until all classes, closure, and dataflow checks passed.
            Files.createDirectories(output.toAbsolutePath().getParent());
            Path staged=Files.createTempDirectory(output.toAbsolutePath().getParent(),"unpublished-");
            for(var e:prepared.entrySet()){Path dest=staged.resolve(e.getKey()+".class");Files.createDirectories(dest.getParent());Files.write(dest,e.getValue());}
            Files.move(staged,output,StandardCopyOption.ATOMIC_MOVE);
            receipt.putAll(result);receipt.put("classSHA256",hashes);
        }catch(Exception e){receipt.put("status","REJECTED");receipt.put("reason",e.getClass().getSimpleName()+": "+e.getMessage());}
        require(Arrays.equals(jarBefore,Files.readAllBytes(input)),"input JAR changed");
        receipt.put("inputUnchanged",true);receipt.put("originalClassNodesUnchanged",true);
        Files.createDirectories(report.toAbsolutePath().getParent());Files.writeString(report,Atlas.JSON.toJson(receipt)+"\n");
        System.out.println(Atlas.JSON.toJson(receipt));
    }
}
