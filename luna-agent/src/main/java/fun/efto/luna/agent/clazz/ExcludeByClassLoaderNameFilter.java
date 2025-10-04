package fun.efto.luna.agent.clazz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.List;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/2 14:13
 */
public class ExcludeByClassLoaderNameFilter implements ExcludeClassFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExcludeByClassLoaderNameFilter.class);
    private final List<String> ignoreClassLoaderNames;

    public ExcludeByClassLoaderNameFilter(String... ignoreClassLoaderNames) {
        this.ignoreClassLoaderNames = Arrays.asList(ignoreClassLoaderNames);
    }

    @Override
    public boolean filter(String fqn, ClassLoader classLoader, ProtectionDomain protectionDomain) {
        if (classLoader == null) {
            LOGGER.error("classloader is null:{} ", fqn);
            return true;
        }
        return ignoreClassLoaderNames.contains(classLoader.getClass().getName());
    }
}
