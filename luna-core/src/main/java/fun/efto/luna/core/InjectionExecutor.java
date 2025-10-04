package fun.efto.luna.core;

import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.transformer.ClassFileTransformerAdapter;
import fun.efto.luna.core.transformer.ClassTransformer;
import fun.efto.luna.core.transformer.DefaultClassTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 14:54
 */
public class InjectionExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(InjectionExecutor.class);
    private static volatile InjectionExecutor instance;
    private final InstrumentationManager instManager;

    private InjectionExecutor(Instrumentation instrumentation) {
        this.instManager = InstrumentationManager.init(instrumentation);
    }

    private InjectionExecutor() {
        this.instManager = InstrumentationManager.getInstance();
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
            throw new IllegalStateException("未初始化");
        }
        return instance;
    }

    public void execute(InjectionPoint injectionPoint) {
        ClassTransformer classTransformer = new DefaultClassTransformer();
        ClassFileTransformerAdapter adapter = new ClassFileTransformerAdapter(
                injectionPoint, classTransformer
        );

        instManager.addTransformer(adapter, true);

        try {
            Class<?>[] allLoadedClasses = instManager.getAllLoadedClasses();
            for (Class<?> clazz : allLoadedClasses) {
                if (clazz.getName().equals(injectionPoint.getTarget().getTargetClass())) {
                    try {
                        LOGGER.info("Retransforming class: {}", clazz.getName());
                        instManager.retransformClasses(clazz);
                    } catch (Exception e) {
                        LOGGER.error("Failed to retransform class {}", clazz.getName(), e);
                    }
                }
            }
        } finally {
            instManager.removeTransformer(adapter);
        }
    }
}
