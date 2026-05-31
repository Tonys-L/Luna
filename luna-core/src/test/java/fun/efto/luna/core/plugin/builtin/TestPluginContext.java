package fun.efto.luna.core.plugin.builtin;

import fun.efto.luna.core.analysis.analyzer.ClassAnalyzer;
import fun.efto.luna.core.infra.RingBuffer;
import fun.efto.luna.core.analysis.decompile.Decompiler;
import fun.efto.luna.core.injection.CodeEngine;
import fun.efto.luna.core.injection.target.InjectionLocation;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjector;
import fun.efto.luna.core.plugin.*;
import fun.efto.luna.core.probe.ProbeMessage;
import fun.efto.luna.core.injection.rule.template.RuleTemplate;

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

    private final List<InjectionLocation> injectionLocations = new ArrayList<>();
    private final Map<InjectionLocation, BytecodeInjector> injectors = new HashMap<>();
    private final List<InjectionRuleConverter> ruleConverters = new ArrayList<>();
    private final Map<InjectionLocation, InjectionRuleConverter> typedRuleConverters = new HashMap<>();
    private final List<RuleTemplate> templates = new ArrayList<>();
    private final List<CodeEngine> codeEngines = new ArrayList<>();
    private final List<ProbeHandler> probeHandlers = new ArrayList<>();
    private final List<String> bootstrapClasses = new ArrayList<>();

    @Override
    public void registerInjectionLocation(InjectionLocation location) {
        injectionLocations.add(location);
    }

    @Override
    public void registerInjector(InjectionLocation location, BytecodeInjector injector) {
        injectors.put(location, injector);
    }

    @Override
    public void registerProbeHandler(ProbeHandler handler) {
        probeHandlers.add(handler);
    }

    @Override
    public void registerCodeEngine(CodeEngine engine) {
        codeEngines.add(engine);
    }

    @Override
    public void registerRuleConverter(InjectionRuleConverter converter) {
        ruleConverters.add(converter);
    }

    @Override
    public void registerRuleConverter(InjectionLocation location, InjectionRuleConverter converter) {
        typedRuleConverters.put(location, converter);
    }

    @Override
    public void registerTemplate(RuleTemplate template) {
        templates.add(template);
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

    public List<InjectionLocation> getInjectionLocations() { return injectionLocations; }
    public Map<InjectionLocation, BytecodeInjector> getInjectors() { return injectors; }
    public List<InjectionRuleConverter> getRuleConverters() { return ruleConverters; }
    public Map<InjectionLocation, InjectionRuleConverter> getTypedRuleConverters() { return typedRuleConverters; }
    public List<RuleTemplate> getTemplates() { return templates; }
    public List<CodeEngine> getCodeEngines() { return codeEngines; }
    public List<ProbeHandler> getProbeHandlers() { return probeHandlers; }
}
