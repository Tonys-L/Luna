package fun.efto.luna.core.plugin.builtin.invocation.model;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/07/05 20:00
 */
public class InvocationSpan {

    private final String spanId;
    private final String parentSpanId;
    private final String className;
    private final String methodName;
    private final long durationMs;
    private final String args;
    private final String returnValue;
    private final boolean threwException;
    private final String exceptionMessage;

    public InvocationSpan(String spanId, String parentSpanId, String className,
                          String methodName, long durationMs, String args,
                          String returnValue, boolean threwException, String exceptionMessage) {
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
        this.className = className;
        this.methodName = methodName;
        this.durationMs = durationMs;
        this.args = args;
        this.returnValue = returnValue;
        this.threwException = threwException;
        this.exceptionMessage = exceptionMessage;
    }

    public String getSpanId() { return spanId; }
    public String getParentSpanId() { return parentSpanId; }
    public String getClassName() { return className; }
    public String getMethodName() { return methodName; }
    public long getDurationMs() { return durationMs; }
    public String getArgs() { return args; }
    public String getReturnValue() { return returnValue; }
    public boolean isThrewException() { return threwException; }
    public String getExceptionMessage() { return exceptionMessage; }

    public String toJson() {
        StringBuilder sb = new StringBuilder(256);
        sb.append("{");
        sb.append("\"spanId\":\"").append(escapeJson(spanId)).append("\",");
        sb.append("\"parentSpanId\":");
        if (parentSpanId != null) {
            sb.append("\"").append(escapeJson(parentSpanId)).append("\"");
        } else {
            sb.append("null");
        }
        sb.append(",\"className\":\"").append(escapeJson(className)).append("\",");
        sb.append("\"methodName\":\"").append(escapeJson(methodName)).append("\",");
        sb.append("\"durationMs\":").append(durationMs).append(",");
        sb.append("\"args\":");
        if (args != null && args.startsWith("{")) {
            sb.append(args);
        } else if (args != null && args.startsWith("[")) {
            sb.append(args);
        } else {
            sb.append("\"").append(escapeJson(args)).append("\"");
        }
        sb.append(",\"returnValue\":");
        if (returnValue != null && returnValue.startsWith("{")) {
            sb.append(returnValue);
        } else if (returnValue != null && returnValue.startsWith("[")) {
            sb.append(returnValue);
        } else {
            sb.append("\"").append(escapeJson(returnValue)).append("\"");
        }
        sb.append(",\"threwException\":").append(threwException).append(",");
        sb.append("\"exceptionMessage\":");
        if (exceptionMessage != null) {
            sb.append("\"").append(escapeJson(exceptionMessage)).append("\"");
        } else {
            sb.append("null");
        }
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"':  sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
