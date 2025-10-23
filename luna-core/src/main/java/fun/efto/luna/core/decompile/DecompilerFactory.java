package fun.efto.luna.core.decompile;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/19 19:52
 */
public final class DecompilerFactory {
    private DecompilerFactory() {

    }
    public static Decompiler getDecompiler() {
        return new CfrDecompiler();
    }
}
