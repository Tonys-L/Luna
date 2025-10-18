package fun.efto.luna.core.injection.target.type;

import fun.efto.luna.core.injection.target.InjectionTarget;
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
    private final Class<? extends InjectionTarget> targetClass;

    public InjectionType(String name, String description, Class<? extends InjectionTarget> targetClass) {
        super(name, description);
        this.targetClass = targetClass;
    }

    public static InjectionType createByType(String name, String description, Class<? extends InjectionType> targetClass) {
        return BaseType.create(name, description, targetClass);
    }

    public static InjectionType valueOf(String name) {
        return (InjectionType) REGISTRY.get(name);
    }

    public Class<? extends InjectionTarget> getTargetClass() {
        return targetClass;
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
