package fun.efto.luna.core.plugin.loader;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:00
 */
public final class PluginClassLoader extends URLClassLoader {
    private final String pluginId;
    private static final List<String> PARENT_FIRST_PREFIXES = Arrays.asList(
        "fun.efto.luna.core.", "java.", "javax.", "sun.", "com.sun."
    );

    public PluginClassLoader(String pluginId, URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.pluginId = pluginId;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (PARENT_FIRST_PREFIXES.stream().anyMatch(name::startsWith)) {
            return super.loadClass(name, resolve);
        }
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded != null) return loaded;
            try {
                Class<?> found = findClass(name);
                if (resolve) resolveClass(found);
                return found;
            } catch (ClassNotFoundException e) {
                return super.loadClass(name, resolve);
            }
        }
    }

    public String getPluginId() { return pluginId; }
}
