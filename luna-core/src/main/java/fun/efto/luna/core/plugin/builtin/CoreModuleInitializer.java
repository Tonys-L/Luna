/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/25 22:00
 */
package fun.efto.luna.core.plugin.builtin;

import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjectorRegistry;
import fun.efto.luna.core.bytecode.bytekit.adapter.ByteKitAroundInjector;
import fun.efto.luna.core.bytecode.bytekit.adapter.ByteKitEnterInjector;
import fun.efto.luna.core.bytecode.bytekit.adapter.ByteKitExceptionExitInjector;
import fun.efto.luna.core.bytecode.bytekit.adapter.ByteKitExitInjector;
import fun.efto.luna.core.bytecode.bytekit.adapter.ByteKitInvokeInjector;
import fun.efto.luna.core.capability.CapabilityKind;
import fun.efto.luna.core.capability.CoreCapabilityRecord;
import fun.efto.luna.core.capability.CoreCapabilityRegistry;
import fun.efto.luna.core.capability.LifecyclePolicy;
import fun.efto.luna.core.capability.ReadinessState;
import fun.efto.luna.core.plugin.builtin.line.*;
import fun.efto.luna.core.plugin.builtin.method.*;
import fun.efto.luna.core.plugin.registry.InjectionTypeRegistry;
import fun.efto.luna.core.plugin.registry.RuleConverterRegistry;

import java.util.Arrays;
import java.util.Collections;

/**
 * Initializes core injection modules (Method/Line) directly,
 * without going through the PluginManager lifecycle.
 * These modules are fundamental infrastructure that cannot be unloaded.
 */
public final class CoreModuleInitializer {

    private CoreModuleInitializer() {}

    public static void initialize() {
        // Method injection types
        InjectionTypeRegistry.getInstance().register(MethodInjectionType.ENTER);
        InjectionTypeRegistry.getInstance().register(MethodInjectionType.EXIT);
        InjectionTypeRegistry.getInstance().register(MethodInjectionType.AROUND);
        InjectionTypeRegistry.getInstance().register(ExceptionExitInjectionType.EXCEPTION_EXIT);
        InjectionTypeRegistry.getInstance().register(InvokeInjectionType.INVOKE);

        BytecodeInjectorRegistry.getInstance().register(MethodInjectionType.ENTER, new ByteKitEnterInjector());
        BytecodeInjectorRegistry.getInstance().register(MethodInjectionType.EXIT, new ByteKitExitInjector());
        BytecodeInjectorRegistry.getInstance().register(MethodInjectionType.AROUND, new ByteKitAroundInjector());
        BytecodeInjectorRegistry.getInstance().register(ExceptionExitInjectionType.EXCEPTION_EXIT, new ByteKitExceptionExitInjector());
        BytecodeInjectorRegistry.getInstance().register(InvokeInjectionType.INVOKE, new ByteKitInvokeInjector());

        MethodRuleConverter methodConverter = new MethodRuleConverter();
        RuleConverterRegistry.getInstance().register(MethodInjectionType.ENTER, methodConverter);
        RuleConverterRegistry.getInstance().register(MethodInjectionType.EXIT, methodConverter);
        RuleConverterRegistry.getInstance().register(MethodInjectionType.AROUND, methodConverter);
        RuleConverterRegistry.getInstance().register(ExceptionExitInjectionType.EXCEPTION_EXIT, methodConverter);
        RuleConverterRegistry.getInstance().register(InvokeInjectionType.INVOKE, methodConverter);

        // Line injection types
        InjectionTypeRegistry.getInstance().register(LineNumberInjectionType.BEFORE);
        InjectionTypeRegistry.getInstance().register(LineNumberInjectionType.AFTER);

        // Line injectors
        BytecodeInjectorRegistry.getInstance().register(LineNumberInjectionType.BEFORE, new BeforeLineInjector());
        BytecodeInjectorRegistry.getInstance().register(LineNumberInjectionType.AFTER, new AfterLineInjector());

        // Line rule converters
        LineRuleConverter lineConverter = new LineRuleConverter();
        RuleConverterRegistry.getInstance().register(LineNumberInjectionType.BEFORE, lineConverter);
        RuleConverterRegistry.getInstance().register(LineNumberInjectionType.AFTER, lineConverter);

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
