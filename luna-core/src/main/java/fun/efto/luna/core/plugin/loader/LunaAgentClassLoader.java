package fun.efto.luna.core.plugin.loader;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.Enumeration;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2025/10/18 18:35
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

    public void appendUrl(URL url) {
        addURL(url);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // Spy 类及其依赖的 Buffer 类必须由 Bootstrap ClassLoader 加载以实现共享，不能隔离
        if (name.startsWith("fun.efto.luna.core.probe.")
                || name.startsWith("fun.efto.luna.core.infra.")
                || name.startsWith("fun.efto.luna.core.expression.context.")
                || name.startsWith("fun.efto.luna.core.expression.ConditionRegistry")) {
            return super.loadClass(name, resolve);
        }

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
        if ("META-INF/org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.json".equals(name)) {
            return findResources(name);
        }
        return super.getResources(name);
    }

}
