package fun.efto.luna.core.plugin;

import fun.efto.luna.core.injection.target.type.InjectionType;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
public final class InjectionTypeRegistry {

    private static final ConcurrentHashMap<String, InjectionType> REGISTRY = new ConcurrentHashMap<>();

    private InjectionTypeRegistry() {}

    public static void register(InjectionType type) {
        REGISTRY.put(type.getName().toLowerCase(), type);
        for (String alias : type.getAliases()) {
            REGISTRY.put(alias.toLowerCase(), type);
        }
    }

    public static InjectionType resolve(String name) {
        InjectionType type = REGISTRY.get(name.toLowerCase());
        if (type == null) {
            throw new IllegalArgumentException("Unknown injection type: " + name + ", registered: " + REGISTRY.keySet());
        }
        return type;
    }

    public static Optional<InjectionType> find(String name) {
        return Optional.ofNullable(REGISTRY.get(name.toLowerCase()));
    }

    public static void unregisterAll(Collection<InjectionType> types) {
        types.forEach(t -> {
            REGISTRY.remove(t.getName().toLowerCase());
            for (String alias : t.getAliases()) {
                REGISTRY.remove(alias.toLowerCase());
            }
        });
    }

    public static Collection<InjectionType> getAll() {
        return REGISTRY.values();
    }

    public static void clear() {
        REGISTRY.clear();
    }
}
