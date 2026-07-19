package fun.efto.luna.agent.web.controller;

import fun.efto.luna.agent.Agent;
import fun.efto.luna.agent.web.vo.CapabilityManifestVO;
import fun.efto.luna.core.bootstrap.capability.CoreCapabilityRecord;
import fun.efto.luna.core.bootstrap.capability.CoreCapabilityRegistry;
import fun.efto.luna.core.bootstrap.capability.ReadinessState;
import fun.efto.luna.core.plugin.LunaPlugin;
import fun.efto.luna.core.plugin.PluginState;
import fun.efto.luna.core.plugin.lifecycle.PluginManagerImpl;
import fun.efto.luna.core.plugin.lifecycle.PluginRegistrationRecord;
import fun.efto.luna.core.infra.web.ApiResult;
import fun.efto.luna.core.infra.web.Controller;
import fun.efto.luna.core.infra.web.GetMapping;
import fun.efto.luna.core.infra.web.RequestMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/29 00:00
 */
@Controller
@RequestMapping("/capabilities")
public class CapabilityController {

    @GetMapping
    public ApiResult getCapabilities() {
        List<CoreCapabilityRecord> coreCapabilities = CoreCapabilityRegistry.getInstance().getAll();

        PluginManagerImpl pluginManager = Agent.getPluginManager();
        List<CapabilityManifestVO.PluginSummary> plugins = new ArrayList<>();
        if (pluginManager != null) {
            Map<String, PluginRegistrationRecord> records = pluginManager.getRecords();
            for (Map.Entry<String, PluginRegistrationRecord> entry : records.entrySet()) {
                String pluginId = entry.getKey();
                LunaPlugin plugin = pluginManager.getPlugin(pluginId);
                String displayName = plugin != null ? plugin.getDisplayName() : pluginId;
                PluginState state = pluginManager.getState(pluginId);
                ReadinessState readiness = mapToReadiness(state);
                plugins.add(new CapabilityManifestVO.PluginSummary(
                        pluginId, displayName, "builtin-plugin", readiness, "builtin-plugin"));
            }
        }

        return ApiResult.ok(new CapabilityManifestVO(coreCapabilities, plugins));
    }

    private ReadinessState mapToReadiness(PluginState state) {
        if (state == null) return ReadinessState.NOT_INITIALIZED;
        switch (state) {
            case ACTIVE: return ReadinessState.READY;
            case DISABLED: return ReadinessState.DEGRADED;
            case UNLOADED: return ReadinessState.FAILED;
            default: return ReadinessState.NOT_INITIALIZED;
        }
    }
}
