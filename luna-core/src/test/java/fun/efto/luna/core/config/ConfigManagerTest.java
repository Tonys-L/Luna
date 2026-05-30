package fun.efto.luna.core.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:00
 */
class ConfigManagerTest {

    @Test
    void testGetAgentConfigWithDefault() {
        String value = ConfigManager.getAgentConfig("non.existent.key", "defaultValue");
        assertEquals("defaultValue", value);
    }

    @Test
    void testGetPluginConfig() {
        Map<String, String> config = ConfigManager.getPluginConfig("non-existent-plugin");
        assertNotNull(config);
        assertTrue(config.isEmpty());
    }
}
