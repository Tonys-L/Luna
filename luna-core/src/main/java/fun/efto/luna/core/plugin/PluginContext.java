package fun.efto.luna.core.plugin;

import fun.efto.luna.core.analysis.analyzer.ClassAnalyzer;
import fun.efto.luna.core.probe.RingBuffer;
import fun.efto.luna.core.analysis.decompile.Decompiler;
import fun.efto.luna.core.injection.CodeEngine;
import fun.efto.luna.core.injection.InjectRequest;
import fun.efto.luna.core.injection.target.InjectionLocation;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjector;
import fun.efto.luna.core.plugin.InjectionLocationUIDescriptor;
import fun.efto.luna.core.probe.ProbeMessage;

import fun.efto.luna.core.injection.port.Retransformer;

import java.util.Map;
import java.util.Set;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:00
 */
public interface PluginContext {

    void registerInjectionLocation(InjectionLocation location);

    void registerInjectionLocation(InjectionLocation location, InjectionLocationUIDescriptor uiDescriptor);

    void registerInjector(InjectionLocation location, BytecodeInjector injector);

    void registerProbeHandler(ProbeHandler handler);

    void registerCodeEngine(CodeEngine engine);

    void registerBootstrapClass(String internalName);

    ClassAnalyzer getClassAnalyzer();

    Decompiler getDecompiler();

    LogEmitter getLogEmitter();

    RingBuffer<ProbeMessage> getLogBuffer();

    Retransformer getRetransformer();

    Map<String, String> getPluginConfig();

    void savePluginConfig(Map<String, String> config);

    byte[] getClassBytes(String className);

    Set<String> getLoadedClassNames();

    String inject(InjectRequest request);
}
