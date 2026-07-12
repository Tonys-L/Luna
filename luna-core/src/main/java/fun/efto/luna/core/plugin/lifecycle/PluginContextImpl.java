package fun.efto.luna.core.plugin.lifecycle;

import fun.efto.luna.core.analysis.analyzer.ClassAnalyzer;
import fun.efto.luna.core.infra.RingBuffer;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjector;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjectorRegistry;
import fun.efto.luna.core.infra.InstrumentationHolder;
import fun.efto.luna.core.infra.config.ConfigManager;
import fun.efto.luna.core.analysis.decompile.Decompiler;
import fun.efto.luna.core.injection.CodeEngine;
import fun.efto.luna.core.injection.CodeEngineRegistry;
import fun.efto.luna.core.injection.InjectRequest;
import fun.efto.luna.core.injection.InjectionService;
import fun.efto.luna.core.injection.target.InjectionLocation;
import fun.efto.luna.core.plugin.*;
import fun.efto.luna.core.probe.ProbeMessage;
import fun.efto.luna.core.plugin.registry.InjectionTypeRegistry;
import fun.efto.luna.core.plugin.registry.ProbeHandlerRegistry;
import fun.efto.luna.core.plugin.InjectionLocationUIDescriptor;

import fun.efto.luna.core.injection.port.BytecodeLoader;
import fun.efto.luna.core.injection.port.Retransformer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    private final BytecodeLoader bytecodeLoader;
    private final InjectionService injectionService;

    PluginContextImpl(PluginRegistrationRecord record, LogEmitter logEmitter,
                      RingBuffer<ProbeMessage> logBuffer, Retransformer retransformer,
                      ClassAnalyzer classAnalyzer, Decompiler decompiler,
                      BytecodeLoader bytecodeLoader, InjectionService injectionService) {
        this.record = record;
        this.logEmitter = logEmitter;
        this.logBuffer = logBuffer;
        this.retransformer = retransformer;
        this.classAnalyzer = classAnalyzer;
        this.decompiler = decompiler;
        this.bytecodeLoader = bytecodeLoader;
        this.injectionService = injectionService;
    }

    @Override
    public void registerInjectionLocation(InjectionLocation location) {
        InjectionTypeRegistry.getInstance().register(location);
        record.addInjectionLocation(location);
    }

    @Override
    public void registerInjectionLocation(InjectionLocation location, InjectionLocationUIDescriptor uiDescriptor) {
        InjectionTypeRegistry.getInstance().register(location, uiDescriptor);
        record.addInjectionLocation(location);
    }

    @Override
    public void registerInjector(InjectionLocation location, BytecodeInjector injector) {
        BytecodeInjectorRegistry.getInstance().register(location, injector);
        record.addInjector(location, injector);
    }

    @Override
    public void registerProbeHandler(ProbeHandler handler) {
        ProbeHandlerRegistry.getInstance().register(handler, this);
        record.addProbeHandler(handler);
    }

    @Override
    public void registerCodeEngine(CodeEngine engine) {
        CodeEngineRegistry.getInstance().register(engine);
        record.addCodeEngine(engine);
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

    @Override
    public byte[] getClassBytes(String className) {
        try {
            return bytecodeLoader.loadBytecode(className);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Set<String> getLoadedClassNames() {
        Class<?>[] loadedClasses = InstrumentationHolder.getAllLoadedClasses();
        Set<String> names = new HashSet<>(loadedClasses.length);
        for (Class<?> clazz : loadedClasses) {
            names.add(clazz.getName());
        }
        return Collections.unmodifiableSet(names);
    }

    @Override
    public String inject(InjectRequest request) {
        InjectionService.InjectResult result = injectionService.inject(request);
        if (result.isSuccess()) {
            return result.getInjectionId();
        }
        throw new RuntimeException("Inject failed: " + result.getError());
    }
}
