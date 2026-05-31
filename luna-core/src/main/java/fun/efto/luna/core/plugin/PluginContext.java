package fun.efto.luna.core.plugin;

import fun.efto.luna.core.analysis.analyzer.ClassAnalyzer;
import fun.efto.luna.core.infra.RingBuffer;
import fun.efto.luna.core.analysis.decompile.Decompiler;
import fun.efto.luna.core.injection.CodeEngine;
import fun.efto.luna.core.injection.target.InjectionLocation;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjector;
import fun.efto.luna.core.probe.ProbeMessage;
import fun.efto.luna.core.injection.rule.template.RuleTemplate;

import fun.efto.luna.core.injection.port.Retransformer;

import java.util.Map;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:00
 */
public interface PluginContext {

    void registerInjectionLocation(InjectionLocation location);

    void registerInjector(InjectionLocation location, BytecodeInjector injector);

    void registerProbeHandler(ProbeHandler handler);

    void registerCodeEngine(CodeEngine engine);

    void registerRuleConverter(InjectionRuleConverter converter);

    void registerRuleConverter(InjectionLocation location, InjectionRuleConverter converter);

    void registerTemplate(RuleTemplate template);

    void registerBootstrapClass(String internalName);

    ClassAnalyzer getClassAnalyzer();

    Decompiler getDecompiler();

    LogEmitter getLogEmitter();

    RingBuffer<ProbeMessage> getLogBuffer();

    Retransformer getRetransformer();

    Map<String, String> getPluginConfig();

    void savePluginConfig(Map<String, String> config);
}
