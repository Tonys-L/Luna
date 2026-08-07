package fun.efto.luna.agent.web.controller;

import fun.efto.luna.agent.runtime.AgentRuntime;
import fun.efto.luna.core.infra.web.ApiResult;
import fun.efto.luna.core.infra.web.Controller;
import fun.efto.luna.core.infra.web.PostMapping;

/**
 * 提供 HTTP 接口供 attacher 远程关闭 agent 的 Web 服务（释放 8421 端口）。
 * 关闭在独立线程异步执行，确保当前 HTTP 响应能正常返回后再停止 Jetty。
 *
 * 注意：本接口只停止 Web 服务，不会回滚已注入的字节码增强。
 * 彻底清理需重启目标 JVM。
 *
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/08/07
 */
@Controller
public class ShutdownController {

    @PostMapping("/shutdown")
    public ApiResult shutdown() {
        new Thread(() -> {
            try {
                Thread.sleep(200);
                AgentRuntime runtime = AgentRuntime.getInstance();
                if (runtime != null) {
                    runtime.stop();
                }
            } catch (Exception e) {
                // 忽略关闭过程中的异常
            }
        }, "luna-shutdown").start();
        return ApiResult.ok();
    }
}
