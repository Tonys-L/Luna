package fun.efto.luna.asm.injector.visitor;

import fun.efto.luna.asm.AsmInjectionContext;
import fun.efto.luna.core.bytecode.BytecodeAssembler;
import org.objectweb.asm.MethodVisitor;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 0:58
 */
public class EnterMethodVisitor extends MethodVisitor {
    private final BytecodeAssembler bytecodeAssembler;
    private AsmInjectionContext context;

    public EnterMethodVisitor(int api, AsmInjectionContext context, BytecodeAssembler bytecodeAssembler) {
        super(api, context.getMethodVisitor());
        this.bytecodeAssembler = bytecodeAssembler;
        this.context = context;
    }

    @Override
    public void visitCode() {
        bytecodeAssembler.assemble(context, context.getBytecode());
        super.visitCode();
    }
}
