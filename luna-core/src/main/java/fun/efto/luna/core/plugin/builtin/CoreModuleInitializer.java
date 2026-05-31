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
import fun.efto.luna.core.plugin.registry.RuleConverterRegistry;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/25 22:00
 */
public final class CoreModuleInitializer {

    private CoreModuleInitializer() {}

    public static void initialize() {
        InjectionTypeRegistry.getInstance().register(MethodInjectionLocation.ENTER);
        InjectionTypeRegistry.getInstance().register(MethodInjectionLocation.EXIT);
        InjectionTypeRegistry.getInstance().register(MethodInjectionLocation.AROUND);
        InjectionTypeRegistry.getInstance().register(ExceptionExitInjectionLocation.EXCEPTION_EXIT);
        InjectionTypeRegistry.getInstance().register(InvokeInjectionLocation.INVOKE);

        BytecodeInjectorRegistry.getInstance().register(MethodInjectionLocation.ENTER, new ByteKitEnterInjector());
        BytecodeInjectorRegistry.getInstance().register(MethodInjectionLocation.EXIT, new ByteKitExitInjector());
        BytecodeInjectorRegistry.getInstance().register(MethodInjectionLocation.AROUND, new ByteKitAroundInjector());
        BytecodeInjectorRegistry.getInstance().register(ExceptionExitInjectionLocation.EXCEPTION_EXIT, new ByteKitExceptionExitInjector());
        BytecodeInjectorRegistry.getInstance().register(InvokeInjectionLocation.INVOKE, new ByteKitInvokeInjector());

        MethodRuleConverter methodConverter = new MethodRuleConverter();
        RuleConverterRegistry.getInstance().register(MethodInjectionLocation.ENTER, methodConverter);
        RuleConverterRegistry.getInstance().register(MethodInjectionLocation.EXIT, methodConverter);
        RuleConverterRegistry.getInstance().register(MethodInjectionLocation.AROUND, methodConverter);
        RuleConverterRegistry.getInstance().register(ExceptionExitInjectionLocation.EXCEPTION_EXIT, methodConverter);
        RuleConverterRegistry.getInstance().register(InvokeInjectionLocation.INVOKE, methodConverter);

        InjectionTypeRegistry.getInstance().register(LineNumberInjectionLocation.BEFORE);
        InjectionTypeRegistry.getInstance().register(LineNumberInjectionLocation.AFTER);

        BytecodeInjectorRegistry.getInstance().register(LineNumberInjectionLocation.BEFORE, new BeforeLineInjector());
        BytecodeInjectorRegistry.getInstance().register(LineNumberInjectionLocation.AFTER, new AfterLineInjector());

        LineRuleConverter lineConverter = new LineRuleConverter();
        RuleConverterRegistry.getInstance().register(LineNumberInjectionLocation.BEFORE, lineConverter);
        RuleConverterRegistry.getInstance().register(LineNumberInjectionLocation.AFTER, lineConverter);

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
