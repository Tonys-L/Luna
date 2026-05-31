package fun.efto.luna.core.bytecode.asm;

import fun.efto.luna.core.bytecode.asm.AsmInjectionContext;
import fun.efto.luna.core.bytecode.asm.ClassLoaderAwareClassWriter;
import fun.efto.luna.core.bytecode.asm.LocalVariableScanner;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjector;
import fun.efto.luna.core.bytecode.asm.injector.TreeApiBytecodeHelper;
import fun.efto.luna.core.injection.InjectionContext;
import fun.efto.luna.core.injection.code.CompiledCode;
import fun.efto.luna.core.injection.target.MethodTarget;
import fun.efto.luna.core.plugin.ProbeHandler;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;

public class AsmMethodExpressionInjector implements BytecodeInjector {

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/01 00:20
 */
    public enum Phase {
        ENTER, EXIT, AROUND
    }

    private final Phase phase;

    public AsmMethodExpressionInjector(Phase phase) {
        this.phase = phase;
    }

    @Override
    public byte[] inject(CompiledCode compiledCode, ProbeHandler probeHandler, InjectionContext injectionContext, byte[] bytecode) {
        AsmInjectionContext asmContext = new AsmInjectionContext(injectionContext, bytecode);

        MethodTarget target = (MethodTarget) asmContext.getInjectionTarget();
        String targetMethodName = target.getMethodName();
        String targetMethodDesc = target.getMethodDescriptor();

        List<AsmInjectionContext.LocalVarInfo> vars = scanMethodParameters(bytecode, targetMethodName, targetMethodDesc);
        asmContext.setLocalVariables(vars);

        ClassNode cn = new ClassNode();
        new ClassReader(bytecode).accept(cn, ClassReader.SKIP_FRAMES);

        for (MethodNode mn : cn.methods) {
            if (!mn.name.equals(targetMethodName)) continue;
            if (targetMethodDesc != null && !targetMethodDesc.isEmpty() && !mn.desc.equals(targetMethodDesc)) continue;

            asmContext.setMethodAccess(mn.access);
            asmContext.setMaxLocals(mn.maxLocals);

            if (phase == Phase.ENTER || phase == Phase.AROUND) {
                InsnList code = TreeApiBytecodeHelper.assemble(asmContext, compiledCode, probeHandler, bytecode);
                updateMaxLocals(mn, code, asmContext);
                mn.instructions.insert(code);
            }
            if (phase == Phase.EXIT || phase == Phase.AROUND) {
                InsnList code = TreeApiBytecodeHelper.assemble(asmContext, compiledCode, probeHandler, bytecode);
                updateMaxLocals(mn, code, asmContext);
                insertBeforeReturns(mn, code);
            }
        }

        removeFrameNodes(cn);

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        ClassWriter cw = new ClassLoaderAwareClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES, loader);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private List<AsmInjectionContext.LocalVarInfo> scanMethodParameters(byte[] bytecode, String methodName, String methodDesc) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytecode).accept(cn, ClassReader.EXPAND_FRAMES);

        for (MethodNode mn : cn.methods) {
            if (!mn.name.equals(methodName)) continue;
            if (methodDesc != null && !methodDesc.isEmpty() && !mn.desc.equals(methodDesc)) continue;

            List<AsmInjectionContext.LocalVarInfo> result = new ArrayList<>();
            if (mn.localVariables == null) return result;

            if (phase == Phase.ENTER) {
                int paramSlotCount = computeParamSlotCount(mn.access, mn.desc);
                for (LocalVariableNode lv : mn.localVariables) {
                    if (lv.index < paramSlotCount) {
                        result.add(new AsmInjectionContext.LocalVarInfo(lv.name, lv.desc, lv.index));
                    }
                }
            } else {
                for (LocalVariableNode lv : mn.localVariables) {
                    result.add(new AsmInjectionContext.LocalVarInfo(lv.name, lv.desc, lv.index));
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    private int computeParamSlotCount(int access, String desc) {
        int slot = 0;
        if ((access & Opcodes.ACC_STATIC) == 0) {
            slot++;
        }
        Type[] argTypes = Type.getArgumentTypes(desc);
        for (Type argType : argTypes) {
            slot += argType.getSize();
        }
        return slot;
    }

    private void insertBeforeReturns(MethodNode mn, InsnList code) {
        List<AbstractInsnNode> returnInsns = new ArrayList<>();
        for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (isReturnOpcode(insn.getOpcode())) {
                returnInsns.add(insn);
            }
        }
        for (AbstractInsnNode ret : returnInsns) {
            InsnList copy = cloneInsnList(code);
            mn.instructions.insertBefore(ret, copy);
        }
    }

    private boolean isReturnOpcode(int opcode) {
        return opcode == Opcodes.RETURN || opcode == Opcodes.IRETURN || opcode == Opcodes.LRETURN
                || opcode == Opcodes.FRETURN || opcode == Opcodes.DRETURN || opcode == Opcodes.ARETURN;
    }

    private InsnList cloneInsnList(InsnList original) {
        InsnList copy = new InsnList();
        for (AbstractInsnNode insn = original.getFirst(); insn != null; insn = insn.getNext()) {
            copy.add(insn.clone(null));
        }
        return copy;
    }

    private void updateMaxLocals(MethodNode mn, InsnList code, AsmInjectionContext asmContext) {
        int maxVarInCode = 0;
        for (AbstractInsnNode insn = code.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof VarInsnNode) {
                VarInsnNode vin = (VarInsnNode) insn;
                int size = (vin.getOpcode() == Opcodes.LLOAD || vin.getOpcode() == Opcodes.LSTORE
                        || vin.getOpcode() == Opcodes.DLOAD || vin.getOpcode() == Opcodes.DSTORE) ? 2 : 1;
                maxVarInCode = Math.max(maxVarInCode, vin.var + size);
            }
        }
        int contextVarIndex = asmContext.getMaxLocals() > 0 ? asmContext.getMaxLocals() : 0;
        maxVarInCode = Math.max(maxVarInCode, contextVarIndex + 1);
        if (maxVarInCode > mn.maxLocals) {
            mn.maxLocals = maxVarInCode;
        }
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
