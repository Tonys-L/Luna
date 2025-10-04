package fun.efto.luna.asm.injector.visitor;

import fun.efto.luna.asm.AsmInjectionContext;
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
        if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
            bytecodeAssembler.assemble(context, context.getBytecode());
        }
        super.visitInsn(opcode);
    }
}
