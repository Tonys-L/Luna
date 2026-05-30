package fun.efto.luna.agent.web.vo;

import java.util.List;
import java.util.Map;

/**
 * 线程转储 VO
 *
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/27 12:00
 */
public class ThreadDumpVO {
    private final List<Map<String, Object>> threads;
    private final List<Long> deadlockedIds;

    public ThreadDumpVO(List<Map<String, Object>> threads, List<Long> deadlockedIds) {
        this.threads = threads;
        this.deadlockedIds = deadlockedIds;
    }

    public List<Map<String, Object>> getThreads() {
        return threads;
    }

    public List<Long> getDeadlockedIds() {
        return deadlockedIds;
    }
}
