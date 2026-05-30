package fun.efto.luna.core.capability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ：Tony.L(286269159@qq.com)
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
        registry.put(record.getCapabilityId(), record);
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

    public void clear() {
        registry.clear();
    }
}
