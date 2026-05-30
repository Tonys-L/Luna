/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/31 22:30
 */
package fun.efto.luna.core.bytekit.bridge;

import fun.efto.luna.core.probe.ProbeMessage;
import fun.efto.luna.core.probe.ProbeOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LunaSpyBridgeTest {

    @BeforeEach
    void setUp() {
        while (ProbeOutput.BUFFER.poll() != null) {}
    }

    private List<ProbeMessage> drainBuffer() {
        List<ProbeMessage> messages = new ArrayList<>();
        ProbeMessage msg;
        while ((msg = ProbeOutput.BUFFER.poll()) != null) {
            messages.add(msg);
        }
        return messages;
    }

    @Nested
    @DisplayName("LunaSpyBridge.onMethodEnter 测试")
    class OnMethodEnterTests {

        @Test
        @DisplayName("onMethodEnter 可被调用并写入 ProbeOutput.BUFFER")
        void testOnMethodEnterWritesToBuffer() {
            Object target = "testTarget";
            Object[] args = new Object[]{"arg1", 42};
            String methodName = "testMethod";

            LunaSpyBridge.onMethodEnter(target, args, methodName);

            List<ProbeMessage> messages = drainBuffer();
            assertEquals(1, messages.size());
            assertEquals("LOG", messages.get(0).getType());
            assertTrue(messages.get(0).getPayload().contains("ENTER"));
            assertTrue(messages.get(0).getPayload().contains(methodName));
        }

        @Test
        @DisplayName("onMethodEnter 多次调用产生多条消息")
        void testOnMethodEnterMultipleCalls() {
            LunaSpyBridge.onMethodEnter("t1", new Object[]{}, "m1");
            LunaSpyBridge.onMethodEnter("t2", new Object[]{"a"}, "m2");

            List<ProbeMessage> messages = drainBuffer();
            assertEquals(2, messages.size());
        }
    }

    @Nested
    @DisplayName("LunaSpyBridge.onMethodExit 测试")
    class OnMethodExitTests {

        @Test
        @DisplayName("onMethodExit 可被调用并写入 ProbeOutput.BUFFER")
        void testOnMethodExitWritesToBuffer() {
            Object target = "testTarget";
            Object[] args = new Object[]{"arg1"};
            String methodName = "testMethod";
            Object returnValue = "result";

            LunaSpyBridge.onMethodExit(target, args, methodName, returnValue);

            List<ProbeMessage> messages = drainBuffer();
            assertEquals(1, messages.size());
            assertEquals("LOG", messages.get(0).getType());
            assertTrue(messages.get(0).getPayload().contains("EXIT"));
            assertTrue(messages.get(0).getPayload().contains(methodName));
        }

        @Test
        @DisplayName("onMethodExit 多次调用产生多条消息")
        void testOnMethodExitMultipleCalls() {
            LunaSpyBridge.onMethodExit("t1", new Object[]{}, "m1", "r1");
            LunaSpyBridge.onMethodExit("t2", new Object[]{}, "m2", null);

            List<ProbeMessage> messages = drainBuffer();
            assertEquals(2, messages.size());
        }
    }
}
