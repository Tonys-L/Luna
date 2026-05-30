package fun.efto.luna.core.bytecode.asm;

import fun.efto.luna.core.analyzer.AnalyzerRegistry;
import fun.efto.luna.core.analyzer.AnalyzerType;
import fun.efto.luna.core.bytecode.asm.analyzer.AsmClassAnalyzer;
import fun.efto.luna.core.init.Initializer;

/**
 * ASM 初始化器
 * @author ：Tony.L(<286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
public class AsmInitializer implements Initializer {
    @Override
    public void initialize() {
        // 注册 ASM 分析器
        AnalyzerRegistry.getInstance().register(
                new AnalyzerType("asm", "ASM 类分析器"),
                new AsmClassAnalyzer()
        );

        // 字节码注入器已在 BytecodeInjectorRegistry 构造时注册
        // 这里只负责 ASM 相关的初始化
    }
}
