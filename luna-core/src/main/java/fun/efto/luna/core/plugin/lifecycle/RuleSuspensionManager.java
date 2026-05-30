package fun.efto.luna.core.plugin.lifecycle;

import fun.efto.luna.core.injection.PersistentInjection;
import fun.efto.luna.core.injection.InjectionManager;
import fun.efto.luna.core.injection.InjectionStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/17 12:00
 */
public class RuleSuspensionManager {
    public static List<String> suspendOrphanedRules(PluginRegistrationRecord record) {
        List<String> suspendedIds = new ArrayList<>();
        Set<String> typeNames = record.getInjectionTypes().stream()
            .map(t -> t.getName())
            .collect(Collectors.toSet());

        InjectionManager injectionManager = InjectionManager.getInstance();
        for (PersistentInjection injection : injectionManager.getInjections()) {
            if (injection.getStatus() == InjectionStatus.ACTIVE && typeNames.contains(injection.getInjectionType())) {
                injection.setStatus(InjectionStatus.SUSPENDED);
                injection.setSuspendReason("Plugin " + record.getPluginId() + " unloaded");
                suspendedIds.add(injection.getId());
                // 重建缓存并触发 retransform
                injectionManager.rebuildCache(injection.getClazz());
                injectionManager.triggerRetransform(injection.getClazz());
            }
        }
        return suspendedIds;
    }

    public static void resumeSuspendedRules(String pluginId, Set<String> restoredTypeNames) {
        InjectionManager injectionManager = InjectionManager.getInstance();
        for (PersistentInjection injection : injectionManager.getInjections()) {
            if (injection.getStatus() == InjectionStatus.SUSPENDED && restoredTypeNames.contains(injection.getInjectionType())) {
                injection.setStatus(InjectionStatus.ACTIVE);
                injection.setSuspendReason(null);
                // 重建缓存并触发 retransform
                injectionManager.rebuildCache(injection.getClazz());
                injectionManager.triggerRetransform(injection.getClazz());
            }
        }
    }
}
