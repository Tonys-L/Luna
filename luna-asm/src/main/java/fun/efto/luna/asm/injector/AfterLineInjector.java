package fun.efto.luna.asm.injector;

import fun.efto.luna.core.InjectionContext;
import fun.efto.luna.core.bytecode.BytecodeAssembler;
import fun.efto.luna.core.injector.BytecodeInjector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 18:00
 */
public class AfterLineInjector implements BytecodeInjector {
    private static final Logger LOGGER = LoggerFactory.getLogger(AfterLineInjector.class);

    @Override
    public byte[] inject(InjectionContext injectionContext, byte[] bytecode, BytecodeAssembler bytecodeAssembler) {
        LOGGER.info("在行后注入");
        return bytecode;
    }
}
