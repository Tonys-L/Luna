package fun.efto.luna.core.plugin.builtin.trace;

import fun.efto.luna.core.probe.ProbeMessage;
import fun.efto.luna.core.probe.ProbeOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/16 20:00
 */
public class TraceProbeTest {

    @BeforeEach
    void setUp() {
        while (ProbeOutput.BUFFER.poll() != null) {}
    }

    @Test
    void testTraceStartAndEnd() {
        TraceProbe.onTraceStart();

        simulateWork();

        TraceProbe.onTraceEnd("com.example.Service", "doWork", 0);

        List<ProbeMessage> messages = drainBuffer();
        assertEquals(1, messages.size());
        assertEquals("TRACE", messages.get(0).getType());
        assertTrue(messages.get(0).getPayload().startsWith("[TRACE]"));
        assertTrue(messages.get(0).getPayload().contains("com.example.Service.doWork"));
        assertTrue(messages.get(0).getPayload().contains("took"));
        assertTrue(messages.get(0).getPayload().contains("ms"));
    }

    @Test
    void testTraceEndWithThresholdBelow() {
        TraceProbe.onTraceStart();

        TraceProbe.onTraceEnd("com.example.Service", "fastMethod", 10000);

        List<ProbeMessage> messages = drainBuffer();
        assertTrue(messages.isEmpty());
    }

    @Test
    void testTraceEndWithThresholdAbove() {
        TraceProbe.onTraceStart();

        simulateWork();

        TraceProbe.onTraceEnd("com.example.Service", "slowMethod", 0);

        List<ProbeMessage> messages = drainBuffer();
        assertEquals(1, messages.size());
    }

    @Test
    void testTraceEndWithoutStart() {
        TraceProbe.onTraceEnd("com.example.Service", "doWork", 0);

        List<ProbeMessage> messages = drainBuffer();
        assertTrue(messages.isEmpty());
    }

    @Test
    void testTraceAlertBelowThreshold() {
        TraceProbe.onTraceStart();

        TraceProbe.onTraceAlert("com.example.Service", "fastMethod", 10000);

        List<ProbeMessage> messages = drainBuffer();
        assertTrue(messages.isEmpty());
    }

    @Test
    void testTraceAlertAboveThreshold() {
        TraceProbe.onTraceStart();

        simulateWork();
        simulateWork();
        simulateWork();

        TraceProbe.onTraceAlert("com.example.Service", "slowMethod", 0);

        List<ProbeMessage> messages = drainBuffer();
        assertEquals(1, messages.size());
        assertEquals("TRACE", messages.get(0).getType());
        assertTrue(messages.get(0).getPayload().startsWith("[SLOW]"));
        assertTrue(messages.get(0).getPayload().contains("threshold"));
    }

    @Test
    void testTraceAlertWithoutStart() {
        TraceProbe.onTraceAlert("com.example.Service", "doWork", 100);

        List<ProbeMessage> messages = drainBuffer();
        assertTrue(messages.isEmpty());
    }

    @Test
    void testTraceThreadIsolation() throws InterruptedException {
        Thread t1 = new Thread(() -> {
            TraceProbe.onTraceStart();
            simulateWork();
            TraceProbe.onTraceEnd("Service1", "method1", 0);
        });

        Thread t2 = new Thread(() -> {
            TraceProbe.onTraceStart();
            simulateWork();
            simulateWork();
            TraceProbe.onTraceEnd("Service2", "method2", 0);
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        List<ProbeMessage> messages = drainBuffer();
        assertEquals(2, messages.size());
    }

    private void simulateWork() {
        long sum = 0;
        for (int i = 0; i < 100000; i++) {
            sum += i;
        }
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
