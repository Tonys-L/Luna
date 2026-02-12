package fun.efto.luna.core.type;

import fun.efto.luna.core.Registry;
import fun.efto.luna.core.TypeRegistry;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2026/01/25
 */
public abstract class RegisterableType<T extends BaseType> extends BaseType {

    public RegisterableType(String name, String description) {
        super(name, description);
    }

    protected static <T extends BaseType> T valueOf(Registry<String, T> registry, String name) {
        return registry.get(name)
                .orElseThrow(() -> new IllegalArgumentException("Unknown type: " + name));
    }

    protected abstract TypeRegistry<T> getRegistry();

    @SuppressWarnings("unchecked")
    public T register() {
        getRegistry().register((T) this);
        return (T) this;
    }
}
