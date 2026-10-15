package io.github.gfander.dualforge;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** Synthetic stack contracts only; never represented as native-Forge fixture or GAME evidence. */
public final class BytecodeVerifierContract {
    private static int assertions;
    public static void main(String[] args) {
        ClassNode valid=node(Opcodes.ICONST_1,Opcodes.IRETURN);RegistryBytecodeVerifier.verify(valid);assertions++;
        reject("underflow",node(Opcodes.POP,Opcodes.ICONST_1,Opcodes.IRETURN));
        reject("wrong return category",node(Opcodes.ACONST_NULL,Opcodes.IRETURN));
        reject("arithmetic category",node(Opcodes.ACONST_NULL,Opcodes.ICONST_1,Opcodes.IADD,Opcodes.IRETURN));
        reject("wide value category",node(Opcodes.LCONST_0,Opcodes.ICONST_1,Opcodes.IADD,Opcodes.IRETURN));
        ClassNode noStack=node(Opcodes.ICONST_1,Opcodes.IRETURN);noStack.methods.getFirst().maxStack=0;reject("stack capacity",noStack);
        ClassNode local=node(Opcodes.ICONST_1,Opcodes.IRETURN);local.methods.getFirst().instructions.insert(new VarInsnNode(Opcodes.ILOAD,0));reject("invalid local",local);
        ClassNode incomplete=node(Opcodes.ICONST_1);reject("fallthrough",incomplete);
        ClassNode array=node(Opcodes.ACONST_NULL,Opcodes.ICONST_0,Opcodes.IASTORE,Opcodes.ICONST_1,Opcodes.IRETURN);reject("array stack shape",array);
        RegistryBytecodeVerifier.verify(node(Opcodes.ICONST_1,Opcodes.ICONST_2,Opcodes.IADD,Opcodes.IRETURN));assertions++;
        System.out.println("BYTECODE VERIFIER CONTRACT PASS assertions="+assertions+"; synthetic ASM only");
    }
    private static ClassNode node(int...code){ClassNode n=new ClassNode();n.version=Opcodes.V21;n.access=Opcodes.ACC_PUBLIC;n.name="synthetic/Probe";n.superName="java/lang/Object";MethodNode m=new MethodNode(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"value","()I",null,null);for(int i:code)m.instructions.add(new InsnNode(i));m.maxStack=3;m.maxLocals=0;n.methods.add(m);return n;}
    private static void reject(String label,ClassNode n){try{RegistryBytecodeVerifier.verify(n);throw new AssertionError("Accepted "+label);}catch(IllegalStateException expected){if(!expected.getMessage().contains("BYTECODE_FRAME_VERIFICATION"))throw new AssertionError(expected);assertions++;System.out.println("REJECT "+label);}}
}
