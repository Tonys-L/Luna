package fun.efto.luna.core.plugin;

import fun.efto.luna.core.infra.RingBuffer;
import fun.efto.luna.core.injection.PersistentInjection;
import fun.efto.luna.core.injection.InjectionManager;
import fun.efto.luna.core.injection.InjectionStatus;
import fun.efto.luna.core.injection.port.Retransformer;
import fun.efto.luna.core.injection.target.InjectionLocation;
import fun.efto.luna.core.plugin.lifecycle.AffectedClassTracker;
import fun.efto.luna.core.plugin.lifecycle.PluginManagerImpl;
import fun.efto.luna.core.plugin.lifecycle.PluginRegistrationRecord;
import fun.efto.luna.core.plugin.lifecycle.ReadyGate;
import fun.efto.luna.core.plugin.DefaultLogEmitter;
import fun.efto.luna.core.plugin.registry.InjectionTypeRegistry;
import fun.efto.luna.core.probe.ProbeOutput;
import fun.efto.luna.core.injection.rule.template.RuleTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:45
 */
@DisplayName("插件架构集成测试")
public class PluginIntegrationTest {

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

    @Nested
    @DisplayName("SPI 扫描与初始化")
    class SpiScanTests {

        @Test
        @DisplayName("4个插件通过 initializeAll 正确初始化")
        void testBuiltinPluginsInitialization() {
            List<LunaPlugin> builtins = createBuiltinPlugins();
            pluginManager.initializeAll(builtins);

            List<PluginInfo> plugins = pluginManager.listPlugins();
            assertEquals(4, plugins.size());

            for (PluginInfo info : plugins) {
                assertEquals(PluginState.ACTIVE, info.getState());
            }
        }

        @Test
        @DisplayName("插件可正常卸载")
        void testPluginsCanBeUnloaded() {
            pluginManager.initializeAll(createBuiltinPlugins());

            for (PluginInfo info : pluginManager.listPlugins()) {
                PluginUnloadResult result = pluginManager.unload(info.getId());
                assertTrue(result.isSuccess());
            }
        }
    }

    @Nested
    @DisplayName("Registry 双写校验")
    class DualWriteTests {

        @Test
        @DisplayName("插件注册 InjectionLocation 时同时写入 Registry 和 Record")
        void testInjectionLocationDualWrite() {
            final InjectionLocation testLocation = InjectionLocation.of("test-type", "Test Type", "TEST");
            LunaPlugin plugin = new StubPlugin("test-plugin", Collections.emptyList()) {
                @Override
                public void initialize(PluginContext context) {
                    context.registerInjectionLocation(testLocation);
                }
            };

            pluginManager.initializeAll(Arrays.asList(plugin));

            InjectionLocation resolved = InjectionTypeRegistry.getInstance().get("test-type").orElse(null);
            assertNotNull(resolved, "InjectionLocation should be registered in Registry");
            assertEquals("test-type", resolved.getName());

            PluginRegistrationRecord record = pluginManager.getRecords().get("test-plugin");
            assertNotNull(record);
            assertTrue(record.getInjectionLocations().stream().anyMatch(t -> t.getName().equals("test-type")),
                "InjectionLocation should be in PluginRegistrationRecord");
        }

        @Test
        @DisplayName("卸载插件后 Registry 中的注册被清除")
        void testRegistryCleanupOnUnload() {
            final InjectionLocation testLocation = InjectionLocation.of("cleanup-type", "Cleanup Type", "CLEANUP");
            LunaPlugin plugin = new StubPlugin("cleanup-plugin", Collections.emptyList()) {
                @Override
                public void initialize(PluginContext context) {
                    context.registerInjectionLocation(testLocation);
                }
            };

            pluginManager.initializeAll(Arrays.asList(plugin));
            assertNotNull(InjectionTypeRegistry.getInstance().get("cleanup-type").orElse(null));

            PluginUnloadResult result = pluginManager.unload("cleanup-plugin");
            assertTrue(result.isSuccess());

            assertNull(InjectionTypeRegistry.getInstance().get("cleanup-type").orElse(null),
                "InjectionLocation should be removed from Registry after unload");
        }
    }

