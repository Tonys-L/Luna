package fun.efto.luna.core;

import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.InjectionPointRegistry;
import fun.efto.luna.core.transformer.ClassFileTransformerAdapter;
import fun.efto.luna.core.transformer.ClassTransformer;
import fun.efto.luna.core.transformer.DefaultClassTransformer;
import fun.efto.luna.core.transformer.InjectionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 14:54
 */
public class InjectionExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(InjectionExecutor.class);
    private static volatile InjectionExecutor instance;

    private InjectionExecutor(Instrumentation instrumentation) {
        InstrumentationHolder.init(instrumentation);
    }

    public static InjectionExecutor init(Instrumentation instrumentation) {
        if (instance == null) {
            synchronized (InjectionExecutor.class) {
                if (instance == null) {
                    instance = new InjectionExecutor(instrumentation);
                }
            }
        }
        return instance;
    }

    public static InjectionExecutor getInstance() {
        if (instance == null) {
            throw new IllegalStateException("InjectionExecutor not initialized");
        }
        return instance;
    }

    public List<InjectionResult> execute(InjectionPoint injectionPoint) {
        String targetClass = injectionPoint.getTarget().getTargetClass();

        InjectionPointRegistry.getInstance().register(injectionPoint);

        List<InjectionPoint> allInjectionPoints = InjectionPointRegistry.getInstance().getInjectionPoints(targetClass);
        LOGGER.info("Retransforming class: {} with {} injection point(s)", targetClass, allInjectionPoints.size());

        ClassTransformer classTransformer = new DefaultClassTransformer();
        ClassFileTransformerAdapter adapter = new ClassFileTransformerAdapter(
                targetClass, allInjectionPoints, classTransformer
        );

        InstrumentationHolder.addTransformer(adapter, true);

        try {
            Class<?>[] allLoadedClasses = InstrumentationHolder.getAllLoadedClasses();
            boolean found = false;
            for (Class<?> clazz : allLoadedClasses) {
                if (clazz.getName().equals(targetClass)) {
                    found = true;
                    LOGGER.info("Found class: {} loader={}", clazz.getName(), clazz.getClassLoader());
                    try {
                        InstrumentationHolder.retransformClasses(clazz);
                        LOGGER.info("Retransform completed for: {}", clazz.getName());
                    } catch (Exception e) {
                        LOGGER.error("Failed to retransform class {}", clazz.getName(), e);
                        List<InjectionResult> errorResults = new ArrayList<>();
                        for (InjectionPoint ip : allInjectionPoints) {
                            errorResults.add(new InjectionResult(false, "retransform失败: " + e.getMessage(),
                                    ip.getInjectionType().toString(), ip.getTarget().getMethodName()));
                        }
                        return errorResults;
                    }
                }
            }
            if (!found) {
                LOGGER.warn("Class NOT found in loaded classes: {}", targetClass);
                List<InjectionResult> notFoundResults = new ArrayList<>();
                for (InjectionPoint ip : allInjectionPoints) {
                    notFoundResults.add(new InjectionResult(false, "类未加载: " + targetClass,
                            ip.getInjectionType().toString(), ip.getTarget().getMethodName()));
                }
                return notFoundResults;
            }
        } finally {
            InstrumentationHolder.removeTransformer(adapter);
        }

        return adapter.getResults();
    }
}
