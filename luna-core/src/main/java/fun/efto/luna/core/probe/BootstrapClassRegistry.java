package fun.efto.luna.core.probe;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/16 21:00
 */
public final class BootstrapClassRegistry {

    private static final Set<String> BOOTSTRAP_CLASSES = ConcurrentHashMap.newKeySet();

    static {
        BOOTSTRAP_CLASSES.add(ProbeOutput.class.getName());
    }

    private BootstrapClassRegistry() {
    }

    public static void register(String className) {
        BOOTSTRAP_CLASSES.add(className);
    }

    public static boolean isBootstrapClass(String name) {
        return BOOTSTRAP_CLASSES.contains(name);
    }

    public static Set<String> getRegisteredClasses() {
        return Collections.unmodifiableSet(BOOTSTRAP_CLASSES);
    }
}
