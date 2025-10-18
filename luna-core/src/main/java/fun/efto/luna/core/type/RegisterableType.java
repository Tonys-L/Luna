package fun.efto.luna.core.type;

import fun.efto.luna.core.Registry;

import java.util.Map;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 3:45
 */
public abstract class RegisterableType<T extends RegisterableType<T>> extends BaseType implements Registry<String, RegisterableType<T>> {
    protected RegisterableType(String name, String description) {
        super(name, description);
    }

    protected static <T extends RegisterableType<T>> T getTypeByName(String name, Map<String, T> registry) {
        return registry.get(name);
    }

    @SuppressWarnings("unchecked")
    public T register() {
        getRegistry().put(this.getName(), this);
        return (T) this;
    }

}
