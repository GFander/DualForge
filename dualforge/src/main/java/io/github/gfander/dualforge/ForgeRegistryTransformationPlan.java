package io.github.gfander.dualforge;

import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import net.neoforged.neoforgespi.language.IModInfo;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Immutable same-ModFile typed Registry/Global closure; no source class is defined while planning. */
final class ForgeRegistryTransformationPlan {
    static final String F = "net/minecraftforge/registries/", N = "net/neoforged/neoforge/registries/";
    static final String ITEM = "net/minecraft/world/item/Item", BUS = ControlledForgeContextProfile.BUS;
    static final String R = "L" + F + "DeferredRegister;", H = "L" + F + "RegistryObject;";
    static final String CREATE = "(L" + F + "IForgeRegistry;Ljava/lang/String;)" + R;
    static final String CREATE_KEY = "(Lnet/minecraft/resources/ResourceKey;Ljava/lang/String;)" + R;
    static final String TAB = "net/minecraft/world/item/CreativeModeTab";
    enum Kind {
        ITEM(ForgeRegistryTransformationPlan.ITEM,"ITEM","minecraft:item"), TAB(ForgeRegistryTransformationPlan.TAB,"CREATIVE_MODE_TAB","minecraft:creative_mode_tab");
        final String type,field,id;Kind(String type,String field,String id){this.type=type;this.field=field;this.id=id;}
        String generic(String source){return "L"+F+source+"<L"+type+";>;";}
    }
    static final String REGISTER = "(Ljava/lang/String;Ljava/util/function/Supplier;)" + H;
    static final String BRIDGE = "io/github/gfander/dualforge/ForgeRegistryBridge";
    private enum Value { REGISTRY, REGISTER, SUPPLIER, HOLDER }
    static final Handle META = new Handle(Opcodes.H_INVOKESTATIC,"java/lang/invoke/LambdaMetafactory","metafactory",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",false);
    final String modId, module, entry, fingerprint;
    final Map<String,String> registers;
    private final Map<String,Kind> kinds;
    final Set<String> helperMethods;
    final ControlledForgeGlobalBus global;
    private final Map<String,byte[]> original, normalized, output;

    private ForgeRegistryTransformationPlan(IModInfo info, String entry, Map<String,byte[]> original,
            Map<String,ClassNode> nodes, Set<String> targets, Map<String,String> registers, Set<String> helpers, ControlledItemSupplier suppliers, ControlledForgeGlobalBus global) {
        this.global=global;this.modId=info.getModId();this.module=info.getOwningFile().moduleName();this.entry=entry;
        Map<String,Kind> typed=new TreeMap<>();for(String name:registers.keySet())typed.put(name,kind(nodes.get(name)));kinds=Map.copyOf(typed);
        this.original=Map.copyOf(original);this.registers=Map.copyOf(registers);this.helperMethods=Set.copyOf(helpers);
        StringBuilder id=new StringBuilder("registry-helper-v5-global\n"+modId+"\n"+module+"\n"+info.getOwningFile().getFile().getFilePath().toAbsolutePath()+"\n");
        original.keySet().stream().sorted().forEach(n -> id.append(n).append(':').append(digest(original.get(n))).append('\n'));
        suppliers.hostFingerprints().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(x -> id.append("host:").append(x.getKey()).append(':').append(x.getValue()).append('\n'));
        if(global!=null)global.hostFingerprints.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(x -> id.append("global-host:").append(x.getKey()).append(':').append(x.getValue()).append('\n'));
        fingerprint=digest(id.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Map<String,byte[]> source=new HashMap<>(), changed=new HashMap<>();
        var context=new ControlledForgeContextProfile.Scope(modId,Set.copyOf(nodes.keySet()),original::get);
        if(global==null)require(ControlledForgeContextProfile.validateRegistry(nodes.get(entry),context,helperMethods).pendingGlobalCalls()==0,"GLOBAL_EVENT_BUS_UNSUPPORTED");
        for(String name:targets) {
            ClassNode n=read(bytes(nodes.get(name)));source.put(name,bytes(n));
            if(name.equals(entry)) {if(global==null)ControlledForgeContextProfile.rewriteRegistry(n,context,helperMethods);else global.rewrite(n,context,helperMethods,fingerprint);}
            else rewrite(n,registers.containsKey(name),fingerprint);
            RegistryBytecodeVerifier.verify(n);changed.put(name,bytes(n));
        }
        normalized=Map.copyOf(source);output=Map.copyOf(changed);
    }
    static boolean isCandidate(ClassNode n) {
        boolean helper=ControlledForgeGlobalBus.isCandidate(n);
        for(MethodNode m:n.methods)for(var i:m.instructions) {
            // Candidate routing never substitutes for full Global/Registry validation.
            if(i instanceof MethodInsnNode c && c.getOpcode()==Opcodes.INVOKESTATIC && c.desc.equals("("+BUS+")V"))helper=true;
        }
        return helper;
    }
    static ForgeRegistryTransformationPlan create(IModInfo info, String entry) {
        return create(info,entry,ControlledItemSupplier::hostResource);
    }
    static ForgeRegistryTransformationPlan create(IModInfo info,String entry,java.util.function.Function<String,byte[]> host) {
        var file=info.getOwningFile();var scan=file.getFile().getScanResult();
        require(scan!=null,"SCAN_NOT_COMPLETE");
        require(!Files.exists(file.getFile().findResource("module-info.class")),"EXPLICIT_JPMS_HELPER_UNSUPPORTED");
        require(ForgeModLocator.isForgeCandidate(file.getFile().getFilePath()),"FORGE_SOURCE_REQUIRED");
        require(file.getMods().size()==1 && file.getMods().getFirst()==info,"MODFILE_OWNER_AMBIGUOUS");
        require(file.moduleName()!=null && !file.moduleName().isBlank(),"MODULE_IDENTITY_MISSING");
        require(DualForgeLanguageLoader.forgeEntrypoints(info,scan).equals(List.of(entry.replace('/','.'))),"ENTRY_METADATA_CONFLICT");
        Map<String,byte[]> raw=new TreeMap<>();Map<String,ClassNode> nodes=new TreeMap<>();
        for(var c:scan.getClasses()) {
            String name=c.clazz().getInternalName();require(!raw.containsKey(name),"DUPLICATE_CLASS");
            try {byte[] b=Files.readAllBytes(file.getFile().findResource(name+".class"));ClassNode n=read(b);require(n.name.equals(name),"CLASS_RESOURCE_IDENTITY");raw.put(name,b);nodes.put(name,n);}
            catch(java.io.IOException e){throw new IllegalStateException("Registry helper rejected: CLASS_RESOURCE_MISSING "+name,e);}
        }
        return prepare(info,entry,raw,nodes,host);
    }
    private static ForgeRegistryTransformationPlan prepare(IModInfo info,String entry,Map<String,byte[]> raw,Map<String,ClassNode> nodes,java.util.function.Function<String,byte[]> host) {
        require(nodes.containsKey(entry),"ENTRY_ABSENT_FROM_SCAN");
        ControlledItemSupplier suppliers=new ControlledItemSupplier(nodes,host);
        Set<String> helpers=new HashSet<>();Map<String,String> regs=new TreeMap<>();Set<String> ids=new HashSet<>();Map<String,Kind> kinds=new TreeMap<>();
        ClassNode e=nodes.get(entry);
        for(MethodNode m:e.methods)for(var i:m.instructions)if(i instanceof MethodInsnNode c && c.getOpcode()==Opcodes.INVOKESTATIC && c.desc.equals("("+BUS+")V")) {
            require(!c.itf && nodes.containsKey(c.owner) && !entry.equals(c.owner),"HELPER_OUTSIDE_MODFILE");
            require(helpers.add(key(c.owner,c.name,c.desc)),"DUPLICATE_TARGET");
            require(!regs.containsKey(c.owner),"DUPLICATE_HELPER_ATTACHMENT");
            regs.put(c.owner,c.name);kinds.put(c.owner,kind(nodes.get(c.owner)));
        }
        require(!helpers.isEmpty() || ControlledForgeGlobalBus.isCandidate(e),"NO_REGISTRY_HELPERS");
        ControlledCreativeTab tabs=new ControlledCreativeTab(nodes,kinds,suppliers);
        for(String owner:new TreeSet<>(regs.keySet()))regs.put(owner,validateHelper(nodes.get(owner),regs.get(owner),info.getModId(),ids,suppliers,kinds.get(owner),tabs));
        Set<String> closure=new HashSet<>(regs.keySet());closure.addAll(tabs.owners());boolean added;
        do {
            added=false;
            for(ClassNode n:nodes.values()) {
                if(n.name.equals(entry) || closure.contains(n.name))continue;
                boolean related=false;
                for(MethodNode m:n.methods)for(var i:m.instructions) {
                    if(i instanceof FieldInsnNode f && closure.contains(f.owner))related=true;
                    if(i instanceof MethodInsnNode c && closure.contains(c.owner))related=true;
                    if(i instanceof InvokeDynamicInsnNode d)for(Object a:d.bsmArgs)if(a instanceof Handle h && closure.contains(h.getOwner()))related=true;
                    if(i instanceof LdcInsnNode l && l.cst instanceof Handle h && closure.contains(h.getOwner()))related=true;
                }
                if(related){closure.add(n.name);added=true;}
            }
        } while(added);
        for(String name:closure) {
            ClassNode n=nodes.get(name);structure(n);
            if(!regs.containsKey(name))require(n.fields.isEmpty(),"UNVERIFIED_CONSUMER_FIELD");
            for(MethodNode m:n.methods) {
                if(m.name.equals("<init>")){emptyConstructor(n,m);continue;}
                if(regs.containsKey(name) && (m.name.equals("<clinit>") || helpers.contains(key(name,m.name,m.desc)) || supplierMethod(m) || tabs.verified(name,m)))continue;
                if(tabs.verified(name,m))continue;
                validateAccessor(nodes,closure,kinds,name,m,new HashSet<>());
            }
        }
        Set<String> targets=new HashSet<>(closure);require(targets.add(entry),"TRANSFORMATION_RULE_CONFLICT");
        var scope=new ControlledForgeContextProfile.Scope(info.getModId(),Set.copyOf(nodes.keySet()),raw::get);
        var global=ControlledForgeGlobalBus.validate(e,scope,helpers,host);
        return new ForgeRegistryTransformationPlan(info,entry,raw,nodes,targets,regs,helpers,suppliers,global);
    }
    Set<String> targets(){return output.keySet();}
    String registerField(String owner){return registers.get(owner);}
    Kind registryKind(String owner){return kinds.get(owner);}
    static Kind kind(ClassNode n){return n.fields.stream().anyMatch(f->f.desc.equals(R)&&Kind.TAB.generic("DeferredRegister").equals(f.signature))?Kind.TAB:Kind.ITEM;}
    ClassNode transform(ClassNode input, byte[] initialSHA) {
        require(normalized.containsKey(input.name),"UNAUTHORIZED_TARGET");
        if(initialSHA!=null)require(Arrays.equals(initialSHA,MessageDigestHolder.sha(original.get(input.name))),"INPUT_JAR_SHA_CHANGED");
        require(Arrays.equals(bytes(input),normalized.get(input.name)),"TRANSFORMATION_INPUT_CHANGED");
        return read(output.get(input.name));
    }
    private static String validateHelper(ClassNode n,String attach,String modId,Set<String> ids,ControlledItemSupplier suppliers,Kind kind,ControlledCreativeTab tabs) {
        structure(n);List<FieldNode> registers=n.fields.stream().filter(f->f.desc.equals(R)).toList();
        require(registers.size()==1,"ONE_TYPED_ITEM_REGISTER");FieldNode reg=registers.getFirst();
        Set<String> names=new HashSet<>();Set<String> holders=new HashSet<>();
        for(FieldNode f:n.fields) {
            require(names.add(f.name) && f.access==(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC|Opcodes.ACC_FINAL) && f.value==null
                    && empty(f.visibleAnnotations) && empty(f.invisibleAnnotations),"FIELD_METADATA");
            require((f==reg || f.desc.equals(H)) && kind.generic(f==reg?"DeferredRegister":"RegistryObject").equals(f.signature),"ITEM_FIELD_DESCRIPTOR");
            if(f!=reg)holders.add(f.name);
        }
        require(!holders.isEmpty(),"NO_ITEM_DECLARATIONS");
        MethodNode init=method(n,"<clinit>","()V");require(init.access==Opcodes.ACC_STATIC,"INITIALIZER_FLAGS");metadata(init,false);require(init.signature==null,"INITIALIZER_SIGNATURE");
        List<Object> stack=new ArrayList<>();Map<Integer,Object> locals=new HashMap<>();Set<String> assigned=new HashSet<>();Set<String> usedSuppliers=new HashSet<>();boolean created=false,done=false;
        for(var i:ops(init)) {
            require(!done,"INITIALIZER_AFTER_RETURN");
            if(i instanceof LdcInsnNode l){require(l.cst instanceof String,"INITIALIZER_LITERAL");stack.add(l.cst);}
            else if(i instanceof FieldInsnNode f) {
                if(f.owner.equals(F+"ForgeRegistries")) {require(f.getOpcode()==Opcodes.GETSTATIC && kind==Kind.ITEM && f.name.equals("ITEMS") && f.desc.equals("L"+F+"IForgeRegistry;"),"UNKNOWN_REGISTRY_FIELD");stack.add(Value.REGISTRY);}
                else if(f.owner.equals("net/minecraft/core/registries/Registries")){require(kind==Kind.TAB && f.getOpcode()==Opcodes.GETSTATIC && f.name.equals("CREATIVE_MODE_TAB") && f.desc.equals("Lnet/minecraft/resources/ResourceKey;"),"WRONG_TYPED_REGISTRY_KEY");stack.add(Value.REGISTRY);}
                else {
                    require(f.owner.equals(n.name) && names.contains(f.name) && field(n,f.name).desc.equals(f.desc),"FOREIGN_FIELD_REFERENCE");
                    if(f.getOpcode()==Opcodes.GETSTATIC){require(f.name.equals(reg.name) && assigned.contains(f.name),"REGISTER_DATAFLOW");stack.add(Value.REGISTER);}
                    else {require(f.getOpcode()==Opcodes.PUTSTATIC && assigned.add(f.name),"FIELD_ASSIGNMENT");Object v=pop(stack);require(f.name.equals(reg.name)?v.equals(Value.REGISTER):v.equals(Value.HOLDER),"REGISTRY_HOLDER_DATAFLOW");}
                }
            } else if(i instanceof VarInsnNode v) {
                if(v.getOpcode()==Opcodes.ASTORE){require(pop(stack).equals(Value.REGISTER) && !locals.containsKey(v.var),"LOCAL_DATAFLOW");locals.put(v.var,Value.REGISTER);}
                else {require(v.getOpcode()==Opcodes.ALOAD && locals.containsKey(v.var),"LOCAL_DATAFLOW");stack.add(locals.get(v.var));}
            } else if(i instanceof InvokeDynamicInsnNode d) {String impl=kind==Kind.ITEM?supplier(n,d,suppliers):tabs.supplier(n,d);usedSuppliers.add(impl);stack.add(Value.SUPPLIER);}
            else if(i instanceof MethodInsnNode c) {
                if(call(c,Opcodes.INVOKESTATIC,F+"DeferredRegister","create",kind==Kind.ITEM?CREATE:CREATE_KEY)) {require(!created && pop(stack).equals(modId) && pop(stack).equals(Value.REGISTRY),"CREATE_REGISTRY_NAMESPACE");created=true;stack.add(Value.REGISTER);}
                else {require(call(c,Opcodes.INVOKEVIRTUAL,F+"DeferredRegister","register",REGISTER),"UNKNOWN_DEFERRED_REGISTER_DESCRIPTOR");require(pop(stack).equals(Value.SUPPLIER),"SUPPLIER_DATAFLOW");Object id=pop(stack);require(id instanceof String s && s.matches("[a-z0-9_./-]+") && ids.add(kind.id+"/"+modId+":"+id),"DUPLICATE_OR_INVALID_REGISTRY_ID");require(pop(stack).equals(Value.REGISTER),"REGISTER_RECEIVER");stack.add(Value.HOLDER);}
            } else {require(i.getOpcode()==Opcodes.RETURN && stack.isEmpty(),"INITIALIZER_INSTRUCTION");done=true;}
        }
        require(done && created && assigned.equals(names),"INCOMPLETE_INITIALIZER");
        for(MethodNode m:n.methods)if(supplierMethod(m))require(usedSuppliers.contains(m.name),"UNUSED_OR_UNKNOWN_FACTORY");
        MethodNode registration=method(n,attach,"("+BUS+")V");metadata(registration,true);require(registration.signature==null,"ATTACHMENT_SIGNATURE");require(registration.access==(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC),"ATTACHMENT_FLAGS");
        var code=ops(registration);int start=0;
        if(code.size()!=4) {
            require(code.size()==9 && code.get(0) instanceof LdcInsnNode l && "false".equals(l.cst)
                    && code.get(1) instanceof LdcInsnNode k && k.cst instanceof String s && s.matches("[A-Z][A-Z0-9_]+")
                    && call(code.get(2),Opcodes.INVOKESTATIC,"java/lang/System","getenv","(Ljava/lang/String;)Ljava/lang/String;")
                    && call(code.get(3),Opcodes.INVOKEVIRTUAL,"java/lang/String","equals","(Ljava/lang/Object;)Z")
                    && code.get(4) instanceof JumpInsnNode j && j.getOpcode()==Opcodes.IFNE && next(j.label)==code.getLast(),"ATTACHMENT_CONDITION");start=5;
        }
        require(code.get(start) instanceof FieldInsnNode f && f.getOpcode()==Opcodes.GETSTATIC && f.owner.equals(n.name) && f.name.equals(reg.name) && f.desc.equals(R)
                && code.get(start+1) instanceof VarInsnNode v && v.getOpcode()==Opcodes.ALOAD && v.var==0
                && call(code.get(start+2),Opcodes.INVOKEVIRTUAL,F+"DeferredRegister","register","("+BUS+")V") && code.getLast().getOpcode()==Opcodes.RETURN,"ATTACHMENT_RECEIVER_BUS");
        return reg.name;
    }
    private static String supplier(ClassNode n,InvokeDynamicInsnNode d,ControlledItemSupplier suppliers) {
        require(d.bsm.equals(META) && d.name.equals("get") && d.desc.equals("()Ljava/util/function/Supplier;") && d.bsmArgs.length==3
                && Type.getMethodType("()Ljava/lang/Object;").equals(d.bsmArgs[0]) && d.bsmArgs[1] instanceof Handle
                && Type.getMethodType("()L"+ITEM+";").equals(d.bsmArgs[2]),"UNKNOWN_LAMBDA_BOOTSTRAP");
        Handle h=(Handle)d.bsmArgs[1];require(h.getTag()==Opcodes.H_INVOKESTATIC && !h.isInterface() && h.getOwner().equals(n.name) && h.getDesc().equals("()L"+ITEM+";"),"SUPPLIER_CAPTURE_OR_FOREIGN_HANDLE");
        MethodNode m=method(n,h.getName(),h.getDesc());require(supplierMethod(m),"SUPPLIER_FLAGS");metadata(m,false);require(m.signature==null,"SUPPLIER_SIGNATURE");var c=ops(m);int a=0;
        if(c.size()==10 && c.get(0) instanceof FieldInsnNode f && f.getOpcode()==Opcodes.GETSTATIC && f.owner.equals("java/lang/System") && f.name.equals("out") && f.desc.equals("Ljava/io/PrintStream;") && c.get(1) instanceof LdcInsnNode l && l.cst instanceof String && call(c.get(2),Opcodes.INVOKEVIRTUAL,"java/io/PrintStream","println","(Ljava/lang/String;)V"))a=3;
        boolean legacy=c.size()-a==7 && type(c.get(a),Opcodes.NEW,ITEM) && c.get(a+1).getOpcode()==Opcodes.DUP && type(c.get(a+2),Opcodes.NEW,ITEM+"$Properties") && c.get(a+3).getOpcode()==Opcodes.DUP && call(c.get(a+4),Opcodes.INVOKESPECIAL,ITEM+"$Properties","<init>","()V") && call(c.get(a+5),Opcodes.INVOKESPECIAL,ITEM,"<init>","(L"+ITEM+"$Properties;)V") && c.get(a+6).getOpcode()==Opcodes.ARETURN;
        if(!legacy) {require(c.stream().noneMatch(i -> i.getOpcode()==Opcodes.NOP),"SUPPLIER_BUSINESS_UNSUPPORTED");suppliers.verify(n.name,m);}
        return h.getName();
    }
    static Kind holderKind(Map<String,ClassNode> nodes,Map<String,Kind> kinds,String owner,String name) {
        require(kinds.containsKey(owner),"CONSUMER_FIELD_CLOSURE");Kind k=kinds.get(owner);FieldNode f=field(nodes.get(owner),name);
        require(f.desc.equals(H)&&k.generic("RegistryObject").equals(f.signature),"CONSUMER_FIELD_CLOSURE");return k;
    }
    static Kind accessorKind(MethodNode m) {
        for(Kind k:Kind.values())if(m.desc.equals("()"+H)?("()"+k.generic("RegistryObject")).equals(m.signature):m.desc.equals("()L"+k.type+";")&&m.signature==null)return k;
        throw new IllegalStateException("Registry helper rejected: CONSUMER_GENERIC_SIGNATURE");
    }
    static void validateAccessor(Map<String,ClassNode> nodes,Set<String> closure,Map<String,Kind> kinds,String owner,MethodNode m,Set<String> visiting) {
        require(m.access==(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC) && (m.desc.equals("()"+H) || m.desc.equals("()L"+ITEM+";") || m.desc.equals("()L"+TAB+";")),"UNVERIFIED_CONSUMER_METHOD");
        Kind expected=accessorKind(m);metadata(m,false);
        require(visiting.add(key(owner,m.name,m.desc)),"CYCLIC_CONSUMER");List<String> stack=new ArrayList<>();boolean done=false;
        for(var i:ops(m)) {
            require(!done,"CONSUMER_AFTER_RETURN");
            if(i instanceof FieldInsnNode f){require(f.getOpcode()==Opcodes.GETSTATIC && closure.contains(f.owner) && f.desc.equals(H),"CONSUMER_FIELD_CLOSURE");stack.add("holder:"+holderKind(nodes,kinds,f.owner,f.name).type);}
            else if(i instanceof MethodInsnNode c) {
                if(call(c,Opcodes.INVOKEVIRTUAL,F+"RegistryObject","get","()Ljava/lang/Object;")){String h=pop(stack);require(h.startsWith("holder:"),"HOLDER_RECEIVER");stack.add(h.substring(7));}
                else {require(c.getOpcode()==Opcodes.INVOKESTATIC && !c.itf && closure.contains(c.owner),"CONSUMER_METHOD_CLOSURE");MethodNode target=method(nodes.get(c.owner),c.name,c.desc);validateAccessor(nodes,closure,kinds,c.owner,target,visiting);stack.add((c.desc.equals("()"+H)?"holder:":"")+accessorKind(target).type);}
            } else if(type(i,Opcodes.CHECKCAST,ITEM)||type(i,Opcodes.CHECKCAST,TAB)){require(!stack.isEmpty() && stack.getLast().equals(((TypeInsnNode)i).desc),"CONSUMER_CHECKCAST");}
            else {require(i.getOpcode()==Opcodes.ARETURN && stack.size()==1 && pop(stack).equals((m.desc.equals("()"+H)?"holder:":"")+expected.type),"CONSUMER_RETURN");done=true;}
        }
        require(done,"CONSUMER_MISSING_RETURN");visiting.remove(key(owner,m.name,m.desc));
    }
    private static String mappedGeneric(String s) {for(Kind k:Kind.values())if(k.generic("RegistryObject").equals(s))return "L"+N+"DeferredHolder<L"+k.type+";L"+k.type+";>;";throw new IllegalStateException("Registry helper rejected: UNVERIFIED_GENERIC_REWRITE");}
    private static void rewrite(ClassNode n,boolean helper,String fingerprint) {
        Kind typed=kind(n);
        for(FieldNode f:n.fields) {if(f.desc.equals(R)){f.desc="L"+N+"DeferredRegister;";f.signature="L"+N+"DeferredRegister<L"+typed.type+";>;";}else if(f.desc.equals(H)){f.desc="L"+N+"DeferredHolder;";f.signature=mappedGeneric(f.signature);}}
        for(MethodNode m:n.methods) {
            if(m.desc.equals("()"+H)){m.desc="()L"+N+"DeferredHolder;";m.signature="()"+mappedGeneric(m.signature.substring(2));}
            for(var i:m.instructions.toArray()) {
                if(i instanceof FieldInsnNode f) {
                    if(f.owner.equals(F+"ForgeRegistries")){f.owner="net/minecraft/core/registries/BuiltInRegistries";f.name="ITEM";f.desc="Lnet/minecraft/core/DefaultedRegistry;";}
                    else if(f.desc.equals(R))f.desc="L"+N+"DeferredRegister;";
                    else if(f.desc.equals(H))f.desc="L"+N+"DeferredHolder;";
                } else if(i instanceof MethodInsnNode c) {
                    if(c.owner.equals(F+"DeferredRegister")) {
                        if(c.desc.equals("("+BUS+")V")) {
                            m.instructions.insertBefore(c,new LdcInsnNode(fingerprint));c.setOpcode(Opcodes.INVOKESTATIC);c.owner=BRIDGE;c.name="register";c.desc="(Ljava/lang/Object;"+BUS+"Ljava/lang/String;)V";c.itf=false;m.maxStack++;
                        } else {c.owner=N+"DeferredRegister";c.desc=c.desc.equals(CREATE_KEY)?"(Lnet/minecraft/resources/ResourceKey;Ljava/lang/String;)L"+N+"DeferredRegister;":c.desc.equals(CREATE)?"(Lnet/minecraft/core/Registry;Ljava/lang/String;)L"+N+"DeferredRegister;":"(Ljava/lang/String;Ljava/util/function/Supplier;)L"+N+"DeferredHolder;";}
                    } else if(c.owner.equals(F+"RegistryObject"))c.owner=N+"DeferredHolder";
                    else if(c.desc.equals("()"+H))c.desc="()L"+N+"DeferredHolder;";
                }
            }
        }
    }
    static void structure(ClassNode n) {
        require(n.version==Opcodes.V21 && (n.access==(Opcodes.ACC_PUBLIC|Opcodes.ACC_FINAL|Opcodes.ACC_SUPER) || n.access==(Opcodes.ACC_PUBLIC|Opcodes.ACC_SUPER)) && n.superName.equals("java/lang/Object") && n.interfaces.isEmpty() && n.signature==null && n.module==null && empty(n.visibleAnnotations) && empty(n.invisibleAnnotations),"HELPER_CLASS_METADATA");
        require(!n.name.startsWith("java/") && !n.name.startsWith("net/minecraft/") && !n.name.startsWith("net/minecraftforge/") && !n.name.startsWith("net/neoforged/"),"PLATFORM_CLASS_EXCLUDED");
        Set<String> unique=new HashSet<>();for(MethodNode m:n.methods)require(unique.add(m.name+m.desc),"DUPLICATE_METHOD");
    }
    static void metadata(MethodNode m,boolean frames) {
        require(m.tryCatchBlocks.isEmpty() && empty(m.exceptions) && empty(m.visibleAnnotations) && empty(m.invisibleAnnotations),"METHOD_METADATA");
        for(var i:m.instructions)if(i instanceof FrameNode f)require(frames && f.type==Opcodes.F_SAME && empty(f.local) && empty(f.stack),"FRAME_NOT_VERIFIED");
        require(m.localVariables==null || m.localVariables.stream().allMatch(v->!v.desc.contains(F+"DeferredRegister") && !v.desc.contains(F+"RegistryObject")),"LOCAL_TYPE_CLOSURE");
    }
    private static void emptyConstructor(ClassNode n,MethodNode m) {metadata(m,false);var c=ops(m);require(m.desc.equals("()V") && (m.access==Opcodes.ACC_PRIVATE || m.access==Opcodes.ACC_PUBLIC) && c.size()==3 && c.get(0) instanceof VarInsnNode v && v.getOpcode()==Opcodes.ALOAD && v.var==0 && call(c.get(1),Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V") && c.get(2).getOpcode()==Opcodes.RETURN,"HELPER_CONSTRUCTOR");}
    private static boolean supplierMethod(MethodNode m){return m.desc.equals("()L"+ITEM+";") && (m.access==(Opcodes.ACC_PRIVATE|Opcodes.ACC_STATIC) || m.access==(Opcodes.ACC_PRIVATE|Opcodes.ACC_STATIC|Opcodes.ACC_SYNTHETIC));}
    static String key(String owner,String name,String desc){return owner+"."+name+desc;}
    static FieldNode field(ClassNode n,String name){return n.fields.stream().filter(f->f.name.equals(name)).findFirst().orElseThrow(()->new IllegalStateException("Registry helper rejected: MISSING_FIELD"));}
    static MethodNode method(ClassNode n,String name,String desc){return n.methods.stream().filter(m->m.name.equals(name)&&m.desc.equals(desc)).findFirst().orElseThrow(()->new IllegalStateException("Registry helper rejected: MISSING_METHOD_CLOSURE"));}
    private static boolean empty(Collection<?> c){return c==null || c.isEmpty();}
    static List<AbstractInsnNode> ops(MethodNode m){return Arrays.stream(m.instructions.toArray()).filter(i->i.getOpcode()>=0).toList();}
    private static AbstractInsnNode next(AbstractInsnNode i){while(i!=null && i.getOpcode()<0)i=i.getNext();return i;}
    private static <T> T pop(List<T> s){require(!s.isEmpty(),"STACK_UNDERFLOW");return s.removeLast();}
    private static boolean type(AbstractInsnNode i,int op,String type){return i instanceof TypeInsnNode t && t.getOpcode()==op && t.desc.equals(type);}
    static boolean call(AbstractInsnNode i,int op,String owner,String name,String desc){return i instanceof MethodInsnNode m && m.getOpcode()==op && !m.itf && m.owner.equals(owner) && m.name.equals(name) && m.desc.equals(desc);}
    static ClassNode read(byte[] b){ClassNode n=new ClassNode();new ClassReader(b).accept(n,0);return n;}
    static byte[] bytes(ClassNode n){ClassWriter w=new ClassWriter(0);n.accept(w);return w.toByteArray();}
    static String digest(byte[] b){return HexFormat.of().formatHex(MessageDigestHolder.sha(b));}
    private static class MessageDigestHolder {static byte[] sha(byte[] b){try{return MessageDigest.getInstance("SHA-256").digest(b);}catch(java.security.NoSuchAlgorithmException e){throw new AssertionError(e);}}}
    static void require(boolean b,String why){if(!b)throw new IllegalStateException("Registry helper rejected: "+why);}
}
