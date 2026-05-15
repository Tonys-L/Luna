package fun.efto.luna.core.plugin;

import java.util.Collections;
import java.util.List;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11
 */
public final class UnloadCheckResult {

    private final boolean canUnload;
    private final String reason;
    private final List<String> dependentPluginIds;

    public static UnloadCheckResult allowed() {
        return new UnloadCheckResult(true, null, Collections.emptyList());
    }

    public static UnloadCheckResult denied(String reason, List<String> dependentPluginIds) {
        return new UnloadCheckResult(false, reason, dependentPluginIds);
    }

    private UnloadCheckResult(boolean canUnload, String reason, List<String> dependentPluginIds) {
        this.canUnload = canUnload;
        this.reason = reason;
        this.dependentPluginIds = dependentPluginIds != null ? dependentPluginIds : Collections.emptyList();
    }

    public boolean canUnload() {
        return canUnload;
    }

    public String getReason() {
        return reason;
    }

    public List<String> getDependentPluginIds() {
        return dependentPluginIds;
    }
}
