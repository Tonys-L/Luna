package fun.efto.luna.agent.web;

import com.alibaba.fastjson.JSON;
import fun.efto.luna.agent.clazz.ClassScanner;
import fun.efto.luna.agent.clazz.LoadedClass;
import fun.efto.luna.core.InjectionExecutor;
import fun.efto.luna.core.analyzer.AnalyzerRegistry;
import fun.efto.luna.core.analyzer.AnalyzerType;
import fun.efto.luna.core.analyzer.ClassAnalysisResult;
import fun.efto.luna.core.analyzer.ClassAnalyzer;
import fun.efto.luna.core.decompile.DecompilerFactory;
import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.injection.code.type.CodeType;
import fun.efto.luna.core.injection.target.InjectionTarget;
import fun.efto.luna.core.injection.target.type.InjectionType;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Luna API Servlet - 处理REST API请求
 */
public class LunaApiServlet extends HttpServlet {
    private final InjectionExecutor injectionExecutor;
    private final ClassScanner classScanner;

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
        } else if (pathInfo != null && pathInfo.startsWith("/analysis")) {
            handleAnalysis(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{\"error\":\"接口不存在\"}");
        }
    }

    private void handleAnalysis(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String className = request.getParameter("class");
        Optional<ClassAnalyzer> classAnalyzer = AnalyzerRegistry.getInstance().get(AnalyzerType.valueOf("ASM"));
        if (classAnalyzer.isPresent()) {
            try {
                byte[] bytes = loadClassBytes(className);
                ClassAnalysisResult analyze = classAnalyzer.get().analyze(bytes);
                response.getWriter().write(JSON.toJSONString(analyze));
            } catch (IOException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\":\"类加载失败\"}");
            }
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
            String decompiledCode = DecompilerFactory.getDecompiler().decompile(className);
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
        } else if (pathInfo != null && pathInfo.startsWith("/inject")) {
            handleInject(request, response, injectionExecutor);
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
        // 从请求体中读取数据
        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        // 解析JSON数据
        String jsonString = sb.toString();
        Map<String, Object> requestData = JSON.parseObject(jsonString, Map.class);

        String clazz = (String) requestData.get("class");
        String method = (String) requestData.get("method");
        String injectionType = (String) requestData.get("injectionType");
        String desc = (String) requestData.get("desc");
        String codeType = (String) requestData.get("codeType");
        String message = (String) requestData.get("code");

        if (clazz == null || method == null || injectionType == null || codeType == null || message == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"缺少必要参数\"}");
            return;
        }

        try {
            InjectionType type = InjectionType.valueOf(injectionType);
            InjectionTarget target = JSON.parseObject(jsonString, type.getTargetClass());
            InjectableCode code = JSON.parseObject(jsonString, CodeType.valueOf(codeType).getCodeClass());

            InjectionPoint injectionPoint = new InjectionPoint(target, code);
            injectionExecutor.execute(injectionPoint);
            response.getWriter().write("{\"success\":true,\"message\":\"注入成功\"}");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"注入失败: " + e.getMessage() + "\"}");
        }
    }

    private void handleCreateLog(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // 简化实现
        response.getWriter().write("{\"success\":true,\"id\":\"test\"}");
    }

    private byte[] loadClassBytes(String className) throws IOException {
        String path = className.replace('.', '/') + ".class";
        InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream(path);
        if (inputStream == null) {
            throw new IOException("Cannot find class file for: " + className);
        }

        byte[] buffer = new byte[inputStream.available()];
        inputStream.read(buffer);
        inputStream.close();
        return buffer;
    }
}