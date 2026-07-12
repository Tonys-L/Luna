package fun.efto.luna.core.plugin.builtin.invocation.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/07/05 20:00
 */
public class InvocationTrace {

    private final String traceId;
    private final String rootClassName;
    private final String rootMethodName;
    private final long totalDurationMs;
    private final long startTimestamp;
    private final List<InvocationSpan> spans;

    public InvocationTrace(String traceId, String rootClassName, String rootMethodName,
                           long totalDurationMs, long startTimestamp, List<InvocationSpan> spans) {
        this.traceId = traceId;
        this.rootClassName = rootClassName;
        this.rootMethodName = rootMethodName;
        this.totalDurationMs = totalDurationMs;
        this.startTimestamp = startTimestamp;
        this.spans = spans != null ? new ArrayList<>(spans) : new ArrayList<>();
    }

    public String getTraceId() { return traceId; }
    public String getRootClassName() { return rootClassName; }
    public String getRootMethodName() { return rootMethodName; }
    public long getTotalDurationMs() { return totalDurationMs; }
    public long getStartTimestamp() { return startTimestamp; }
    public List<InvocationSpan> getSpans() { return new ArrayList<>(spans); }

    public String toJson() {
        StringBuilder sb = new StringBuilder(512);
        sb.append("{");
        sb.append("\"traceId\":\"").append(escapeJson(traceId)).append("\",");
        sb.append("\"rootClassName\":\"").append(escapeJson(rootClassName)).append("\",");
        sb.append("\"rootMethodName\":\"").append(escapeJson(rootMethodName)).append("\",");
        sb.append("\"totalDurationMs\":").append(totalDurationMs).append(",");
        sb.append("\"startTimestamp\":").append(startTimestamp).append(",");
        sb.append("\"spans\":[");
        for (int i = 0; i < spans.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(spans.get(i).toJson());
        }
        sb.append("]");
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
