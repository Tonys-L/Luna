package fun.efto.luna.agent.web.vo;

/**
 * 健康检查 VO
 *
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/27 12:00
 */
public class HealthVO {
    private final String status;
    private final long timestamp;
    private final int loadedClasses;

    public HealthVO(String status, long timestamp, int loadedClasses) {
        this.status = status;
        this.timestamp = timestamp;
        this.loadedClasses = loadedClasses;
    }

    public String getStatus() {
        return status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getLoadedClasses() {
        return loadedClasses;
    }
}
