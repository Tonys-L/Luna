package fun.efto.luna.core.plugin.builtin;

import fun.efto.luna.core.plugin.builtin.snapshot.SnapshotPlugin;
import fun.efto.luna.core.plugin.builtin.snapshot.SnapshotProbeHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:00
 */
@DisplayName("SnapshotPlugin 测试")
public class SnapshotPluginTest {

    private SnapshotPlugin plugin;
    private TestPluginContext ctx;

    @BeforeEach
    void setUp() {
        plugin = new SnapshotPlugin();
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
        assertInstanceOf(SnapshotProbeHandler.class, ctx.getProbeHandlers().get(0));
    }

    @Test
    @DisplayName("注册了CodeEngine")
    void testRegisteredCodeEngine() {
        plugin.initialize(ctx);
        assertFalse(ctx.getCodeEngines().isEmpty());
    }

    @Test
    @DisplayName("插件元信息正确")
    void testPluginMetadata() {
        assertEquals("snapshot", plugin.getId());
        assertEquals("快照注入", plugin.getDisplayName());
        assertEquals("1.0.0", plugin.getVersion());
        assertEquals("Luna Core Team", plugin.getAuthor());
        assertEquals("injection", plugin.getCategory());
        assertTrue(plugin.getDependencies().isEmpty());
    }
}
