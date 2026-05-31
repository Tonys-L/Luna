package fun.efto.luna.core.transformer;

import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.InjectionQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.List;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/17 19:15
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

        List<InjectionPoint> points = injectionQuery.getActivePointsForClass(normalizedClassName);
        if (points.isEmpty()) {
            return null;
        }

        LOGGER.info("Applying {} active injection point(s) to class: {}", points.size(), normalizedClassName);

        byte[] currentBytecode = classfileBuffer;
        boolean anyTransformed = false;

        for (InjectionPoint point : points) {
            try {
                TransformerResult result = classTransformer.transform(point, normalizedClassName, currentBytecode);
                if (result.isTransformed()) {
                    currentBytecode = result.getBytecode();
                    anyTransformed = true;
                    LOGGER.info("Successfully applied injection point [id={}, location={}, method={}] to class: {}",
                            point.getId(), point.getInjectionLocation(), point.getTarget().getMethodName(), normalizedClassName);
                } else {
                    LOGGER.warn("Failed to apply injection point [id={}, location={}, method={}] to class: {}, reason: {}",
                            point.getId(), point.getInjectionLocation(), point.getTarget().getMethodName(), normalizedClassName, result.getMessage());
                }
            } catch (Exception e) {
                LOGGER.error("Error occurred while applying injection point [id={}] to class: {}", point.getId(), normalizedClassName, e);
            }
        }

        return anyTransformed ? currentBytecode : null;
    }
}
