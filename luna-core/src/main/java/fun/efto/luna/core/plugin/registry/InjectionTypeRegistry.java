package fun.efto.luna.core.plugin.registry;

import fun.efto.luna.core.infra.type.Registry;
import fun.efto.luna.core.injection.target.InjectionLocation;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:00
 */
public final class InjectionTypeRegistry implements Registry<String, InjectionLocation> {

    private static final Map<String, InjectionLocation> REGISTRY = new ConcurrentHashMap<>();
    private static final InjectionTypeRegistry INSTANCE = new InjectionTypeRegistry();

    private InjectionTypeRegistry() {}

    public static InjectionTypeRegistry getInstance() {
        return INSTANCE;
    }

    @Override
    public Map<String, InjectionLocation> getRegistry() {
        return REGISTRY;
    }

    @Override
    public Optional<InjectionLocation> get(String name) {
        return Optional.ofNullable(REGISTRY.get(name.toLowerCase()));
    }

    @Override
    public InjectionLocation register(String key, InjectionLocation location) {
        REGISTRY.put(key.toLowerCase(), location);
        return location;
    }

    public InjectionLocation register(InjectionLocation location) {
        REGISTRY.put(location.getName().toLowerCase(), location);
        for (String alias : location.getAliases()) {
            REGISTRY.put(alias.toLowerCase(), location);
        }
        return location;
    }

    public InjectionLocation resolve(String name) {
        InjectionLocation location = REGISTRY.get(name.toLowerCase());
        if (location == null) {
            throw new IllegalArgumentException("Unknown injection location: " + name + ", registered: " + REGISTRY.keySet());
        }
        return location;
    }

    public void unregisterAll(Collection<InjectionLocation> locations) {
        locations.forEach(t -> {
            REGISTRY.remove(t.getName().toLowerCase());
            for (String alias : t.getAliases()) {
                REGISTRY.remove(alias.toLowerCase());
            }
        });
    }

    public Collection<InjectionLocation> getAll() {
        return REGISTRY.values();
    }

    public void clear() {
        REGISTRY.clear();
    }
}
