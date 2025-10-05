package fun.efto.luna.core.analyzer;

/**
 * 类分析器接口
 * 定义了通用的类分析操作，不依赖于任何具体的字节码操作库
 *
 * @author Tony.L(286269159 @ qq.com)
 * @since 2025/10/4
 */
public interface ClassAnalyzer {
    /**
     * 分析类字节码
     *
     * @param bytecode 类字节码
     * @return 类分析结果
     */
    ClassAnalysisResult analyze(byte[] bytecode);
}