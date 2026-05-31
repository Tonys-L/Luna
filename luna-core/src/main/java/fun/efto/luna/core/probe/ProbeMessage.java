package fun.efto.luna.core.probe;

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

    public ProbeMessage(String type, String payload) {
        this.type = type;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
    }

    public String getType() { return type; }
    public String getPayload() { return payload; }
    public long getTimestamp() { return timestamp; }

    public String toJson() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("{\"type\":\"").append(escapeJson(type)).append("\",");
        sb.append("\"payload\":");
        if (payload != null && payload.startsWith("{")) {
            sb.append(payload);
        } else {
            sb.append("\"").append(escapeJson(payload)).append("\"");
        }
        sb.append(",\"timestamp\":").append(timestamp).append("}");
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