package fun.efto.luna.core.plugin.builtin;

import fun.efto.luna.core.asm.injector.AroundMethodInjector;
import fun.efto.luna.core.asm.injector.EnterMethodInjector;
import fun.efto.luna.core.asm.injector.ExitMethodInjector;
import fun.efto.luna.core.injection.target.type.MethodInjectionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
@DisplayName("MethodInjectionPlugin æµ‹è¯•")
public class MethodInjectionPluginTest {

    private MethodInjectionPlugin plugin;
    private TestPluginContext ctx;

    @BeforeEach
    void setUp() {
        plugin = new MethodInjectionPlugin();
        ctx = new TestPluginContext();
    }

    @Test
    @DisplayName("initialize ä¸æŠ›å¼‚å¸¸")
    void testInitializeNoException() {
        assertDoesNotThrow(() -> plugin.initialize(ctx));
    }

    @Test
    @DisplayName("æ³¨å†Œäº?3 ä¸?InjectionType")
    void testRegisteredInjectionTypes() {
        plugin.initialize(ctx);
        assertEquals(3, ctx.getInjectionTypes().size());
        assertTrue(ctx.getInjectionTypes().contains(MethodInjectionType.ENTER));
        assertTrue(ctx.getInjectionTypes().contains(MethodInjectionType.EXIT));
        assertTrue(ctx.getInjectionTypes().contains(MethodInjectionType.AROUND));
    }

    @Test
    @DisplayName("æ³¨å†Œäº?3 ä¸?BytecodeInjector")
    void testRegisteredInjectors() {
        plugin.initialize(ctx);
        assertEquals(3, ctx.getInjectors().size());
        assertInstanceOf(EnterMethodInjector.class, ctx.getInjectors().get(MethodInjectionType.ENTER));
        assertInstanceOf(ExitMethodInjector.class, ctx.getInjectors().get(MethodInjectionType.EXIT));
        assertInstanceOf(AroundMethodInjector.class, ctx.getInjectors().get(MethodInjectionType.AROUND));
    }

    @Test
    @DisplayName("æ³¨å†Œäº?3 ä¸?RuleConverter")
    void testRegisteredRuleConverters() {
        plugin.initialize(ctx);
        assertEquals(3, ctx.getTypedRuleConverters().size());
        assertNotNull(ctx.getTypedRuleConverters().get(MethodInjectionType.ENTER));
        assertNotNull(ctx.getTypedRuleConverters().get(MethodInjectionType.EXIT));
        assertNotNull(ctx.getTypedRuleConverters().get(MethodInjectionType.AROUND));
    }

    @Test
    @DisplayName("插件元信息正确")
    void testPluginMetadata() {
        assertEquals("method-injection", plugin.getId());
        assertEquals("方法注入", plugin.getDisplayName());
        assertEquals("1.0.0", plugin.getVersion());
        assertEquals("Luna Core Team", plugin.getAuthor());
        assertEquals("injection", plugin.getCategory());
        assertTrue(plugin.isBuiltin());
        assertTrue(plugin.getDependencies().isEmpty());
    }
}
