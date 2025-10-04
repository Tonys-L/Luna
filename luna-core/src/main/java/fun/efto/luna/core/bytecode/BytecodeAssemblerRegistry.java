package fun.efto.luna.core.bytecode;

import fun.efto.luna.core.Registry;
import fun.efto.luna.core.injection.code.type.CodeType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 15:59
 */
public final class BytecodeAssemblerRegistry implements Registry<CodeType, BytecodeAssembler> {
    private static final BytecodeAssemblerRegistry INSTANCE = new BytecodeAssemblerRegistry();
    private static final Map<CodeType, BytecodeAssembler> ASSEMBLER_REGISTRY = new ConcurrentHashMap<>();

    private BytecodeAssemblerRegistry() {
    }

    public static BytecodeAssemblerRegistry getInstance() {
        return INSTANCE;
    }

    @Override
    public Map<CodeType, BytecodeAssembler> getRegistry() {
        return ASSEMBLER_REGISTRY;
    }
}
