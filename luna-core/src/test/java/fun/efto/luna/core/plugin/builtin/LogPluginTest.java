package fun.efto.luna.core.plugin.builtin;

import fun.efto.luna.core.injection.code.type.CodeType;
import fun.efto.luna.core.plugin.handler.LogExpressionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
@DisplayName("LogPlugin æµ‹è¯•")
public class LogPluginTest {

    private LogPlugin plugin;
    private TestPluginContext ctx;

    @BeforeEach
    void setUp() {
        plugin = new LogPlugin();
        ctx = new TestPluginContext();
    }

    @Test
    @DisplayName("initialize ä¸æŠ›å¼‚å¸¸")
    void testInitializeNoException() {
        assertDoesNotThrow(() -> plugin.initialize(ctx));
    }

    @Test
    @DisplayName("æ³¨å†Œäº?1 ä¸?ExpressionHandler")
    void testRegisteredExpressionHandler() {
        plugin.initialize(ctx);
        assertEquals(1, ctx.getExpressionHandlers().size());
        assertInstanceOf(LogExpressionHandler.class, ctx.getExpressionHandlers().get(0));
    }

    @Test
    @DisplayName("æ³¨å†Œäº?EXPRESSION Assembler")
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
        assertTrue(plugin.isBuiltin());
        assertTrue(plugin.getDependencies().isEmpty());
    }
}
