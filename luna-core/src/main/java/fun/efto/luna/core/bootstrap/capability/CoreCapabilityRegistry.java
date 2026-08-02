package fun.efto.luna.core.bootstrap.capability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/29 00:00
 */
public class CoreCapabilityRegistry {

    private static volatile CoreCapabilityRegistry instance;

    private final ConcurrentHashMap<String, CoreCapabilityRecord> registry = new ConcurrentHashMap<>();

    private CoreCapabilityRegistry() {
    }

    public static CoreCapabilityRegistry getInstance() {
        if (instance == null) {
            synchronized (CoreCapabilityRegistry.class) {
                if (instance == null) {
                    instance = new CoreCapabilityRegistry();
                }
            }
        }
        return instance;
    }

    public void register(CoreCapabilityRecord record) {
        ReadinessState effectiveState = resolveReadiness(record);
        record.setReadinessState(effectiveState);
        registry.put(record.getCapabilityId(), record);
    }

    /**
     * 解析能力的有效就绪状态：声明 READY 时校验所有依赖是否已注册且 READY。
     * 依赖未满足时降级为 NOT_INITIALIZED，避免能力声明与实际可用性不一致。
     */
    private ReadinessState resolveReadiness(CoreCapabilityRecord record) {
        if (record.getReadinessState() != ReadinessState.READY) {
            return record.getReadinessState();
        }
        for (String dep : record.getDependencies()) {
            CoreCapabilityRecord depRecord = registry.get(dep);
            if (depRecord == null || depRecord.getReadinessState() != ReadinessState.READY) {
                return ReadinessState.NOT_INITIALIZED;
            }
        }
        return ReadinessState.READY;
    }

    public CoreCapabilityRecord get(String capabilityId) {
        return registry.get(capabilityId);
    }

    public List<CoreCapabilityRecord> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(registry.values()));
    }

    public boolean isReady(String capabilityId) {
        CoreCapabilityRecord record = registry.get(capabilityId);
        return record != null && record.getReadinessState() == ReadinessState.READY;
    }

    public List<String> getProvidedEntries(String capabilityId) {
        CoreCapabilityRecord record = registry.get(capabilityId);
        if (record == null) {
            return Collections.emptyList();
        }
        return record.getProvidedEntries();
    }

    /**
     * 查询指定能力的依赖列表（声明值）。
     */
    public List<String> getDependencies(String capabilityId) {
        CoreCapabilityRecord record = registry.get(capabilityId);
        if (record == null) {
            return Collections.emptyList();
        }
        return record.getDependencies();
    }

    /**
     * 查询依赖指定能力的下游能力列表（反向依赖图）。
     */
    public List<String> getDependents(String capabilityId) {
        List<String> dependents = new ArrayList<>();
        for (CoreCapabilityRecord record : registry.values()) {
            if (record.getDependencies().contains(capabilityId)) {
                dependents.add(record.getCapabilityId());
            }
        }
        return Collections.unmodifiableList(dependents);
    }

    public void clear() {
        registry.clear();
    }
}
