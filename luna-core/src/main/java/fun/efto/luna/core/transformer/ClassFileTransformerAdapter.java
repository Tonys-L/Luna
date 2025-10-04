package fun.efto.luna.core.transformer;

import fun.efto.luna.core.injection.InjectionPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 14:17
 */
public class ClassFileTransformerAdapter implements ClassFileTransformer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassFileTransformerAdapter.class);
    private final InjectionPoint injectionPoint;
    private final ClassTransformer classTransformer;

    public ClassFileTransformerAdapter(InjectionPoint injectionPoint, ClassTransformer classTransformer) {
        this.injectionPoint = injectionPoint;
        this.classTransformer = classTransformer;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        TransformerResult result = classTransformer.transform(injectionPoint, className, classfileBuffer);

        if (result.isTransformed()) {
            return result.getBytecode();
        } else {
            LOGGER.error("[{}]转换失败:{}", className, result.getMessage());
        }
        return classfileBuffer;
    }
}
