package fun.efto.luna.core.plugin;

import fun.efto.luna.core.injection.CodeEngine;
import fun.efto.luna.core.injection.CodeEngineRegistry;
import fun.efto.luna.core.injection.code.CompiledCode;
import fun.efto.luna.core.injection.PersistentInjection;
import fun.efto.luna.core.plugin.builtin.log.LogProbeHandler;
import fun.efto.luna.core.plugin.builtin.snapshot.SnapshotProbeHandler;
import fun.efto.luna.core.plugin.builtin.trace.TraceProbeHandler;
import fun.efto.luna.core.plugin.registry.ProbeHandlerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/01 02:30
 */
public class ProbeControllerTest {

    @BeforeEach
    void setUp() {
        ProbeHandlerRegistry.getInstance().clear();
        CodeEngineRegistry.getInstance().clear();
    }

    @AfterEach
    void tearDown() {
        ProbeHandlerRegistry.getInstance().clear();
        CodeEngineRegistry.getInstance().clear();
    }

    @Nested
    @DisplayName("ProbeHandlerRegistry.getRegisteredTypes() 测试")
    class ProbeHandlerRegistryTypesTest {

        @Test
        @DisplayName("注册后 getRegisteredTypes 返回所有已注册的 probeType")
        void getRegisteredTypesReturnsAllTypes() {
            ProbeHandlerRegistry.getInstance().register(new LogProbeHandler());
            ProbeHandlerRegistry.getInstance().register(new SnapshotProbeHandler());
            ProbeHandlerRegistry.getInstance().register(new TraceProbeHandler());

            Set<String> types = ProbeHandlerRegistry.getInstance().getRegisteredTypes();

            assertEquals(3, types.size());
            assertTrue(types.contains("LOG"));
            assertTrue(types.contains("SNAPSHOT"));
            assertTrue(types.contains("TRACE"));
        }

        @Test
        @DisplayName("空注册表返回空集合")
        void emptyRegistryReturnsEmptySet() {
            Set<String> types = ProbeHandlerRegistry.getInstance().getRegisteredTypes();
            assertTrue(types.isEmpty());
        }
    }

    @Nested
    @DisplayName("CodeEngineRegistry.getRegisteredTypes() 测试")
    class CodeEngineRegistryTypesTest {

        @Test
        @DisplayName("注册后 getRegisteredTypes 返回所有已注册的 codeType")
        void getRegisteredTypesReturnsAllTypes() {
            CodeEngineRegistry.getInstance().register(new StubCodeEngine("EXPRESSION"));
            CodeEngineRegistry.getInstance().register(new StubCodeEngine("GROOVY"));

            Set<String> types = CodeEngineRegistry.getInstance().getRegisteredTypes();

            assertEquals(2, types.size());
            assertTrue(types.contains("EXPRESSION"));
            assertTrue(types.contains("GROOVY"));
        }

        @Test
        @DisplayName("空注册表返回空集合")
        void emptyRegistryReturnsEmptySet() {
            Set<String> types = CodeEngineRegistry.getInstance().getRegisteredTypes();
            assertTrue(types.isEmpty());
        }
    }

    @Nested
    @DisplayName("Probe 能力描述 测试")
    class ProbeCapabilityTest {

        @Test
        @DisplayName("每个 ProbeHandler 返回正确的 probeType、usesCode、supportedInjectionLocations")
        void probeHandlerSelfDescription() {
            ProbeHandlerRegistry.getInstance().register(new LogProbeHandler());
            ProbeHandlerRegistry.getInstance().register(new SnapshotProbeHandler());
            ProbeHandlerRegistry.getInstance().register(new TraceProbeHandler());

            for (String probeType : ProbeHandlerRegistry.getInstance().getRegisteredTypes()) {
                ProbeHandler handler = ProbeHandlerRegistry.getInstance().get(probeType).orElse(null);
                assertNotNull(handler, "Handler should exist for type: " + probeType);
                assertEquals(probeType, handler.getProbeType());

                Set<String> locations = handler.supportedInjectionLocations();
                assertNotNull(locations, "supportedInjectionLocations should not be null for: " + probeType);
                assertFalse(locations.isEmpty(), "supportedInjectionLocations should not be empty for: " + probeType);
            }
        }

        @Test
        @DisplayName("LOG 探针 usesCode=true，SNAPSHOT/TRACE usesCode=false")
        void probeUsesCode() {
            LogProbeHandler log = new LogProbeHandler();
            SnapshotProbeHandler snapshot = new SnapshotProbeHandler();
            TraceProbeHandler trace = new TraceProbeHandler();

            assertTrue(log.usesCode());
            assertFalse(snapshot.usesCode());
            assertFalse(trace.usesCode());
        }
    }

    @Nested
    @DisplayName("Engine 能力描述 测试")
    class EngineCapabilityTest {

        @Test
        @DisplayName("每个 CodeEngine 返回正确的 codeType")
        void codeEngineSelfDescription() {
            CodeEngineRegistry.getInstance().register(new StubCodeEngine("EXPRESSION"));

            for (String codeType : CodeEngineRegistry.getInstance().getRegisteredTypes()) {
                CodeEngine engine = CodeEngineRegistry.getInstance().get(codeType).orElse(null);
                assertNotNull(engine, "Engine should exist for type: " + codeType);
                assertEquals(codeType, engine.getCodeType());
            }
        }
    }

    private static class StubCodeEngine implements CodeEngine {
        private final String codeType;

        StubCodeEngine(String codeType) {
            this.codeType = codeType;
        }

        @Override
        public String getCodeType() {
            return codeType;
        }

        @Override
        public CompiledCode compile(PersistentInjection persistent) {
            return new CompiledCode(null, "");
        }
    }
}
