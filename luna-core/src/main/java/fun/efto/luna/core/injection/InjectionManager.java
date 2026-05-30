package fun.efto.luna.core.injection;

import fun.efto.luna.core.injection.port.InjectionStore;
import fun.efto.luna.core.injection.port.Retransformer;
import fun.efto.luna.core.infra.util.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 注入管理器（领域服务）。
 * 只负责注入的生命周期管理（CRUD + 持久化 + 缓存 + retransform 触发）。
 * 校验、预览、验证等编排逻辑由 InjectionService 负责。
 *
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/17 19:00
 */
public class InjectionManager implements InjectionQuery, InjectionLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(InjectionManager.class);

    private static volatile InjectionManager instance;

    private volatile Retransformer retransformer;
    private volatile InjectionStore injectionStore;

    private final Map<String, PersistentInjection> injections = new ConcurrentHashMap<>();

    // classIndex 中的 ArrayList 值只能在 synchronized 块内进行读写与遍历
    private final Map<String, List<PersistentInjection>> classIndex = new ConcurrentHashMap<>();

    private final Map<String, List<InjectionPoint>> pointCache = new ConcurrentHashMap<>();

    private volatile List<PatternInjectionEntry> patternPoints = Collections.emptyList();

    private final InjectionPersistenceService persistenceService = new InjectionPersistenceService();

    private InjectionManager() {
        try {
            List<PersistentInjection> loaded = persistenceService.load();
            for (PersistentInjection inj : loaded) {
                inj.setStatus(InjectionStatus.ACTIVE);
                injections.put(inj.getId(), inj);
                classIndex.computeIfAbsent(inj.getClazz(), k -> new ArrayList<>()).add(inj);
            }
            for (String className : classIndex.keySet()) {
                rebuildCache(className);
            }
        } catch (Exception e) {
            System.err.println("Failed to load injections from disk: " + e.getMessage());
        }
    }

    public static InjectionManager getInstance() {
        if (instance == null) {
            synchronized (InjectionManager.class) {
                if (instance == null) {
                    instance = new InjectionManager();
                }
            }
        }
        return instance;
    }

    @VisibleForTesting
    public static void setInstance(InjectionManager newInstance) {
        instance = newInstance;
    }

    // ==================== 出站端口注册 ====================

    public void setRetransformer(Retransformer retransformer) { this.retransformer = retransformer; }
    public void setInjectionStore(InjectionStore injectionStore) { this.injectionStore = injectionStore; }

    // ==================== InjectionQuery ====================

    @Override
    public List<InjectionPoint> getActivePointsForClass(String className) {
        List<InjectionPoint> exactMatches = pointCache.getOrDefault(className, Collections.emptyList());
        List<PatternInjectionEntry> currentPatterns = patternPoints;
        if (currentPatterns.isEmpty()) {
            return exactMatches;
        }

        List<InjectionPoint> patternMatches = null;
        for (PatternInjectionEntry entry : currentPatterns) {
            if (entry.pattern.matcher(className).matches()) {
                if (patternMatches == null) {
                    patternMatches = new ArrayList<>();
                }
                patternMatches.addAll(entry.points);
            }
        }

        if (patternMatches == null) {
            return exactMatches;
        }
        if (exactMatches.isEmpty()) {
            return patternMatches;
        }

        List<InjectionPoint> result = new ArrayList<>(exactMatches.size() + patternMatches.size());
        result.addAll(exactMatches);
        result.addAll(patternMatches);
        return result;
    }

    public synchronized List<PersistentInjection> getInjections() {
        return new ArrayList<>(injections.values());
    }

    public synchronized PersistentInjection getInjection(String id) {
        return injections.get(id);
    }

    @Override
    public List<InjectionPoint> getInjectionPoints(String className) {
        if (injectionStore == null) {
            return getActivePointsForClass(className);
        }
        return injectionStore.findByClassName(className);
    }

    @Override
    public int getInjectionCount(String className) {
        if (injectionStore == null) {
            List<InjectionPoint> points = getActivePointsForClass(className);
            return points.size();
        }
        return injectionStore.countByClassName(className);
    }

    // ==================== InjectionLifecycle ====================

    @Override
    public synchronized String addInjection(PersistentInjection injection) {
        if (injection.getClazz() == null || injection.getClazz().isEmpty()) {
            throw new IllegalArgumentException("Injection clazz must not be null or empty");
        }
        if (injection.getId() == null) {
            injection.setId(UUID.randomUUID().toString());
        }

        PersistentInjection old = injections.put(injection.getId(), injection);
        String oldClazz = null;
        if (old != null) {
            oldClazz = old.getClazz();
            List<PersistentInjection> oldList = classIndex.get(oldClazz);
            if (oldList != null) {
                oldList.remove(old);
                if (oldList.isEmpty()) {
                    classIndex.remove(oldClazz);
                }
            }
        }

        classIndex.computeIfAbsent(injection.getClazz(), k -> new ArrayList<>()).add(injection);
        rebuildCache(injection.getClazz());

        if (oldClazz != null && !oldClazz.equals(injection.getClazz())) {
            rebuildCache(oldClazz);
            triggerRetransform(oldClazz);
        }

        if (!injection.isEphemeral()) {
            markDirty();
        }

        triggerRetransform(injection.getClazz());
        return injection.getId();
    }

    @Override
    public synchronized void removeInjection(String id) {
        PersistentInjection removed = injections.remove(id);
        if (removed == null) return;

        List<PersistentInjection> groupMembers = collectGroupMembers(removed.getGroupId(), id);

        for (PersistentInjection groupMember : groupMembers) {
            injections.remove(groupMember.getId());
            List<PersistentInjection> list = classIndex.get(groupMember.getClazz());
            if (list != null) {
                list.remove(groupMember);
                if (list.isEmpty()) {
                    classIndex.remove(groupMember.getClazz());
                }
            }
        }

        List<PersistentInjection> mainList = classIndex.get(removed.getClazz());
        if (mainList != null) {
            mainList.remove(removed);
            if (mainList.isEmpty()) {
                classIndex.remove(removed.getClazz());
            }
        }

        Set<String> affectedClasses = new LinkedHashSet<>();
        affectedClasses.add(removed.getClazz());
        for (PersistentInjection gm : groupMembers) {
            affectedClasses.add(gm.getClazz());
        }
        for (String className : affectedClasses) {
            rebuildCache(className);
            triggerRetransform(className);
        }

        if (!removed.isEphemeral()) {
            markDirty();
        }
    }

    @Override
    public synchronized void updateInjection(String id, PersistentInjection injection) {
        injection.setId(id);
        PersistentInjection old = injections.put(id, injection);

        String oldClazz = null;
        if (old != null) {
            oldClazz = old.getClazz();
            List<PersistentInjection> oldList = classIndex.get(oldClazz);
            if (oldList != null) {
                oldList.remove(old);
                if (oldList.isEmpty()) {
                    classIndex.remove(oldClazz);
                }
            }
        }

        classIndex.computeIfAbsent(injection.getClazz(), k -> new ArrayList<>()).add(injection);
        rebuildCache(injection.getClazz());

        if (oldClazz != null && !oldClazz.equals(injection.getClazz())) {
            rebuildCache(oldClazz);
            triggerRetransform(oldClazz);
        }

        if (!injection.isEphemeral() || (old != null && !old.isEphemeral())) {
            markDirty();
        }

        triggerRetransform(injection.getClazz());
    }

    @Override
    public synchronized void toggleEnabled(String id, boolean enabled) {
        PersistentInjection inj = injections.get(id);
        if (inj == null) return;
        inj.setEnabled(enabled);

        List<PersistentInjection> groupMembers = collectGroupMembers(inj.getGroupId(), id);
        Set<String> affectedClasses = new LinkedHashSet<>();
        affectedClasses.add(inj.getClazz());

        for (PersistentInjection groupMember : groupMembers) {
            groupMember.setEnabled(enabled);
            affectedClasses.add(groupMember.getClazz());
        }

        for (String className : affectedClasses) {
            rebuildCache(className);
            triggerRetransform(className);
        }

        if (!inj.isEphemeral()) {
            markDirty();
        }
    }

    // ==================== retransform 触发 ====================

    public void triggerRetransform(String className) {
        if (retransformer == null) {
            LOGGER.warn("Retransformer is null, skipping retransform for class [{}]", className);
            return;
        }
        try {
            if (isPattern(className)) {
                retransformer.retransformByPattern(className);
            } else {
                retransformer.retransform(className);
            }
            LOGGER.info("Retransform triggered for class [{}]", className);
        } catch (Throwable e) {
            LOGGER.error("Retransform failed for class [{}]: {} - {}", className, e.getClass().getSimpleName(), e.getMessage());
        }
    }

    // ==================== 内部方法 ====================

    public synchronized void rebuildCache(String className) {
        List<PersistentInjection> list = classIndex.get(className);
        if (list == null || list.isEmpty()) {
            pointCache.remove(className);
            removePatternEntry(className);
            if (injectionStore != null) {
                injectionStore.clear(className);
            }
            return;
        }

        List<InjectionPoint> compiledPoints = list.stream()
            .filter(PersistentInjection::isEnabled)
            .filter(inj -> inj.getStatus() == InjectionStatus.ACTIVE)
            .map(inj -> {
                try {
                    return InjectionPointFactory.create(inj);
                } catch (Exception e) {
                    System.err.println("Failed to compile injection point [id=" + inj.getId() + "]: " + e.getMessage());
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        if (isPattern(className)) {
            removePatternEntry(className);
            if (!compiledPoints.isEmpty()) {
                List<PatternInjectionEntry> updated = new ArrayList<>(patternPoints);
                updated.add(new PatternInjectionEntry(className, compiledPoints));
                patternPoints = Collections.unmodifiableList(updated);
            }
            pointCache.remove(className);
        } else {
            pointCache.put(className, compiledPoints);
            if (injectionStore != null) {
                injectionStore.clear(className);
                compiledPoints.forEach(injectionStore::save);
            }
        }
    }

    private void markDirty() {
        try {
            persistenceService.save(new ArrayList<>(injections.values()));
        } catch (Exception e) {
            System.err.println("Failed to save injections: " + e.getMessage());
        }
    }

    private List<PersistentInjection> collectGroupMembers(String groupId, String excludeId) {
        if (groupId == null) return Collections.emptyList();
        return injections.values().stream()
            .filter(i -> groupId.equals(i.getGroupId()) && !i.getId().equals(excludeId))
            .collect(Collectors.toList());
    }

    private Retransformer requireRetransformer() {
        if (retransformer == null) {
            throw new IllegalStateException("Retransformer port not configured");
        }
        return retransformer;
    }

    private InjectionStore requireInjectionStore() {
        if (injectionStore == null) {
            throw new IllegalStateException("InjectionStore port not configured");
        }
        return injectionStore;
    }

    private static boolean isPattern(String className) {
        return className != null && (className.contains("*") || className.contains("?"));
    }

    private static Pattern compilePattern(String pattern) {
        String regex = pattern.replace(".", "\\.").replace("*", ".*").replace("?", ".");
        return Pattern.compile(regex);
    }

    private void removePatternEntry(String className) {
        if (!isPattern(className)) return;
        List<PatternInjectionEntry> current = patternPoints;
        if (current.isEmpty()) return;
        Pattern toRemove = compilePattern(className);
        List<PatternInjectionEntry> updated = new ArrayList<>();
        for (PatternInjectionEntry entry : current) {
            if (!entry.pattern.pattern().equals(toRemove.pattern())) {
                updated.add(entry);
            }
        }
        if (updated.size() != current.size()) {
            patternPoints = Collections.unmodifiableList(updated);
        }
    }

    private static class PatternInjectionEntry {
        final Pattern pattern;
        final List<InjectionPoint> points;

        PatternInjectionEntry(String patternStr, List<InjectionPoint> points) {
            this.pattern = compilePattern(patternStr);
            this.points = Collections.unmodifiableList(new ArrayList<>(points));
        }
    }
}