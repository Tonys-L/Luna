package fun.efto.luna.core.probe;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since : 2026/07/05 22:00
 */
public class ProbeMessageStructuredPayloadTest {

    @Test
    void testNewConstructorWithStructuredPayload() {
        Map<String, Object> structured = new LinkedHashMap<>();
        structured.put("traceId", "abc-123");
        structured.put("duration", 42L);

        ProbeMessage msg = new ProbeMessage("INVOCATION", "test payload", structured);

        assertEquals("INVOCATION", msg.getType());
        assertEquals("test payload", msg.getPayload());
        assertNotNull(msg.getStructuredPayload());
        assertEquals("abc-123", msg.getStructuredPayload().get("traceId"));
        assertEquals(42L, msg.getStructuredPayload().get("duration"));
    }

    @Test
    void testOldConstructorNullStructuredPayload() {
        ProbeMessage msg = new ProbeMessage("LOG", "log message");

        assertEquals("LOG", msg.getType());
        assertEquals("log message", msg.getPayload());
        assertNull(msg.getStructuredPayload(), "Old 2-arg constructor should set structuredPayload to null");
    }

    @Test
    void testToJsonWithStructuredPayload() {
        Map<String, Object> structured = new LinkedHashMap<>();
        structured.put("traceId", "abc-123");
        structured.put("totalDurationMs", 100L);
        structured.put("active", true);

        ProbeMessage msg = new ProbeMessage("INVOCATION", "test payload", structured);
        String json = msg.toJson();

        assertTrue(json.contains("\"structuredPayload\":"), "JSON should contain structuredPayload field");
        assertTrue(json.contains("\"traceId\":"), "JSON should contain traceId within structuredPayload");
        assertTrue(json.contains("\"abc-123\""), "JSON should contain traceId value");
        assertTrue(json.contains("\"totalDurationMs\":"), "JSON should contain totalDurationMs");
        assertTrue(json.contains("\"active\":"), "JSON should contain active flag");
        assertTrue(json.contains("true"), "JSON should contain boolean value");
    }

    @Test
    void testToJsonWithoutStructuredPayload() {
        ProbeMessage msg = new ProbeMessage("LOG", "log message");
        String json = msg.toJson();

        assertFalse(json.contains("\"structuredPayload\""), "JSON should NOT contain structuredPayload field when null");
        assertTrue(json.contains("\"type\":\"LOG\""), "JSON should contain type field");
        assertTrue(json.contains("\"payload\":\"log message\""), "JSON should contain payload field");
    }

    @Test
    void testToJsonNestedMapInStructuredPayload() {
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("key", "value");

        Map<String, Object> structured = new LinkedHashMap<>();
        structured.put("nested", inner);
        structured.put("count", 5);

        ProbeMessage msg = new ProbeMessage("TEST", "payload", structured);
        String json = msg.toJson();

        assertTrue(json.contains("\"nested\":"), "JSON should contain nested map key");
        assertTrue(json.contains("\"key\":\"value\""), "JSON should contain inner map entry");
        assertTrue(json.contains("\"count\":5"), "JSON should contain count field");
    }

    @Test
    void testTimestampIsSet() {
        long before = System.currentTimeMillis();
        ProbeMessage msg = new ProbeMessage("LOG", "test");
        long after = System.currentTimeMillis();

        assertTrue(msg.getTimestamp() >= before && msg.getTimestamp() <= after,
            "Timestamp should be approximately current time");
    }
}
