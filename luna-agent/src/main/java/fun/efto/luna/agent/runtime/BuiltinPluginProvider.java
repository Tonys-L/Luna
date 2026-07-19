package fun.efto.luna.agent.runtime;

import fun.efto.luna.core.plugin.LunaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/07 20:00
 */
public final class BuiltinPluginProvider {

    private static final Logger logger = LoggerFactory.getLogger(BuiltinPluginProvider.class);

    private BuiltinPluginProvider() {
    }

    public static List<LunaPlugin> builtins() {
        List<LunaPlugin> plugins = new ArrayList<>();
        java.util.ServiceLoader<LunaPlugin> loader = java.util.ServiceLoader.load(LunaPlugin.class);
        for (LunaPlugin plugin : loader) {
            plugins.add(plugin);
            logger.debug("Discovered builtin plugin: {} ({})", plugin.getId(), plugin.getDisplayName());
        }
        return Collections.unmodifiableList(plugins);
    }
}
