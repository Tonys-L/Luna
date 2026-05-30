package fun.efto.luna.core.plugin.builtin.line;

import fun.efto.luna.core.injection.InjectionContext;
import fun.efto.luna.core.bytecode.asm.AsmInjectionContext;
import fun.efto.luna.core.bytecode.asm.ClassLoaderAwareClassWriter;
import fun.efto.luna.core.bytecode.asm.LocalVariableScanner;
import fun.efto.luna.core.bytecode.BytecodeAssembler;
import fun.efto.luna.core.injection.target.LineNumberTarget;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjector;
import fun.efto.luna.core.bytecode.asm.injector.TreeApiBytecodeHelper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2025/10/4 18:00
 */
public class AfterLineInjector implements BytecodeInjector {
    private static final Logger LOGGER = LoggerFactory.getLogger(AfterLineInjector.class);

    @Override
    public byte[] inject(InjectionContext injectionContext, byte[] bytecode, BytecodeAssembler bytecodeAssembler) {
        AsmInjectionContext asmContext = new AsmInjectionContext(injectionContext, bytecode);

        LineNumberTarget target = (LineNumberTarget) asmContext.getInjectionTarget();
        List<AsmInjectionContext.LocalVarInfo> visibleVars = LocalVariableScanner.scanVisibleLocalVariables(
                bytecode, target.getMethodName(), target.getMethodDescriptor(), target.getLineNumber());
        asmContext.setLocalVariables(visibleVars);

        ClassNode cn = new ClassNode();
        new ClassReader(bytecode).accept(cn, ClassReader.SKIP_FRAMES);

        boolean injected = false;
        for (MethodNode mn : cn.methods) {
            if (!mn.name.equals(target.getMethodName())) continue;
            if (target.getMethodDescriptor() != null && !target.getMethodDescriptor().isEmpty()
                    && !mn.desc.equals(target.getMethodDescriptor())) continue;

            if (!hasLineNumberTable(mn)) {
                throw new RuntimeException("类缺少调试信息(LineNumberTable)，请使用 -g 或 -g:lines 编译。"
                        + "方法: " + mn.name + mn.desc);
            }

            asmContext.setMethodAccess(mn.access);
            asmContext.setMaxLocals(mn.maxLocals);

            LOGGER.debug("[AfterLine] method={} maxLocals={} visibleVars={}", mn.name, mn.maxLocals, visibleVars.size());

            AbstractInsnNode insn = mn.instructions.getFirst();
            while (insn != null) {
                if (insn instanceof LineNumberNode) {
                    LineNumberNode lnn = (LineNumberNode) insn;
                    if (lnn.line == target.getLineNumber()) {
                        AbstractInsnNode lastInsn = findLastInsnOfLine(mn, lnn);
                        InsnList injectedCode = TreeApiBytecodeHelper.assemble(asmContext, bytecode, bytecodeAssembler);

                        int maxVarInCode = computeMaxLocalIndex(asmContext, injectedCode);
                        int neededLocals = Math.max(mn.maxLocals, maxVarInCode + 1);
                        if (neededLocals > mn.maxLocals) {
                            LOGGER.debug("[AfterLine] Updating maxLocals from {} to {}", mn.maxLocals, neededLocals);
                            mn.maxLocals = neededLocals;
                        }

                        if (lastInsn != null && isReturnOrThrow(lastInsn.getOpcode())) {
                            mn.instructions.insertBefore(lastInsn, injectedCode);
                            LOGGER.debug("[AfterLine] Inserted BEFORE return/throw at line {}", target.getLineNumber());
                        } else if (lastInsn != null) {
                            mn.instructions.insert(lastInsn, injectedCode);
                            LOGGER.debug("[AfterLine] Inserted AFTER last instruction at line {}", target.getLineNumber());
                        } else {
                            mn.instructions.insertBefore(lnn, injectedCode);
                            LOGGER.debug("[AfterLine] Inserted before LineNumberNode at line {}", target.getLineNumber());
                        }
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
                    case Opcodes.LLOAD:
                    case Opcodes.LSTORE:
                    case Opcodes.DLOAD:
                    case Opcodes.DSTORE:
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

    private AbstractInsnNode findLastInsnOfLine(MethodNode mn, LineNumberNode startLnn) {
        AbstractInsnNode insn = startLnn;
        AbstractInsnNode lastRealInsn = null;

        while (insn != null) {
            if (insn instanceof LineNumberNode && insn != startLnn) {
                break;
            }
            if (!(insn instanceof LineNumberNode) && insn.getOpcode() != -1) {
                lastRealInsn = insn;
            }
            insn = insn.getNext();
        }

        return lastRealInsn;
    }

    private boolean isReturnOrThrow(int opcode) {
        return opcode == Opcodes.RETURN || opcode == Opcodes.IRETURN
                || opcode == Opcodes.LRETURN || opcode == Opcodes.FRETURN
                || opcode == Opcodes.DRETURN || opcode == Opcodes.ARETURN
                || opcode == Opcodes.ATHROW;
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

    private boolean hasLineNumberTable(MethodNode mn) {
        for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof LineNumberNode) {
                return true;
            }
        }
        return false;
    }
}
