package fun.efto.luna.core.plugin;

import fun.efto.luna.core.common.RingBuffer;
import fun.efto.luna.core.injection.port.Retransformer;
import fun.efto.luna.core.plugin.lifecycle.AffectedClassTracker;
import fun.efto.luna.core.plugin.lifecycle.PluginManagerImpl;
import fun.efto.luna.core.plugin.lifecycle.ReadyGate;
import fun.efto.luna.core.probe.ProbeOutput;
import fun.efto.luna.core.rule.InjectionRule;
import fun.efto.luna.core.rule.RuleManager;
import fun.efto.luna.core.rule.RuleStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:00
 */
@DisplayName("插件卸载测试")
public class UnloadTest {

    private PluginManagerImpl pluginManager;
    private ReadyGate readyGate;

    @BeforeEach
    void setUp() {
        readyGate = new ReadyGate();
        pluginManager = new PluginManagerImpl(
            readyGate,
            new DefaultLogEmitter(),
            ProbeOutput.BUFFER,
            (Retransformer) className -> {},
            null,
            null
        );
        AffectedClassTracker.clear();
    }

    @AfterEach
    void tearDown() {
        AffectedClassTracker.clear();
    }

    @Test
    @DisplayName("卸载不存在的插件返回失败")
    void testUnloadNonExistentPlugin() {
        PluginUnloadResult result = pluginManager.unload("nonexistent");
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("not found"));
    }

    @Test
    @DisplayName("被依赖的插件不可卸载")
    void testUnloadDependedPlugin() {
        LunaPlugin base = new TestPlugin("base", Collections.emptyList());
        LunaPlugin dependent = new TestPlugin("dependent", Arrays.asList("base"));
        pluginManager.initializeAll(Arrays.asList(base, dependent));

        PluginUnloadResult result = pluginManager.unload("base");
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("required by"));
    }

    @Test
    @DisplayName("成功卸载插件后状态变为UNLOADED")
    void testUnloadNonBuiltinPlugin() {
        LunaPlugin plugin = new TestPlugin("removable", Collections.emptyList());
        pluginManager.initializeAll(Arrays.asList(plugin));

        assertEquals(PluginState.ACTIVE, pluginManager.getState("removable"));

        PluginUnloadResult result = pluginManager.unload("removable");
        assertTrue(result.isSuccess());
        assertEquals(PluginState.UNLOADED, pluginManager.getState("removable"));
        assertNull(pluginManager.getPlugin("removable"));
    }

    @Test
    @DisplayName("卸载插件后相关规则被挂起")
    void testSuspendOrphanedRulesOnUnload() {
        LunaPlugin plugin = new TestPlugin("rule-provider", Collections.emptyList());
        pluginManager.initializeAll(Arrays.asList(plugin));

        InjectionRule rule = new InjectionRule();
        rule.setInjectionType("method-enter");
        rule.setStatus(RuleStatus.ACTIVE);
        RuleManager.getInstance().addRule(rule);

        PluginUnloadResult result = pluginManager.unload("rule-provider");
        assertTrue(result.isSuccess());
    }

    private static class TestPlugin implements LunaPlugin {
        private final String id;
        private final List<String> dependencies;

        TestPlugin(String id, List<String> dependencies) {
            this.id = id;
            this.dependencies = dependencies;
        }

        @Override public String getId() { return id; }
        @Override public String getDisplayName() { return id; }
        @Override public String getVersion() { return "1.0.0"; }
        @Override public String getAuthor() { return "test"; }
        @Override public String getCategory() { return "test"; }
        @Override public List<String> getDependencies() { return dependencies; }
        @Override public void initialize(PluginContext context) {}
        @Override public void destroy() {}
        @Override public void getControllers(List<LunaController> controllers) {}
    }
}
