package fun.efto.luna.core.plugin.lifecycle;

import fun.efto.luna.core.injection.PersistentInjection;
import fun.efto.luna.core.injection.InjectionManager;
import fun.efto.luna.core.injection.InjectionStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/17 12:00
 */
public class RuleSuspensionManager {
    public static List<String> suspendOrphanedRules(PluginRegistrationRecord record) {
        List<String> suspendedIds = new ArrayList<>();
        Set<String> locationNames = record.getInjectionLocations().stream()
            .map(t -> t.getName())
            .collect(Collectors.toSet());

        InjectionManager injectionManager = InjectionManager.getInstance();
        for (PersistentInjection injection : injectionManager.getInjections()) {
            if (injection.getStatus() == InjectionStatus.ACTIVE && locationNames.contains(injection.getInjectionLocation())) {
                injection.setStatus(InjectionStatus.SUSPENDED);
                injection.setSuspendReason("Plugin " + record.getPluginId() + " unloaded");
                suspendedIds.add(injection.getId());
                injectionManager.rebuildCache(injection.getClazz());
                injectionManager.triggerRetransform(injection.getClazz());
            }
        }
        return suspendedIds;
    }

    public static void resumeSuspendedRules(String pluginId, Set<String> restoredLocationNames) {
        InjectionManager injectionManager = InjectionManager.getInstance();
        for (PersistentInjection injection : injectionManager.getInjections()) {
            if (injection.getStatus() == InjectionStatus.SUSPENDED && restoredLocationNames.contains(injection.getInjectionLocation())) {
                injection.setStatus(InjectionStatus.ACTIVE);
                injection.setSuspendReason(null);
                injectionManager.rebuildCache(injection.getClazz());
                injectionManager.triggerRetransform(injection.getClazz());
            }
        }
    }
}
