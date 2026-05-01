package fun.efto.luna.core.asm.analyzer;

import fun.efto.luna.core.analyzer.ClassAnalyzer;
import fun.efto.luna.core.analyzer.ClassAnalysisResult;

import java.util.Collections;

/**
 * ASM 类分析器
 * @author ：Tony.L(<286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
public class AsmClassAnalyzer implements ClassAnalyzer {

    @Override
    public ClassAnalysisResult analyze(byte[] classBytes) {
        // 这里实现 ASM 分析逻辑
        // 暂时返回一个空的分析结果
        return new ClassAnalysisResult(
                "",
                Collections.emptyList(),
                Collections.emptyList(),
                "",
                Collections.emptyList(),
                0
        );
    }
}

