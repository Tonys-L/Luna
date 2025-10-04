package fun.efto.luna.core.transformer;

import fun.efto.luna.core.injection.InjectionPoint;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 13:59
 */
public interface ClassTransformer {

    TransformerResult transform(InjectionPoint injectionPoint, String className, byte[] bytecode);
}
