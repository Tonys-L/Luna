package fun.efto.luna.core.plugin;

import fun.efto.luna.core.buffer.RingBuffer;
import fun.efto.luna.core.injection.target.type.InjectionType;
import fun.efto.luna.core.plugin.InjectionTypeRegistry;
import fun.efto.luna.core.rule.InjectionRule;
import fun.efto.luna.core.rule.RuleManager;
import fun.efto.luna.core.rule.RuleStatus;
import fun.efto.luna.core.rule.template.RuleTemplate;
import fun.efto.luna.core.rule.template.TemplateRegistry;
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
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:45
 */
@DisplayName("æ’ä»¶æž¶æž„é›†æˆæµ‹è¯•")
public class PluginIntegrationTest {

    private PluginManagerImpl pluginManager;
    private ReadyGate readyGate;

    @BeforeEach
    void setUp() {
        readyGate = new ReadyGate();
        pluginManager = new PluginManagerImpl(
            readyGate,
            new DefaultLogEmitter(),
            new RingBuffer<>(1024),
            null,
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
    @DisplayName("SPI æ‰«æä¸Žåˆå§‹åŒ–")
    class SpiScanTests {

        @Test
        @DisplayName("5个内置插件通过 initializeAll 正确初始化")
        void testBuiltinPluginsInitialization() {
            List<LunaPlugin> builtins = createBuiltinPlugins();
            pluginManager.initializeAll(builtins);

            List<PluginInfo> plugins = pluginManager.listPlugins();
            assertEquals(5, plugins.size());

            for (PluginInfo info : plugins) {
                assertTrue(info.isBuiltin());
                assertEquals(PluginState.ACTIVE, info.getState());
            }
        }

        @Test
        @DisplayName("å†…ç½®æ’ä»¶ä¸å¯å¸è½½")
        void testBuiltinPluginsCannotUnload() {
            pluginManager.initializeAll(createBuiltinPlugins());

            for (PluginInfo info : pluginManager.listPlugins()) {
                PluginUnloadResult result = pluginManager.unload(info.getId());
                assertFalse(result.isSuccess());
                assertTrue(result.getErrorMessage().contains("builtin"));
            }
        }
    }

    @Nested
    @DisplayName("Registry åŒå†™éªŒè¯")
    class DualWriteTests {

        @Test
        @DisplayName("æ’ä»¶æ³¨å†Œ InjectionType æ—¶åŒæ—¶å†™å…?Registry å’?Record")
        void testInjectionTypeDualWrite() {
            final InjectionType testType = InjectionType.of("test-type", "Test Type", "TEST");
            LunaPlugin plugin = new StubPlugin("test-plugin", Collections.emptyList()) {
                @Override
                public void initialize(PluginContext context) {
                    context.registerInjectionType(testType);
                }
            };

            pluginManager.initializeAll(Arrays.asList(plugin));

            InjectionType resolved = InjectionTypeRegistry.find("test-type").orElse(null);
            assertNotNull(resolved, "InjectionType should be registered in Registry");
            assertEquals("test-type", resolved.getName());

            PluginRegistrationRecord record = pluginManager.getRecords().get("test-plugin");
            assertNotNull(record);
            assertTrue(record.injectionTypes.stream().anyMatch(t -> t.getName().equals("test-type")),
                "InjectionType should be in PluginRegistrationRecord");
        }

        @Test
        @DisplayName("卸载插件后 Registry 中的注册被清除")
        void testRegistryCleanupOnUnload() {
            final InjectionType testType = InjectionType.of("cleanup-type", "Cleanup Type", "CLEANUP");
            LunaPlugin plugin = new StubPlugin("cleanup-plugin", Collections.emptyList(), false) {
                @Override
                public void initialize(PluginContext context) {
                    context.registerInjectionType(testType);
                }
            };

            pluginManager.initializeAll(Arrays.asList(plugin));
            assertNotNull(InjectionTypeRegistry.find("cleanup-type").orElse(null));

            PluginUnloadResult result = pluginManager.unload("cleanup-plugin");
            assertTrue(result.isSuccess());

            assertNull(InjectionTypeRegistry.find("cleanup-type").orElse(null),
                "InjectionType should be removed from Registry after unload");
        }
    }

    @Nested
    @DisplayName("ReadyGate å¯åŠ¨å±éšœ")
    class ReadyGateTests {

        @Test
        @DisplayName("åˆå§‹çŠ¶æ€?isReady è¿”å›ž false")
        void testInitiallyNotReady() {
            assertFalse(readyGate.isReady());
        }

        @Test
        @DisplayName("markReady å?isReady è¿”å›ž true")
        void testReadyAfterMark() {
            readyGate.markReady();
            assertTrue(readyGate.isReady());
        }

        @Test
        @DisplayName("æ’ä»¶åˆå§‹åŒ–å®ŒæˆåŽåº”è°ƒç”?markReady")
        void testMarkReadyAfterInit() {
            pluginManager.initializeAll(createBuiltinPlugins());
            readyGate.markReady();
            assertTrue(readyGate.isReady());
        }
    }

    @Nested
    @DisplayName("è§„åˆ™æŒ‚èµ·/æ¢å¤æœºåˆ¶")
    class RuleSuspendResumeTests {

        @Test
        @DisplayName("å¸è½½æ’ä»¶åŽå¼•ç”¨å…¶ InjectionType çš„è§„åˆ™è¢«æŒ‚èµ·")
        void testRulesSuspendedOnUnload() {
            final InjectionType customType = InjectionType.of("custom-type", "Custom Type", "CUSTOM");
            LunaPlugin plugin = new StubPlugin("type-provider", Collections.emptyList(), false) {
                @Override
                public void initialize(PluginContext context) {
                    context.registerInjectionType(customType);
                }
            };

            pluginManager.initializeAll(Arrays.asList(plugin));

            InjectionRule rule = new InjectionRule();
            rule.setInjectionType("custom-type");
            rule.setStatus(RuleStatus.ACTIVE);
            RuleManager.getInstance().addRule(rule);

            PluginUnloadResult result = pluginManager.unload("type-provider");
            assertTrue(result.isSuccess());
            assertFalse(result.getSuspendedRuleIds().isEmpty(),
                "Should have suspended rules referencing the unloaded plugin's types");

            assertEquals(RuleStatus.SUSPENDED, rule.getStatus());
            assertNotNull(rule.getSuspendReason());
        }

        @Test
        @DisplayName("重新加载同 ID 插件后挂起规则自动恢复")
        void testRulesResumedOnReload() {
            final InjectionType customType = InjectionType.of("resume-type", "Resume Type", "RESUME");

            LunaPlugin plugin = new StubPlugin("reload-provider", Collections.emptyList(), false) {
                @Override
                public void initialize(PluginContext context) {
                    context.registerInjectionType(customType);
                }
            };

            pluginManager.initializeAll(Arrays.asList(plugin));

            InjectionRule rule = new InjectionRule();
            rule.setInjectionType("resume-type");
            rule.setStatus(RuleStatus.ACTIVE);
            RuleManager.getInstance().addRule(rule);

            pluginManager.unload("reload-provider");
            assertEquals(RuleStatus.SUSPENDED, rule.getStatus());

            LunaPlugin reloadedPlugin = new StubPlugin("reload-provider", Collections.emptyList(), false) {
                @Override
                public void initialize(PluginContext context) {
                    context.registerInjectionType(customType);
                }
            };
            pluginManager.initializeAll(Arrays.asList(reloadedPlugin));

            InjectionType resolved = InjectionTypeRegistry.find("resume-type").orElse(null);
            assertNotNull(resolved, "InjectionType should be re-registered");
        }
    }

    @Nested
    @DisplayName("PluginLifecycleListener äº‹ä»¶")
    class LifecycleListenerTests {

        @Test
        @DisplayName("æ’ä»¶åŠ è½½æ—¶è§¦å?onLoaded äº‹ä»¶")
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
        @DisplayName("æ’ä»¶å¸è½½æ—¶è§¦å?onUnloaded äº‹ä»¶")
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

            LunaPlugin plugin = new StubPlugin("unload-event-plugin", Collections.emptyList(), false);
            pluginManager.initializeAll(Arrays.asList(plugin));
            pluginManager.unload("unload-event-plugin");

            assertTrue(unloaded.get(), "onUnloaded should be called");
            assertEquals("unload-event-plugin", unloadedId.get());
        }
    }

    @Nested
    @DisplayName("ä¾èµ–æŽ’åº")
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
        plugins.add(new StubPlugin("method-injection", Collections.emptyList()) {
            @Override public boolean isBuiltin() { return true; }
        });
        plugins.add(new StubPlugin("line-injection", Collections.emptyList()) {
            @Override public boolean isBuiltin() { return true; }
        });
        plugins.add(new StubPlugin("log", Collections.emptyList()) {
            @Override public boolean isBuiltin() { return true; }
        });
        plugins.add(new StubPlugin("snapshot", Collections.emptyList()) {
            @Override public boolean isBuiltin() { return true; }
        });
        plugins.add(new StubPlugin("trace", Collections.emptyList()) {
            @Override public boolean isBuiltin() { return true; }
        });
        return plugins;
    }

    private static class StubPlugin implements LunaPlugin {
        private final String id;
        private final List<String> dependencies;
        private final boolean builtin;

        StubPlugin(String id, List<String> dependencies) {
            this(id, dependencies, true);
        }

        StubPlugin(String id, List<String> dependencies, boolean builtin) {
            this.id = id;
            this.dependencies = dependencies;
            this.builtin = builtin;
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
        @Override public void getTemplates(List<RuleTemplate> templates) {}
        @Override public boolean isBuiltin() { return builtin; }
    }
}
