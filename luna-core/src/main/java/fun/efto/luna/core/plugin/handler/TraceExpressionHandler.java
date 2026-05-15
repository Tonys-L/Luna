package fun.efto.luna.core.plugin.handler;

import fun.efto.luna.core.asm.assmebler.ExpressionBytecodeAssembler;
import fun.efto.luna.core.plugin.ExpressionHandler;
import fun.efto.luna.core.plugin.GenerateContext;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
public class TraceExpressionHandler implements ExpressionHandler {
    @Override public String getProtocol() { return "trace"; }

    @Override
    public void generateBytecode(GenerateContext ctx) {
        ExpressionBytecodeAssembler.generateTraceBytecode(
            ctx.mv(), ctx.expression(), ctx.asmContext(), ctx.hasCondition());
    }
}
