package fun.efto.luna.core.plugin;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
class PluginDependencyResolver {
    static List<LunaPlugin> resolve(List<LunaPlugin> plugins) {
        Map<String, LunaPlugin> byId = new LinkedHashMap<>();
        for (LunaPlugin p : plugins) {
            byId.put(p.getId(), p);
        }
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        for (LunaPlugin p : plugins) {
            graph.putIfAbsent(p.getId(), new HashSet<>());
            inDegree.putIfAbsent(p.getId(), 0);
            for (String dep : p.getDependencies()) {
                if (!byId.containsKey(dep)) {
                    throw new IllegalStateException("Plugin [" + p.getId() + "] depends on [" + dep + "] which is not found");
                }
                graph.computeIfAbsent(dep, k -> new HashSet<>()).add(p.getId());
                inDegree.merge(p.getId(), 1, Integer::sum);
            }
        }
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> e : inDegree.entrySet()) {
            if (e.getValue() == 0) queue.add(e.getKey());
        }
        List<LunaPlugin> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            String id = queue.poll();
            sorted.add(byId.get(id));
            for (String next : graph.getOrDefault(id, Collections.emptySet())) {
                int newDeg = inDegree.merge(next, -1, Integer::sum);
                if (newDeg == 0) queue.add(next);
            }
        }
        if (sorted.size() != plugins.size()) {
            Set<String> remaining = new HashSet<>(byId.keySet());
            sorted.forEach(p -> remaining.remove(p.getId()));
            throw new IllegalStateException("Cyclic dependency detected among plugins: " + remaining);
        }
        return sorted;
    }
}
