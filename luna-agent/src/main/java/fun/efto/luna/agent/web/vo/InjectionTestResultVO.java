package fun.efto.luna.agent.web.vo;

import java.util.Map;

/**
 * 注入测试结果 VO
 *
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/27 12:00
 */
public class InjectionTestResultVO {
    private final boolean success;
    private final Map<String, TestStepVO> steps;
    private final String error;

    public InjectionTestResultVO(boolean success, Map<String, TestStepVO> steps, String error) {
        this.success = success;
        this.steps = steps;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public Map<String, TestStepVO> getSteps() {
        return steps;
    }

    public String getError() {
        return error;
    }
}
