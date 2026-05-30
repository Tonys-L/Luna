package fun.efto.luna.core.probe;

import fun.efto.luna.core.probe.ProbeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 14:00
 */
public class LunaSpyTraceTest {

    @BeforeEach
    void setUp() {
        while (LunaSpy.LOG_BUFFER.poll() != null) {}
    }

    @Test
    void testTraceStartAndEnd() {
        LunaSpy.onTraceStart();

        simulateWork();

        LunaSpy.onTraceEnd("com.example.Service", "doWork", 0);

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
        LunaSpy.onTraceStart();

        LunaSpy.onTraceEnd("com.example.Service", "fastMethod", 10000);

        List<ProbeMessage> messages = drainBuffer();
        assertTrue(messages.isEmpty());
    }

    @Test
    void testTraceEndWithThresholdAbove() {
        LunaSpy.onTraceStart();

        simulateWork();

        LunaSpy.onTraceEnd("com.example.Service", "slowMethod", 0);

        List<ProbeMessage> messages = drainBuffer();
        assertEquals(1, messages.size());
    }

    @Test
    void testTraceEndWithoutStart() {
        LunaSpy.onTraceEnd("com.example.Service", "doWork", 0);

        List<ProbeMessage> messages = drainBuffer();
        assertTrue(messages.isEmpty());
    }

    @Test
    void testTraceAlertBelowThreshold() {
        LunaSpy.onTraceStart();

        LunaSpy.onTraceAlert("com.example.Service", "fastMethod", 10000);

        List<ProbeMessage> messages = drainBuffer();
        assertTrue(messages.isEmpty());
    }

    @Test
    void testTraceAlertAboveThreshold() {
        LunaSpy.onTraceStart();

        simulateWork();
        simulateWork();
        simulateWork();

        LunaSpy.onTraceAlert("com.example.Service", "slowMethod", 0);

        List<ProbeMessage> messages = drainBuffer();
        assertEquals(1, messages.size());
        assertEquals("TRACE", messages.get(0).getType());
        assertTrue(messages.get(0).getPayload().startsWith("[SLOW]"));
        assertTrue(messages.get(0).getPayload().contains("threshold"));
    }

    @Test
    void testTraceAlertWithoutStart() {
        LunaSpy.onTraceAlert("com.example.Service", "doWork", 100);

        List<ProbeMessage> messages = drainBuffer();
        assertTrue(messages.isEmpty());
    }

    @Test
    void testTraceThreadIsolation() throws InterruptedException {
        Thread t1 = new Thread(() -> {
            LunaSpy.onTraceStart();
            simulateWork();
            LunaSpy.onTraceEnd("Service1", "method1", 0);
        });

        Thread t2 = new Thread(() -> {
            LunaSpy.onTraceStart();
            simulateWork();
            simulateWork();
            LunaSpy.onTraceEnd("Service2", "method2", 0);
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
        while ((msg = LunaSpy.LOG_BUFFER.poll()) != null) {
            messages.add(msg);
        }
        return messages;
    }
}
