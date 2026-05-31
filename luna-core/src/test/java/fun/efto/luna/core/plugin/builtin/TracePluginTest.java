package fun.efto.luna.core.plugin.builtin;

import fun.efto.luna.core.plugin.builtin.trace.TracePlugin;
import fun.efto.luna.core.plugin.builtin.trace.TraceProbeHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:00
 */
@DisplayName("TracePlugin 测试")
public class TracePluginTest {

    private TracePlugin plugin;
    private TestPluginContext ctx;

    @BeforeEach
    void setUp() {
        plugin = new TracePlugin();
        ctx = new TestPluginContext();
    }

    @Test
    @DisplayName("initialize 不抛异常")
    void testInitializeNoException() {
        assertDoesNotThrow(() -> plugin.initialize(ctx));
    }

    @Test
    @DisplayName("注册了1个ProbeHandler")
    void testRegisteredProbeHandler() {
        plugin.initialize(ctx);
        assertEquals(1, ctx.getProbeHandlers().size());
        assertInstanceOf(TraceProbeHandler.class, ctx.getProbeHandlers().get(0));
    }

    @Test
    @DisplayName("依赖 method-target 核心能力")
    void testDependencies() {
        List<String> deps = plugin.getDependencies();
        assertEquals(1, deps.size());
        assertEquals("method-target", deps.get(0));
    }

    @Test
    @DisplayName("registerTemplate 注册 trace 模板")
    void testRegisterTemplates() {
        plugin.initialize(ctx);
        assertFalse(ctx.getTemplates().isEmpty());
        assertTrue(ctx.getTemplates().stream().anyMatch(t -> "method-timing".equals(t.getName())));
        assertTrue(ctx.getTemplates().stream().anyMatch(t -> "method-timing-threshold".equals(t.getName())));
        assertTrue(ctx.getTemplates().stream().anyMatch(t -> "slow-method-alert".equals(t.getName())));
    }

    @Test
    @DisplayName("插件元信息正确")
    void testPluginMetadata() {
        assertEquals("trace", plugin.getId());
        assertEquals("方法耗时追踪", plugin.getDisplayName());
        assertEquals("1.0.0", plugin.getVersion());
        assertEquals("Luna Core Team", plugin.getAuthor());
        assertEquals("performance", plugin.getCategory());
    }
}
