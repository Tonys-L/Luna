package fun.efto.luna.core.transformer;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 13:59
 */
public class TransformerResult {
    private final byte[] bytecode;
    private final boolean transformed;
    private final String message;

    public TransformerResult(byte[] bytecode, boolean transformed, String message) {
        this.bytecode = bytecode;
        this.transformed = transformed;
        this.message = message;
    }

    public byte[] getBytecode() {
        return bytecode;
    }

    public boolean isTransformed() {
        return transformed;
    }

    public String getMessage() {
        return message;
    }
}
