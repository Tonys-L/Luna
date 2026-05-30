package fun.efto.luna.core.plugin.lifecycle;

import fun.efto.luna.core.injection.target.InjectionType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:00
 */
public final class AffectedClassTracker {
    private static final ConcurrentHashMap<String, Set<String>> TYPE_TO_CLASSES = new ConcurrentHashMap<>();

    private AffectedClassTracker() {}

    public static void record(String injectionTypeName, String className) {
        TYPE_TO_CLASSES.computeIfAbsent(injectionTypeName, k -> ConcurrentHashMap.newKeySet()).add(className);
    }

    public static Set<String> getAffectedClasses(PluginRegistrationRecord record) {
        Set<String> affected = new HashSet<>();
        for (InjectionType type : record.getInjectionTypes()) {
            Set<String> classes = TYPE_TO_CLASSES.get(type.getName());
            if (classes != null) affected.addAll(classes);
        }
        return affected;
    }

    public static void remove(String injectionTypeName, String className) {
        Set<String> classes = TYPE_TO_CLASSES.get(injectionTypeName);
        if (classes != null) classes.remove(className);
    }

    public static void clear() { TYPE_TO_CLASSES.clear(); }
}
