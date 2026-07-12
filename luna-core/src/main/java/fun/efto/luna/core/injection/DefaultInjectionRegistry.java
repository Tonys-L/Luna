package fun.efto.luna.core.injection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/06/01 14:00
 */
public class DefaultInjectionRegistry implements InjectionRegistry {
    
    private final Map<String, List<InjectionPoint>> exactIndex = new ConcurrentHashMap<>();
    private final PackageTrie prefixTrie = new PackageTrie();
    private final List<RegexEntry> regexFallback = new CopyOnWriteArrayList<>();
    
    private static class RegexEntry {
        final Pattern pattern;
        final InjectionPoint point;
        RegexEntry(Pattern pattern, InjectionPoint point) {
            this.pattern = pattern;
            this.point = point;
        }
    }

    @Override
    public void register(InjectionPoint point) {
        String targetClass = point.getTarget().getTargetClass();
        if (targetClass == null || targetClass.isEmpty()) return;
        
        if (targetClass.contains("*")) {
            if (targetClass.endsWith(".*") && targetClass.indexOf('*') == targetClass.length() - 1) {
                String prefix = targetClass.substring(0, targetClass.length() - 2);
                prefixTrie.insert(prefix, point);
            } else {
                try {
                    Pattern pattern = Pattern.compile(targetClass);
                    regexFallback.add(new RegexEntry(pattern, point));
                } catch (PatternSyntaxException e) {
                    // Ignore invalid patterns
                }
            }
        } else {
            exactIndex.computeIfAbsent(targetClass, k -> new CopyOnWriteArrayList<>()).add(point);
        }
    }

    @Override
    public void unregister(String pointId) {
        for (List<InjectionPoint> list : exactIndex.values()) {
            list.removeIf(p -> p.getId().equals(pointId));
        }
        prefixTrie.removeById(pointId);
        regexFallback.removeIf(e -> e.point.getId().equals(pointId));
    }

    @Override
    public void unregisterByPluginId(String pluginId) {
        // To be implemented based on PersistentInjection plugin source
    }

    @Override
    public List<InjectionPoint> getActivePointsForClass(String className) {
        List<InjectionPoint> results = new ArrayList<>();
        
        List<InjectionPoint> exacts = exactIndex.get(className);
        if (exacts != null) {
            results.addAll(exacts);
        }
        
        results.addAll(prefixTrie.findPrefixes(className));
        
        for (RegexEntry entry : regexFallback) {
            if (entry.pattern.matcher(className).matches()) {
                results.add(entry.point);
            }
        }
        
        return results;
    }

    @Override
    public int getInjectionCount(String className) {
        return getActivePointsForClass(className).size();
    }

    @Override
    public List<InjectionPoint> getInjectionPoints(String className) {
        return getActivePointsForClass(className);
    }

    @Override
    public boolean contains(String pointId) {
        for (List<InjectionPoint> list : exactIndex.values()) {
            for (InjectionPoint p : list) {
                if (p.getId().equals(pointId)) return true;
            }
        }
        for (RegexEntry entry : regexFallback) {
            if (entry.point.getId().equals(pointId)) return true;
        }
        return prefixTrie.containsById(pointId);
    }
}
