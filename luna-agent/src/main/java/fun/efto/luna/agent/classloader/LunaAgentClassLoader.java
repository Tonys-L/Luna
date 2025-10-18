package fun.efto.luna.agent.classloader;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.Enumeration;

import static org.apache.logging.log4j.core.config.plugins.processor.PluginProcessor.PLUGIN_CACHE_FILE;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/18 18:35
 */
public class LunaAgentClassLoader extends URLClassLoader {
    private final String[] excludedPackages = {
            "fun.efto.luna.",
    };
    public LunaAgentClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public LunaAgentClassLoader(URL[] urls) {
        super(urls);
    }

    public LunaAgentClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // 检查是否为需要隔离的包
        for (String excludedPackage : excludedPackages) {
            if (name.startsWith(excludedPackage)) {
                return loadClassIsolated(name);
            }
        }
        return super.loadClass(name, resolve);
    }

    private Class<?> loadClassIsolated(String name) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> loadedClass = findLoadedClass(name);
            if (loadedClass == null) {
                try {
                    loadedClass = findClass(name);
                } catch (ClassNotFoundException e) {
                    loadedClass = super.loadClass(name, false);
                }
            }
            return loadedClass;
        }
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        if (PLUGIN_CACHE_FILE.equals(name)) {
            return findResources(name);
        }
        return super.getResources(name);
    }

}
