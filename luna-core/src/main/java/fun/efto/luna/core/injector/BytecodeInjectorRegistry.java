package fun.efto.luna.core.injector;

import fun.efto.luna.core.Registry;
import fun.efto.luna.core.asm.injector.AfterLineInjector;
import fun.efto.luna.core.asm.injector.AroundMethodInjector;
import fun.efto.luna.core.asm.injector.BeforeLineInjector;
import fun.efto.luna.core.asm.injector.EnterMethodInjector;
import fun.efto.luna.core.asm.injector.ExitMethodInjector;
import fun.efto.luna.core.injection.target.type.InjectionType;
import fun.efto.luna.core.injection.target.type.LineNumberInjectionType;
import fun.efto.luna.core.injection.target.type.MethodInjectionType;

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
        // 注册方法级注入器
        try {
            INJECTOR_REGISTRY.put(MethodInjectionType.ENTER, new EnterMethodInjector());
            INJECTOR_REGISTRY.put(MethodInjectionType.EXIT, new ExitMethodInjector());
            INJECTOR_REGISTRY.put(MethodInjectionType.AROUND, new AroundMethodInjector());
            // 注册行号级注入器
            INJECTOR_REGISTRY.put(LineNumberInjectionType.BEFORE, new BeforeLineInjector());
            INJECTOR_REGISTRY.put(LineNumberInjectionType.AFTER, new AfterLineInjector());
        } catch (Exception e) {
            System.err.println("Error initializing BytecodeInjectorRegistry: " + e.getMessage());
            e.printStackTrace();
        }
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
