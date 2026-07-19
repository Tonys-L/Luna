package fun.efto.luna.core.bytecode.asm.injector;

import fun.efto.luna.core.injection.InjectionContext;
import fun.efto.luna.core.injection.code.CompiledCode;
import fun.efto.luna.core.plugin.ProbeHandler;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2025/10/2 20:44
 */
public interface BytecodeInjector {
    byte[] inject(CompiledCode compiledCode, ProbeHandler probeHandler, InjectionContext injectionContext, byte[] bytecode);
}
