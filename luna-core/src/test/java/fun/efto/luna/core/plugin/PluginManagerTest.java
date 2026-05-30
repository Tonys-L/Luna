package fun.efto.luna.core.plugin;

import fun.efto.luna.core.injection.port.Retransformer;
import fun.efto.luna.core.plugin.lifecycle.PluginManagerImpl;
import fun.efto.luna.core.plugin.lifecycle.ReadyGate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
@DisplayName("PluginManager 测试")
public class PluginManagerTest {

    private PluginManagerImpl pluginManager;
    private ReadyGate readyGate;

    @BeforeEach
    void setUp() {
        readyGate = new ReadyGate();
        pluginManager = new PluginManagerImpl(
            readyGate,
            new DefaultLogEmitter(),
            fun.efto.luna.core.probe.ProbeOutput.BUFFER,
            (Retransformer) className -> {},
            null,
            null
        );
    }

    @Test
    @DisplayName("初始化3个插件，验证listPlugins返回正确数量")
    void testInitializeAll() {
        List<LunaPlugin> plugins = new ArrayList<>();
        plugins.add(new StubPlugin("plugin-a", Collections.emptyList()));
        plugins.add(new StubPlugin("plugin-b", Collections.emptyList()));
        plugins.add(new StubPlugin("plugin-c", Collections.emptyList()));

        pluginManager.initializeAll(plugins);

        List<PluginInfo> result = pluginManager.listPlugins();
        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("依赖排序正确：被依赖的插件先初始化")
    void testDependencyResolution() {
        List<String> initOrder = Collections.synchronizedList(new ArrayList<>());

        StubPlugin c = new StubPlugin("c", Arrays.asList("a", "b")) {
            @Override
            public void initialize(PluginContext ctx) {
                initOrder.add(getId());
            }
        };
        StubPlugin b = new StubPlugin("b", Arrays.asList("a")) {
            @Override
            public void initialize(PluginContext ctx) {
                initOrder.add(getId());
            }
        };
        StubPlugin a = new StubPlugin("a", Collections.emptyList()) {
            @Override
            public void initialize(PluginContext ctx) {
                initOrder.add(getId());
            }
        };

        List<LunaPlugin> plugins = Arrays.asList(c, b, a);
        pluginManager.initializeAll(plugins);

        assertEquals(Arrays.asList("a", "b", "c"), initOrder);
    }

    @Test
    @DisplayName("循环依赖抛出异常")
    void testCyclicDependency() {
        LunaPlugin a = new StubPlugin("a", Arrays.asList("b"));
        LunaPlugin b = new StubPlugin("b", Arrays.asList("a"));

        assertThrows(IllegalStateException.class, () -> {
            pluginManager.initializeAll(Arrays.asList(a, b));
        });
    }

    @Test
    @DisplayName("缺少依赖抛出异常")
    void testMissingDependency() {
        LunaPlugin a = new StubPlugin("a", Arrays.asList("nonexistent"));

        assertThrows(IllegalStateException.class, () -> {
            pluginManager.initializeAll(Arrays.asList(a));
        });
    }

    @Test
    @DisplayName("插件状态正确：初始化后为ACTIVE")
    void testPluginState() {
        LunaPlugin plugin = new StubPlugin("test-plugin", Collections.emptyList());
        pluginManager.initializeAll(Arrays.asList(plugin));

        assertEquals(PluginState.ACTIVE, pluginManager.getState("test-plugin"));
    }

    @Test
    @DisplayName("getPlugin返回正确的插件实例")
    void testGetPlugin() {
        LunaPlugin plugin = new StubPlugin("test-plugin", Collections.emptyList());
        pluginManager.initializeAll(Arrays.asList(plugin));

        LunaPlugin result = pluginManager.getPlugin("test-plugin");
        assertNotNull(result);
        assertEquals("test-plugin", result.getId());
    }

    @Test
    @DisplayName("getPlugin返回null对于不存在的插件")
    void testGetPluginNotFound() {
        assertNull(pluginManager.getPlugin("nonexistent"));
    }

    @Test
    @DisplayName("getState返回UNLOADED对于不存在的插件")
    void testGetStateNotFound() {
        assertEquals(PluginState.UNLOADED, pluginManager.getState("nonexistent"));
    }

    @Test
    @DisplayName("disable插件：状态从ACTIVE变为DISABLED")
    void testDisablePlugin() {
        LunaPlugin plugin = new StubPlugin("ext-plugin", Collections.emptyList());
        pluginManager.initializeAll(Arrays.asList(plugin));

        assertEquals(PluginState.ACTIVE, pluginManager.getState("ext-plugin"));

        pluginManager.disable("ext-plugin");

        assertEquals(PluginState.DISABLED, pluginManager.getState("ext-plugin"));
    }

    @Test
    @DisplayName("disable不存在的插件抛出IllegalArgumentException")
    void testDisableNonExistentPluginThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            pluginManager.disable("nonexistent");
        });
    }

    @Test
    @DisplayName("disable非ACTIVE插件抛出IllegalStateException")
    void testDisableNonActivePluginThrows() {
        LunaPlugin plugin = new StubPlugin("ext-plugin", Collections.emptyList());
        pluginManager.initializeAll(Arrays.asList(plugin));

        pluginManager.disable("ext-plugin");
        assertEquals(PluginState.DISABLED, pluginManager.getState("ext-plugin"));

        assertThrows(IllegalStateException.class, () -> {
            pluginManager.disable("ext-plugin");
        });
    }

    @Test
    @DisplayName("enable DISABLED插件：状态恢复为ACTIVE")
    void testEnableDisabledPlugin() {
        LunaPlugin plugin = new StubPlugin("ext-plugin", Collections.emptyList());
        pluginManager.initializeAll(Arrays.asList(plugin));

        pluginManager.disable("ext-plugin");
        assertEquals(PluginState.DISABLED, pluginManager.getState("ext-plugin"));

        pluginManager.enable("ext-plugin");
        assertEquals(PluginState.ACTIVE, pluginManager.getState("ext-plugin"));
    }

    @Test
    @DisplayName("enable不存在的插件抛出IllegalArgumentException")
    void testEnableNonExistentPluginThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            pluginManager.enable("nonexistent");
        });
    }

    @Test
    @DisplayName("enable非DISABLED插件抛出IllegalStateException")
    void testEnableNonDisabledPluginThrows() {
        LunaPlugin plugin = new StubPlugin("ext-plugin", Collections.emptyList());
        pluginManager.initializeAll(Arrays.asList(plugin));

        assertThrows(IllegalStateException.class, () -> {
            pluginManager.enable("ext-plugin");
        });
    }

    @Test
    @DisplayName("disable/enable触发LifecycleListener事件")
    void testDisableEnableFiresListenerEvents() {
        LunaPlugin plugin = new StubPlugin("ext-plugin", Collections.emptyList());
        pluginManager.initializeAll(Arrays.asList(plugin));

        List<String> events = new ArrayList<>();
        pluginManager.addListener(new PluginLifecycleListener() {
            @Override public void onLoaded(PluginInfo info) { events.add("loaded:" + info.getId()); }
            @Override public void onUnloaded(PluginInfo info) { events.add("unloaded:" + info.getId()); }
            @Override public void onUpdated(PluginInfo info, String oldV, String newV) { events.add("updated:" + info.getId()); }
            @Override public void onLoadFailed(String pluginId, String errorMessage) { events.add("loadFailed:" + pluginId); }
            @Override public void onUnloadFailed(String pluginId, String errorMessage) { events.add("unloadFailed:" + pluginId); }
            @Override public void onDisabled(PluginInfo info) { events.add("disabled:" + info.getId()); }
            @Override public void onEnabled(PluginInfo info) { events.add("enabled:" + info.getId()); }
        });

        pluginManager.disable("ext-plugin");
        pluginManager.enable("ext-plugin");

        assertEquals(Arrays.asList("disabled:ext-plugin", "enabled:ext-plugin"), events);
    }

    private static class StubPlugin implements LunaPlugin {
        private final String id;
        private final List<String> dependencies;

        StubPlugin(String id, List<String> dependencies) {
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
