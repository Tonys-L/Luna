package fun.efto.luna.core.injector;

import fun.efto.luna.core.InjectionContext;
import fun.efto.luna.core.bytecode.BytecodeAssembler;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/2 20:44
 */
public interface BytecodeInjector {
    byte[] inject(InjectionContext injectionContext, byte[] bytecode, BytecodeAssembler bytecodeAssembler);
}
