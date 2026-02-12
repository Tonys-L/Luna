package fun.efto.luna.core.injection.target.type;

import fun.efto.luna.core.TypeRegistry;
import fun.efto.luna.core.injection.target.InjectionTarget;
import fun.efto.luna.core.type.RegisterableType;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/2 20:42
 */
public class InjectionType extends RegisterableType<InjectionType> {
    private static final TypeRegistry<InjectionType> REGISTRY = new TypeRegistry<>();
    private final Class<? extends InjectionTarget> targetClass;

    public InjectionType(String name, String description, Class<? extends InjectionTarget> targetClass) {
        super(name, description);
        this.targetClass = targetClass;
    }

    public static InjectionType valueOf(String name) {
        return valueOf(REGISTRY, name);
    }

    public Class<? extends InjectionTarget> getTargetClass() {
        return targetClass;
    }

    @Override
    protected TypeRegistry<InjectionType> getRegistry() {
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
