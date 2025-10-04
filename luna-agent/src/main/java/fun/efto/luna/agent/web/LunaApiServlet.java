package fun.efto.luna.agent.web;

import com.alibaba.fastjson.JSON;
import fun.efto.luna.agent.clazz.ClassScanner;
import fun.efto.luna.agent.clazz.LoadedClass;
import fun.efto.luna.core.InjectionExecutor;
import fun.efto.luna.core.injection.InjectionPoint;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Luna API Servlet - 处理REST API请求
 */
public class LunaApiServlet extends HttpServlet {
    private InjectionExecutor injectionExecutor;
    private ClassScanner classScanner;

    public LunaApiServlet(InjectionExecutor injectionExecutor, ClassScanner classScanner) {
        this.injectionExecutor = injectionExecutor;
        this.classScanner = classScanner;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String pathInfo = request.getPathInfo();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        System.out.println("agent 收到请求:" + pathInfo);
        // 简化的路由处理
        if ("/status".equals(pathInfo)) {
            handleStatus(response);
        } else if (pathInfo != null && pathInfo.startsWith("/classes")) {
            handleClasses(request, response);
        } else if (pathInfo != null && pathInfo.startsWith("/inject")) {
            handleInject(request, response, injectionExecutor);
        } else if (pathInfo != null && pathInfo.startsWith("/decompile")) {
            handleDecompile(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{\"error\":\"接口不存在\"}");
        }
    }

    private void handleDecompile(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String className = request.getParameter("class");
        if (className == null || className.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"缺少 class 参数\"}");
            return;
        }

        try {
            String decompiledCode = "";//CFRUtil.decompile(className);
            response.getWriter().write("{\"decompiled\":\"" +
                    decompiledCode.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") + "\"}");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"反编译失败: " + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String pathInfo = request.getPathInfo();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if ("/logs".equals(pathInfo)) {
            handleCreateLog(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{\"error\":\"接口不存在\"}");
        }
    }

    private void handleStatus(HttpServletResponse response) throws IOException {
        // 简化实现
        response.getWriter().write("{\"status\":\"running\",\"version\":\"1.0.0\"}");
    }

    private void handleClasses(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String refresh = request.getParameter("refresh");
        Map<String, Set<LoadedClass>> result;
        if (refresh != null && refresh.equals("true")) {
            result = classScanner.scan();
        } else {
            result = classScanner.getLoadedClasses();
        }
        String jsonString = JSON.toJSONString(result);
        // 简化实现
        response.getWriter().write(jsonString);
    }

    private void handleInject(HttpServletRequest request, HttpServletResponse response, InjectionExecutor injectionExecutor) throws IOException {
        String clazz = request.getParameter("class");
        String method = request.getParameter("method");
        String injectionType = request.getParameter("injectionType");
        String desc = request.getParameter("desc");
        String codeType = request.getParameter("codeType");
        String message = request.getParameter("code");
        InjectionPoint injectionPoint = null;//new InjectionPoint(InjectionType.valueOf(injectionType), clazz, method, desc, CodeType.valueOf(codeType), message);
        injectionExecutor.execute(injectionPoint);
        // 简化实现
        response.getWriter().write("{\"logs\":[]}");
    }

    private void handleCreateLog(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // 简化实现
        response.getWriter().write("{\"success\":true,\"id\":\"test\"}");
    }
}
