package fun.efto.luna.core.plugin.registry;

import fun.efto.luna.core.plugin.ProbeHandler;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/01 01:30
 */
public final class ProbeHandlerRegistry {

    private static final Map<String, ProbeHandler> REGISTRY = new ConcurrentHashMap<>();
    private static final ProbeHandlerRegistry INSTANCE = new ProbeHandlerRegistry();

    private ProbeHandlerRegistry() {}

    public static ProbeHandlerRegistry getInstance() {
        return INSTANCE;
    }

    public ProbeHandler register(ProbeHandler handler) {
        REGISTRY.put(handler.getProbeType().toUpperCase(), handler);
        return handler;
    }

    public Optional<ProbeHandler> get(String probeType) {
        if (probeType == null) return Optional.empty();
        return Optional.ofNullable(REGISTRY.get(probeType.toUpperCase()));
    }

    public void unregisterAll(Collection<ProbeHandler> handlers) {
        handlers.forEach(h -> REGISTRY.remove(h.getProbeType().toUpperCase()));
    }

    public Collection<ProbeHandler> getAll() {
        return REGISTRY.values();
    }

    public Set<String> getRegisteredTypes() {
        return REGISTRY.keySet();
    }

    public void clear() {
        REGISTRY.clear();
    }
}
