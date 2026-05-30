package fun.efto.luna.agent.web.controller;

import fun.efto.luna.core.web.ApiResult;
import fun.efto.luna.core.web.Controller;
import fun.efto.luna.core.web.GetMapping;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/09 10:00
 */
@Controller
public class StatusController {

    @GetMapping("/status")
    public ApiResult status() {
        return ApiResult.ok(new StatusInfo("running", "1.0.0"));
    }

    private static class StatusInfo {
        private final String status;
        private final String version;

        StatusInfo(String status, String version) {
            this.status = status;
            this.version = version;
        }

        public String getStatus() { return status; }
        public String getVersion() { return version; }
    }
}
