package fun.efto.luna.core.plugin.builtin;

import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjectorRegistry;
import fun.efto.luna.core.bytecode.bytekit.adapter.ByteKitAroundInjector;
import fun.efto.luna.core.bytecode.bytekit.adapter.ByteKitEnterInjector;
import fun.efto.luna.core.bytecode.bytekit.adapter.ByteKitExceptionExitInjector;
import fun.efto.luna.core.bytecode.bytekit.adapter.ByteKitExitInjector;
import fun.efto.luna.core.bytecode.bytekit.adapter.ByteKitInvokeInjector;
import fun.efto.luna.core.bootstrap.capability.CapabilityKind;
import fun.efto.luna.core.bootstrap.capability.CoreCapabilityRecord;
import fun.efto.luna.core.bootstrap.capability.CoreCapabilityRegistry;
import fun.efto.luna.core.bootstrap.capability.LifecyclePolicy;
import fun.efto.luna.core.bootstrap.capability.ReadinessState;
import fun.efto.luna.core.plugin.builtin.line.*;
import fun.efto.luna.core.plugin.builtin.method.*;
import fun.efto.luna.core.plugin.registry.InjectionTypeRegistry;
import fun.efto.luna.core.plugin.InjectionLocationUIDescriptor;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since : 2026/05/25 22:00
 */
public final class CoreModuleInitializer {

    private CoreModuleInitializer() {}

    public static void initialize() {
        InjectionTypeRegistry registry = InjectionTypeRegistry.getInstance();

        registry.register(MethodInjectionLocation.ENTER, new InjectionLocationUIDescriptor("方法注入", "#10b981"));
        registry.register(MethodInjectionLocation.EXIT, new InjectionLocationUIDescriptor("方法注入", "#3b82f6"));
        registry.register(MethodInjectionLocation.AROUND, new InjectionLocationUIDescriptor("方法注入", "#8b5cf6"));
        registry.register(ExceptionExitInjectionLocation.EXCEPTION_EXIT, new InjectionLocationUIDescriptor("方法注入", "#ef4444"));
        registry.register(InvokeInjectionLocation.INVOKE, new InjectionLocationUIDescriptor("方法注入", "#f59e0b"));

        BytecodeInjectorRegistry.getInstance().register(MethodInjectionLocation.ENTER, new ByteKitEnterInjector());
        BytecodeInjectorRegistry.getInstance().register(MethodInjectionLocation.EXIT, new ByteKitExitInjector());
        BytecodeInjectorRegistry.getInstance().register(MethodInjectionLocation.AROUND, new ByteKitAroundInjector());
        BytecodeInjectorRegistry.getInstance().register(ExceptionExitInjectionLocation.EXCEPTION_EXIT, new ByteKitExceptionExitInjector());
        BytecodeInjectorRegistry.getInstance().register(InvokeInjectionLocation.INVOKE, new ByteKitInvokeInjector());

        registry.register(LineNumberInjectionLocation.BEFORE, new InjectionLocationUIDescriptor("行号注入", "#6366f1"));
        registry.register(LineNumberInjectionLocation.AFTER, new InjectionLocationUIDescriptor("行号注入", "#a78bfa"));

        BytecodeInjectorRegistry.getInstance().register(LineNumberInjectionLocation.BEFORE, new BeforeLineInjector());
        BytecodeInjectorRegistry.getInstance().register(LineNumberInjectionLocation.AFTER, new AfterLineInjector());

        CoreCapabilityRegistry capRegistry = CoreCapabilityRegistry.getInstance();

        capRegistry.register(new CoreCapabilityRecord(
                "method-target", "方法注入目标", CapabilityKind.KERNEL,
                Arrays.asList("method_enter", "method_exit", "method_around", "exception_exit", "invoke"),
                ReadinessState.READY, Collections.emptyList(), LifecyclePolicy.CORE_ONLY
        ));

        capRegistry.register(new CoreCapabilityRecord(
                "line-target", "行号注入目标", CapabilityKind.KERNEL,
                Arrays.asList("line_before", "line_after"),
                ReadinessState.READY, Collections.emptyList(), LifecyclePolicy.CORE_ONLY
        ));
    }
}
