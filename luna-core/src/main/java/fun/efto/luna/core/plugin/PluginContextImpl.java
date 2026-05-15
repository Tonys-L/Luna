package fun.efto.luna.core.plugin;

import fun.efto.luna.core.analyzer.ClassAnalyzer;
import fun.efto.luna.core.buffer.RingBuffer;
import fun.efto.luna.core.bytecode.BytecodeAssembler;
import fun.efto.luna.core.bytecode.BytecodeAssemblerRegistry;
import fun.efto.luna.core.config.ConfigManager;
import fun.efto.luna.core.decompile.Decompiler;
import fun.efto.luna.core.injection.target.type.InjectionType;
import fun.efto.luna.core.injection.code.type.CodeType;
import fun.efto.luna.core.injector.BytecodeInjector;
import fun.efto.luna.core.injector.BytecodeInjectorRegistry;

import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.Map;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since : 2026/05/11 22:00
 */
class PluginContextImpl implements PluginContext {
    private final PluginRegistrationRecord record;
    private final LogEmitter logEmitter;
    private final RingBuffer<String> logBuffer;
    private final Instrumentation instrumentation;
    private final ClassAnalyzer classAnalyzer;
    private final Decompiler decompiler;

    PluginContextImpl(PluginRegistrationRecord record, LogEmitter logEmitter,
                      RingBuffer<String> logBuffer, Instrumentation instrumentation,
                      ClassAnalyzer classAnalyzer, Decompiler decompiler) {
        this.record = record;
        this.logEmitter = logEmitter;
        this.logBuffer = logBuffer;
        this.instrumentation = instrumentation;
        this.classAnalyzer = classAnalyzer;
        this.decompiler = decompiler;
    }

    @Override
    public void registerInjectionType(InjectionType type) {
        InjectionTypeRegistry.register(type);
        record.injectionTypes.add(type);
    }

    @Override
    public void registerInjector(InjectionType type, BytecodeInjector injector) {
        BytecodeInjectorRegistry.getInstance().register(type, injector);
        record.injectors.put(type, injector);
    }

    @Override
    public void registerAssembler(CodeType type, BytecodeAssembler assembler) {
        BytecodeAssemblerRegistry.getInstance().register(type, assembler);
        record.assemblers.put(type, assembler);
    }

    @Override
    public void registerExpressionHandler(ExpressionHandler handler) {
        ExpressionHandlerRegistry.register(handler);
        record.expressionHandlers.add(handler);
    }

    @Override
    public void registerRuleConverter(InjectionRuleConverter converter) {
        throw new IllegalArgumentException(
                "registerRuleConverter(InjectionRuleConverter) without InjectionType is not supported. " +
                        "Use registerRuleConverter(InjectionType, InjectionRuleConverter) instead.");
    }

    @Override
    public void registerRuleConverter(InjectionType type, InjectionRuleConverter converter) {
        RuleConverterRegistry.register(type, converter);
        record.ruleConverters.put(type, converter);
    }

    @Override
    public ClassAnalyzer getClassAnalyzer() {
        return classAnalyzer;
    }

    @Override
    public Decompiler getDecompiler() {
        return decompiler;
    }

    @Override
    public LogEmitter getLogEmitter() {
        return logEmitter;
    }

    @Override
    public RingBuffer<String> getLogBuffer() {
        return logBuffer;
    }

    @Override
    public Instrumentation getInstrumentation() {
        return instrumentation;
    }

    @Override
    public Map<String, String> getPluginConfig() {
        return Collections.unmodifiableMap(ConfigManager.getPluginConfig(record.getPluginId()));
    }

    @Override
    public void savePluginConfig(Map<String, String> config) {
        ConfigManager.savePluginConfig(record.getPluginId(), config);
    }
}
