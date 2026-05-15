package fun.efto.luna.core.plugin.builtin;

import fun.efto.luna.core.asm.injector.AfterLineInjector;
import fun.efto.luna.core.asm.injector.BeforeLineInjector;
import fun.efto.luna.core.injection.target.type.LineNumberInjectionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
@DisplayName("LineInjectionPlugin æµ‹è¯•")
public class LineInjectionPluginTest {

    private LineInjectionPlugin plugin;
    private TestPluginContext ctx;

    @BeforeEach
    void setUp() {
        plugin = new LineInjectionPlugin();
        ctx = new TestPluginContext();
    }

    @Test
    @DisplayName("initialize ä¸æŠ›å¼‚å¸¸")
    void testInitializeNoException() {
        assertDoesNotThrow(() -> plugin.initialize(ctx));
    }

    @Test
    @DisplayName("æ³¨å†Œäº?2 ä¸?InjectionType")
    void testRegisteredInjectionTypes() {
        plugin.initialize(ctx);
        assertEquals(2, ctx.getInjectionTypes().size());
        assertTrue(ctx.getInjectionTypes().contains(LineNumberInjectionType.BEFORE));
        assertTrue(ctx.getInjectionTypes().contains(LineNumberInjectionType.AFTER));
    }

    @Test
    @DisplayName("æ³¨å†Œäº?2 ä¸?BytecodeInjector")
    void testRegisteredInjectors() {
        plugin.initialize(ctx);
        assertEquals(2, ctx.getInjectors().size());
        assertInstanceOf(BeforeLineInjector.class, ctx.getInjectors().get(LineNumberInjectionType.BEFORE));
        assertInstanceOf(AfterLineInjector.class, ctx.getInjectors().get(LineNumberInjectionType.AFTER));
    }

    @Test
    @DisplayName("æ³¨å†Œäº?2 ä¸?RuleConverter")
    void testRegisteredRuleConverters() {
        plugin.initialize(ctx);
        assertEquals(2, ctx.getTypedRuleConverters().size());
        assertNotNull(ctx.getTypedRuleConverters().get(LineNumberInjectionType.BEFORE));
        assertNotNull(ctx.getTypedRuleConverters().get(LineNumberInjectionType.AFTER));
    }

    @Test
    @DisplayName("插件元信息正确")
    void testPluginMetadata() {
        assertEquals("line-injection", plugin.getId());
        assertEquals("行号注入", plugin.getDisplayName());
        assertEquals("1.0.0", plugin.getVersion());
        assertEquals("Luna Core Team", plugin.getAuthor());
        assertEquals("injection", plugin.getCategory());
        assertTrue(plugin.isBuiltin());
        assertTrue(plugin.getDependencies().isEmpty());
    }
}
