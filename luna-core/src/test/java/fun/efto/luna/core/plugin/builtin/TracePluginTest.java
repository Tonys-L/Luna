package fun.efto.luna.core.plugin.builtin;

import fun.efto.luna.core.plugin.handler.TraceExpressionHandler;
import fun.efto.luna.core.rule.template.RuleTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
@DisplayName("TracePlugin æµ‹è¯•")
public class TracePluginTest {

    private TracePlugin plugin;
    private TestPluginContext ctx;

    @BeforeEach
    void setUp() {
        plugin = new TracePlugin();
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
        assertInstanceOf(TraceExpressionHandler.class, ctx.getExpressionHandlers().get(0));
    }

    @Test
    @DisplayName("ä¾èµ– method-injection æ’ä»¶")
    void testDependencies() {
        List<String> deps = plugin.getDependencies();
        assertEquals(1, deps.size());
        assertEquals("method-injection", deps.get(0));
    }

    @Test
    @DisplayName("getTemplates è¿”å›ž trace æ¨¡æ¿")
    void testGetTemplates() {
        List<RuleTemplate> templates = new ArrayList<>();
        plugin.getTemplates(templates);
        assertFalse(templates.isEmpty());
        assertTrue(templates.stream().anyMatch(t -> "method-timing".equals(t.getName())));
        assertTrue(templates.stream().anyMatch(t -> "method-timing-threshold".equals(t.getName())));
        assertTrue(templates.stream().anyMatch(t -> "slow-method-alert".equals(t.getName())));
    }

    @Test
    @DisplayName("插件元信息正确")
    void testPluginMetadata() {
        assertEquals("trace", plugin.getId());
        assertEquals("方法耗时追踪", plugin.getDisplayName());
        assertEquals("1.0.0", plugin.getVersion());
        assertEquals("Luna Core Team", plugin.getAuthor());
        assertEquals("performance", plugin.getCategory());
        assertTrue(plugin.isBuiltin());
    }
}
