package fun.efto.luna.core.probe.invocation.model;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/07/05 20:00
 */
public class ActiveInvocation {

    private final String traceId;
    private final String spanId;
    private final String parentSpanId;
    private final String className;
    private final String methodName;
    private final long startTimeNanos;
    private final String args;

    public ActiveInvocation(String traceId, String spanId, String parentSpanId,
                            String className, String methodName,
                            long startTimeNanos, String args) {
        this.traceId = traceId;
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
        this.className = className;
        this.methodName = methodName;
        this.startTimeNanos = startTimeNanos;
        this.args = args;
    }

    public String getTraceId() { return traceId; }
    public String getSpanId() { return spanId; }
    public String getParentSpanId() { return parentSpanId; }
    public String getClassName() { return className; }
    public String getMethodName() { return methodName; }
    public long getStartTimeNanos() { return startTimeNanos; }
    public String getArgs() { return args; }
}
