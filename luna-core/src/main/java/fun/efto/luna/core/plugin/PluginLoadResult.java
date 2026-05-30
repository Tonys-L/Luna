package fun.efto.luna.core.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:00
 */
public class PluginLoadResult {

    private final boolean success;
    private final String pluginId;
    private final String version;
    private final String errorMessage;
    private final List<String> warnings;

    private PluginLoadResult(boolean success, String pluginId, String version,
                             String errorMessage, List<String> warnings) {
        this.success = success;
        this.pluginId = pluginId;
        this.version = version;
        this.errorMessage = errorMessage;
        this.warnings = warnings != null ? new ArrayList<>(warnings) : new ArrayList<>();
    }

    public static PluginLoadResult success(String pluginId, String version) {
        return new PluginLoadResult(true, pluginId, version, null, null);
    }

    public static PluginLoadResult success(String pluginId, String version, List<String> warnings) {
        return new PluginLoadResult(true, pluginId, version, null, warnings);
    }

    public static PluginLoadResult failure(String pluginId, String errorMessage) {
        return new PluginLoadResult(false, pluginId, null, errorMessage, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getPluginId() {
        return pluginId;
    }

    public String getVersion() {
        return version;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }
}
