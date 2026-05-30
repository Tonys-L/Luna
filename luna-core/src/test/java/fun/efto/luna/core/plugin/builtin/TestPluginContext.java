package fun.efto.luna.core.plugin.builtin;

import fun.efto.luna.core.analyzer.ClassAnalyzer;
import fun.efto.luna.core.common.RingBuffer;
import fun.efto.luna.core.bytecode.BytecodeAssembler;
import fun.efto.luna.core.decompile.Decompiler;
import fun.efto.luna.core.injection.CodeCompilerStrategy;
import fun.efto.luna.core.injection.code.CodeType;
import fun.efto.luna.core.injection.target.InjectionType;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjector;
import fun.efto.luna.core.plugin.*;
import fun.efto.luna.core.probe.ProbeMessage;
import fun.efto.luna.core.rule.template.RuleTemplate;

import fun.efto.luna.core.injection.port.Retransformer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:00
 */
public class TestPluginContext implements PluginContext {

    private final List<InjectionType> injectionTypes = new ArrayList<>();
    private final Map<InjectionType, BytecodeInjector> injectors = new HashMap<>();
    private final Map<CodeType, BytecodeAssembler> assemblers = new HashMap<>();
    private final List<ExpressionHandler> expressionHandlers = new ArrayList<>();
    private final List<InjectionRuleConverter> ruleConverters = new ArrayList<>();
    private final Map<InjectionType, InjectionRuleConverter> typedRuleConverters = new HashMap<>();
    private final List<RuleTemplate> templates = new ArrayList<>();
    private final List<CodeCompilerStrategy> codeCompilerStrategies = new ArrayList<>();
    private final List<String> bootstrapClasses = new ArrayList<>();

    @Override
    public void registerInjectionType(InjectionType type) {
        injectionTypes.add(type);
    }

    @Override
    public void registerInjector(InjectionType type, BytecodeInjector injector) {
        injectors.put(type, injector);
    }

    @Override
    public void registerAssembler(CodeType type, BytecodeAssembler assembler) {
        assemblers.put(type, assembler);
    }

    @Override
    public void registerExpressionHandler(ExpressionHandler handler) {
        expressionHandlers.add(handler);
    }

    @Override
    public void registerRuleConverter(InjectionRuleConverter converter) {
        ruleConverters.add(converter);
    }

    @Override
    public void registerRuleConverter(InjectionType type, InjectionRuleConverter converter) {
        typedRuleConverters.put(type, converter);
    }

    @Override
    public void registerTemplate(RuleTemplate template) {
        templates.add(template);
    }

    @Override
    public void registerCodeCompilerStrategy(CodeCompilerStrategy strategy) {
        codeCompilerStrategies.add(strategy);
    }

    @Override
    public void registerBootstrapClass(String internalName) {
        bootstrapClasses.add(internalName);
    }

    @Override
    public ClassAnalyzer getClassAnalyzer() { return null; }

    @Override
    public Decompiler getDecompiler() { return null; }

    @Override
    public LogEmitter getLogEmitter() { return null; }

    @Override
    public RingBuffer<ProbeMessage> getLogBuffer() { return null; }

    @Override
    public Retransformer getRetransformer() { return className -> {}; }

    @Override
    public Map<String, String> getPluginConfig() { return new HashMap<>(); }

    @Override
    public void savePluginConfig(Map<String, String> config) {}

    public List<InjectionType> getInjectionTypes() { return injectionTypes; }
    public Map<InjectionType, BytecodeInjector> getInjectors() { return injectors; }
    public Map<CodeType, BytecodeAssembler> getAssemblers() { return assemblers; }
    public List<ExpressionHandler> getExpressionHandlers() { return expressionHandlers; }
    public List<InjectionRuleConverter> getRuleConverters() { return ruleConverters; }
    public Map<InjectionType, InjectionRuleConverter> getTypedRuleConverters() { return typedRuleConverters; }
    public List<RuleTemplate> getTemplates() { return templates; }
}
