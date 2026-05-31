package fun.efto.luna.core.bytecode.asm;

import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjector;
import fun.efto.luna.core.injection.CodeInjector;
import fun.efto.luna.core.injection.InjectionContext;
import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.code.CompiledCode;
import fun.efto.luna.core.plugin.ProbeHandler;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/31 21:00
 */
public final class AsmCodeInjector implements CodeInjector {

    private final BytecodeInjector bytecodeInjector;
    private final ProbeHandler probeHandler;

    public AsmCodeInjector(BytecodeInjector bytecodeInjector, ProbeHandler probeHandler) {
        this.bytecodeInjector = bytecodeInjector;
        this.probeHandler = probeHandler;
    }

    @Override
    public byte[] inject(InjectionPoint point, byte[] bytecode) {
        InjectionContext context = new InjectionContext(point);
        CompiledCode compiledCode = point.getCode();
        return bytecodeInjector.inject(compiledCode, probeHandler, context, bytecode);
    }
}
