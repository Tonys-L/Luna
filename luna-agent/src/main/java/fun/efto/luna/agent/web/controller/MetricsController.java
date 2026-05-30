package fun.efto.luna.agent.web.controller;

import fun.efto.luna.agent.web.MetricsService;
import fun.efto.luna.agent.web.vo.ThreadDumpVO;
import fun.efto.luna.core.web.ApiResult;
import fun.efto.luna.core.web.Controller;
import fun.efto.luna.core.web.GetMapping;

import java.util.ArrayList;
import java.util.List;

/**
 * JVM 指标与线程分析控制器
 *
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/10 00:35
 */
@Controller
public class MetricsController {

    @GetMapping("/metrics/jvm")
    public ApiResult getJvmMetrics() {
        return ApiResult.ok(MetricsService.getJvmMetrics());
    }

    @GetMapping("/metrics/threads")
    public ApiResult getThreadDump() {
        long[] deadlocked = MetricsService.findDeadlockedThreads();
        List<Long> deadlockedIds = new ArrayList<>();
        if (deadlocked != null) {
            for (long id : deadlocked) {
                deadlockedIds.add(id);
            }
        }
        return ApiResult.ok(new ThreadDumpVO(MetricsService.getThreadDump(), deadlockedIds));
    }
}
