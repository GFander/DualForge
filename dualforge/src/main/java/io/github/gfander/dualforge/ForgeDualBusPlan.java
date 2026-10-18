package io.github.gfander.dualforge;

import java.nio.file.Files;
import java.util.*;
import net.neoforged.neoforgespi.language.IModInfo;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Closed constructor profile with independently verified initializer/config segments. Plans are validated and rendered before publication. */
final class ForgeDualBusPlan {
    static final String F = "net/minecraftforge/", BUS = F+"eventbus/api/IEventBus", B = "L"+BUS+";";
    static final String CTX = F+"fml/ModLoadingContext", JAVA = F+"fml/javafmlmod/FMLJavaModLoadingContext";
    static final String SPEC = F+"common/ForgeConfigSpec", BUILDER = SPEC+"$Builder";
    static final String TYPE = F+"fml/config/ModConfig$Type", MARKER = F+"fml/event/IModBusEvent";
    static final String PAIR = "(Ljava/lang/Class;Ljava/util/function/Consumer;)V";
    static final String BRIDGE = "io/github/gfander/dualforge/ForgeDualBusBridge", ANCHOR = "dualforge$busSession";
    static final List<String> EVENTS = List.of(F+"fml/event/lifecycle/FMLCommonSetupEvent",
        F+"event/server/ServerStartedEvent", F+"event/entity/living/LivingDeathEvent");
    static final Map<String,String> TYPES;
    static {
        Map<String,String> m = new HashMap<>();
        m.put(BUS,"net/neoforged/bus/api/IEventBus");m.put(F+"eventbus/api/Event","net/neoforged/bus/api/Event");
        m.put(F+"eventbus/api/EventPriority","net/neoforged/bus/api/EventPriority");m.put(MARKER,"net/neoforged/fml/event/IModBusEvent");
        m.put(CTX,"java/lang/Object");m.put(JAVA,"java/lang/Object");
        m.put(SPEC,"net/neoforged/neoforge/common/ModConfigSpec");
        m.put(BUILDER,m.get(SPEC)+"$Builder");m.put(SPEC+"$BooleanValue",m.get(SPEC)+"$BooleanValue");
        m.put(F+"fml/config/ModConfig","net/neoforged/fml/config/ModConfig");
        m.put(F+"fml/config/IConfigSpec","net/neoforged/fml/config/IConfigSpec");m.put(TYPE,"net/neoforged/fml/config/ModConfig$Type");
        for(String e:EVENTS)m.put(e,e.replace(F+"fml/","net/neoforged/fml/").replace(F+"event/","net/neoforged/neoforge/event/"));
        TYPES = Map.copyOf(m);
    }
    record Listener(String event, String field, String callback, boolean canceled) { }
    final String modId, module, entry, router, config, configType, fingerprint;
    final List<Listener> listeners;
    private final Map<String,byte[]> raw, normalized, output;
    private ForgeDualBusPlan(String id,String module,String entry,String router,String config,String configType,
            List<Listener> listeners,Map<String,byte[]> source) {
        this.modId=id;this.module=module;this.entry=entry;this.router=router;this.config=config;this.configType=configType;
        this.listeners=List.copyOf(listeners);
        Map<String,byte[]> copy=new TreeMap<>();source.forEach((k,v)->copy.put(k,v.clone()));raw=Collections.unmodifiableMap(copy);
        StringBuilder identity=new StringBuilder("dual-bus-boolean-config-v1\n"+id+"\n"+module+"\n");
        raw.forEach((k,v)->identity.append(k).append(':').append(digest(v)).append('\n'));
        fingerprint=digest(identity.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Map<String,byte[]> n=new TreeMap<>(), o=new TreeMap<>();
        for(var row:raw.entrySet()) {
            ClassNode node=read(row.getValue());n.put(row.getKey(),bytes(node));rewrite(node);
            RegistryBytecodeVerifier.verify(node);o.put(row.getKey(),bytes(node));
        }
        normalized=Map.copyOf(n);output=Map.copyOf(o);
    }
    static boolean isCandidate(ClassNode n) {
        boolean game=false,mod=false,paired=false;
        for(var m:n.methods)for(var op:m.instructions){
            if(op instanceof FieldInsnNode f && f.owner.equals(F+"common/MinecraftForge") && f.name.equals("EVENT_BUS"))game=true;
            if(op instanceof MethodInsnNode c && c.owner.equals(JAVA) && c.name.equals("getModEventBus"))mod=true;
            if(op instanceof MethodInsnNode c && c.getOpcode()==Opcodes.INVOKESPECIAL && c.name.equals("<init>") && c.desc.equals("("+B+B+")V"))paired=true;
        }
        return game && mod && paired;
    }
    static ForgeDualBusPlan create(IModInfo info,String entry) {
        var file=info.getOwningFile();var scan=file.getFile().getScanResult();
        require(scan!=null && ForgeModLocator.isForgeCandidate(file.getFile().getFilePath()),"WRONG_MODFILE");
        require(file.getMods().size()==1 && file.getMods().getFirst()==info,"WRONG_MOD_CONTAINER");
        require(!Files.exists(file.getFile().findResource("module-info.class")) && !Files.exists(file.getFile().findResource("META-INF/versions")),"WRONG_MODULE");
        require(DualForgeLanguageLoader.forgeEntrypoints(info,scan).equals(List.of(entry.replace('/','.'))),"WRONG_LISTENER_OWNER");
        Map<String,byte[]> raw=new TreeMap<>();
        try { for(var c:scan.getClasses())require(raw.putIfAbsent(c.clazz().getInternalName(),Files.readAllBytes(file.getFile().findResource(c.clazz().getInternalName()+".class")))==null,"DUPLICATE_CLASS"); }
        catch(java.io.IOException e){throw new IllegalStateException("Dual bus source unavailable",e);}
        return validate(info.getModId(),file.moduleName(),entry,raw);
    }
    static ForgeDualBusPlan validate(String id,String module,String entry,Map<String,byte[]> raw) {
        return validate(id,module,entry,raw,ForgeEventSemantics::hostBytes);
    }
    static ForgeDualBusPlan validate(String id,String module,String entry,Map<String,byte[]> raw,java.util.function.Function<String,byte[]> host) {
        require(id!=null && !id.isBlank() && module!=null && !module.isBlank(),"WRONG_MOD_CONTAINER");
        Map<String,ClassNode> nodes=new TreeMap<>();raw.forEach((k,v)->{ClassNode n=read(v);require(k.equals(n.name),"WRONG_MODFILE");nodes.put(k,n);});
        ClassNode e=nodes.get(entry);require(e!=null,"WRONG_LISTENER_OWNER");structure(e);
        require(e.visibleAnnotations!=null && e.visibleAnnotations.size()==1 && e.visibleAnnotations.getFirst().desc.equals("L"+F+"fml/common/Mod;")
            && e.visibleAnnotations.getFirst().values.equals(List.of("value",id)),"WRONG_MOD_CONTAINER");
        Cursor c=new Cursor(method(e,"<init>","()V"));require(c.method.access==Opcodes.ACC_PUBLIC,"UNSUPPORTED_BOOTSTRAP");c.base();
        var locale=ForgeConstructorSegments.locale(c,nodes);
        c.field(Opcodes.GETSTATIC,F+"common/MinecraftForge","EVENT_BUS",B);c.var(Opcodes.ASTORE,1);
        c.call(Opcodes.INVOKESTATIC,JAVA,"get","()L"+JAVA+";");c.call(Opcodes.INVOKEVIRTUAL,JAVA,"getModEventBus","()"+B);c.var(Opcodes.ASTORE,2);
        if(locale!=null && c.peek() instanceof TypeInsnNode)rejectLocalBoundary(c,nodes,host);
        c.var(Opcodes.ALOAD,0);String router=c.allocation(nodes);c.op(Opcodes.DUP);c.var(Opcodes.ALOAD,1);c.var(Opcodes.ALOAD,2);
        c.call(Opcodes.INVOKESPECIAL,router,"<init>","("+B+B+")V");String routerField=c.ownField(Opcodes.PUTFIELD,e,"L"+router+";",true);
        c.var(Opcodes.ALOAD,0);c.call(Opcodes.INVOKESTATIC,CTX,"get","()L"+CTX+";");
        MethodInsnNode configCall=c.ownedCall(nodes);String config=configCall.owner;
        String configResult=Type.getReturnType(configCall.desc).getInternalName();
        require(configCall.getOpcode()==Opcodes.INVOKESTATIC && configCall.desc.equals("(L"+CTX+";)L"+configResult+";"),"CONFIG_SPEC_UNSUPPORTED");
        c.ownField(Opcodes.PUTFIELD,e,"L"+configResult+";",true);
        List<Listener> listeners=new ArrayList<>();Set<String> fields=new HashSet<>(), callbacks=new HashSet<>();
        while(c.peek() instanceof VarInsnNode && c.remaining()>1) {
            int saved=c.pos;c.var(Opcodes.ALOAD,0);
            if(!(c.peek() instanceof VarInsnNode)){c.pos=saved;break;}
            c.var(Opcodes.ALOAD,0);AbstractInsnNode op=c.next();
            require(op instanceof InvokeDynamicInsnNode,"WRONG_CONSUMER_TYPE");InvokeDynamicInsnNode d=(InvokeDynamicInsnNode)op;
            require(d.bsm.equals(ForgeRegistryTransformationPlan.META) && d.name.equals("accept") && d.desc.equals("(L"+entry+";)Ljava/util/function/Consumer;")
                && d.bsmArgs.length==3 && d.bsmArgs[0].equals(Type.getMethodType("(Ljava/lang/Object;)V")) && d.bsmArgs[1] instanceof Handle,"UNSUPPORTED_BOOTSTRAP");
            Handle h=(Handle)d.bsmArgs[1];require(h.getTag()==Opcodes.H_INVOKEVIRTUAL && !h.isInterface() && h.getOwner().equals(entry),"WRONG_LISTENER_OWNER");
            Type[] params=Type.getArgumentTypes(h.getDesc());require(params.length==1 && params[0].getSort()==Type.OBJECT && EVENTS.contains(params[0].getInternalName())
                && h.getDesc().equals("("+params[0].getDescriptor()+")V") && d.bsmArgs[2].equals(Type.getMethodType(h.getDesc())),"WRONG_EVENT_CLASS");
            String field=c.ownField(Opcodes.PUTFIELD,e,"Ljava/util/function/Consumer;",true);
            require(fields.add(field) && callbacks.add(h.getName()+h.getDesc()) && method(e,h.getName(),h.getDesc()).access==Opcodes.ACC_PRIVATE,"DUPLICATE_LISTENER");
            listeners.add(new Listener(params[0].getInternalName(),field,h.getName(),false));
        }
        require(!listeners.isEmpty(),"WRONG_CONSUMER_TYPE");
        for(int i=0;i<listeners.size();i++) {
            Listener l=listeners.get(i);c.var(Opcodes.ALOAD,0);c.field(Opcodes.GETFIELD,entry,routerField,"L"+router+";");c.literal(Type.getObjectType(l.event));
            c.var(Opcodes.ALOAD,0);c.field(Opcodes.GETFIELD,entry,l.field,"Ljava/util/function/Consumer;");
            MethodInsnNode call=c.ownedCall(nodes);require(call.getOpcode()==Opcodes.INVOKEVIRTUAL && call.owner.equals(router) && call.desc.equals(PAIR),"WRONG_REGISTRATION_ORDER");
            boolean cancel=routerMethod(nodes.get(router),call.name,false);require(!cancel || l.event.equals(EVENTS.get(2)),"UNVERIFIED_RECEIVE_CANCELED");listeners.set(i,new Listener(l.event,l.field,l.callback,cancel));
        }
        c.op(Opcodes.RETURN);c.end();
        require(e.methods.size()==listeners.size()+1,"UNKNOWN_CONSTRUCTOR_EFFECT");
        Set<String> closure=new HashSet<>(List.of(entry,router,config));
        if(locale!=null)closure.addAll(locale.classes());
        if(!configResult.equals(config)){validateConfigInterface(nodes.get(config),nodes.get(configResult));closure.add(configResult);}
        require(raw.keySet().equals(closure) && !router.equals(config),"UNCLAIMED_CLASS_MODIFIED");
        validateRouter(nodes.get(router));String configType=validateConfig(nodes.get(config),configCall.name,configResult);
        Set<String> allowed=new HashSet<>(TYPES.keySet());allowed.add(F+"common/MinecraftForge");allowed.add(F+"fml/common/Mod");
        for(var n:nodes.values()) {
            if((locale!=null && locale.classes().contains(n.name)) || (!configResult.equals(config) && n.name.equals(configResult)))continue;
            structure(n,n.name.equals(config));
            for(var f:n.fields)require(!f.name.equals(ANCHOR),"UNKNOWN_CONSTRUCTOR_EFFECT");
            for(var m:n.methods)for(var op:m.instructions) {
                if(op instanceof MethodInsnNode call && call.owner.startsWith(F) && n==e) {
                    boolean ctor=m.name.equals("<init>");
                    require(ctor || call.owner.equals(EVENTS.get(2)) && (call.name.equals("setCanceled") && call.desc.equals("(Z)V")
                        || call.name.equals("isCanceled") && call.desc.equals("()Z")),"UNSUPPORTED_EVENT_SEMANTICS");
                }
                require(!(op instanceof LdcInsnNode l && (l.cst instanceof ConstantDynamic || l.cst instanceof Handle)),"UNSUPPORTED_BOOTSTRAP");
                if(op instanceof InvokeDynamicInsnNode d && !m.name.equals("<init>"))require(d.bsm.equals(new Handle(Opcodes.H_INVOKESTATIC,"java/lang/invoke/StringConcatFactory","makeConcatWithConstants",
                    "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;",false))
                    && d.name.equals("makeConcatWithConstants") && d.bsmArgs.length==1 && d.bsmArgs[0] instanceof String,"UNSUPPORTED_BOOTSTRAP");
            }
            var matcher=java.util.regex.Pattern.compile("net/minecraftforge/[A-Za-z0-9_/$]+").matcher(new String(raw.get(n.name),java.nio.charset.StandardCharsets.ISO_8859_1));
            while(matcher.find())require(allowed.contains(matcher.group()),"UNSUPPORTED_FULL_ENTRY_API "+matcher.group());
        }
        ControlledForgeGlobalBus.verifyHostBus();ForgeEventSemantics.verifyHost(host);
        return new ForgeDualBusPlan(id,module,entry,router,config,configType,listeners,raw);
    }
    private static void rejectLocalBoundary(Cursor c,Map<String,ClassNode> nodes,java.util.function.Function<String,byte[]> host) {
        String router=c.allocation(nodes);c.op(Opcodes.DUP);c.var(Opcodes.ALOAD,1);c.var(Opcodes.ALOAD,2);
        c.call(Opcodes.INVOKESPECIAL,router,"<init>","("+B+B+")V");c.var(Opcodes.ASTORE,3);validateRouter(nodes.get(router));
        c.call(Opcodes.INVOKESTATIC,CTX,"get","()L"+CTX+";");c.var(Opcodes.ASTORE,4);c.var(Opcodes.ALOAD,4);
        MethodInsnNode call=c.ownedCall(nodes);require(call.getOpcode()==Opcodes.INVOKESTATIC,"CONFIG_OWNER_MISMATCH");
        String result=Type.getReturnType(call.desc).getInternalName();require(call.desc.equals("(L"+CTX+";)L"+result+";"),"CONFIG_OWNER_MISMATCH");
        ClassNode config=nodes.get(call.owner);if(!result.equals(call.owner))validateConfigInterface(config,nodes.get(result));
        String type=validateConfig(config,call.name,result);c.var(Opcodes.ASTORE,5);
        ControlledForgeGlobalBus.verifyHostBus();ForgeEventSemantics.verifyHost(host);
        String boundary="PARTIAL_CONSTRUCTOR_PLAN";
        if(c.peek() instanceof TypeInsnNode allocation && allocation.getOpcode()==Opcodes.NEW) {
            ClassNode next=nodes.get(allocation.desc);require(next!=null,"CROSS_MODFILE_INITIALIZER");
            boolean network=next.methods.stream().flatMap(m->ForgeRegistryTransformationPlan.ops(m).stream()).anyMatch(op->op instanceof MethodInsnNode m && m.owner.startsWith(F+"network/"));
            boundary=network?"UNVERIFIED_NETWORK_INITIALIZATION":"UNKNOWN_INITIALIZER_SIDE_EFFECT";
        } else if(c.peek() instanceof FieldInsnNode f && f.owner.equals(F+"api/distmarker/Dist"))boundary="UNVERIFIED_DIST_INITIALIZATION";
        throw new IllegalStateException("Dual bus rejected: "+boundary+" locale=STATIC_VERIFIED busIdentity=STATIC_VERIFIED router=STATIC_VERIFIED config="+type
            +" eventSemantics=BLOCKED_NO_EXPLICIT_SUBSCRIPTIONS combined=REJECTED_NO_TARGETS nextOpcode="+c.peek().getOpcode()
            +(c.peek() instanceof TypeInsnNode t?" nextType="+t.desc:""));
    }
    private static void validateRouter(ClassNode n) {
        structure(n);require(n.fields.size()==2 && n.methods.size()>=2 && n.methods.size()<=4,"UNKNOWN_BUS_ORIGIN");
        Cursor c=new Cursor(method(n,"<init>","("+B+B+")V"));c.base();c.var(Opcodes.ALOAD,0);c.var(Opcodes.ALOAD,1);
        String game=c.ownField(Opcodes.PUTFIELD,n,B,true);c.var(Opcodes.ALOAD,0);c.var(Opcodes.ALOAD,2);String mod=c.ownField(Opcodes.PUTFIELD,n,B,true);
        require(!game.equals(mod),"FOREIGN_MOD_BUS");c.op(Opcodes.RETURN);c.end();
        for(var m:n.methods)if(!m.name.equals("<init>")) {
            if(m.desc.equals("()"+B)){Cursor getter=new Cursor(m);require(m.access==Opcodes.ACC_PUBLIC,"WRONG_LISTENER_OWNER");getter.var(Opcodes.ALOAD,0);getter.field(Opcodes.GETFIELD,n.name,mod,B);getter.op(Opcodes.ARETURN);getter.end();continue;}
            routerMethod(n,m.name,m.desc.equals(PAIR) && m.instructions.iterator().hasNext()
            && ForgeRegistryTransformationPlan.ops(m).stream().anyMatch(op->op instanceof MethodInsnNode call && call.name.equals("unregister")));
        }
    }
    private static boolean routerMethod(ClassNode n,String name,boolean remove) {
        Cursor c=new Cursor(method(n,name,PAIR));require(c.method.access==Opcodes.ACC_PUBLIC,"WRONG_LISTENER_OWNER");c.literal(Type.getObjectType(MARKER));c.var(Opcodes.ALOAD,1);
        c.call(Opcodes.INVOKEVIRTUAL,"java/lang/Class","isAssignableFrom","(Ljava/lang/Class;)Z");LabelNode other=c.jump(Opcodes.IFEQ);
        boolean canceled=false;LabelNode end=null;
        for(int branch=0;branch<2;branch++) {
            if(branch==1)require(next(other)==c.peek(),"WRONG_EVENT_DOMAIN");
            c.var(Opcodes.ALOAD,0);String field=c.ownField(Opcodes.GETFIELD,n,B,true);
            Cursor ctor=new Cursor(method(n,"<init>","("+B+B+")V"));var list=ctor.code;
            require(field.equals(((FieldInsnNode)list.get(branch==0?7:4)).name),"WRONG_EVENT_DOMAIN");
            if(!remove) {
                c.field(Opcodes.GETSTATIC,F+"eventbus/api/EventPriority","NORMAL","L"+F+"eventbus/api/EventPriority;");
                int code=c.next().getOpcode();require(code==Opcodes.ICONST_0 || code==Opcodes.ICONST_1,"UNVERIFIED_RECEIVE_CANCELED");
                if(branch==0)canceled=code==Opcodes.ICONST_1;else require(canceled==(code==Opcodes.ICONST_1),"UNVERIFIED_RECEIVE_CANCELED");
                c.var(Opcodes.ALOAD,1);c.var(Opcodes.ALOAD,2);c.call(Opcodes.INVOKEINTERFACE,BUS,"addListener","(L"+F+"eventbus/api/EventPriority;ZLjava/lang/Class;Ljava/util/function/Consumer;)V");
            } else {c.var(Opcodes.ALOAD,2);c.call(Opcodes.INVOKEINTERFACE,BUS,"unregister","(Ljava/lang/Object;)V");}
            if(branch==0)end=c.jump(Opcodes.GOTO);
        }
        require(next(end)==c.peek(),"WRONG_EVENT_DOMAIN");c.op(Opcodes.RETURN);c.end();return canceled;
    }
    static String validateConfig(ClassNode n,String register,String result) {
        structure(n,true);require(n.interfaces.isEmpty()==result.equals(n.name),"UNSUPPORTED_CONFIG_INTERFACE");
        require(n.fields.size()>0 && n.methods.size()==n.fields.size()+2,"CONFIG_SPEC_UNSUPPORTED");
        Cursor c=new Cursor(method(n,register,"(L"+CTX+";)L"+result+";"));
        require(c.method.access==(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC),"CONFIG_OWNER_MISMATCH");
        c.type(Opcodes.NEW,BUILDER);c.op(Opcodes.DUP);c.call(Opcodes.INVOKESPECIAL,BUILDER,"<init>","()V");c.var(Opcodes.ASTORE,1);
        c.type(Opcodes.NEW,n.name);c.op(Opcodes.DUP);c.var(Opcodes.ALOAD,1);c.call(Opcodes.INVOKESPECIAL,n.name,"<init>","(L"+BUILDER+";)V");c.var(Opcodes.ASTORE,2);
        c.var(Opcodes.ALOAD,1);c.call(Opcodes.INVOKEVIRTUAL,BUILDER,"build","()L"+SPEC+";");c.var(Opcodes.ASTORE,3);c.var(Opcodes.ALOAD,0);
        AbstractInsnNode op=c.next();require(op instanceof FieldInsnNode f && f.getOpcode()==Opcodes.GETSTATIC && f.owner.equals(TYPE) && f.desc.equals("L"+TYPE+";")
            && List.of("COMMON","SERVER","CLIENT").contains(f.name),"CONFIG_SPEC_UNSUPPORTED");String type=((FieldInsnNode)op).name;
        c.var(Opcodes.ALOAD,3);c.call(Opcodes.INVOKEVIRTUAL,CTX,"registerConfig","(L"+TYPE+";L"+F+"fml/config/IConfigSpec;)V");c.var(Opcodes.ALOAD,2);c.op(Opcodes.ARETURN);c.end();
        c=new Cursor(method(n,"<init>","(L"+BUILDER+";)V"));require(c.method.access==Opcodes.ACC_PRIVATE,"CONFIG_SPEC_UNSUPPORTED");c.base();
        c.var(Opcodes.ALOAD,1);c.string();c.call(Opcodes.INVOKEVIRTUAL,BUILDER,"push","(Ljava/lang/String;)L"+BUILDER+";");c.op(Opcodes.POP);
        Set<String> fields=new HashSet<>(),keys=new HashSet<>(),getters=new HashSet<>();
        for(int i=0;i<n.fields.size();i++) {
            c.var(Opcodes.ALOAD,1);c.string();c.call(Opcodes.INVOKEVIRTUAL,BUILDER,"comment","(Ljava/lang/String;)L"+BUILDER+";");c.op(Opcodes.POP);
            c.var(Opcodes.ALOAD,0);c.var(Opcodes.ALOAD,1);require(keys.add(c.string()),"CONFIG_SPEC_UNSUPPORTED");
            int value=c.next().getOpcode();require(value==Opcodes.ICONST_0 || value==Opcodes.ICONST_1,"CONFIG_SPEC_UNSUPPORTED");
            c.call(Opcodes.INVOKEVIRTUAL,BUILDER,"define","(Ljava/lang/String;Z)L"+SPEC+"$BooleanValue;");require(fields.add(c.ownField(Opcodes.PUTFIELD,n,"Ljava/util/function/Supplier;",true)),"CONFIG_SPEC_UNSUPPORTED");
        }
        c.var(Opcodes.ALOAD,1);c.call(Opcodes.INVOKEVIRTUAL,BUILDER,"pop","()L"+BUILDER+";");c.op(Opcodes.POP);c.op(Opcodes.RETURN);c.end();
        for(var m:n.methods)if(!m.name.equals(register) && !m.name.equals("<init>")) {
            require(m.access==Opcodes.ACC_PUBLIC && m.desc.equals("()Z"),"CONFIG_SPEC_UNSUPPORTED");c=new Cursor(m);c.var(Opcodes.ALOAD,0);
            require(getters.add(c.ownField(Opcodes.GETFIELD,n,"Ljava/util/function/Supplier;",true)),"CONFIG_SUPPLIER_DATAFLOW_MISMATCH");
            c.call(Opcodes.INVOKEINTERFACE,"java/util/function/Supplier","get","()Ljava/lang/Object;");c.type(Opcodes.CHECKCAST,"java/lang/Boolean");
            c.call(Opcodes.INVOKEVIRTUAL,"java/lang/Boolean","booleanValue","()Z");c.op(Opcodes.IRETURN);c.end();
        }
        require(getters.equals(fields),"CONFIG_SUPPLIER_DATAFLOW_MISMATCH");
        return type;
    }
    static void validateConfigInterface(ClassNode config,ClassNode api) {
        require(config!=null && api!=null && config.interfaces.equals(List.of(api.name)),"UNSUPPORTED_CONFIG_INTERFACE");
        require(api.version==Opcodes.V21 && api.access==(Opcodes.ACC_PUBLIC|Opcodes.ACC_ABSTRACT|Opcodes.ACC_INTERFACE)
            && api.superName.equals("java/lang/Object") && api.interfaces.isEmpty() && api.fields.isEmpty() && api.methods.size()==config.fields.size()
            && api.visibleAnnotations==null && api.invisibleAnnotations==null && api.nestHostClass==null && api.outerClass==null
            && (api.nestMembers==null || api.nestMembers.isEmpty()) && !api.name.startsWith("java/") && !api.name.startsWith("javax/")
            && !api.name.startsWith("net/minecraft") && !api.name.startsWith("net/neoforged/") && !api.name.startsWith("io/github/gfander/dualforge/"),"UNSUPPORTED_CONFIG_INTERFACE");
        Set<String> names=new HashSet<>();
        for(var m:api.methods)require(names.add(m.name) && m.access==(Opcodes.ACC_PUBLIC|Opcodes.ACC_ABSTRACT) && m.desc.equals("()Z")
            && m.instructions.size()==0 && m.annotationDefault==null && config.methods.stream().anyMatch(c->c.name.equals(m.name) && c.desc.equals(m.desc) && c.access==Opcodes.ACC_PUBLIC),"UNSUPPORTED_CONFIG_INTERFACE");
    }
    private void rewrite(ClassNode n) {
        if(n.name.equals(entry) || n.name.equals(router))n.fields.add(new FieldNode(Opcodes.ACC_PRIVATE|Opcodes.ACC_SYNTHETIC,ANCHOR,"Ljava/lang/Object;",null,null));
        for(var m:n.methods) {
            if(m.name.equals("<init>") && n.name.equals(entry)) {
                InsnList guard=new InsnList();guard.add(new VarInsnNode(Opcodes.ALOAD,0));guard.add(new VarInsnNode(Opcodes.ALOAD,0));guard.add(new LdcInsnNode(fingerprint));
                guard.add(bridge("begin","(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;"));guard.add(new FieldInsnNode(Opcodes.PUTFIELD,n.name,ANCHOR,"Ljava/lang/Object;"));
                m.instructions.insert(ForgeRegistryTransformationPlan.ops(m).get(1),guard);
                for(var op:m.instructions.toArray())if(op.getOpcode()==Opcodes.RETURN){InsnList done=new InsnList();done.add(new VarInsnNode(Opcodes.ALOAD,0));done.add(bridge("complete","(Ljava/lang/Object;)V"));m.instructions.insertBefore(op,done);}
            }
            if(m.name.equals("<init>") && n.name.equals(router))for(var op:m.instructions.toArray())if(op.getOpcode()==Opcodes.RETURN) {
                InsnList bind=new InsnList();bind.add(new VarInsnNode(Opcodes.ALOAD,0));bind.add(new VarInsnNode(Opcodes.ALOAD,0));bind.add(new VarInsnNode(Opcodes.ALOAD,1));bind.add(new VarInsnNode(Opcodes.ALOAD,2));
                bind.add(bridge("bind","(Ljava/lang/Object;Lnet/neoforged/bus/api/IEventBus;Lnet/neoforged/bus/api/IEventBus;)Ljava/lang/Object;"));bind.add(new FieldInsnNode(Opcodes.PUTFIELD,n.name,ANCHOR,"Ljava/lang/Object;"));m.instructions.insertBefore(op,bind);
            }
            for(var op:m.instructions.toArray()) {
                if(op instanceof FieldInsnNode f && f.owner.equals(F+"common/MinecraftForge"))m.instructions.set(op,bridge("game","()Lnet/neoforged/bus/api/IEventBus;"));
                if(op instanceof MethodInsnNode call) {
                    if((call.owner.equals(JAVA) || call.owner.equals(CTX)) && call.name.equals("get"))m.instructions.set(op,bridge("context","()Ljava/lang/Object;"));
                    else if(call.owner.equals(JAVA) && call.name.equals("getModEventBus"))m.instructions.set(op,bridge("mod","(Ljava/lang/Object;)Lnet/neoforged/bus/api/IEventBus;"));
                    else if(call.owner.equals(CTX) && call.name.equals("registerConfig"))m.instructions.set(op,bridge("config","(Ljava/lang/Object;Lnet/neoforged/fml/config/ModConfig$Type;Lnet/neoforged/fml/config/IConfigSpec;)V"));
                    else if(call.owner.equals(BUS)) {
                        InsnList token=new InsnList();if(call.name.equals("unregister"))token.add(new VarInsnNode(Opcodes.ALOAD,1));token.add(new VarInsnNode(Opcodes.ALOAD,0));token.add(new FieldInsnNode(Opcodes.GETFIELD,n.name,ANCHOR,"Ljava/lang/Object;"));m.instructions.insertBefore(op,token);
                        String desc=call.name.equals("addListener")?"(Lnet/neoforged/bus/api/IEventBus;Lnet/neoforged/bus/api/EventPriority;ZLjava/lang/Class;Ljava/util/function/Consumer;Ljava/lang/Object;)V":"(Lnet/neoforged/bus/api/IEventBus;Ljava/lang/Object;Ljava/lang/Class;Ljava/lang/Object;)V";
                        m.instructions.set(op,bridge(call.name,desc));
                    } else if(call.owner.equals(EVENTS.get(2)) && List.of("setCanceled","isCanceled").contains(call.name)) {
                        call.owner="net/neoforged/bus/api/ICancellableEvent";call.setOpcode(Opcodes.INVOKEINTERFACE);call.itf=true;
                    }
                }
            }
            m.maxStack+=6;
        }
        for(var f:n.fields){f.desc=descriptor(f.desc);f.signature=descriptor(f.signature);}
        for(var inner:n.innerClasses){inner.name=type(inner.name);inner.outerName=type(inner.outerName);}
        for(var m:n.methods) {
            m.desc=descriptor(m.desc);m.signature=descriptor(m.signature);
            if(m.localVariables!=null)for(var v:m.localVariables){v.desc=descriptor(v.desc);v.signature=descriptor(v.signature);}
            for(var op:m.instructions) {
                if(op instanceof FieldInsnNode f){f.owner=type(f.owner);f.desc=descriptor(f.desc);}
                if(op instanceof MethodInsnNode call){call.owner=type(call.owner);call.desc=descriptor(call.desc);}
                if(op instanceof TypeInsnNode t)t.desc=type(t.desc);
                if(op instanceof LdcInsnNode l)l.cst=constant(l.cst);
                if(op instanceof InvokeDynamicInsnNode d){d.desc=descriptor(d.desc);for(int i=0;i<d.bsmArgs.length;i++)d.bsmArgs[i]=constant(d.bsmArgs[i]);}
                if(op instanceof FrameNode frame){mapFrame(frame.local);mapFrame(frame.stack);}
            }
        }
    }
    private static String type(String name){return name==null?null:name.startsWith("[")?descriptor(name):TYPES.getOrDefault(name,name);}
    private static String descriptor(String value){if(value==null)return null;for(var e:TYPES.entrySet())value=value.replace("L"+e.getKey()+";","L"+e.getValue()+";").replace("L"+e.getKey()+"<","L"+e.getValue()+"<");return value;}
    private static Object constant(Object v){if(v instanceof Type t)return Type.getType(descriptor(t.getDescriptor()));if(v instanceof Handle h)return new Handle(h.getTag(),type(h.getOwner()),h.getName(),descriptor(h.getDesc()),h.isInterface());return v;}
    private static void mapFrame(List<Object> frame){if(frame!=null)for(int i=0;i<frame.size();i++)if(frame.get(i) instanceof String s)frame.set(i,type(s));}

    Set<String> targets(){return raw.keySet();}
    void rejectForeignClasses(Set<String> foreign){require(Collections.disjoint(raw.keySet(),foreign),"CROSS_MODFILE_CONFLICT");}
    ClassNode transform(ClassNode n,byte[] sha){require(raw.containsKey(n.name),"UNCLAIMED_CLASS_MODIFIED");require(sha==null || Arrays.equals(sha,HexFormat.of().parseHex(digest(raw.get(n.name)))),"INPUT_SHA_CHANGED");require(Arrays.equals(bytes(n),normalized.get(n.name)),"PARTIAL_TRANSFORM_REJECTED");return read(output.get(n.name));}
    private static MethodInsnNode bridge(String name,String desc){return new MethodInsnNode(Opcodes.INVOKESTATIC,BRIDGE,name,desc,false);}
    static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Dual bus rejected: "+why);}
    static ClassNode read(byte[] b){return ForgeRegistryTransformationPlan.read(b);}static byte[] bytes(ClassNode n){return ForgeRegistryTransformationPlan.bytes(n);}static String digest(byte[] b){return ForgeRegistryTransformationPlan.digest(b);}
    static MethodNode method(ClassNode n,String name,String desc){return n.methods.stream().filter(m->m.name.equals(name)&&m.desc.equals(desc)).findFirst().orElseThrow(()->new IllegalStateException("Dual bus rejected: MISSING_METHOD "+name+desc));}
    private static AbstractInsnNode next(AbstractInsnNode n){do{n=n.getNext();}while(n!=null&&n.getOpcode()<0);return n;}
    private static void structure(ClassNode n){structure(n,false);}
    private static void structure(ClassNode n,boolean configInterface){RegistryBytecodeVerifier.verify(n);
        require(!n.name.startsWith("java/") && !n.name.startsWith("javax/") && !n.name.startsWith("net/minecraft/")
            && !n.name.startsWith("net/minecraftforge/") && !n.name.startsWith("net/neoforged/") && !n.name.startsWith("io/github/gfander/dualforge/"),"WRONG_MODFILE");
        Set<String> fieldNames=new HashSet<>(),methods=new HashSet<>();
        for(var f:n.fields)require(fieldNames.add(f.name),"WRONG_LISTENER_OWNER");
        for(var m:n.methods)require(methods.add(m.name+m.desc),"WRONG_LISTENER_OWNER");
        require((n.access== (Opcodes.ACC_PUBLIC|Opcodes.ACC_SUPER) || n.access==(Opcodes.ACC_PUBLIC|Opcodes.ACC_SUPER|Opcodes.ACC_FINAL)),"WRONG_LISTENER_OWNER");
        require(n.version==Opcodes.V21 && n.superName.equals("java/lang/Object") && (n.interfaces.isEmpty() || configInterface && n.interfaces.size()==1 && !n.fields.isEmpty() && n.fields.stream().allMatch(f->f.desc.equals("Ljava/util/function/Supplier;")))
        && n.outerClass==null && n.nestHostClass==null && (n.nestMembers==null||n.nestMembers.isEmpty()) && n.fields.stream().noneMatch(f->f.name.equals(ANCHOR)),"UNKNOWN_CONSTRUCTOR_EFFECT");
        for(var m:n.methods)require(m.tryCatchBlocks.isEmpty() && m.annotationDefault==null && (m.access&(Opcodes.ACC_NATIVE|Opcodes.ACC_ABSTRACT|Opcodes.ACC_SYNCHRONIZED))==0,"UNKNOWN_CONSTRUCTOR_EFFECT");}
    static final class Cursor {
        final MethodNode method;final List<AbstractInsnNode> code;int pos;
        Cursor(MethodNode m){method=m;code=ForgeRegistryTransformationPlan.ops(m);}
        int remaining(){return code.size()-pos;}AbstractInsnNode peek(){require(pos<code.size(),"UNKNOWN_CONSTRUCTOR_EFFECT");return code.get(pos);}AbstractInsnNode next(){var n=peek();pos++;return n;}
        void op(int c){require(next().getOpcode()==c,"UNKNOWN_CONSTRUCTOR_EFFECT opcode="+c);}
        void var(int c,int slot){require(next() instanceof VarInsnNode v&&v.getOpcode()==c&&v.var==slot,"UNKNOWN_CONSTRUCTOR_EFFECT local="+slot);}
        void call(int c,String o,String n,String d){require(next() instanceof MethodInsnNode m&&m.getOpcode()==c&&m.owner.equals(o)&&m.name.equals(n)&&m.desc.equals(d)&&m.itf==(c==Opcodes.INVOKEINTERFACE),"UNKNOWN_CONSTRUCTOR_EFFECT call="+o+"."+n);}
        void field(int c,String o,String n,String d){var op=next();require(op instanceof FieldInsnNode,"UNKNOWN_CONSTRUCTOR_EFFECT expectedField="+o+"."+n+" actualOpcode="+op.getOpcode()+(op instanceof TypeInsnNode t?" actualType="+t.desc:""));
            FieldInsnNode f=(FieldInsnNode)op;require(f.getOpcode()==c&&f.owner.equals(o)&&f.name.equals(n)&&f.desc.equals(d),"UNKNOWN_BUS_ORIGIN field="+o+"."+n);}
        String ownField(int c,ClassNode n,String d,boolean fin){var op=next();require(op instanceof FieldInsnNode f&&f.getOpcode()==c&&f.owner.equals(n.name)&&f.desc.equals(d),"WRONG_LISTENER_OWNER");var f=(FieldInsnNode)op;
            require(n.fields.stream().anyMatch(v->v.name.equals(f.name)&&v.desc.equals(d)&&v.access==(Opcodes.ACC_PRIVATE|(fin?Opcodes.ACC_FINAL:0))),"WRONG_LISTENER_OWNER");return f.name;}
        void type(int c,String t){require(next() instanceof TypeInsnNode n&&n.getOpcode()==c&&n.desc.equals(t),"CONFIG_SPEC_UNSUPPORTED");}
        String allocation(Map<String,ClassNode> nodes){var n=next();require(n instanceof TypeInsnNode t&&t.getOpcode()==Opcodes.NEW&&nodes.containsKey(t.desc),"UNKNOWN_CONSTRUCTOR_EFFECT");return ((TypeInsnNode)n).desc;}
        MethodInsnNode ownedCall(Map<String,ClassNode> nodes){var n=next();require(n instanceof MethodInsnNode m&&!m.itf&&nodes.containsKey(m.owner),"UNKNOWN_CONSTRUCTOR_EFFECT");return (MethodInsnNode)n;}
        void literal(Object v){require(next() instanceof LdcInsnNode l&&l.cst.equals(v),"WRONG_EVENT_CLASS");}
        String string(){var n=next();require(n instanceof LdcInsnNode l&&l.cst instanceof String,"CONFIG_SPEC_UNSUPPORTED");return (String)((LdcInsnNode)n).cst;}
        LabelNode jump(int c){var n=next();require(n instanceof JumpInsnNode j&&j.getOpcode()==c,"WRONG_EVENT_DOMAIN");return ((JumpInsnNode)n).label;}
        void base(){var(Opcodes.ALOAD,0);call(Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V");}
        void end(){require(pos==code.size(),"UNKNOWN_CONSTRUCTOR_EFFECT trailing");}
    }
}
