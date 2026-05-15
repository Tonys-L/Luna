package fun.efto.luna.core.plugin;

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
        ExpressionHandlerRegistry.clear();
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

        ExpressionHandlerRegistry.register(handler);
        assertSame(handler, ExpressionHandlerRegistry.get("log"));
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

        ExpressionHandlerRegistry.register(handler);
        assertTrue(ExpressionHandlerRegistry.hasProtocol("log:message"));
        assertFalse(ExpressionHandlerRegistry.hasProtocol("unknown:message"));
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

        ExpressionHandlerRegistry.register(handler);
        ExpressionHandlerRegistry.unregisterAll(Collections.singletonList(handler));
        assertNull(ExpressionHandlerRegistry.get("log"));
    }
}
