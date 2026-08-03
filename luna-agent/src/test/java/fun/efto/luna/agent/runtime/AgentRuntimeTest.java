package fun.efto.luna.agent.runtime;

import fun.efto.luna.core.plugin.LunaPlugin;
import fun.efto.luna.core.plugin.lifecycle.ReadyGate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/07 20:00
 */
class AgentRuntimeTest {

    @Test
    void builtinPluginProvider_returnsNonNullList() {
        List<LunaPlugin> plugins = BuiltinPluginProvider.builtins();
        assertNotNull(plugins, "builtins() should never return null");
    }

    @Test
    void builtinPluginProvider_returnsUnmodifiableList() {
        List<LunaPlugin> plugins = BuiltinPluginProvider.builtins();
        assertThrows(UnsupportedOperationException.class, () -> plugins.add(null),
                "builtins() should return an unmodifiable list");
    }

    @Test
    void readyGate_markReady_setsReadyState() {
        ReadyGate gate = new ReadyGate();
        assertFalse(gate.isReady(), "ReadyGate should not be ready initially");
        gate.markReady();
        assertTrue(gate.isReady(), "ReadyGate should be ready after markReady()");
    }

    @Test
    void agentRuntimeContext_holdsAllReferences() {
        AgentRuntimeContext context = new AgentRuntimeContext(
                null, null, null, null, null);
        assertNotNull(context, "AgentRuntimeContext should be constructable");
        assertNull(context.getInstrumentation(), "Unset instrumentation should be null");
        assertNull(context.getInjectionService(), "Unset injectionService should be null");
        assertNull(context.getPluginManager(), "Unset pluginManager should be null");
        assertNull(context.getWebServer(), "Unset webServer should be null");
        assertNull(context.getRetransformer(), "Unset retransformer should be null");
    }

    @Test
    void agentRuntime_getInstance_beforeStart_returnsNull() {
        assertNull(AgentRuntime.getInstance(), "Instance should be null before start");
    }
}
