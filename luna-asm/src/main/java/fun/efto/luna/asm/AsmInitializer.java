package fun.efto.luna.asm;

import fun.efto.luna.asm.analyzer.AsmClassAnalyzer;
import fun.efto.luna.asm.assmebler.ExpressionBytecodeAssembler;
import fun.efto.luna.asm.injector.*;
import fun.efto.luna.core.analyzer.AnalyzerRegistry;
import fun.efto.luna.core.analyzer.AnalyzerType;
import fun.efto.luna.core.bytecode.BytecodeAssemblerRegistry;
import fun.efto.luna.core.init.Initializer;
import fun.efto.luna.core.injection.code.type.CodeType;
import fun.efto.luna.core.injection.code.type.ExpressionCodeType;
import fun.efto.luna.core.injection.target.type.InjectionType;
import fun.efto.luna.core.injection.target.type.LineNumberInjectionType;
import fun.efto.luna.core.injection.target.type.MethodInjectionType;
import fun.efto.luna.core.injector.BytecodeInjectorRegistry;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 17:44
 */
public class AsmInitializer implements Initializer {
    @Override
    public void initialize() {
        BytecodeInjectorRegistry injectorRegistry = BytecodeInjectorRegistry.getInstance();
        injectorRegistry.register(InjectionType.valueOf(MethodInjectionType.ENTER_METHOD), new EnterMethodInjector());
        injectorRegistry.register(InjectionType.valueOf(MethodInjectionType.EXIT_METHOD), new ExitMethodInjector());
        injectorRegistry.register(InjectionType.valueOf(MethodInjectionType.AROUND_METHOD), new AroundMethodInjector());
        injectorRegistry.register(InjectionType.valueOf(LineNumberInjectionType.BEFORE_LINE), new BeforeLineInjector());
        injectorRegistry.register(InjectionType.valueOf(LineNumberInjectionType.AFTER_LINE), new AfterLineInjector());

        BytecodeAssemblerRegistry assemblerRegistry = BytecodeAssemblerRegistry.getInstance();
        assemblerRegistry.register(CodeType.valueOf(ExpressionCodeType.EXPRESSION), new ExpressionBytecodeAssembler());

        AnalyzerRegistry analyzerRegistry = AnalyzerRegistry.getInstance();
        analyzerRegistry.register(new AnalyzerType("ASM", "ASM字节码分析器").register(), new AsmClassAnalyzer());
    }
}
