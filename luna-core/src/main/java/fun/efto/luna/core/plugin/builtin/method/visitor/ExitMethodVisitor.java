package fun.efto.luna.core.plugin.builtin.method.visitor;

import fun.efto.luna.core.asm.AsmInjectionContext;
import fun.efto.luna.core.bytecode.BytecodeAssembler;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 0:58
 */
public class ExitMethodVisitor extends MethodVisitor {
    private final BytecodeAssembler bytecodeAssembler;
    private final AsmInjectionContext context;

    public ExitMethodVisitor(int api, AsmInjectionContext context, BytecodeAssembler bytecodeAssembler) {
        super(api, context.getMethodVisitor());
        this.bytecodeAssembler = bytecodeAssembler;
        this.context = context;
    }

    @Override
    public void visitInsn(int opcode) {
        if (isExitOpcode(opcode)) {
            bytecodeAssembler.assemble(context, context.getBytecode());
        }
        super.visitInsn(opcode);
    }

    /**
     * 判断是否为方法退出字节码（RETURN 系列或 ATHROW）。
     *
     * <p>Java 伪代码对照：
     * <pre>
     * return opcode >= IRETURN && opcode <= RETURN || opcode == ATHROW
     * </pre>
     */
    private static boolean isExitOpcode(int opcode) {
        return (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) || opcode == Opcodes.ATHROW;
    }
}