    @Nested
    @DisplayName("ReadyGate 启动屏障")
    class ReadyGateTests {

        @Test
        @DisplayName("初始状态 isReady 返回 false")
        void testInitiallyNotReady() {
            assertFalse(readyGate.isReady());
        }

        @Test
        @DisplayName("markReady 后 isReady 返回 true")
        void testReadyAfterMark() {
            readyGate.markReady();
            assertTrue(readyGate.isReady());
        }

        @Test
        @DisplayName("插件初始化完成后应调用 markReady")
        void testMarkReadyAfterInit() {
            pluginManager.initializeAll(createBuiltinPlugins());
            readyGate.markReady();
            assertTrue(readyGate.isReady());
        }
    }

    @Nested
    @DisplayName("规则挂起/恢复机制")
    class RuleSuspendResumeTests {

        @Test
        @DisplayName("卸载插件后引用其 InjectionLocation 的规则被挂起")
        void testRulesSuspendedOnUnload() {
            final InjectionLocation customLocation = InjectionLocation.of("custom-type", "Custom Type", "CUSTOM");
            LunaPlugin plugin = new StubPlugin("type-provider", Collections.emptyList()) {
                @Override
                public void initialize(PluginContext context) {
                    context.registerInjectionLocation(customLocation);
                }
            };

            pluginManager.initializeAll(Arrays.asList(plugin));

            PersistentInjection injection = new PersistentInjection();
            injection.setClazz("com.example.TestService");
            injection.setMethodName("someMethod");
            injection.setInjectionLocation("custom-type");
            injection.setStatus(InjectionStatus.ACTIVE);
            InjectionManager.getInstance().addInjection(injection);

            PluginUnloadResult result = pluginManager.unload("type-provider");
            assertTrue(result.isSuccess());
            assertFalse(result.getSuspendedRuleIds().isEmpty(),
                "Should have suspended rules referencing the unloaded plugin's locations");

            assertEquals(InjectionStatus.SUSPENDED, injection.getStatus());
            assertNotNull(injection.getSuspendReason());
        }

        @Test
        @DisplayName("重新加载同 ID 插件后挂起规则自动恢复")
        void testRulesResumedOnReload() {
            final InjectionLocation customLocation = InjectionLocation.of("resume-type", "Resume Type", "RESUME");

            LunaPlugin plugin = new StubPlugin("reload-provider", Collections.emptyList()) {
                @Override
                public void initialize(PluginContext context) {
                    context.registerInjectionLocation(customLocation);
                }
            };

            pluginManager.initializeAll(Arrays.asList(plugin));

            PersistentInjection injection = new PersistentInjection();
            injection.setClazz("com.example.ResumeService");
            injection.setMethodName("resumeMethod");
            injection.setInjectionLocation("resume-type");
            injection.setStatus(InjectionStatus.ACTIVE);
            InjectionManager.getInstance().addInjection(injection);

            pluginManager.unload("reload-provider");
            assertEquals(InjectionStatus.SUSPENDED, injection.getStatus());

            LunaPlugin reloadedPlugin = new StubPlugin("reload-provider", Collections.emptyList()) {
                @Override
                public void initialize(PluginContext context) {
                    context.registerInjectionLocation(customLocation);
                }
            };
            pluginManager.initializeAll(Arrays.asList(reloadedPlugin));

            InjectionLocation resolved = InjectionTypeRegistry.getInstance().get("resume-type").orElse(null);
            assertNotNull(resolved, "InjectionLocation should be re-registered");
        }
    }

    @Nested
    @DisplayName("PluginLifecycleListener 事件")
    class LifecycleListenerTests {

