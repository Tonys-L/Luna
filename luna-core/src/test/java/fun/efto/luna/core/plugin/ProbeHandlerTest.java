package fun.efto.luna.core.plugin;

import fun.efto.luna.core.injection.InjectRequest;
import fun.efto.luna.core.injection.target.InjectionLocation;
import fun.efto.luna.core.plugin.builtin.log.LogProbeHandler;
import fun.efto.luna.core.plugin.builtin.snapshot.SnapshotProbeHandler;
import fun.efto.luna.core.plugin.builtin.trace.TraceProbeHandler;
import fun.efto.luna.core.plugin.registry.InjectionTypeRegistry;
import fun.efto.luna.core.plugin.registry.ProbeHandlerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/01 01:30
 */
public class ProbeHandlerTest {

    @BeforeEach
    void setUp() {
        InjectionTypeRegistry.getInstance().register(InjectionLocation.of("method_enter", "test", "method"));
        InjectionTypeRegistry.getInstance().register(InjectionLocation.of("method_exit", "test", "method"));
        InjectionTypeRegistry.getInstance().register(InjectionLocation.of("method_around", "test", "method"));
        InjectionTypeRegistry.getInstance().register(InjectionLocation.of("line_before", "test", "line"));
        InjectionTypeRegistry.getInstance().register(InjectionLocation.of("line_after", "test", "line"));
        InjectionTypeRegistry.getInstance().register(InjectionLocation.of("invoke", "test", "method"));
        InjectionTypeRegistry.getInstance().register(InjectionLocation.of("exception_exit", "test", "method"));
    }

    @AfterEach
    void tearDown() {
        ProbeHandlerRegistry.getInstance().clear();
        InjectionTypeRegistry.getInstance().clear();
    }

    @Nested
    @DisplayName("ValidationResult 测试")
    class ValidationResultTest {

        @Test
        @DisplayName("ok() 返回有效结果")
        void okReturnsValid() {
            ValidationResult result = ValidationResult.ok();
            assertTrue(result.isValid());
            assertNull(result.getErrorMessage());
            assertTrue(result.getWarnings().isEmpty());
        }

        @Test
        @DisplayName("fail() 返回无效结果")
        void failReturnsInvalid() {
            ValidationResult result = ValidationResult.fail("something wrong");
            assertFalse(result.isValid());
            assertEquals("something wrong", result.getErrorMessage());
            assertTrue(result.getWarnings().isEmpty());
        }

        @Test
        @DisplayName("okWithWarnings() 返回有效但有警告")
        void okWithWarningsReturnsValidWithWarnings() {
            ValidationResult result = ValidationResult.okWithWarnings(Arrays.asList("warning1", "warning2"));
            assertTrue(result.isValid());
            assertNull(result.getErrorMessage());
            assertEquals(2, result.getWarnings().size());
            assertEquals("warning1", result.getWarnings().get(0));
            assertEquals("warning2", result.getWarnings().get(1));
        }
    }

    @Nested
    @DisplayName("LogProbeHandler 测试")
    class LogProbeHandlerTest {

        private final LogProbeHandler handler = new LogProbeHandler();

        @Test
        @DisplayName("自描述属性: probeType=LOG, usesCode=true, supportedLocations 包含所有位置")
        void selfDescription() {
            assertEquals("LOG", handler.getProbeType());
            assertTrue(handler.usesCode());
            Set<String> locations = handler.supportedInjectionLocations();
            assertTrue(locations.contains("method_enter"));
            assertTrue(locations.contains("method_exit"));
            assertTrue(locations.contains("method_around"));
            assertTrue(locations.contains("line_before"));
            assertTrue(locations.contains("line_after"));
            assertTrue(locations.contains("invoke"));
            assertTrue(locations.contains("exception_exit"));
        }

