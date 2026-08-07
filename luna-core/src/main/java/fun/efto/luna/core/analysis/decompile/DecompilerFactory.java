package fun.efto.luna.core.analysis.decompile;

import fun.efto.luna.core.injection.port.BytecodeLoader;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since ：2025/10/19 19:52
 */
public final class DecompilerFactory {
    private DecompilerFactory() {

    }
    public static Decompiler getDecompiler(BytecodeLoader bytecodeLoader) {
        return new CfrDecompiler(bytecodeLoader);
    }
}
