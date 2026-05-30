package fun.efto.luna.core.plugin.builtin;

import fun.efto.luna.core.injection.code.CodeType;
import fun.efto.luna.core.plugin.builtin.snapshot.SnapshotPlugin;
import fun.efto.luna.core.plugin.builtin.snapshot.SnapshotExpressionHandler;
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
    @DisplayName("注册了1个ExpressionHandler")
    void testRegisteredExpressionHandler() {
        plugin.initialize(ctx);
        assertEquals(1, ctx.getExpressionHandlers().size());
        assertInstanceOf(SnapshotExpressionHandler.class, ctx.getExpressionHandlers().get(0));
    }

    @Test
    @DisplayName("注册了SNAPSHOT Assembler")
    void testRegisteredAssembler() {
        plugin.initialize(ctx);
        assertNotNull(ctx.getAssemblers().get(CodeType.SNAPSHOT));
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
