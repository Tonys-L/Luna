package fun.efto.luna.asm.assmebler;

import fun.efto.luna.asm.AsmInjectionContext;
import fun.efto.luna.core.InjectionContext;
import fun.efto.luna.core.bytecode.BytecodeAssembler;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/5 15:29
 */
public abstract class BaseAsmBytecodeAssembler implements BytecodeAssembler {
    @Override
    public void assemble(InjectionContext injectionContext, byte[] bytecode) {
        if (!(injectionContext instanceof AsmInjectionContext)) {
            throw new IllegalArgumentException("InjectionContext must be an instance of AsmInjectionContext");
        }

        AsmInjectionContext asmContext = (AsmInjectionContext) injectionContext;

        doAssemble(asmContext, bytecode);
    }

    protected abstract void doAssemble(AsmInjectionContext asmContext, byte[] bytecode);
}
