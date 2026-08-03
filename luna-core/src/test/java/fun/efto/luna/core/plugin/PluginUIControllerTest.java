package fun.efto.luna.core.plugin;

import fun.efto.luna.core.injection.InjectRequest;
import fun.efto.luna.core.injection.code.CompiledCode;
import fun.efto.luna.core.injection.target.InjectionLocation;
import fun.efto.luna.core.plugin.registry.ProbeHandlerRegistry;
import fun.efto.luna.core.plugin.web.PluginUIController;
import fun.efto.luna.core.plugin.web.UiManifestVO;
import fun.efto.luna.core.infra.web.ApiResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since : 2026/06/20 10:00
 */
@DisplayName("PluginUIController 测试")
public class PluginUIControllerTest {

    private PluginUIController controller;
    private StubPluginManager pluginManager;

    @BeforeEach
    void setUp() {
        ProbeHandlerRegistry.getInstance().clear();
        pluginManager = new StubPluginManager();
        controller = new PluginUIController(pluginManager);
    }

    @AfterEach
    void tearDown() {
        ProbeHandlerRegistry.getInstance().clear();
    }

    @Nested
    @DisplayName("getUiManifest probeTypes 测试")
    class ProbeTypeEntriesTest {

        @Test
        @DisplayName("返回所有已注册 ProbeHandler 的完整元数据")
        void returnsFullMetadataForAllProbeTypes() {
            ProbeHandlerRegistry.getInstance().register(new StubProbeHandler(
                    "LOG", "日志表达式", "使用 {} 占位符", "fa-print", "injection",
                    true, new HashSet<>(Arrays.asList("method_enter", "method_exit")),
                    QuickActionBehavior.FORM,
                    Arrays.asList(new FormFieldSchema("code", "表达式", "textarea", null, null, true, "输入表达式"))
            ));
            ProbeHandlerRegistry.getInstance().register(new StubProbeHandler(
                    "SNAPSHOT", "内存快照", "自动捕获", "fa-camera", "debug",
                    false, new HashSet<>(Arrays.asList("method_enter")),
                    QuickActionBehavior.DIRECT,
                    Collections.emptyList()
            ));

            ApiResult result = controller.getUiManifest();
            UiManifestVO manifest = (UiManifestVO) result.getData();

            List<UiManifestVO.ProbeTypeEntry> probeTypes = manifest.getProbeTypes();
            assertEquals(2, probeTypes.size());

            Map<String, UiManifestVO.ProbeTypeEntry> byType = new HashMap<>();
            for (UiManifestVO.ProbeTypeEntry entry : probeTypes) {
                byType.put(entry.getProbeType(), entry);
            }

            // LOG
            UiManifestVO.ProbeTypeEntry log = byType.get("LOG");
            assertNotNull(log);
            assertEquals("日志表达式", log.getDisplayName());
            assertEquals("fa-print", log.getIcon());
            assertEquals("injection", log.getCategory());
            assertTrue(log.isUsesCode());
            assertNotNull(log.getSupportedInjectionLocations());
            assertFalse(log.getSupportedInjectionLocations().isEmpty());
            assertNotNull(log.getConfigSchema());
            assertFalse(log.getConfigSchema().isEmpty());
            assertEquals("FORM", log.getQuickActionBehavior());

            // SNAPSHOT
            UiManifestVO.ProbeTypeEntry snapshot = byType.get("SNAPSHOT");
            assertNotNull(snapshot);
            assertEquals("内存快照", snapshot.getDisplayName());
            assertEquals("fa-camera", snapshot.getIcon());
            assertEquals("debug", snapshot.getCategory());
            assertFalse(snapshot.isUsesCode());
            assertEquals("DIRECT", snapshot.getQuickActionBehavior());
        }

        @Test
        @DisplayName("过滤已禁用插件的 probeType")
        void filtersDisabledPluginProbeTypes() {
            StubProbeHandler logHandler = new StubProbeHandler("LOG", "日志", "", "", "", true,
                    new HashSet<>(Arrays.asList("method_enter")),
                    QuickActionBehavior.FORM, Collections.emptyList());
            StubProbeHandler traceHandler = new StubProbeHandler("TRACE", "耗时", "", "", "", false,
                    new HashSet<>(Arrays.asList("method_around")),
                    QuickActionBehavior.FORM, Collections.emptyList());

            ProbeHandlerRegistry.getInstance().register(logHandler);
            ProbeHandlerRegistry.getInstance().register(traceHandler);

            pluginManager.addDisabledPlugin("disabled-plugin", Collections.singleton(traceHandler));

            ApiResult result = controller.getUiManifest();
            UiManifestVO manifest = (UiManifestVO) result.getData();

            List<UiManifestVO.ProbeTypeEntry> probeTypes = manifest.getProbeTypes();
            assertEquals(1, probeTypes.size());
            assertEquals("LOG", probeTypes.get(0).getProbeType());
        }

        @Test
        @DisplayName("空注册表返回空 probeTypes 列表")
        void emptyRegistryReturnsEmptyProbeTypes() {
            ApiResult result = controller.getUiManifest();
            UiManifestVO manifest = (UiManifestVO) result.getData();

            assertTrue(manifest.getProbeTypes().isEmpty());
        }

