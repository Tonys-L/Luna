package fun.efto.luna.core.analyzer;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/23 23:22
 */
public abstract class AbstractAnalyzer implements ClassAnalyzer {
    @Override
    public ClassAnalysisResult analyze(byte[] bytecode) {
        ClassAnalysisResult result = doAnalyze(bytecode);
        return new ClassAnalysisResultWithReadableAccess(
                result.getClassName(),
                result.getFields(),
                result.getMethods(),
                result.getSuperClass(),
                result.getInterfaces(),
                result.getAccessFlags()
        );
    }

    protected abstract ClassAnalysisResult doAnalyze(byte[] bytecode);
}
