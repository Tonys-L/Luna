package fun.efto.luna.core.plugin;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:00
 */

public class PluginUpdateResult {

    private final boolean success;
    private final String pluginId;
    private final String oldVersion;
    private final String newVersion;
    private final String errorMessage;
    private final boolean rolledBack;

    private PluginUpdateResult(boolean success, String pluginId, String oldVersion,
                               String newVersion, String errorMessage, boolean rolledBack) {
        this.success = success;
        this.pluginId = pluginId;
        this.oldVersion = oldVersion;
        this.newVersion = newVersion;
        this.errorMessage = errorMessage;
        this.rolledBack = rolledBack;
    }

    public static PluginUpdateResult success(String pluginId, String oldVersion, String newVersion) {
        return new PluginUpdateResult(true, pluginId, oldVersion, newVersion, null, false);
    }

    public static PluginUpdateResult failedWithRollback(String pluginId, String oldVersion,
                                                         String newVersion, String errorMessage) {
        return new PluginUpdateResult(false, pluginId, oldVersion, newVersion, errorMessage, true);
    }

    public static PluginUpdateResult failedNoRollback(String pluginId, String oldVersion,
                                                       String newVersion, String errorMessage) {
        return new PluginUpdateResult(false, pluginId, oldVersion, newVersion, errorMessage, false);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getPluginId() {
        return pluginId;
    }

    public String getOldVersion() {
        return oldVersion;
    }

    public String getNewVersion() {
        return newVersion;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isRolledBack() {
        return rolledBack;
    }
}
