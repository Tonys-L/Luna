package fun.efto.luna.core.injection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/06/01 15:00
 */
public class DefaultInjectionRepository implements InjectionRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultInjectionRepository.class);

    private final Map<String, PersistentInjection> injections = new ConcurrentHashMap<>();
    private final InjectionPersistenceService persistenceService = new InjectionPersistenceService();
    private final Object persistLock = new Object();

    public DefaultInjectionRepository() {
        try {
            List<PersistentInjection> loaded = persistenceService.load();
            for (PersistentInjection inj : loaded) {
                inj.setStatus(InjectionStatus.ACTIVE);
                injections.put(inj.getId(), inj);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load injections from disk", e);
        }
    }

    @Override
    public void save(PersistentInjection injection) {
        injections.put(injection.getId(), injection);
        if (!injection.isEphemeral()) {
            persist();
        }
    }

    @Override
    public void delete(String id) {
        PersistentInjection removed = injections.remove(id);
        if (removed != null && !removed.isEphemeral()) {
            persist();
        }
    }

    @Override
    public PersistentInjection findById(String id) {
        return injections.get(id);
    }

    @Override
    public List<PersistentInjection> findAll() {
        return new ArrayList<>(injections.values());
    }

    @Override
    public List<PersistentInjection> findByGroupId(String groupId) {
        List<PersistentInjection> result = new ArrayList<>();
        for (PersistentInjection inj : injections.values()) {
            if (groupId.equals(inj.getGroupId())) {
                result.add(inj);
            }
        }
        return result;
    }
    
    private void persist() {
        synchronized (persistLock) {
            try {
                persistenceService.save(new ArrayList<>(injections.values()));
            } catch (Exception e) {
                LOGGER.error("Failed to save injections to disk", e);
            }
        }
    }
}
