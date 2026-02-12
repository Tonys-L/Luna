package fun.efto.luna.core.injection.code.type;

import fun.efto.luna.core.TypeRegistry;
import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.type.RegisterableType;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/3 18:03
 */
public class CodeType extends RegisterableType<CodeType> {

    private static final TypeRegistry<CodeType> REGISTRY = new TypeRegistry<>();
    private final Class<? extends InjectableCode> codeClass;

    public CodeType(String name, String description, Class<? extends InjectableCode> codeClass) {
        super(name, description);
        this.codeClass = codeClass;
    }

    public static CodeType valueOf(String name) {
        return valueOf(REGISTRY, name);
    }

    public Class<? extends InjectableCode> getCodeClass() {
        return codeClass;
    }

    @Override
    protected TypeRegistry<CodeType> getRegistry() {
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
