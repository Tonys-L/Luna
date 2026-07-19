package fun.efto.luna.core.infra.config;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:00
 */
public final class ConfigManager {

    private static final Path CONFIG_DIR =
        Paths.get(System.getProperty("user.home"), ".luna", "config");

    private static final Map<String, String> AGENT_CONFIG = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, String>> PLUGIN_CONFIGS = new ConcurrentHashMap<>();

    private ConfigManager() {}

    public static void loadAgentConfig() {
        Path configFile = CONFIG_DIR.resolve("agent.properties");
        if (!Files.exists(configFile)) return;
        try (BufferedReader reader = Files.newBufferedReader(configFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq > 0) {
                    AGENT_CONFIG.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
                }
            }
        } catch (IOException ignored) {}
    }

    public static String getAgentConfig(String key, String defaultValue) {
        return AGENT_CONFIG.getOrDefault(key, defaultValue);
    }

    public static Map<String, String> getPluginConfig(String pluginId) {
        return PLUGIN_CONFIGS.computeIfAbsent(pluginId, id -> {
            Path pluginConfig = CONFIG_DIR.resolve("plugins").resolve(id + ".properties");
            return loadProperties(pluginConfig);
        });
    }

    public static void savePluginConfig(String pluginId, Map<String, String> config) {
        PLUGIN_CONFIGS.put(pluginId, config);
        Path pluginConfig = CONFIG_DIR.resolve("plugins").resolve(pluginId + ".properties");
        try {
            Files.createDirectories(pluginConfig.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(pluginConfig)) {
                for (Map.Entry<String, String> entry : config.entrySet()) {
                    writer.write(entry.getKey() + "=" + entry.getValue());
                    writer.newLine();
                }
            }
        } catch (IOException ignored) {}
    }

    private static Map<String, String> loadProperties(Path path) {
        Map<String, String> map = new HashMap<>();
        if (!Files.exists(path)) return map;
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq > 0) {
                    map.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
                }
            }
        } catch (IOException ignored) {}
        return map;
    }
}
