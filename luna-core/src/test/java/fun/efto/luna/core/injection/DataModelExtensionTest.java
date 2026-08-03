package fun.efto.luna.core.injection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/01 10:00
 */
class DataModelExtensionTest {

    @Nested
    @DisplayName("PersistentInjection 新字段测试")
    class PersistentInjectionTest {

        @Test
        @DisplayName("probeType getter/setter")
        void probeTypeGetterSetter() {
            PersistentInjection injection = new PersistentInjection();
            assertNull(injection.getProbeType());
            injection.setProbeType("TRACE");
            assertEquals("TRACE", injection.getProbeType());
        }

        @Test
        @DisplayName("injectionLocation getter/setter")
        void injectionLocationGetterSetter() {
            PersistentInjection injection = new PersistentInjection();
            assertNull(injection.getInjectionLocation());
            injection.setInjectionLocation("method_exit");
            assertEquals("method_exit", injection.getInjectionLocation());
        }
    }

    @Nested
    @DisplayName("InjectRequest 新字段测试")
    class InjectRequestTest {

        @Test
        @DisplayName("probeType getter/setter")
        void probeTypeGetterSetter() {
            InjectRequest command = new InjectRequest();
            assertNull(command.getProbeType());
            command.setProbeType("SNAPSHOT");
            assertEquals("SNAPSHOT", command.getProbeType());
        }

        @Test
        @DisplayName("injectionLocation getter/setter")
        void injectionLocationGetterSetter() {
            InjectRequest command = new InjectRequest();
            assertNull(command.getInjectionLocation());
            command.setInjectionLocation("line_before");
            assertEquals("line_before", command.getInjectionLocation());
        }
    }
}
