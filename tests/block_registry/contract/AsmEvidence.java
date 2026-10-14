package io.github.gfander.dualforge.test;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
/** Produces actual ASM nodes; does not load or modify the inspected Forge class. */
public final class AsmEvidence {
    public static void main(String[] args) throws Exception {
        for (int p=0;p<args.length;p+=2) {
            try (JarFile jar=new JarFile(args[p])) {
                ClassNode n=new ClassNode();new ClassReader(jar.getInputStream(jar.getJarEntry(args[p+1]))).accept(n,0);
                System.out.println("CLASS "+n.name+" access="+n.access+" super="+n.superName);
                for(FieldNode f:n.fields)System.out.println("FIELD "+f.name+" desc="+f.desc+" signature="+f.signature+" access="+f.access);
                for(MethodNode m:n.methods) {
                    System.out.println("METHOD "+m.name+m.desc+" access="+m.access);
                    int index=0;
                    for(AbstractInsnNode i:m.instructions) {
                        String detail=i.getClass().getSimpleName()+" opcode="+i.getOpcode();
                        if(i instanceof FieldInsnNode f)detail+=" "+f.owner+"."+f.name+" "+f.desc;
                        if(i instanceof MethodInsnNode c)detail+=" "+c.owner+"."+c.name+c.desc+" itf="+c.itf;
                        if(i instanceof VarInsnNode v)detail+=" local="+v.var;
                        if(i instanceof LdcInsnNode c)detail+=" value="+c.cst;
                        if(i instanceof TypeInsnNode t)detail+=" type="+t.desc;
                        if(i instanceof InvokeDynamicInsnNode d)detail+=" "+d.name+d.desc+" BSM="+d.bsm+" ARGS="+Arrays.toString(d.bsmArgs);
                        System.out.println("  NODE "+index+++" "+detail);
                    }
                }
            }
        }
    }
}
