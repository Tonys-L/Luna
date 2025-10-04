package fun.efto.luna.core.injection.target.type;

import fun.efto.luna.core.type.BaseType;
import fun.efto.luna.core.type.RegisterableType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/2 20:42
 */
public class InjectionType extends RegisterableType<InjectionType> {
    private static final Map<String, RegisterableType<InjectionType>> REGISTRY = new ConcurrentHashMap<>();
    public InjectionType(String name, String description) {
        super(name, description);
    }

    public static InjectionType valueOf(String name) {
        return (InjectionType) REGISTRY.get(name);
    }

    public static InjectionType create(String name, String description) {
        return BaseType.create(name, description, InjectionType.class);
    }

    @Override
    public Map<String, RegisterableType<InjectionType>> getRegistry() {
        return REGISTRY;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
