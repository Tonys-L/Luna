package fun.efto.luna.core.plugin;

import fun.efto.luna.core.infra.RingBuffer;
import fun.efto.luna.core.injection.port.Retransformer;
import fun.efto.luna.core.plugin.lifecycle.PluginManagerImpl;
import fun.efto.luna.core.plugin.lifecycle.ReadyGate;
import fun.efto.luna.core.probe.ProbeOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:00
 */
@DisplayName("插件热更新测试")
public class UpdateTest {

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
    }

    @Test
    @DisplayName("更新不存在的插件返回失败")
    void testUpdateNonExistentPlugin() {
        PluginUpdateResult result = pluginManager.update("nonexistent");
        assertFalse(result.isSuccess());
        assertFalse(result.isRolledBack());
        assertTrue(result.getErrorMessage().contains("not found"));
    }

    @Test
    @DisplayName("更新没有存储路径的插件返回失败")
    void testUpdatePluginWithoutStoredPath() {
        LunaPlugin plugin = new TestPlugin("no-path-plugin", Collections.emptyList(), "1.0.0");
        pluginManager.initializeAll(Arrays.asList(plugin));

        PluginUpdateResult result = pluginManager.update("no-path-plugin");
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("path not found"));
    }

    @Test
    @DisplayName("PluginUpdateResult success 工厂方法")
    void testUpdateResultSuccess() {
        PluginUpdateResult result = PluginUpdateResult.success("test", "1.0.0", "2.0.0");
        assertTrue(result.isSuccess());
        assertEquals("test", result.getPluginId());
        assertEquals("1.0.0", result.getOldVersion());
        assertEquals("2.0.0", result.getNewVersion());
        assertFalse(result.isRolledBack());
    }

    @Test
    @DisplayName("PluginUpdateResult failedWithRollback 工厂方法")
    void testUpdateResultFailedWithRollback() {
        PluginUpdateResult result = PluginUpdateResult.failedWithRollback("test", "1.0.0", "2.0.0", "error");
        assertFalse(result.isSuccess());
        assertTrue(result.isRolledBack());
        assertEquals("error", result.getErrorMessage());
    }

    @Test
    @DisplayName("PluginUpdateResult failedNoRollback 工厂方法")
    void testUpdateResultFailedNoRollback() {
        PluginUpdateResult result = PluginUpdateResult.failedNoRollback("test", "1.0.0", "2.0.0", "error");
        assertFalse(result.isSuccess());
        assertFalse(result.isRolledBack());
    }

    private static class TestPlugin implements LunaPlugin {
        private final String id;
        private final List<String> dependencies;
        private final String version;

        TestPlugin(String id, List<String> dependencies, String version) {
            this.id = id;
            this.dependencies = dependencies;
            this.version = version;
        }

        @Override public String getId() { return id; }
        @Override public String getDisplayName() { return id; }
        @Override public String getVersion() { return version; }
        @Override public String getAuthor() { return "test"; }
        @Override public String getCategory() { return "test"; }
        @Override public List<String> getDependencies() { return dependencies; }
        @Override public void initialize(PluginContext context) {}
        @Override public void destroy() {}
        @Override public void getControllers(List<LunaController> controllers) {}
    }
}
