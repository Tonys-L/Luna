package fun.efto.luna.agent.web.controller;

import fun.efto.luna.agent.web.MetricsService;
import fun.efto.luna.agent.web.mvc.ApiResult;
import fun.efto.luna.agent.web.mvc.Controller;
import fun.efto.luna.agent.web.mvc.GetMapping;

import java.util.HashMap;
import java.util.Map;

/**
 * JVM 指标与线程分析控制器
 *
 * @author : Tony.L(<286269159@qq.com>)
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
        Map<String, Object> data = new HashMap<>();
        data.put("threads", MetricsService.getThreadDump());
        data.put("deadlockedIds", MetricsService.findDeadlockedThreads());
        return ApiResult.ok(data);
    }
}
