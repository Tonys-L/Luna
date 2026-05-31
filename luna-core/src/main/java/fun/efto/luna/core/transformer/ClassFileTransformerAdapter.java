package fun.efto.luna.core.transformer;

import fun.efto.luna.core.injection.InjectionPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since ：2025/10/4 14:17
 */
public class ClassFileTransformerAdapter implements ClassFileTransformer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassFileTransformerAdapter.class);
    private final String targetClass;
    private final List<InjectionPoint> injectionPoints;
    private final ClassTransformer classTransformer;
    private final List<InjectionResult> results = new CopyOnWriteArrayList<>();

    public ClassFileTransformerAdapter(InjectionPoint injectionPoint, ClassTransformer classTransformer) {
        this.targetClass = injectionPoint.getTarget().getTargetClass();
        this.injectionPoints = java.util.Collections.singletonList(injectionPoint);
        this.classTransformer = classTransformer;
    }

    public ClassFileTransformerAdapter(String targetClass, List<InjectionPoint> injectionPoints, ClassTransformer classTransformer) {
        this.targetClass = targetClass;
        this.injectionPoints = injectionPoints;
        this.classTransformer = classTransformer;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        String javaClassName = className != null ? className.replace('/', '.') : "";
        if (!javaClassName.equals(targetClass)) {
            return null;
        }

        LOGGER.info("Class matched, starting transformation for: {} with {} injection point(s)", javaClassName, injectionPoints.size());

        byte[] currentBytecode = classfileBuffer;
        boolean anyTransformed = false;

        for (InjectionPoint injectionPoint : injectionPoints) {
            TransformerResult result = classTransformer.transform(injectionPoint, javaClassName, currentBytecode);
            String injLocation = injectionPoint.getInjectionLocation().toString();
            String methodName = injectionPoint.getTarget().getMethodName();

            if (result.isTransformed()) {
                currentBytecode = result.getBytecode();
                anyTransformed = true;
                results.add(new InjectionResult(true, "注入成功", injLocation, methodName));
                LOGGER.info("Injection applied: {} -> {}, bytecode size={}",
                        injLocation, methodName, currentBytecode.length);
            } else {
                results.add(new InjectionResult(false, result.getMessage(), injLocation, methodName));
                LOGGER.warn("Injection failed: {} -> {}, reason: {}",
                        injLocation, methodName, result.getMessage());
            }
        }

        if (anyTransformed) {
            LOGGER.info("Transformation succeeded for: {}, final bytecode size={}", javaClassName, currentBytecode.length);
            return currentBytecode;
        }

        return null;
    }

    public List<InjectionResult> getResults() {
        return new ArrayList<>(results);
    }
}
