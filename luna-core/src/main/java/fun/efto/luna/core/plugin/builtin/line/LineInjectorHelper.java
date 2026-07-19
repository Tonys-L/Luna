package fun.efto.luna.core.plugin.builtin.line;

import fun.efto.luna.core.bytecode.asm.AsmInjectionContext;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * 行级注入器公共工具方法。
 *
 * @author : Tony.L(286269159@qq.com)
 * @since : 2026/06/11 00:30
 */
public final class LineInjectorHelper {

    private LineInjectorHelper() {}

    public static int computeMaxLocalIndex(AsmInjectionContext asmContext, InsnList code) {
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

    public static void removeFrameNodes(ClassNode cn) {
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

    public static boolean hasLineNumberTable(MethodNode mn) {
        for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof LineNumberNode) {
                return true;
            }
        }
        return false;
    }

    public static boolean isReturnOrThrow(int opcode) {
        return opcode == Opcodes.RETURN || opcode == Opcodes.IRETURN
                || opcode == Opcodes.LRETURN || opcode == Opcodes.FRETURN
                || opcode == Opcodes.DRETURN || opcode == Opcodes.ARETURN
                || opcode == Opcodes.ATHROW;
    }
}
