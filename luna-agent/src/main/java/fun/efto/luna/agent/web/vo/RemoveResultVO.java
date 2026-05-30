package fun.efto.luna.agent.web.vo;

/**
 * 移除结果 VO
 *
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/27 12:00
 */
public class RemoveResultVO {
    private final boolean success;
    private final String removedId;
    private final String className;

    public RemoveResultVO(boolean success, String removedId, String className) {
        this.success = success;
        this.removedId = removedId;
        this.className = className;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getRemovedId() {
        return removedId;
    }

    public String getClassName() {
        return className;
    }
}
