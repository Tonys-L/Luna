package fun.efto.luna.core.injection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since : 2026/05/02 12:00
 */
public class InjectionPointRegistry {

    private static final InjectionPointRegistry INSTANCE = new InjectionPointRegistry();

    private final Map<String, List<InjectionPoint>> registry = new ConcurrentHashMap<>();

    private InjectionPointRegistry() {
    }

    public static InjectionPointRegistry getInstance() {
        return INSTANCE;
    }

    public void register(InjectionPoint injectionPoint) {
        String className = injectionPoint.getTarget().getTargetClass();
        registry.computeIfAbsent(className, k -> new CopyOnWriteArrayList<>()).add(injectionPoint);
    }

    public List<InjectionPoint> getInjectionPoints(String className) {
        return new ArrayList<>(registry.getOrDefault(className, new CopyOnWriteArrayList<>()));
    }

    public boolean remove(String className, InjectionPoint injectionPoint) {
        List<InjectionPoint> points = registry.get(className);
        if (points != null) {
            boolean removed = points.remove(injectionPoint);
            if (points.isEmpty()) {
                registry.remove(className);
            }
            return removed;
        }
        return false;
    }

    public void clear(String className) {
        registry.remove(className);
    }

    public void clearAll() {
        registry.clear();
    }

    public boolean hasInjectionPoints(String className) {
        List<InjectionPoint> points = registry.get(className);
        return points != null && !points.isEmpty();
    }
}
