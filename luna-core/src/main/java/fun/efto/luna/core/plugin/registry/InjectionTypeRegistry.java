package fun.efto.luna.core.plugin.registry;

import fun.efto.luna.core.infra.type.Registry;
import fun.efto.luna.core.injection.target.InjectionType;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:00
 */
public final class InjectionTypeRegistry implements Registry<String, InjectionType> {

    private static final Map<String, InjectionType> REGISTRY = new ConcurrentHashMap<>();
    private static final InjectionTypeRegistry INSTANCE = new InjectionTypeRegistry();

    private InjectionTypeRegistry() {}

    public static InjectionTypeRegistry getInstance() {
        return INSTANCE;
    }

    @Override
    public Map<String, InjectionType> getRegistry() {
        return REGISTRY;
    }

    @Override
    public Optional<InjectionType> get(String name) {
        return Optional.ofNullable(REGISTRY.get(name.toLowerCase()));
    }

    @Override
    public InjectionType register(String key, InjectionType type) {
        REGISTRY.put(key.toLowerCase(), type);
        return type;
    }

    public InjectionType register(InjectionType type) {
        REGISTRY.put(type.getName().toLowerCase(), type);
        for (String alias : type.getAliases()) {
            REGISTRY.put(alias.toLowerCase(), type);
        }
        return type;
    }

    public InjectionType resolve(String name) {
        InjectionType type = REGISTRY.get(name.toLowerCase());
        if (type == null) {
            throw new IllegalArgumentException("Unknown injection type: " + name + ", registered: " + REGISTRY.keySet());
        }
        return type;
    }

    public void unregisterAll(Collection<InjectionType> types) {
        types.forEach(t -> {
            REGISTRY.remove(t.getName().toLowerCase());
            for (String alias : t.getAliases()) {
                REGISTRY.remove(alias.toLowerCase());
            }
        });
    }

    public Collection<InjectionType> getAll() {
        return REGISTRY.values();
    }

    public void clear() {
        REGISTRY.clear();
    }
}
