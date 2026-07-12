package fun.efto.luna.core.probe;

import java.util.List;
import java.util.Map;

/**
 * Typed probe message with type discriminator for frontend rendering.
 *
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/25 23:00
 */
public class ProbeMessage {

    private final String type;
    private final String payload;
    private final long timestamp;
    private final Map<String, Object> structuredPayload;

    public ProbeMessage(String type, String payload) {
        this.type = type;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
        this.structuredPayload = null;
    }

    public ProbeMessage(String type, String payload, Map<String, Object> structuredPayload) {
        this.type = type;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
        this.structuredPayload = structuredPayload;
    }

    public String getType() { return type; }
    public String getPayload() { return payload; }
    public long getTimestamp() { return timestamp; }
    public Map<String, Object> getStructuredPayload() { return structuredPayload; }

    public String toJson() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("{\"type\":\"").append(escapeJson(type)).append("\",");
        sb.append("\"payload\":");
        if (payload != null && payload.startsWith("{")) {
            sb.append(payload);
        } else {
            sb.append("\"").append(escapeJson(payload)).append("\"");
        }
        sb.append(",\"timestamp\":").append(timestamp);
        if (structuredPayload != null) {
            sb.append(",\"structuredPayload\":");
            sb.append(mapToJson(structuredPayload));
        }
        sb.append("}");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static String mapToJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder(64);
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
            Object value = entry.getValue();
            if (value == null) {
                sb.append("null");
            } else if (value instanceof String) {
                sb.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Boolean) {
                sb.append(value.toString());
            } else if (value instanceof Number) {
                sb.append(value.toString());
            } else if (value instanceof Map) {
                sb.append(mapToJson((Map<String, Object>) value));
            } else if (value instanceof List) {
                sb.append(listToJson((List<?>) value));
            } else {
                sb.append("\"").append(escapeJson(value.toString())).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static String listToJson(List<?> list) {
        StringBuilder sb = new StringBuilder(64);
        sb.append("[");
        boolean first = true;
        for (Object item : list) {
            if (!first) sb.append(",");
            first = false;
            if (item == null) {
                sb.append("null");
            } else if (item instanceof String) {
                sb.append("\"").append(escapeJson((String) item)).append("\"");
            } else if (item instanceof Boolean) {
                sb.append(item.toString());
            } else if (item instanceof Number) {
                sb.append(item.toString());
            } else if (item instanceof Map) {
                sb.append(mapToJson((Map<String, Object>) item));
            } else if (item instanceof List) {
                sb.append(listToJson((List<?>) item));
            } else {
                sb.append("\"").append(escapeJson(item.toString())).append("\"");
            }
        }
        sb.append("]");
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