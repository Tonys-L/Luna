package fun.efto.luna.core.plugin.lifecycle;

import fun.efto.luna.core.injection.InjectionService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/17 12:00
 */
public class RuleSuspensionManager {
    private final InjectionService injectionService;

    public RuleSuspensionManager(InjectionService injectionService) {
        this.injectionService = injectionService;
    }

    public List<String> suspendOrphanedRules(PluginRegistrationRecord record) {
        Set<String> locationNames = record.getInjectionLocations().stream()
            .map(t -> t.getName())
            .collect(Collectors.toSet());
        return injectionService.suspendInjectionsByLocation(locationNames,
            "Plugin " + record.getPluginId() + " unloaded");
    }

    public void resumeSuspendedRules(String pluginId, Set<String> restoredLocationNames) {
        injectionService.resumeInjectionsByLocation(restoredLocationNames);
    }
}
