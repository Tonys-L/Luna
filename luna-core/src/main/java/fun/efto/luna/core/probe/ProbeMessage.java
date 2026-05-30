/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/25 23:00
 */
package fun.efto.luna.core.probe;

/**
 * Typed probe message with type discriminator for frontend rendering.
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
        return "{\"type\":\"" + escapeJson(type) + "\",\"payload\":" +
               (payload.startsWith("{") ? payload : "\"" + escapeJson(payload) + "\"") +
               ",\"timestamp\":" + timestamp + "}";
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
