package fun.efto.luna.core.plugin.builtin;

import fun.efto.luna.core.injection.code.CodeType;
import fun.efto.luna.core.plugin.builtin.log.LogPlugin;
import fun.efto.luna.core.plugin.builtin.log.LogExpressionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:00
 */
@DisplayName("LogPlugin 测试")
public class LogPluginTest {

    private LogPlugin plugin;
    private TestPluginContext ctx;

    @BeforeEach
    void setUp() {
        plugin = new LogPlugin();
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
        assertInstanceOf(LogExpressionHandler.class, ctx.getExpressionHandlers().get(0));
    }

    @Test
    @DisplayName("注册了EXPRESSION Assembler")
    void testRegisteredAssembler() {
        plugin.initialize(ctx);
        assertNotNull(ctx.getAssemblers().get(CodeType.EXPRESSION));
    }

    @Test
    @DisplayName("插件元信息正确")
    void testPluginMetadata() {
        assertEquals("log", plugin.getId());
        assertEquals("日志注入", plugin.getDisplayName());
        assertEquals("1.0.0", plugin.getVersion());
        assertEquals("Luna Core Team", plugin.getAuthor());
        assertEquals("injection", plugin.getCategory());
        assertTrue(plugin.getDependencies().isEmpty());
    }
}
