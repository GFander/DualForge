package io.github.gfander.dualforge;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;

/** Stack/local/type-category validation without defining any source or game classes. */
final class RegistryBytecodeVerifier {
    private RegistryBytecodeVerifier() { }
    static void verify(ClassNode node) {
        for (MethodNode method : node.methods) {
            try { new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name,method); }
            catch (AnalyzerException failure) {
                throw new IllegalStateException("Registry helper rejected: BYTECODE_FRAME_VERIFICATION "
                        +node.name+"."+method.name,failure);
            }
        }
    }
}