        @Test
        @DisplayName("validate() 拒绝空 code")
        void rejectEmptyCode() {
            InjectRequest cmd = new InjectRequest();
            cmd.setProbeType("LOG");
            cmd.setInjectionLocation("method_enter");
            cmd.setCode(null);
            cmd.setCodeType("EXPRESSION");

            ValidationResult result = handler.validate(cmd);
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("validate() 拒绝空 codeType（当 usesCode=true）")
        void rejectEmptyCodeTypeWhenUsesCode() {
            InjectRequest cmd = new InjectRequest();
            cmd.setProbeType("LOG");
            cmd.setInjectionLocation("method_enter");
            cmd.setCode("hello");
            cmd.setCodeType(null);

            ValidationResult result = handler.validate(cmd);
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("validate() 通过合法请求")
        void acceptValidRequest() {
            InjectRequest cmd = new InjectRequest();
            cmd.setProbeType("LOG");
            cmd.setInjectionLocation("method_enter");
            cmd.setCode("hello");
            cmd.setCodeType("EXPRESSION");

            ValidationResult result = handler.validate(cmd);
            assertTrue(result.isValid());
        }
    }

    @Nested
    @DisplayName("SnapshotProbeHandler 测试")
    class SnapshotProbeHandlerTest {

        private final SnapshotProbeHandler handler = new SnapshotProbeHandler();

        @Test
        @DisplayName("自描述属性: probeType=SNAPSHOT, usesCode=false, supportedLocations 不含 invoke")
        void selfDescription() {
            assertEquals("SNAPSHOT", handler.getProbeType());
            assertFalse(handler.usesCode());
            Set<String> locations = handler.supportedInjectionLocations();
            assertTrue(locations.contains("line_before"));
            assertTrue(locations.contains("line_after"));
            assertTrue(locations.contains("method_enter"));
            assertTrue(locations.contains("method_exit"));
            assertFalse(locations.contains("invoke"));
        }

        @Test
        @DisplayName("validate() 对 codeType 给出警告")
        void warnOnCodeType() {
            InjectRequest cmd = new InjectRequest();
            cmd.setProbeType("SNAPSHOT");
            cmd.setInjectionLocation("method_enter");
            cmd.setCodeType("EXPRESSION");

            ValidationResult result = handler.validate(cmd);
            assertTrue(result.isValid());
            assertFalse(result.getWarnings().isEmpty());
        }

        @Test
        @DisplayName("validate() 无 codeType 时通过")
        void acceptWithoutCodeType() {
            InjectRequest cmd = new InjectRequest();
            cmd.setProbeType("SNAPSHOT");
            cmd.setInjectionLocation("method_enter");

            ValidationResult result = handler.validate(cmd);
            assertTrue(result.isValid());
            assertTrue(result.getWarnings().isEmpty());
        }
    }

    @Nested
    @DisplayName("TraceProbeHandler 测试")
    class TraceProbeHandlerTest {

        private final TraceProbeHandler handler = new TraceProbeHandler();

        @Test
        @DisplayName("自描述属性: probeType=TRACE, usesCode=false, supportedLocations 只有 method_around")
        void selfDescription() {
            assertEquals("TRACE", handler.getProbeType());
            assertFalse(handler.usesCode());
            Set<String> locations = handler.supportedInjectionLocations();
            assertTrue(locations.contains("method_around"));
            assertEquals(1, locations.size());
        }

        @Test
        @DisplayName("validate() 拒绝 method_enter (TRACE 只支持 method_around)")
        void rejectMethodEnter() {
            InjectRequest cmd = new InjectRequest();
            cmd.setProbeType("TRACE");
            cmd.setInjectionLocation("method_enter");

            ValidationResult result = handler.validate(cmd);
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("validate() 通过 method_around 请求")
        void acceptValidRequest() {
            InjectRequest cmd = new InjectRequest();
            cmd.setProbeType("TRACE");
            cmd.setInjectionLocation("method_around");

            ValidationResult result = handler.validate(cmd);
            assertTrue(result.isValid());
        }
    }

    @Nested
    @DisplayName("ProbeHandlerRegistry 测试")
    class ProbeHandlerRegistryTest {

        @Test
        @DisplayName("注册/查找/卸载")
        void registerGetAndUnregister() {
            LogProbeHandler handler = new LogProbeHandler();
            ProbeHandlerRegistry.getInstance().register(handler);

            assertTrue(ProbeHandlerRegistry.getInstance().get("LOG").isPresent());
            assertSame(handler, ProbeHandlerRegistry.getInstance().get("LOG").orElse(null));

            ProbeHandlerRegistry.getInstance().unregisterAll(Collections.singletonList(handler));
            assertFalse(ProbeHandlerRegistry.getInstance().get("LOG").isPresent());
        }

        @Test
        @DisplayName("大小写不敏感匹配")
        void caseInsensitiveLookup() {
            LogProbeHandler handler = new LogProbeHandler();
            ProbeHandlerRegistry.getInstance().register(handler);

            assertTrue(ProbeHandlerRegistry.getInstance().get("log").isPresent());
            assertTrue(ProbeHandlerRegistry.getInstance().get("LOG").isPresent());
            assertTrue(ProbeHandlerRegistry.getInstance().get("Log").isPresent());
        }
    }

    @Nested
    @DisplayName("AbstractProbeHandler 通用校验测试")
    class AbstractProbeHandlerValidationTest {

        private final LogProbeHandler handler = new LogProbeHandler();

        @Test
        @DisplayName("位置不支持时拒绝")
        void rejectUnsupportedLocation() {
            InjectRequest cmd = new InjectRequest();
            cmd.setProbeType("LOG");
            cmd.setInjectionLocation("unknown_location");
            cmd.setCode("hello");
            cmd.setCodeType("EXPRESSION");

            ValidationResult result = handler.validate(cmd);
            assertFalse(result.isValid());
        }
    }
}
