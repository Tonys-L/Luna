package fun.efto.luna.agent.web.vo;

/**
 * 验证结果 VO
 *
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/27 12:00
 */
public class VerifyResultVO {
    private final String output;
    private final String error;

    private VerifyResultVO(String output, String error) {
        this.output = output;
        this.error = error;
    }

    public static VerifyResultVO success(String output) {
        return new VerifyResultVO(output, null);
    }

    public static VerifyResultVO failure(String error) {
        return new VerifyResultVO(null, error);
    }

    public String getOutput() {
        return output;
    }

    public String getError() {
        return error;
    }
}
