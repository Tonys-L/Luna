package fun.efto.luna.core.plugin;

import fun.efto.luna.core.plugin.registry.ExpressionHandlerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
public class ExpressionHandlerRegistryTest {

    @AfterEach
    void tearDown() {
        ExpressionHandlerRegistry.getInstance().clear();
    }

    @Test
    void testRegisterAndGet() {
        ExpressionHandler handler = new ExpressionHandler() {
            @Override
            public String getProtocol() {
                return "log";
            }

            @Override
            public void generateBytecode(GenerateContext ctx) {
            }
        };

        ExpressionHandlerRegistry.getInstance().register(handler);
        assertSame(handler, ExpressionHandlerRegistry.getInstance().get("log").orElse(null));
    }

    @Test
    void testHasProtocol() {
        ExpressionHandler handler = new ExpressionHandler() {
            @Override
            public String getProtocol() {
                return "log";
            }

            @Override
            public void generateBytecode(GenerateContext ctx) {
            }
        };

        ExpressionHandlerRegistry.getInstance().register(handler);
        assertTrue(ExpressionHandlerRegistry.getInstance().hasProtocol("log:message"));
        assertFalse(ExpressionHandlerRegistry.getInstance().hasProtocol("unknown:message"));
    }

    @Test
    void testUnregisterAll() {
        ExpressionHandler handler = new ExpressionHandler() {
            @Override
            public String getProtocol() {
                return "log";
            }

            @Override
            public void generateBytecode(GenerateContext ctx) {
            }
        };

        ExpressionHandlerRegistry.getInstance().register(handler);
        ExpressionHandlerRegistry.getInstance().unregisterAll(Collections.singletonList(handler));
        assertFalse(ExpressionHandlerRegistry.getInstance().get("log").isPresent());
    }
}
