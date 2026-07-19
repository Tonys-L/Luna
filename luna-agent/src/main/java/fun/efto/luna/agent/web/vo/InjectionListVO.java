package fun.efto.luna.agent.web.vo;

import java.util.List;

/**
 * 注入点列表 VO
 *
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/27 12:00
 */
public class InjectionListVO {
    private final List<InjectionPointVO> injections;

    public InjectionListVO(List<InjectionPointVO> injections) {
        this.injections = injections;
    }

    public List<InjectionPointVO> getInjections() {
        return injections;
    }
}
