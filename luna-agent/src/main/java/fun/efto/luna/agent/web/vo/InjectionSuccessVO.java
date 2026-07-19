package fun.efto.luna.agent.web.vo;

import java.util.List;

/**
 * 注入成功 VO
 *
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/27 12:00
 */
public class InjectionSuccessVO {
    private final boolean success;
    private final List<InjectionResultVO> results;
    private final String injectionPointId;

    public InjectionSuccessVO(boolean success, List<InjectionResultVO> results, String injectionPointId) {
        this.success = success;
        this.results = results;
        this.injectionPointId = injectionPointId;
    }

    public boolean isSuccess() {
        return success;
    }

    public List<InjectionResultVO> getResults() {
        return results;
    }

    public String getInjectionPointId() {
        return injectionPointId;
    }
}
