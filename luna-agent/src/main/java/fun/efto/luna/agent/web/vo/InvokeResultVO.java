package fun.efto.luna.agent.web.vo;

/**
 * 方法调用结果 VO
 *
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/27 12:00
 */
public class InvokeResultVO {
    private final String result;
    private final String output;
    private final String error;

    private InvokeResultVO(String result, String output, String error) {
        this.result = result;
        this.output = output;
        this.error = error;
    }

    public static InvokeResultVO success(String result, String output) {
        return new InvokeResultVO(result, output, null);
    }

    public static InvokeResultVO failure(String error, String output) {
        return new InvokeResultVO(null, output, error);
    }

    public String getResult() {
        return result;
    }

    public String getOutput() {
        return output;
    }

    public String getError() {
        return error;
    }
}
