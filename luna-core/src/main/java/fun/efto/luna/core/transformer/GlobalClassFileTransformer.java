package fun.efto.luna.core.transformer;

import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.InjectionQuery;
import fun.efto.luna.core.injection.InjectionRegistry;
import fun.efto.luna.core.injection.port.BytecodeLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.List;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since : 2026/05/17 19:15
 */
public class GlobalClassFileTransformer implements ClassFileTransformer {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalClassFileTransformer.class);
    private final ClassTransformer classTransformer = new DefaultClassTransformer();
    private final InjectionRegistry injectionRegistry;
    private final BytecodeLoader bytecodeLoader;

    public GlobalClassFileTransformer(InjectionRegistry injectionRegistry, BytecodeLoader bytecodeLoader) {
        this.injectionRegistry = injectionRegistry;
        this.bytecodeLoader = bytecodeLoader;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (className == null) {
            return null;
        }

        String normalizedClassName = className.replace('/', '.');

        List<InjectionPoint> points = injectionRegistry.getActivePointsForClass(normalizedClassName);
        if (points.isEmpty()) {
            return null;
        }

        LOGGER.info("Applying {} active injection point(s) to class: {}", points.size(), normalizedClassName);

        // INV-001: Always start from original bytecode to prevent stacking injection.
        // During retransform, classfileBuffer may contain previously injected bytecode,
        // which would cause new injection points to be applied on top of existing ones.
        byte[] currentBytecode = loadOriginalBytecode(normalizedClassName, classfileBuffer);
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

    /**
     * Load original bytecode from BytecodeLoader to ensure retransform always starts from
     * the unmodified class file. Falls back to classfileBuffer if loading fails
     * (e.g., dynamically generated classes not on disk).
     */
    private byte[] loadOriginalBytecode(String className, byte[] classfileBuffer) {
        if (bytecodeLoader != null) {
            try {
                byte[] original = bytecodeLoader.loadBytecode(className);
                if (original != null && original.length > 0) {
                    LOGGER.info("[DIAG] Loaded original bytecode for {} ({} bytes, classfileBuffer={} bytes)",
                        className, original.length, classfileBuffer.length);
                    return original;
                }
                LOGGER.warn("[DIAG] BytecodeLoader returned null/empty for {}, falling back to classfileBuffer ({} bytes)",
                    className, classfileBuffer.length);
            } catch (Exception e) {
                LOGGER.warn("[DIAG] Failed to load original bytecode for {}, falling back to classfileBuffer ({} bytes): {}",
                    className, classfileBuffer.length, e.getMessage());
            }
        }
        return classfileBuffer;
    }
}
