package fun.efto.luna.core.bytecode.asm.injector;

import fun.efto.luna.core.type.Registry;
import fun.efto.luna.core.injection.target.InjectionType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2025/10/2 21:16
 */
public final class BytecodeInjectorRegistry implements Registry<InjectionType, BytecodeInjector> {
    private static final Map<InjectionType, BytecodeInjector> INJECTOR_REGISTRY = new ConcurrentHashMap<>();
    private static final BytecodeInjectorRegistry INSTANCE = new BytecodeInjectorRegistry();

    private BytecodeInjectorRegistry() {
    }

    public static BytecodeInjectorRegistry getInstance() {
        return INSTANCE;
    }

    @Override
    public Map<InjectionType, BytecodeInjector> getRegistry() {
        return INJECTOR_REGISTRY;
    }

    @Override
    public BytecodeInjector register(InjectionType type, BytecodeInjector injector) {
        INJECTOR_REGISTRY.put(type, injector);
        return injector;
    }
}
