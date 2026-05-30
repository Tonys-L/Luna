/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/17 19:15
 */
package fun.efto.luna.core.transformer;

import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.InjectionQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.List;

/**
 * 全局唯一常驻类字节码转换器：在类初次装载或 retransform 时执行注入匹配与织入。
 * 通过 InjectionQuery 接口查询注入点，不直接依赖 InjectionManager。
 */
public class GlobalClassFileTransformer implements ClassFileTransformer {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalClassFileTransformer.class);
    private final ClassTransformer classTransformer = new DefaultClassTransformer();
    private final InjectionQuery injectionQuery;

    public GlobalClassFileTransformer(InjectionQuery injectionQuery) {
        this.injectionQuery = injectionQuery;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (className == null) {
            return null;
        }

        String normalizedClassName = className.replace('/', '.');

        // 1. O(1) 无锁只读查询激活的注入点列表
        List<InjectionPoint> points = injectionQuery.getActivePointsForClass(normalizedClassName);
        if (points.isEmpty()) {
            // 语义等价性保证：无任何激活注入点时直接向 JVM 返回 null（无额外转换开销）
            return null;
        }

        LOGGER.info("Applying {} active injection point(s) to class: {}", points.size(), normalizedClassName);

        byte[] currentBytecode = classfileBuffer;
        boolean anyTransformed = false;

        // 2. 逐个执行 ASM 字节码插桩
        for (InjectionPoint point : points) {
            try {
                TransformerResult result = classTransformer.transform(point, normalizedClassName, currentBytecode);
                if (result.isTransformed()) {
                    currentBytecode = result.getBytecode();
                    anyTransformed = true;
                    LOGGER.info("Successfully applied injection point [id={}, type={}, method={}] to class: {}",
                            point.getId(), point.getInjectionType(), point.getTarget().getMethodName(), normalizedClassName);
                } else {
                    LOGGER.warn("Failed to apply injection point [id={}, type={}, method={}] to class: {}, reason: {}",
                            point.getId(), point.getInjectionType(), point.getTarget().getMethodName(), normalizedClassName, result.getMessage());
                }
            } catch (Exception e) {
                LOGGER.error("Error occurred while applying injection point [id={}] to class: {}", point.getId(), normalizedClassName, e);
            }
        }

        return anyTransformed ? currentBytecode : null;
    }
}
