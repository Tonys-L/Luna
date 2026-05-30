package fun.efto.luna.core.analyzer;

import java.util.List;
import java.util.Map;

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

    /**
     * 获取类中每个方法的行号表
     *
     * @param classBytes 类字节码
     * @return 方法签名 → 行号列表
     */
    Map<String, List<Integer>> getLineNumbers(byte[] classBytes);

    /**
     * 获取指定方法在指定行号可见的局部变量
     *
     * @param classBytes      类字节码
     * @param methodName      方法名
     * @param methodDescriptor 方法描述符（可为 null 或空表示不筛选）
     * @param lineNumber      目标行号
     * @return 可见的局部变量列表
     * @throws RuntimeException 如果方法未找到
     */
    List<ClassAnalysisResult.LocalVariableInfo> getVisibleLocalVariables(
            byte[] classBytes, String methodName, String methodDescriptor, int lineNumber);
}
