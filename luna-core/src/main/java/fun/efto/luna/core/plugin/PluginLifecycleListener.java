package fun.efto.luna.core.plugin;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
public interface PluginLifecycleListener {

    default void onLoaded(PluginInfo info) {}

    default void onUnloaded(PluginInfo info) {}

    default void onUpdated(PluginInfo info, String oldVersion, String newVersion) {}

    default void onLoadFailed(String pluginId, String errorMessage) {}

    default void onUnloadFailed(String pluginId, String errorMessage) {}

    default void onDisabled(PluginInfo info) {}

    default void onEnabled(PluginInfo info) {}
}
