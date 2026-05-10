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
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2025/10/4 18:00
 */
public class BeforeLineInjector implements BytecodeInjector {
    private static final Logger LOGGER = LoggerFactory.getLogger(BeforeLineInjector.class);

    @Override
    public byte[] inject(InjectionContext injectionContext, byte[] bytecode, BytecodeAssembler bytecodeAssembler) {
        AsmInjectionContext asmContext = new AsmInjectionContext(injectionContext, bytecode);

        LineNumberTarget target = (LineNumberTarget) asmContext.getInjectionTarget();
        List<AsmInjectionContext.LocalVarInfo> visibleVars = LocalVariableScanner.scanVisibleLocalVariables(
                bytecode, target.getMethodName(), target.getMethodDescriptor(), target.getLineNumber(), true);
        asmContext.setLocalVariables(visibleVars);

        ClassNode cn = new ClassNode();
        new ClassReader(bytecode).accept(cn, ClassReader.SKIP_FRAMES);

        boolean injected = false;
        for (MethodNode mn : cn.methods) {
            if (!mn.name.equals(target.getMethodName())) continue;
            if (target.getMethodDescriptor() != null && !target.getMethodDescriptor().isEmpty()
                    && !mn.desc.equals(target.getMethodDescriptor())) continue;

            asmContext.setMethodAccess(mn.access);
            asmContext.setMaxLocals(mn.maxLocals);

            LOGGER.debug("[BeforeLine] method={} maxLocals={} visibleVars={}", mn.name, mn.maxLocals, visibleVars.size());

            AbstractInsnNode insn = mn.instructions.getFirst();
            while (insn != null) {
                if (insn instanceof LineNumberNode) {
                    LineNumberNode lnn = (LineNumberNode) insn;
                    if (lnn.line == target.getLineNumber()) {
                        InsnList injectedCode = TreeApiBytecodeHelper.assemble(asmContext, bytecode, bytecodeAssembler);

                        int maxVarInCode = computeMaxLocalIndex(asmContext, injectedCode);
                        int neededLocals = Math.max(mn.maxLocals, maxVarInCode + 1);
                        if (neededLocals > mn.maxLocals) {
                            LOGGER.debug("[BeforeLine] Updating maxLocals from {} to {}", mn.maxLocals, neededLocals);
                            mn.maxLocals = neededLocals;
                        }

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

        removeFrameNodes(cn);

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        ClassWriter cw = new ClassLoaderAwareClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES, loader);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private int computeMaxLocalIndex(AsmInjectionContext asmContext, InsnList code) {
        int maxIndex = 0;
        for (AbstractInsnNode insn = code.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof VarInsnNode) {
                VarInsnNode vin = (VarInsnNode) insn;
                int size = 1;
                switch (vin.getOpcode()) {
                    case org.objectweb.asm.Opcodes.LLOAD:
                    case org.objectweb.asm.Opcodes.LSTORE:
                    case org.objectweb.asm.Opcodes.DLOAD:
                    case org.objectweb.asm.Opcodes.DSTORE:
                        size = 2;
                        break;
                    default:
                        break;
                }
                maxIndex = Math.max(maxIndex, vin.var + size);
            }
        }
        int contextVarIndex = asmContext.getMaxLocals() > 0 ? asmContext.getMaxLocals() : 0;
        maxIndex = Math.max(maxIndex, contextVarIndex + 1);
        return maxIndex;
    }

    private void removeFrameNodes(ClassNode cn) {
        for (MethodNode mn : cn.methods) {
            AbstractInsnNode insn = mn.instructions.getFirst();
            while (insn != null) {
                AbstractInsnNode next = insn.getNext();
                if (insn instanceof FrameNode) {
                    mn.instructions.remove(insn);
                }
                insn = next;
            }
        }
    }
}
