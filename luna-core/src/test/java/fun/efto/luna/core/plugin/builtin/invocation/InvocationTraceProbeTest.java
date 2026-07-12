package fun.efto.luna.core.plugin.builtin.invocation;

import fun.efto.luna.core.probe.ProbeMessage;
import fun.efto.luna.core.probe.ProbeOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since : 2026/07/05 22:00
 */
public class InvocationTraceProbeTest {

    @BeforeEach
    void setUp() {
        drainBuffer();
    }

    @Test
    void testSimpleMethodEnterExit() {
        InvocationTraceProbe.onMethodEnter("com.example.Service", "doWork", new Object[]{"arg1", 42});
        InvocationTraceProbe.onMethodExit("com.example.Service", "doWork", "result", null);

        List<ProbeMessage> messages = drainBuffer();
        assertEquals(1, messages.size(), "Should produce one probe message for a single method trace");

        ProbeMessage msg = messages.get(0);
        assertEquals("INVOCATION", msg.getType());
        assertTrue(msg.getPayload().contains("com.example.Service.doWork"), "Payload should contain class.method");
        assertTrue(msg.getPayload().contains("took"), "Payload should contain 'took'");
        assertTrue(msg.getPayload().contains("1 spans"), "Payload should contain span count");

        // Verify structured payload
        Map<String, Object> structured = msg.getStructuredPayload();
        assertNotNull(structured, "Structured payload should not be null");
        assertNotNull(structured.get("traceId"), "traceId should be set");
        assertEquals("com.example.Service", structured.get("rootClassName"));
        assertEquals("doWork", structured.get("rootMethodName"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> spans = (List<Map<String, Object>>) structured.get("spans");
        assertNotNull(spans, "spans should not be null");
        assertEquals(1, spans.size(), "Should have exactly 1 span");

        Map<String, Object> span = spans.get(0);
        assertEquals("com.example.Service", span.get("className"));
        assertEquals("doWork", span.get("methodName"));
        assertNull(span.get("parentSpanId"), "Root span should have null parentSpanId");
        assertEquals(false, span.get("threwException"));
        assertNull(span.get("exceptionMessage"));
    }

    @Test
    void testNestedCalls() {
        // Root method A calls B
        InvocationTraceProbe.onMethodEnter("com.example.Service", "methodA", null);
        InvocationTraceProbe.onMethodEnter("com.example.Service", "methodB", null);

        // Exit B first (inner)
        InvocationTraceProbe.onMethodExit("com.example.Service", "methodB", "bResult", null);
        // Exit A (root)
        InvocationTraceProbe.onMethodExit("com.example.Service", "methodA", "aResult", null);

        List<ProbeMessage> messages = drainBuffer();
        assertEquals(1, messages.size(), "Should produce one message for the whole trace");

        ProbeMessage msg = messages.get(0);
        Map<String, Object> structured = msg.getStructuredPayload();
        assertNotNull(structured);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> spans = (List<Map<String, Object>>) structured.get("spans");
        assertEquals(2, spans.size(), "Should have 2 spans (A and B)");

        // Find spans by method name since order in pending list is B first, A second
        Map<String, Object> spanB = spans.get(0);
        Map<String, Object> spanA = spans.get(1);

        assertEquals("methodB", spanB.get("methodName"));
        assertEquals("methodA", spanA.get("methodName"));

        // Verify parent-child: B's parentSpanId should be A's spanId
        assertNotNull(spanB.get("parentSpanId"), "Inner span should have a parentSpanId");
        assertEquals(spanA.get("spanId"), spanB.get("parentSpanId"),
            "Inner span's parentSpanId should match outer span's spanId");

        assertTrue(msg.getPayload().contains("2 spans"), "Payload should indicate 2 spans");
    }

    @Test
    void testExceptionExit() {
        InvocationTraceProbe.onMethodEnter("com.example.Service", "failingMethod", null);
        RuntimeException exception = new RuntimeException("something went wrong");
        InvocationTraceProbe.onMethodExit("com.example.Service", "failingMethod", null, exception);

        List<ProbeMessage> messages = drainBuffer();
        assertEquals(1, messages.size());

        Map<String, Object> structured = messages.get(0).getStructuredPayload();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> spans = (List<Map<String, Object>>) structured.get("spans");
        Map<String, Object> span = spans.get(0);

        assertEquals(true, span.get("threwException"), "threwException should be true");
        String exMsg = (String) span.get("exceptionMessage");
        assertNotNull(exMsg, "exceptionMessage should not be null");
        assertTrue(exMsg.contains("RuntimeException"), "Exception message should contain exception class name");
        assertTrue(exMsg.contains("something went wrong"), "Exception message should contain exception message");
    }

    @Test
    void testDeepRecursion() {
        // Simulate calls beyond max stack depth (20)
        int depth = 25;
        for (int i = 0; i < depth; i++) {
            InvocationTraceProbe.onMethodEnter("com.example.DeepService", "level" + i, null);
        }

        // Exit all methods
        for (int i = depth - 1; i >= 0; i--) {
            InvocationTraceProbe.onMethodExit("com.example.DeepService", "level" + i, "result" + i, null);
        }

        // Should produce at least one message (root exit)
        List<ProbeMessage> messages = drainBuffer();
        assertFalse(messages.isEmpty(), "Should produce at least one probe message");

        // The trace should have been handled gracefully without exceptions
        ProbeMessage msg = messages.get(0);
        assertEquals("INVOCATION", msg.getType());
    }

    @Test
    void testNullArgs() {
        InvocationTraceProbe.onMethodEnter("com.example.Service", "noArgsMethod", null);
        InvocationTraceProbe.onMethodExit("com.example.Service", "noArgsMethod", null, null);

        List<ProbeMessage> messages = drainBuffer();
        assertEquals(1, messages.size(), "Should produce one message even with null args");

        Map<String, Object> structured = messages.get(0).getStructuredPayload();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> spans = (List<Map<String, Object>>) structured.get("spans");
        Map<String, Object> span = spans.get(0);
        assertEquals("null", span.get("args"), "Null args should be serialized as 'null'");
    }

    @Test
    void testExitWithoutEnter() {
        // Calling exit without a matching enter should not throw
        InvocationTraceProbe.onMethodExit("com.example.Service", "orphanMethod", null, null);

        List<ProbeMessage> messages = drainBuffer();
        assertTrue(messages.isEmpty(), "Exit without enter should produce no message");
    }

    @Test
    void testProbeExceptionIsolation() {
        // Verify that probe exceptions don't propagate to business code
        // onMethodEnter/onMethodExit catch Throwable internally
        assertDoesNotThrow(() -> {
            InvocationTraceProbe.onMethodEnter("com.example.Service", "safeMethod", new Object[]{null});
            InvocationTraceProbe.onMethodExit("com.example.Service", "safeMethod", null, null);
        }, "Probe methods should not throw exceptions to business code");
    }

    private List<ProbeMessage> drainBuffer() {
        List<ProbeMessage> messages = new ArrayList<>();
        ProbeMessage msg;
        while ((msg = ProbeOutput.BUFFER.poll()) != null) {
            messages.add(msg);
        }
        return messages;
    }
}
