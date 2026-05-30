/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/17 19:00
 */
package fun.efto.luna.core.injection;

import fun.efto.luna.core.injection.port.InjectionStore;
import fun.efto.luna.core.injection.port.Retransformer;
import fun.efto.luna.core.util.VisibleForTesting;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 注入管理器（领域服务）。
 * 只负责注入的生命周期管理（CRUD + 持久化 + 缓存 + retransform 触发）。
 * 校验、预览、验证等编排逻辑由 InjectionService 负责。
 */
public class InjectionManager implements InjectionQuery, InjectionLifecycle {

    private static volatile InjectionManager instance;

    private volatile Retransformer retransformer;
    private volatile InjectionStore injectionStore;

    private final Map<String, PersistentInjection> injections = new ConcurrentHashMap<>();

    // classIndex 中的 ArrayList 值只能在 synchronized 块内进行读写与遍历
    private final Map<String, List<PersistentInjection>> classIndex = new ConcurrentHashMap<>();

    private final Map<String, List<InjectionPoint>> pointCache = new ConcurrentHashMap<>();

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
        return pointCache.getOrDefault(className, Collections.emptyList());
    }

    public synchronized List<PersistentInjection> getInjections() {
        return new ArrayList<>(injections.values());
    }

    public synchronized PersistentInjection getInjection(String id) {
        return injections.get(id);
    }

    @Override
    public List<InjectionPoint> getInjectionPoints(String className) {
        return requireInjectionStore().findByClassName(className);
    }

    @Override
    public int getInjectionCount(String className) {
        return requireInjectionStore().countByClassName(className);
    }

    // ==================== InjectionLifecycle ====================

    @Override
    public synchronized String addInjection(PersistentInjection injection) {
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
        requireRetransformer().retransform(className);
    }

    // ==================== 内部方法 ====================

    public synchronized void rebuildCache(String className) {
        List<PersistentInjection> list = classIndex.get(className);
        if (list == null || list.isEmpty()) {
            pointCache.remove(className);
            requireInjectionStore().clear(className);
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

        pointCache.put(className, compiledPoints);

        InjectionStore store = requireInjectionStore();
        store.clear(className);
        compiledPoints.forEach(store::save);
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
}
