package fun.efto.luna.core.plugin;

import fun.efto.luna.core.analysis.analyzer.ClassAnalyzer;
import fun.efto.luna.core.infra.RingBuffer;
import fun.efto.luna.core.bytecode.BytecodeAssembler;
import fun.efto.luna.core.analysis.decompile.Decompiler;
import fun.efto.luna.core.injection.code.CodeType;
import fun.efto.luna.core.injection.CodeCompilerStrategy;
import fun.efto.luna.core.injection.target.InjectionType;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjector;
import fun.efto.luna.core.probe.ProbeMessage;
import fun.efto.luna.core.rule.template.RuleTemplate;

import fun.efto.luna.core.injection.port.Retransformer;

import java.util.Map;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:00
 */
public interface PluginContext {

    void registerInjectionType(InjectionType type);

    void registerInjector(InjectionType type, BytecodeInjector injector);

    void registerAssembler(CodeType type, BytecodeAssembler assembler);

    void registerExpressionHandler(ExpressionHandler handler);

    void registerRuleConverter(InjectionRuleConverter converter);

    void registerRuleConverter(InjectionType type, InjectionRuleConverter converter);

    void registerTemplate(RuleTemplate template);

    void registerCodeCompilerStrategy(CodeCompilerStrategy strategy);

    void registerBootstrapClass(String internalName);

    ClassAnalyzer getClassAnalyzer();

    Decompiler getDecompiler();

    LogEmitter getLogEmitter();

    RingBuffer<ProbeMessage> getLogBuffer();

    Retransformer getRetransformer();

    Map<String, String> getPluginConfig();

    void savePluginConfig(Map<String, String> config);
}
