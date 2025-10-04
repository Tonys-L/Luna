package fun.efto.luna.asm.injector.visitor;

import fun.efto.luna.asm.AsmInjectionContext;
import fun.efto.luna.core.bytecode.BytecodeAssembler;
import org.objectweb.asm.MethodVisitor;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 1:39
 */
public class AroundMethodVisitor extends MethodVisitor {
    public AroundMethodVisitor(int api, AsmInjectionContext context, BytecodeAssembler bytecodeAssembler) {
        super(api, context.getMethodVisitor());
        EnterMethodVisitor enterMethodVisitor = new EnterMethodVisitor(api, context, bytecodeAssembler);
        AsmInjectionContext asmInjectionContext = new AsmInjectionContext(context, context.getBytecode());
        asmInjectionContext.setMethodVisitor(enterMethodVisitor);
        asmInjectionContext.setClassVisitor(context.getClassVisitor());
        this.mv = new ExitMethodVisitor(api, asmInjectionContext, bytecodeAssembler);
    }

}
