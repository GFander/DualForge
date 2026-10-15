import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** ClassFile shapes absent from FnF still belong in the scanner's discovery surface. */
public class ScannerContract {
    static int checks;
    static void check(boolean b){if(!b)throw new AssertionError();checks++;}
    public static void main(String[] args){
        ClassNode c=new ClassNode();c.name="research/Condy";c.version=Opcodes.V21;c.access=Opcodes.ACC_PUBLIC|Opcodes.ACC_SUPER;c.superName="java/lang/Object";
        MethodNode m=new MethodNode(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"read","()Ljava/lang/Object;",null,null);
        Handle bsm=new Handle(Opcodes.H_INVOKESTATIC,"java/lang/invoke/ConstantBootstraps","nullConstant",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;",false);
        m.instructions.add(new LdcInsnNode(new ConstantDynamic("value","Ljava/lang/String;",bsm)));
        m.instructions.add(new InsnNode(Opcodes.ARETURN));m.maxStack=1;c.methods.add(m);
        ClassNode parsed=OfflinePrototype.node(OfflinePrototype.bytes(c));Atlas.Usage usage=new Atlas.Usage(parsed,"test");usage.scan();
        check(usage.refs.stream().anyMatch(x->((Map<?,?>)x).get("owner").equals("java/lang/invoke/ConstantBootstraps")));
        check(usage.refs.stream().anyMatch(x->((Map<?,?>)x).get("context").equals("constantDynamicBootstrap:handle")));
        check(usage.refs.stream().anyMatch(x->((Map<?,?>)x).get("owner").equals("java/lang/String")));
        ClassNode mod=new ClassNode();mod.name="module-info";mod.version=Opcodes.V21;mod.access=Opcodes.ACC_MODULE;mod.module=new ModuleNode("research.module",0,"1");
        mod.module.requires=new ArrayList<>(List.of(new ModuleRequireNode("java.base",0,null)));
        mod.module.exports=new ArrayList<>(List.of(new ModuleExportNode("research",0,null)));
        Object module=Atlas.abi(OfflinePrototype.node(OfflinePrototype.bytes(mod)),"module","module-info.class").get("module");
        check(Atlas.JSON.toJson(module).contains("research.module"));check(Atlas.JSON.toJson(module).contains("java.base"));check(Atlas.JSON.toJson(module).contains("exports"));
        MethodNode bridge=new MethodNode(Opcodes.ACC_PUBLIC|Opcodes.ACC_BRIDGE|Opcodes.ACC_SYNTHETIC,"bridge","()Ljava/lang/Object;",null,null);
        c.methods.add(bridge);String encoded=Atlas.JSON.toJson(Atlas.abi(c,"test","Condy.class"));
        check(encoded.contains("\"bridge\":true"));check(encoded.contains("\"synthetic\":true"));
        check(Atlas.JSON.toJson(Atlas.constant(Double.NEGATIVE_INFINITY)).contains("fff0000000000000"));
        System.out.println("SCANNER_CONTRACT_PASS assertions="+checks+"; metadata only, dynamic constants NOT EXECUTED");
    }
}
