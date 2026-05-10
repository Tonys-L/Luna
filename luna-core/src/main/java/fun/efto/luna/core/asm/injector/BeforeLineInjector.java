package fun.efto.luna.core.asm.injector;

import fun.efto.luna.core.InjectionContext;
import fun.efto.luna.core.asm.AsmInjectionContext;
import fun.efto.luna.core.asm.ClassLoaderAwareClassWriter;
import fun.efto.luna.core.asm.LocalVariableScanner;
import fun.efto.luna.core.bytecode.BytecodeAssembler;
import fun.efto.luna.core.injection.target.LineNumberTarget;
import fun.efto.luna.core.injector.BytecodeInjector;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2025/10/4 18:00
 */
public class BeforeLineInjector implements BytecodeInjector {

    @Override
    public byte[] inject(InjectionContext injectionContext, byte[] bytecode, BytecodeAssembler bytecodeAssembler) {
        AsmInjectionContext asmContext = new AsmInjectionContext(injectionContext, bytecode);

        LineNumberTarget target = (LineNumberTarget) asmContext.getInjectionTarget();
        List<AsmInjectionContext.LocalVarInfo> visibleVars = LocalVariableScanner.scanVisibleLocalVariables(
                bytecode, target.getMethodName(), target.getMethodDescriptor(), target.getLineNumber());
        asmContext.setLocalVariables(visibleVars);

        ClassNode cn = new ClassNode();
        new ClassReader(bytecode).accept(cn, ClassReader.EXPAND_FRAMES);

        boolean injected = false;
        for (MethodNode mn : cn.methods) {
            if (!mn.name.equals(target.getMethodName())) continue;
            if (target.getMethodDescriptor() != null && !target.getMethodDescriptor().isEmpty()
                    && !mn.desc.equals(target.getMethodDescriptor())) continue;

            asmContext.setMethodAccess(mn.access);

            AbstractInsnNode insn = mn.instructions.getFirst();
            while (insn != null) {
                if (insn instanceof LineNumberNode) {
                    LineNumberNode lnn = (LineNumberNode) insn;
                    if (lnn.line == target.getLineNumber()) {
                        InsnList injectedCode = TreeApiBytecodeHelper.assemble(asmContext, bytecode, bytecodeAssembler);
                        mn.instructions.insertBefore(lnn, injectedCode);
                        injected = true;
                        break;
                    }
                }
                insn = insn.getNext();
            }
            if (injected) break;
        }

        if (!injected) {
            throw new RuntimeException("line " + target.getLineNumber() + " not found in method " + target.getMethodName());
        }

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        ClassWriter cw = new ClassLoaderAwareClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES, loader);
        cn.accept(cw);
        return cw.toByteArray();
    }
}
