package fun.efto.luna.core.injection.port;

import java.util.Set;

/**
 * Retransformer（出站端口）。
 * 领域层需要"触发类 retransform"的能力，不直接依赖 Instrumentation API。
 *
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/27 18:00
 */
public interface Retransformer {

    /**
     * 触发指定类的 JVM retransform
     *
     * @param className 全限定类名
     */
    void retransform(String className);

    /**
     * 批量触发多个类的 JVM retransform
     *
     * @param classNames 全限定类名集合
     */
    default void retransformAll(Set<String> classNames) {
        if (classNames == null) return;
        for (String className : classNames) {
            retransform(className);
        }
    }

    default void retransformByPattern(String pattern) {
        retransform(pattern);
    }
}
