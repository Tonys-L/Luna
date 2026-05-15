package fun.efto.luna.core.plugin;

import java.util.List;
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
}
