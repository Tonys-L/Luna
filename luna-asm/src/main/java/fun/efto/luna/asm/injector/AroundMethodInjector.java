package fun.efto.luna.asm.injector;

import fun.efto.luna.asm.AsmInjectionContext;
import fun.efto.luna.asm.injector.visitor.AroundMethodVisitor;
import fun.efto.luna.core.Constants;
import fun.efto.luna.core.bytecode.BytecodeAssembler;
import org.objectweb.asm.MethodVisitor;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/2 20:46
 */
public class AroundMethodInjector extends AbstractMethodInjector {

    @Override
    protected MethodVisitor createMethodVisitor(AsmInjectionContext context, BytecodeAssembler bytecodeAssembler) {
        return new AroundMethodVisitor(Constants.AMS_API_VERSION, context, bytecodeAssembler);
    }

}
