package fun.efto.luna.core.injection.code.type;

import fun.efto.luna.core.type.BaseType;
import fun.efto.luna.core.type.RegisterableType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/3 18:03
 */
public class CodeType extends RegisterableType<CodeType> {

    private static final Map<String, RegisterableType<CodeType>> REGISTRY = new ConcurrentHashMap<>();

    public CodeType(String name, String description) {
        super(name, description);
    }

    public static CodeType valueOf(String name) {
        return (CodeType) REGISTRY.get(name);
    }

    public static CodeType create(String name, String description) {
        return BaseType.create(name, description, CodeType.class);
    }

    @Override
    public Map<String, RegisterableType<CodeType>> getRegistry() {
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
