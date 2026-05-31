package fun.efto.luna.core.plugin.builtin.trace;

import fun.efto.luna.core.plugin.BytecodeHelper;
import fun.efto.luna.core.plugin.GenerateContext;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/16 10:00
 */
public class TraceExpressionHandler {

    public void generateBytecode(GenerateContext ctx) {
        String expr = ctx.expression();
        BytecodeHelper h = ctx.helper();
        String className = ctx.asmContext().getInjectionPoint().getTarget().getTargetClass();
        String methodName = ctx.asmContext().getInjectionPoint().getTarget().getMethodName();

        if ("start".equals(expr)) {
            h.invokeStatic(TraceProbe.INTERNAL_NAME, "onTraceStart", "()V");
        } else if (expr.startsWith("end:")) {
            long threshold = Long.parseLong(expr.substring(4).trim());
            h.loadString(className);
            h.loadString(methodName);
            h.loadLong(threshold);
            h.invokeStatic(TraceProbe.INTERNAL_NAME, "onTraceEnd", "(Ljava/lang/String;Ljava/lang/String;J)V");
        } else if (expr.startsWith("alert:")) {
            long threshold = Long.parseLong(expr.substring(6).trim());
            h.loadString(className);
            h.loadString(methodName);
            h.loadLong(threshold);
            h.invokeStatic(TraceProbe.INTERNAL_NAME, "onTraceAlert", "(Ljava/lang/String;Ljava/lang/String;J)V");
        }
    }
}
