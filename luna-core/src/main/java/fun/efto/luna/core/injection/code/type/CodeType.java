package fun.efto.luna.core.injection.code.type;

import fun.efto.luna.core.injection.code.InjectableCode;
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
    private final Class<? extends InjectableCode> codeClass;

    public CodeType(String name, String description, Class<? extends InjectableCode> codeClass) {
        super(name, description);
        this.codeClass = codeClass;
    }

    public static CodeType createByType(String name, String description, Class<? extends CodeType> typeClass) {
        return BaseType.create(name, description, typeClass);
    }

    public static CodeType valueOf(String name) {
        return (CodeType) REGISTRY.get(name);
    }

    public Class<? extends InjectableCode> getCodeClass() {
        return codeClass;
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
