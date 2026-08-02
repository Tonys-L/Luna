package fun.efto.luna.core.bootstrap.init;

import fun.efto.luna.core.analysis.analyzer.AnalyzerRegistry;
import fun.efto.luna.core.analysis.analyzer.AnalyzerType;
import fun.efto.luna.core.bytecode.asm.analyzer.AsmClassAnalyzer;
import fun.efto.luna.core.bootstrap.capability.CapabilityKind;
import fun.efto.luna.core.bootstrap.capability.CoreCapabilityRecord;
import fun.efto.luna.core.bootstrap.capability.CoreCapabilityRegistry;
import fun.efto.luna.core.bootstrap.capability.LifecyclePolicy;
import fun.efto.luna.core.bootstrap.capability.ReadinessState;
import fun.efto.luna.core.plugin.registry.InjectionTypeRegistry;
import fun.efto.luna.core.plugin.LunaPlugin;
import fun.efto.luna.core.plugin.builtin.CoreModuleInitializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2025/10/4 17:36
 */
public class DefaultInitializer implements Initializer {
    @Override
    public void initialize() {
        // Initialize core modules (Method/Line) directly - they are not plugins
        CoreModuleInitializer.initialize();

        AnalyzerRegistry analyzerRegistry = AnalyzerRegistry.getInstance();
        analyzerRegistry.register(new AnalyzerType("ASM", "ASM字节码分析器").register(), new AsmClassAnalyzer());

        CoreCapabilityRegistry capRegistry = CoreCapabilityRegistry.getInstance();

        capRegistry.register(new CoreCapabilityRecord(
                "bytecode-assembly", "字节码装配", CapabilityKind.KERNEL,
                Arrays.asList("expression"),
                ReadinessState.READY, Collections.emptyList(), LifecyclePolicy.CORE_ONLY
        ));

        capRegistry.register(new CoreCapabilityRecord(
                "injection-lifecycle", "注入生命周期", CapabilityKind.KERNEL,
                Arrays.asList("injection_lifecycle", "injection_query", "class_index", "point_cache"),
                ReadinessState.READY, Collections.emptyList(), LifecyclePolicy.CORE_ONLY
        ));

        capRegistry.register(new CoreCapabilityRecord(
                "code-compiler-dispatch", "代码编译调度", CapabilityKind.KERNEL,
                Arrays.asList("code_compiler_strategies"),
                ReadinessState.READY, Arrays.asList("injection-lifecycle"), LifecyclePolicy.CORE_ONLY
        ));

        capRegistry.register(new CoreCapabilityRecord(
                "transform-pipeline", "转换管线", CapabilityKind.KERNEL,
                Arrays.asList("global_class_file_transformer"),
                ReadinessState.READY, Arrays.asList("injection-lifecycle"), LifecyclePolicy.CORE_ONLY
        ));

        // Phase 2 Runtime Support capabilities (existing implementations registered explicitly)
        capRegistry.register(new CoreCapabilityRecord(
                "instrumentation", "Instrumentation 能力", CapabilityKind.RUNTIME_SUPPORT,
                Arrays.asList("instrumentation_holder", "retransform", "class_scan"),
                ReadinessState.READY, Collections.emptyList(), LifecyclePolicy.CORE_ONLY
        ));

        capRegistry.register(new CoreCapabilityRecord(
                "class-analysis", "类分析", CapabilityKind.RUNTIME_SUPPORT,
                Arrays.asList("asm_analyzer", "cfr_decompiler"),
                ReadinessState.READY, Arrays.asList("instrumentation"), LifecyclePolicy.CORE_ONLY
        ));

        capRegistry.register(new CoreCapabilityRecord(
                "verification-preview", "验证预览", CapabilityKind.RUNTIME_SUPPORT,
                Arrays.asList("injection_verifier", "bytecode_previewer", "test_harness"),
                ReadinessState.READY,
                Arrays.asList("instrumentation", "injection-lifecycle", "transform-pipeline"),
                LifecyclePolicy.CORE_ONLY
        ));

        // External plugins are loaded via PluginManager, not here
    }
}
