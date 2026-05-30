package fun.efto.luna.core.bytecode.asm.assembler;

import fun.efto.luna.core.common.type.Registry;
import fun.efto.luna.core.bytecode.BytecodeAssembler;
import fun.efto.luna.core.injection.code.CodeType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2025/10/4 15:59
 */
public final class BytecodeAssemblerRegistry implements Registry<CodeType, BytecodeAssembler> {
    private static final Map<CodeType, BytecodeAssembler> ASSEMBLER_REGISTRY = new ConcurrentHashMap<>();
    private static final BytecodeAssemblerRegistry INSTANCE = new BytecodeAssemblerRegistry();

    private BytecodeAssemblerRegistry() {
        ASSEMBLER_REGISTRY.put(CodeType.EXPRESSION, new ExpressionBytecodeAssembler());
    }

    public static BytecodeAssemblerRegistry getInstance() {
        return INSTANCE;
    }

    @Override
    public Map<CodeType, BytecodeAssembler> getRegistry() {
        return ASSEMBLER_REGISTRY;
    }

    @Override
    public BytecodeAssembler register(CodeType type, BytecodeAssembler assembler) {
        ASSEMBLER_REGISTRY.put(type, assembler);
        return assembler;
    }
}
