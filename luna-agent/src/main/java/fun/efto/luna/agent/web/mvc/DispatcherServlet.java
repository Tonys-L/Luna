package fun.efto.luna.agent.web.mvc;

import fun.efto.luna.core.web.ApiResult;
import fun.efto.luna.core.web.RouteEngine;
import fun.efto.luna.core.plugin.LunaController;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Servlet 适配器，将 HTTP 请求委托给容器无关的 RouteEngine 处理。
 * 本类仅负责 Servlet 协议适配，不包含任何路由逻辑。
 *
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/09 10:00
 */
public class DispatcherServlet extends HttpServlet {

    private final RouteEngine routeEngine = new RouteEngine();

    public void registerController(Object controller) {
        routeEngine.registerController(controller);
    }

    public void unregisterController(Object controller) {
        routeEngine.unregisterController(controller);
    }

    public void registerControllers(List<LunaController> controllers) {
        for (LunaController controller : controllers) {
            routeEngine.registerController(controller);
        }
    }

    public void unregisterControllers(List<LunaController> controllers) {
        for (LunaController controller : controllers) {
            routeEngine.unregisterController(controller);
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        ServletRequestContext ctx = new ServletRequestContext(req, resp);
        boolean handled = routeEngine.handle(ctx);

        if (!handled) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write(com.alibaba.fastjson.JSON.toJSONString(
                    ApiResult.fail("接口不存在", 404)));
        }
    }
}
