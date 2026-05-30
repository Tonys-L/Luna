package fun.efto.luna.agent.web.vo;

import fun.efto.luna.core.bootstrap.capability.CoreCapabilityRecord;
import fun.efto.luna.core.bootstrap.capability.ReadinessState;

import java.util.List;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/29 00:00
 */
public class CapabilityManifestVO {

    private final List<CoreCapabilityRecord> coreCapabilities;
    private final List<PluginSummary> plugins;

    public CapabilityManifestVO(List<CoreCapabilityRecord> coreCapabilities, List<PluginSummary> plugins) {
        this.coreCapabilities = coreCapabilities;
        this.plugins = plugins;
    }

    public List<CoreCapabilityRecord> getCoreCapabilities() { return coreCapabilities; }
    public List<PluginSummary> getPlugins() { return plugins; }

    public static class PluginSummary {
        private final String pluginId;
        private final String displayName;
        private final String kind;
        private final ReadinessState readiness;
        private final String lifecyclePolicy;

        public PluginSummary(String pluginId, String displayName, String kind,
                             ReadinessState readiness, String lifecyclePolicy) {
            this.pluginId = pluginId;
            this.displayName = displayName;
            this.kind = kind;
            this.readiness = readiness;
            this.lifecyclePolicy = lifecyclePolicy;
        }

        public String getPluginId() { return pluginId; }
        public String getDisplayName() { return displayName; }
        public String getKind() { return kind; }
        public ReadinessState getReadiness() { return readiness; }
        public String getLifecyclePolicy() { return lifecyclePolicy; }
    }
}
