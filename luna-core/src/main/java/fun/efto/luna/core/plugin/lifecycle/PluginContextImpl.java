package fun.efto.luna.core.plugin.lifecycle;

import fun.efto.luna.core.analyzer.ClassAnalyzer;
import fun.efto.luna.core.common.RingBuffer;
import fun.efto.luna.core.bytecode.BytecodeAssembler;
import fun.efto.luna.core.bytecode.asm.assembler.BytecodeAssemblerRegistry;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjector;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjectorRegistry;
import fun.efto.luna.core.config.ConfigManager;
import fun.efto.luna.core.decompile.Decompiler;
import fun.efto.luna.core.injection.CodeCompiler;
import fun.efto.luna.core.injection.CodeCompilerStrategy;
import fun.efto.luna.core.injection.target.InjectionType;
import fun.efto.luna.core.injection.code.CodeType;
import fun.efto.luna.core.plugin.*;
import fun.efto.luna.core.probe.ProbeMessage;
import fun.efto.luna.core.plugin.registry.ExpressionHandlerRegistry;
import fun.efto.luna.core.plugin.registry.InjectionTypeRegistry;
import fun.efto.luna.core.plugin.registry.RuleConverterRegistry;
import fun.efto.luna.core.rule.template.RuleTemplate;
import fun.efto.luna.core.rule.template.TemplateRegistry;

import fun.efto.luna.core.injection.port.Retransformer;

import java.util.Collections;
import java.util.Map;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:00
 */
class PluginContextImpl implements PluginContext {
    private final PluginRegistrationRecord record;
    private final LogEmitter logEmitter;
    private final RingBuffer<ProbeMessage> logBuffer;
    private final Retransformer retransformer;
    private final ClassAnalyzer classAnalyzer;
    private final Decompiler decompiler;

    PluginContextImpl(PluginRegistrationRecord record, LogEmitter logEmitter,
                      RingBuffer<ProbeMessage> logBuffer, Retransformer retransformer,
                      ClassAnalyzer classAnalyzer, Decompiler decompiler) {
        this.record = record;
        this.logEmitter = logEmitter;
        this.logBuffer = logBuffer;
        this.retransformer = retransformer;
        this.classAnalyzer = classAnalyzer;
        this.decompiler = decompiler;
    }

    @Override
    public void registerInjectionType(InjectionType type) {
        InjectionTypeRegistry.getInstance().register(type);
        record.addInjectionType(type);
    }

    @Override
    public void registerInjector(InjectionType type, BytecodeInjector injector) {
        BytecodeInjectorRegistry.getInstance().register(type, injector);
        record.addInjector(type, injector);
    }

    @Override
    public void registerAssembler(CodeType type, BytecodeAssembler assembler) {
        BytecodeAssemblerRegistry.getInstance().register(type, assembler);
        record.addAssembler(type, assembler);
    }

    @Override
    public void registerExpressionHandler(ExpressionHandler handler) {
        ExpressionHandlerRegistry.getInstance().register(handler);
        record.addExpressionHandler(handler);
    }

    @Override
    public void registerRuleConverter(InjectionRuleConverter converter) {
        // Default implementation - no type-specific registration
    }

    @Override
    public void registerRuleConverter(InjectionType type, InjectionRuleConverter converter) {
        RuleConverterRegistry.getInstance().register(type, converter);
        record.addRuleConverter(type, converter);
    }

    @Override
    public void registerTemplate(RuleTemplate template) {
        TemplateRegistry.getInstance().register(template);
        record.addTemplate(template);
    }

    @Override
    public void registerCodeCompilerStrategy(CodeCompilerStrategy strategy) {
        CodeCompiler.register(strategy);
        record.addCodeCompilerStrategy(strategy);
    }

    @Override
    public void registerBootstrapClass(String internalName) {
        record.addBootstrapClass(internalName);
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
    public RingBuffer<ProbeMessage> getLogBuffer() {
        return logBuffer;
    }

    @Override
    public Retransformer getRetransformer() {
        return retransformer;
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
