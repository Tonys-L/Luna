package fun.efto.luna.core.injector;

import fun.efto.luna.core.Registry;
import fun.efto.luna.core.injection.target.type.InjectionType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/2 21:16
 */
public final class BytecodeInjectorRegistry implements Registry<InjectionType, BytecodeInjector> {
    private static final BytecodeInjectorRegistry INSTANCE = new BytecodeInjectorRegistry();
    private static final Map<InjectionType, BytecodeInjector> INJECTOR_REGISTRY = new ConcurrentHashMap<>();

    private BytecodeInjectorRegistry() {
    }

    public static BytecodeInjectorRegistry getInstance() {
        return INSTANCE;
    }

    @Override
    public Map<InjectionType, BytecodeInjector> getRegistry() {
        return INJECTOR_REGISTRY;
    }

}
