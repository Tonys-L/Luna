package fun.efto.luna.core.probe.log;

import fun.efto.luna.core.probe.ProbeMessage;
import fun.efto.luna.core.probe.ProbeOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/16 20:00
 */
public class LogProbeTest {

    @BeforeEach
    void setUp() {
        while (ProbeOutput.BUFFER.poll() != null) {}
    }

    @Test
    void testOnLogNormal() {
        LogProbe.onLog("hello world");

        ProbeMessage msg = ProbeOutput.BUFFER.poll();
        assertNotNull(msg);
        assertEquals("LOG", msg.getType());
        assertEquals("hello world", msg.getPayload());
    }

    @Test
    void testOnLogNull() {
        LogProbe.onLog(null);

        ProbeMessage msg = ProbeOutput.BUFFER.poll();
        assertNull(msg);
    }

    @Test
    void testOnLogEmpty() {
        LogProbe.onLog("");

        ProbeMessage msg = ProbeOutput.BUFFER.poll();
        assertNull(msg);
    }

    @Test
    void testOnLogMultiple() {
        LogProbe.onLog("msg1");
        LogProbe.onLog("msg2");
        LogProbe.onLog("msg3");

        List<ProbeMessage> messages = new ArrayList<>();
        ProbeMessage msg;
        while ((msg = ProbeOutput.BUFFER.poll()) != null) {
            messages.add(msg);
        }
        assertEquals(3, messages.size());
        assertEquals("LOG", messages.get(0).getType());
        assertEquals("msg1", messages.get(0).getPayload());
        assertEquals("msg2", messages.get(1).getPayload());
        assertEquals("msg3", messages.get(2).getPayload());
    }
}
