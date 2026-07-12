package fun.efto.luna.agent.runtime;

import fun.efto.luna.core.plugin.LunaPlugin;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/07 20:00
 */
class BuiltinPluginProviderTest {

    @Test
    void builtins_returnsNonNullList() {
        List<LunaPlugin> plugins = BuiltinPluginProvider.builtins();
        assertNotNull(plugins, "builtins() should never return null");
    }

    @Test
    void builtins_returnsUnmodifiableList() {
        List<LunaPlugin> plugins = BuiltinPluginProvider.builtins();
        assertThrows(UnsupportedOperationException.class, () -> plugins.add(null),
                "builtins() should return an unmodifiable list");
    }

    @Test
    void builtins_returnsConsistentResults() {
        List<LunaPlugin> first = BuiltinPluginProvider.builtins();
        List<LunaPlugin> second = BuiltinPluginProvider.builtins();
        assertEquals(first.size(), second.size(),
                "Multiple calls to builtins() should return consistent size");
    }
}
