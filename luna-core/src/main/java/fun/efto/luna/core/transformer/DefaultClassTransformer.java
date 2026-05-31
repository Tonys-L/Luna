package fun.efto.luna.core.transformer;

import fun.efto.luna.core.injection.CodeEngine;
import fun.efto.luna.core.injection.CodeEngineRegistry;
import fun.efto.luna.core.injection.InjectionContext;
import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.code.CompiledCode;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjector;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjectorRegistry;
import fun.efto.luna.core.plugin.ProbeHandler;
import fun.efto.luna.core.plugin.registry.ProbeHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since ：2025/10/4 14:23
 */
public class DefaultClassTransformer implements ClassTransformer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultClassTransformer.class);

    private static TransformerResult buildErrorResult(byte[] bytecode, String errorMessage) {
        return new TransformerResult(
                bytecode,
                false,
                errorMessage
        );
    }

    @Override
    public TransformerResult transform(InjectionPoint injectionPoint, String className, byte[] bytecode) {
        try {
            Optional<BytecodeInjector> injectorOptional = BytecodeInjectorRegistry.getInstance().get(injectionPoint.getInjectionLocation());
            if (!injectorOptional.isPresent()) {
                return buildErrorResult(bytecode, "未找到对应的字节码注入器: " + injectionPoint.getInjectionLocation());
            }

            BytecodeInjector injector = injectorOptional.get();

            String probeType = injectionPoint.getProbeType();
            String codeType = injectionPoint.getCodeType();

            ProbeHandler probeHandler = null;
            if (probeType != null && !probeType.isEmpty()) {
                probeHandler = ProbeHandlerRegistry.getInstance().get(probeType).orElse(null);
            }

            CompiledCode compiledCode = null;
            if (probeHandler != null && probeHandler.usesCode() && codeType != null && !codeType.isEmpty()) {
                Optional<CodeEngine> engineOptional = CodeEngineRegistry.getInstance().get(codeType);
                if (!engineOptional.isPresent()) {
                    return buildErrorResult(bytecode, "未找到对应的代码引擎: " + codeType);
                }
                CodeEngine engine = engineOptional.get();
                compiledCode = engine.compile(injectionPoint.toPersistentInjection());
            }

            byte[] transformedBytecode = injector.inject(compiledCode, probeHandler, new InjectionContext(injectionPoint), bytecode);

            return new TransformerResult(
                    transformedBytecode,
                    true,
                    "转换成功: " + className
            );
        } catch (Exception e) {
            LOGGER.error("[{}]转换失败", className, e);
            return buildErrorResult(bytecode, "转换失败: " + className + ", error: " + e.getMessage());
        }
    }
}