        @Test
        @DisplayName("ProbeTypeEntry 包含所有必要字段")
        void probeTypeEntryContainsAllFields() {
            ProbeHandlerRegistry.getInstance().register(new StubProbeHandler(
                    "TRACE", "方法耗时", "自动统计方法耗时", "fa-stopwatch", "performance",
                    false, new HashSet<>(Collections.singletonList("method_around")),
                    QuickActionBehavior.FORM,
                    Arrays.asList(new FormFieldSchema("code", "耗时阈值(ms)", "number", "0", null, false, "仅输出超过阈值的耗时"))
            ));

            ApiResult result = controller.getUiManifest();
            UiManifestVO manifest = (UiManifestVO) result.getData();

            UiManifestVO.ProbeTypeEntry trace = manifest.getProbeTypes().get(0);
            assertEquals("TRACE", trace.getProbeType());
            assertEquals("方法耗时", trace.getDisplayName());
            assertEquals("自动统计方法耗时", trace.getSyntax());
            assertEquals("fa-stopwatch", trace.getIcon());
            assertEquals("performance", trace.getCategory());
            assertFalse(trace.isUsesCode());
            assertNotNull(trace.getSupportedInjectionLocations());
            assertEquals(1, trace.getSupportedInjectionLocations().size());
            assertTrue(trace.getSupportedInjectionLocations().contains("method_around"));
            assertEquals("FORM", trace.getQuickActionBehavior());
            assertNotNull(trace.getConfigSchema());
            assertEquals(1, trace.getConfigSchema().size());
        }
    }

    private static class StubProbeHandler extends AbstractProbeHandler {

        private final String probeType;
        private final String displayName;
        private final String syntax;
        private final String icon;
        private final String category;
        private final boolean usesCode;
        private final Set<String> supportedLocations;
        private final QuickActionBehavior quickActionBehavior;
        private final List<FormFieldSchema> configSchema;

        StubProbeHandler(String probeType, String displayName, String syntax, String icon,
                         String category, boolean usesCode, Set<String> supportedLocations,
                         QuickActionBehavior quickActionBehavior, List<FormFieldSchema> configSchema) {
            this.probeType = probeType;
            this.displayName = displayName;
            this.syntax = syntax;
            this.icon = icon;
            this.category = category;
            this.usesCode = usesCode;
            this.supportedLocations = supportedLocations;
            this.quickActionBehavior = quickActionBehavior;
            this.configSchema = configSchema;
        }

        @Override public String getProbeType() { return probeType; }
        @Override public boolean usesCode() { return usesCode; }
        @Override public Set<String> supportedInjectionLocations() { return supportedLocations; }
        @Override public String getDisplayName() { return displayName; }
        @Override public String getSyntax() { return syntax; }
        @Override public String getIcon() { return icon; }
        @Override public String getCategory() { return category; }
        @Override public QuickActionBehavior getQuickActionBehavior() { return quickActionBehavior; }
        @Override public List<FormFieldSchema> getConfigSchema() { return configSchema; }

        @Override
        protected ValidationResult doValidate(InjectRequest request) {
            return ValidationResult.ok();
        }

        @Override
        public void handle(CompiledCode code, GenerateContext ctx) {}
    }

    private static class StubPluginManager implements PluginManager {

        private final List<PluginInfo> plugins = new ArrayList<>();
        private final Map<String, Set<ProbeHandler>> disabledProbeHandlers = new HashMap<>();

        void addDisabledPlugin(String pluginId, Set<ProbeHandler> probeHandlers) {
            plugins.add(new PluginInfo(pluginId, pluginId, "1.0", "", "",
                    PluginState.DISABLED, Collections.emptyList()));
            disabledProbeHandlers.put(pluginId, probeHandlers);
        }

        @Override
        public List<PluginInfo> listPlugins() {
            return plugins;
        }

        @Override
        public Set<ProbeHandler> getProbeHandlersForPlugin(String pluginId) {
            return disabledProbeHandlers.getOrDefault(pluginId, Collections.emptySet());
        }

        @Override
        public PluginLoadResult load(String pluginId) { return null; }

        @Override
        public PluginUnloadResult unload(String pluginId) { return null; }

        @Override
        public PluginUpdateResult update(String pluginId) { return null; }

        @Override
        public UnloadCheckResult checkUnloadable(String pluginId) { return null; }

        @Override
        public void disable(String pluginId) {}

        @Override
        public void enable(String pluginId) {}

        @Override
        public LunaPlugin getPlugin(String pluginId) { return null; }

        @Override
        public PluginState getState(String pluginId) { return null; }

        @Override
        public void addListener(PluginLifecycleListener listener) {}

        @Override
        public void removeListener(PluginLifecycleListener listener) {}

        @Override
        public java.util.concurrent.locks.StampedLock getTransformLock() { return null; }

        @Override
        public Set<InjectionLocation> getInjectionLocationsForPlugin(String pluginId) {
            return Collections.emptySet();
        }

        @Override
        public Map<String, String> getPluginConfig(String pluginId) { return Collections.emptyMap(); }

        @Override
        public void savePluginConfig(String pluginId, Map<String, String> config) {}
    }
}
