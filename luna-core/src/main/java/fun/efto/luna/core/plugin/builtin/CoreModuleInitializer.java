/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/25 22:00
 */
package fun.efto.luna.core.plugin.builtin;

import fun.efto.luna.core.asm.injector.BytecodeInjectorRegistry;
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

        // Method injectors
        BytecodeInjectorRegistry.getInstance().register(MethodInjectionType.ENTER, new EnterMethodInjector());
        BytecodeInjectorRegistry.getInstance().register(MethodInjectionType.EXIT, new ExitMethodInjector());
        BytecodeInjectorRegistry.getInstance().register(MethodInjectionType.AROUND, new AroundMethodInjector());

        // Method rule converters
        MethodRuleConverter methodConverter = new MethodRuleConverter();
        RuleConverterRegistry.getInstance().register(MethodInjectionType.ENTER, methodConverter);
        RuleConverterRegistry.getInstance().register(MethodInjectionType.EXIT, methodConverter);
        RuleConverterRegistry.getInstance().register(MethodInjectionType.AROUND, methodConverter);

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
                Arrays.asList("method_enter", "method_exit", "method_around"),
                ReadinessState.READY, Collections.emptyList(), LifecyclePolicy.CORE_ONLY
        ));

        capRegistry.register(new CoreCapabilityRecord(
                "line-target", "行号注入目标", CapabilityKind.KERNEL,
                Arrays.asList("line_before", "line_after"),
                ReadinessState.READY, Collections.emptyList(), LifecyclePolicy.CORE_ONLY
        ));
    }
}
