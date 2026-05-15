package fun.efto.luna.core.plugin.handler;

import fun.efto.luna.core.asm.assmebler.ExpressionBytecodeAssembler;
import fun.efto.luna.core.plugin.ExpressionHandler;
import fun.efto.luna.core.plugin.GenerateContext;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
public class SnapshotExpressionHandler implements ExpressionHandler {
    @Override public String getProtocol() { return "snapshot"; }

    @Override
    public void generateBytecode(GenerateContext ctx) {
        ExpressionBytecodeAssembler.generateSnapshotBytecode(
            ctx.mv(), ctx.asmContext(), ctx.hasCondition());
    }
}
