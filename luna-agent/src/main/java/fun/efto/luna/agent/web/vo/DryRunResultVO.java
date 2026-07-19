package fun.efto.luna.agent.web.vo;

/**
 * 预运行结果 VO
 *
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/27 12:00
 */
public class DryRunResultVO {
    private final int bytecodeSize;
    private final int originalSize;
    private final String injectedMethod;
    private final String message;

    public DryRunResultVO(int bytecodeSize, int originalSize, String injectedMethod, String message) {
        this.bytecodeSize = bytecodeSize;
        this.originalSize = originalSize;
        this.injectedMethod = injectedMethod;
        this.message = message;
    }

    public int getBytecodeSize() {
        return bytecodeSize;
    }

    public int getOriginalSize() {
        return originalSize;
    }

    public String getInjectedMethod() {
        return injectedMethod;
    }

    public String getMessage() {
        return message;
    }
}
