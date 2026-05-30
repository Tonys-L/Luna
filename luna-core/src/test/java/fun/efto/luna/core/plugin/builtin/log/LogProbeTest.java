package fun.efto.luna.core.plugin.builtin.log;

import fun.efto.luna.core.probe.ProbeMessage;
import fun.efto.luna.core.probe.ProbeOutput;
import fun.efto.luna.core.probe.LunaSpy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(<286269159@qq.com>)
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

    @Test
    void testBufferUnification_LogProbeWritesToLunaSpyBuffer() {
        while (LunaSpy.LOG_BUFFER.poll() != null) {}

        LogProbe.onLog("via-log-probe");

        ProbeMessage msg = LunaSpy.LOG_BUFFER.poll();
        assertNotNull(msg, "LogProbe.onLog() 写入的消息必须能从 LunaSpy.LOG_BUFFER 读取到");
        assertEquals("LOG", msg.getType());
        assertEquals("via-log-probe", msg.getPayload());
    }

    @Test
    void testBufferUnification_SameInstance() {
        assertSame(ProbeOutput.BUFFER, LunaSpy.LOG_BUFFER,
                "ProbeOutput.BUFFER 和 LunaSpy.LOG_BUFFER 必须是同一个 RingBuffer 实例");
    }
}
