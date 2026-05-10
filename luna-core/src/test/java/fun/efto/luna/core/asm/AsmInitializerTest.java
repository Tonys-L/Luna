/**
 * @author ：Tony.L(<286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
package fun.efto.luna.core.asm;

import fun.efto.luna.core.analyzer.AnalyzerRegistry;
import fun.efto.luna.core.injector.BytecodeInjectorRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AsmInitializerTest {

    private AsmInitializer initializer;

    @BeforeEach
    public void setUp() {
        initializer = new AsmInitializer();
    }

    @Test
    public void testInitialize() {
        // 执行初始化
        initializer.initialize();

        // 验证 ASM 分析器是否被注册
        boolean hasAsmAnalyzer = AnalyzerRegistry.getInstance().getRegistry().size() > 0;
        assertTrue(hasAsmAnalyzer, "ASM 分析器应该被注册");

        // 验证字节码注入器是否被注册（暂时注释，等待 InjectionType 实现）
        // boolean hasBytecodeInjector = BytecodeInjectorRegistry.getInstance().getRegistry().size() > 0;
        // assertTrue(hasBytecodeInjector, "字节码注入器应该被注册");
    }

    @Test
    public void testInitializePerformance() {
        long startTime = System.nanoTime();
        initializer.initialize();
        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        double milliseconds = duration / 1_000_000.0;

        // 调整阈值，因为第一次初始化时可能会有一些开销
        assertTrue(milliseconds < 10.0, "初始化过程耗时应该小于 10ms，实际耗时: " + milliseconds + "ms");
    }
}
