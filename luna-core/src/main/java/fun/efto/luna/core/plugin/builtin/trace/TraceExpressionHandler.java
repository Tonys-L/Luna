package fun.efto.luna.core.plugin.builtin.trace;

import fun.efto.luna.core.plugin.BytecodeHelper;
import fun.efto.luna.core.plugin.GenerateContext;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since : 2026/05/16 10:00
 */
public class TraceExpressionHandler {

    public void generateBytecode(GenerateContext ctx) {
        BytecodeHelper h = ctx.helper();
        String className = ctx.asmContext().getInjectionPoint().getTarget().getTargetClass();
        String methodName = ctx.asmContext().getInjectionPoint().getTarget().getMethodName();
        long threshold = resolveThreshold(ctx);

        if (ctx.phase() == GenerateContext.Phase.ENTER) {
            h.invokeStatic(TraceProbe.INTERNAL_NAME, "onTraceStart", "()V");
        } else {
            h.loadString(className);
            h.loadString(methodName);
            h.loadLong(threshold);
            h.invokeStatic(TraceProbe.INTERNAL_NAME, "onTraceEnd", "(Ljava/lang/String;Ljava/lang/String;J)V");
        }
    }

    /**
     * 从 configValues 中解析 threshold，默认 0（全部输出）。
     * 表单字段 "threshold" 由前端通过 configValues 传递。
     */
    private long resolveThreshold(GenerateContext ctx) {
        try {
            String expr = ctx.expression();
            if (expr != null && !expr.isEmpty()) {
                return Long.parseLong(expr.trim());
            }
        } catch (NumberFormatException ignored) {
        }
        return 0;
    }
}