        @Test
        @DisplayName("插件加载时触发 onLoaded 事件")
        void testOnLoadedEvent() {
            AtomicBoolean loaded = new AtomicBoolean(false);
            AtomicReference<String> loadedId = new AtomicReference<>();

            pluginManager.addListener(new PluginLifecycleListener() {
                @Override
                public void onLoaded(PluginInfo info) {
                    loaded.set(true);
                    loadedId.set(info.getId());
                }
                @Override public void onUnloaded(PluginInfo info) {}
                @Override public void onUpdated(PluginInfo info, String o, String n) {}
                @Override public void onLoadFailed(String id, String err) {}
                @Override public void onUnloadFailed(String id, String err) {}
                @Override public void onDisabled(PluginInfo info) {}
                @Override public void onEnabled(PluginInfo info) {}
            });

            LunaPlugin plugin = new StubPlugin("event-plugin", Collections.emptyList());
            pluginManager.initializeAll(Arrays.asList(plugin));

            assertTrue(loaded.get(), "onLoaded should be called");
            assertEquals("event-plugin", loadedId.get());
        }

        @Test
        @DisplayName("插件卸载时触发 onUnloaded 事件")
        void testOnUnloadedEvent() {
            AtomicBoolean unloaded = new AtomicBoolean(false);
            AtomicReference<String> unloadedId = new AtomicReference<>();

            pluginManager.addListener(new PluginLifecycleListener() {
                @Override
                public void onUnloaded(PluginInfo info) {
                    unloaded.set(true);
                    unloadedId.set(info.getId());
                }
                @Override public void onLoaded(PluginInfo info) {}
                @Override public void onUpdated(PluginInfo info, String o, String n) {}
                @Override public void onLoadFailed(String id, String err) {}
                @Override public void onUnloadFailed(String id, String err) {}
                @Override public void onDisabled(PluginInfo info) {}
                @Override public void onEnabled(PluginInfo info) {}
            });

            LunaPlugin plugin = new StubPlugin("unload-event-plugin", Collections.emptyList());
            pluginManager.initializeAll(Arrays.asList(plugin));
            pluginManager.unload("unload-event-plugin");

            assertTrue(unloaded.get(), "onUnloaded should be called");
            assertEquals("unload-event-plugin", unloadedId.get());
        }
    }

    @Nested
    @DisplayName("依赖排序")
    class DependencySortTests {

        @Test
        @DisplayName("复杂依赖链正确排序")
        void testComplexDependencyChain() {
            List<String> initOrder = Collections.synchronizedList(new ArrayList<>());

            LunaPlugin d = new StubPlugin("d", Arrays.asList("b", "c")) {
                @Override
                public void initialize(PluginContext ctx) { initOrder.add(getId()); }
            };
            LunaPlugin c = new StubPlugin("c", Arrays.asList("a")) {
                @Override
                public void initialize(PluginContext ctx) { initOrder.add(getId()); }
            };
            LunaPlugin b = new StubPlugin("b", Arrays.asList("a")) {
                @Override
                public void initialize(PluginContext ctx) { initOrder.add(getId()); }
            };
            LunaPlugin a = new StubPlugin("a", Collections.emptyList()) {
                @Override
                public void initialize(PluginContext ctx) { initOrder.add(getId()); }
            };

            pluginManager.initializeAll(Arrays.asList(d, c, b, a));

            int idxA = initOrder.indexOf("a");
            int idxB = initOrder.indexOf("b");
            int idxC = initOrder.indexOf("c");
            int idxD = initOrder.indexOf("d");

            assertTrue(idxA < idxB, "a should init before b");
            assertTrue(idxA < idxC, "a should init before c");
            assertTrue(idxB < idxD, "b should init before d");
            assertTrue(idxC < idxD, "c should init before d");
        }
    }

    private List<LunaPlugin> createBuiltinPlugins() {
        List<LunaPlugin> plugins = new ArrayList<>();
        plugins.add(new StubPlugin("log", Collections.emptyList()));
        plugins.add(new StubPlugin("snapshot", Collections.emptyList()));
        plugins.add(new StubPlugin("trace", Collections.emptyList()));
        plugins.add(new StubPlugin("conditional-breakpoint", Collections.emptyList()));
        return plugins;
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
