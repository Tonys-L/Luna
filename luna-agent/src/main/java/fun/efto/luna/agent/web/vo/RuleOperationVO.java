package fun.efto.luna.agent.web.vo;

/**
 * 规则操作结果 VO
 *
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/27 12:00
 */
public class RuleOperationVO {
    private final boolean success;
    private final Long id;

    private RuleOperationVO(boolean success, Long id) {
        this.success = success;
        this.id = id;
    }

    public static RuleOperationVO success() {
        return new RuleOperationVO(true, null);
    }

    public static RuleOperationVO successWithId(long id) {
        return new RuleOperationVO(true, id);
    }

    public boolean isSuccess() {
        return success;
    }

    public Long getId() {
        return id;
    }
}
