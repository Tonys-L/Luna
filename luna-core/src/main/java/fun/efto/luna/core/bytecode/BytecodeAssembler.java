package fun.efto.luna.core.bytecode;

import fun.efto.luna.core.InjectionContext;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/2 21:07
 */
public interface BytecodeAssembler {
    void assemble(InjectionContext injectionContext, byte[] bytecode);
}
