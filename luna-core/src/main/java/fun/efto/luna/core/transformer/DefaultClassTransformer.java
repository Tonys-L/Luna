package fun.efto.luna.core.transformer;

import fun.efto.luna.core.InjectionContext;
import fun.efto.luna.core.bytecode.BytecodeAssembler;
import fun.efto.luna.core.bytecode.BytecodeAssemblerRegistry;
import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injector.BytecodeInjector;
import fun.efto.luna.core.injector.BytecodeInjectorRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 14:23
 */
public class DefaultClassTransformer implements ClassTransformer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultClassTransformer.class);

    @Override
    public TransformerResult transform(InjectionPoint injectionPoint, String className, byte[] bytecode) {
        try {
            Optional<BytecodeInjector> injectorOptional = BytecodeInjectorRegistry.getInstance().get(injectionPoint.getInjectionType());
            Optional<BytecodeAssembler> assemblerOptional = BytecodeAssemblerRegistry.getInstance().get(injectionPoint.getCodeType());
            if (!injectorOptional.isPresent()) {
                return new TransformerResult(
                        bytecode,
                        false,
                        "未找到对应的字节码注入器: " + injectionPoint.getInjectionType()
                );
            }

            if (!assemblerOptional.isPresent()) {
                return new TransformerResult(
                        bytecode,
                        false,
                        "未找到对应的字节码组装器: " + injectionPoint.getCodeType()
                );
            }

            BytecodeInjector injector = injectorOptional.get();
            byte[] transformedBytecode = injector.inject(new InjectionContext(injectionPoint), bytecode, assemblerOptional.get());

            return new TransformerResult(
                    transformedBytecode,
                    true,
                    "转换成功: " + className
            );
        } catch (Exception e) {
            LOGGER.error("[{}]转换失败", className, e);
            return new TransformerResult(
                    bytecode,
                    false,
                    "转换失败: " + className + ", error: " + e.getMessage()
            );
        }
    }
}
