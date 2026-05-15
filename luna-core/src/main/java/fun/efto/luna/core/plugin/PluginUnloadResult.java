package fun.efto.luna.core.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
public class PluginUnloadResult {

    private final boolean success;
    private final String pluginId;
    private final String errorMessage;
    private final List<String> suspendedRuleIds;

    private PluginUnloadResult(boolean success, String pluginId, String errorMessage, List<String> suspendedRuleIds) {
        this.success = success;
        this.pluginId = pluginId;
        this.errorMessage = errorMessage;
        this.suspendedRuleIds = suspendedRuleIds != null ? new ArrayList<>(suspendedRuleIds) : new ArrayList<>();
    }

    public static PluginUnloadResult success(String pluginId) {
        return new PluginUnloadResult(true, pluginId, null, null);
    }

    public static PluginUnloadResult success(String pluginId, List<String> suspendedRuleIds) {
        return new PluginUnloadResult(true, pluginId, null, suspendedRuleIds);
    }

    public static PluginUnloadResult failure(String pluginId, String errorMessage) {
        return new PluginUnloadResult(false, pluginId, errorMessage, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getPluginId() {
        return pluginId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public List<String> getSuspendedRuleIds() {
        return Collections.unmodifiableList(suspendedRuleIds);
    }
}
