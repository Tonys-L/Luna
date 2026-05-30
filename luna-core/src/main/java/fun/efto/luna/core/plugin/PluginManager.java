package fun.efto.luna.core.plugin;

import fun.efto.luna.core.injection.target.InjectionType;
import fun.efto.luna.core.rule.template.RuleTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.StampedLock;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
public interface PluginManager {

    PluginLoadResult load(String pluginId);

    PluginUnloadResult unload(String pluginId);

    PluginUpdateResult update(String pluginId);

    UnloadCheckResult checkUnloadable(String pluginId);

    void disable(String pluginId);

    void enable(String pluginId);

    List<PluginInfo> listPlugins();

    LunaPlugin getPlugin(String pluginId);

    PluginState getState(String pluginId);

    void addListener(PluginLifecycleListener listener);

    void removeListener(PluginLifecycleListener listener);

    StampedLock getTransformLock();

    default Set<InjectionType> getInjectionTypesForPlugin(String pluginId) {
        return Collections.emptySet();
    }

    default Set<ExpressionHandler> getExpressionHandlersForPlugin(String pluginId) {
        return Collections.emptySet();
    }

    default Set<RuleTemplate> getTemplatesForPlugin(String pluginId) {
        return Collections.emptySet();
    }

    Map<String, String> getPluginConfig(String pluginId);

    void savePluginConfig(String pluginId, Map<String, String> config);
}
