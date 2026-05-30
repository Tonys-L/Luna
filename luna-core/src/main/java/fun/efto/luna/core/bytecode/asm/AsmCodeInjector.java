package fun.efto.luna.core.bytecode.asm;

import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjector;
import fun.efto.luna.core.bytecode.BytecodeAssembler;
import fun.efto.luna.core.injection.CodeInjector;
import fun.efto.luna.core.injection.InjectionContext;
import fun.efto.luna.core.injection.InjectionPoint;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/31 21:00
 */
public final class AsmCodeInjector implements CodeInjector {

    private final BytecodeInjector bytecodeInjector;
    private final BytecodeAssembler bytecodeAssembler;

    public AsmCodeInjector(BytecodeInjector bytecodeInjector, BytecodeAssembler bytecodeAssembler) {
        this.bytecodeInjector = bytecodeInjector;
        this.bytecodeAssembler = bytecodeAssembler;
    }

    @Override
    public byte[] inject(InjectionPoint point, byte[] bytecode) {
        InjectionContext context = new InjectionContext(point);
        return bytecodeInjector.inject(context, bytecode, bytecodeAssembler);
    }
}
