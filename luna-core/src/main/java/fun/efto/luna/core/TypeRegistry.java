package fun.efto.luna.core;

import fun.efto.luna.core.type.BaseType;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2026/01/25
 */
public class TypeRegistry<T extends BaseType> implements Registry<String, T> {
    private final Map<String, T> map = new ConcurrentHashMap<>();

    @Override
    public Map<String, T> getRegistry() {
        return map;
    }

    public void register(T type) {
        register(type.getName(), type);
    }

    public Collection<T> values() {
        return map.values();
    }
}
