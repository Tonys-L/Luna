package fun.efto.luna.core.plugin.handler;

import fun.efto.luna.core.asm.AsmInjectionContext;
import fun.efto.luna.core.asm.assmebler.ExpressionBytecodeAssembler;
import fun.efto.luna.core.plugin.BytecodeHelper;
import fun.efto.luna.core.plugin.ExpressionHandler;
import fun.efto.luna.core.plugin.GenerateContext;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
public class LogExpressionHandler implements ExpressionHandler {
    @Override public String getProtocol() { return "log"; }

    @Override
    public void generateBytecode(GenerateContext ctx) {
        ExpressionBytecodeAssembler.generateLogBytecode(
            ctx.mv(), ctx.expression(), ctx.asmContext(), ctx.hasCondition());
    }
}
