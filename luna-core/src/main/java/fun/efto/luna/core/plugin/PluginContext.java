package fun.efto.luna.core.plugin;

import fun.efto.luna.core.analyzer.ClassAnalyzer;
import fun.efto.luna.core.buffer.RingBuffer;
import fun.efto.luna.core.bytecode.BytecodeAssembler;
import fun.efto.luna.core.decompile.Decompiler;
import fun.efto.luna.core.injection.code.type.CodeType;
import fun.efto.luna.core.injection.target.type.InjectionType;
import fun.efto.luna.core.injector.BytecodeInjector;

import java.lang.instrument.Instrumentation;
import java.util.Map;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
public interface PluginContext {

    void registerInjectionType(InjectionType type);

    void registerInjector(InjectionType type, BytecodeInjector injector);

    void registerAssembler(CodeType type, BytecodeAssembler assembler);

    void registerExpressionHandler(ExpressionHandler handler);

    void registerRuleConverter(InjectionRuleConverter converter);

    void registerRuleConverter(InjectionType type, InjectionRuleConverter converter);

    ClassAnalyzer getClassAnalyzer();

    Decompiler getDecompiler();

    LogEmitter getLogEmitter();

    RingBuffer<String> getLogBuffer();

    Instrumentation getInstrumentation();

    Map<String, String> getPluginConfig();

    void savePluginConfig(Map<String, String> config);
}
