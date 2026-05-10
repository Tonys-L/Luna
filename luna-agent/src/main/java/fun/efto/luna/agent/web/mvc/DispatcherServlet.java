package fun.efto.luna.agent.web.mvc;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/09 10:00
 */
public class DispatcherServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(DispatcherServlet.class);

    private final Map<String, RouteHandler> exactRoutes = new ConcurrentHashMap<>();
    private final List<PatternRoute> patternRoutes = new ArrayList<>();

    public void registerController(Object controller) {
        Class<?> clazz = controller.getClass();
        String classPath = "";
        if (clazz.isAnnotationPresent(RequestMapping.class)) {
            classPath = clazz.getAnnotation(RequestMapping.class).value();
        }

        for (Method method : clazz.getDeclaredMethods()) {
            registerMethod(controller, method, classPath);
        }

        LOGGER.info("Registered controller: {} (prefix={})", clazz.getSimpleName(), classPath);
    }

    private void registerMethod(Object controller, Method method, String classPath) {
        GetMapping get = method.getAnnotation(GetMapping.class);
        PostMapping post = method.getAnnotation(PostMapping.class);
        PutMapping put = method.getAnnotation(PutMapping.class);
        DeleteMapping delete = method.getAnnotation(DeleteMapping.class);

        if (get != null) {
            addRoute("GET", classPath + get.value(), controller, method);
        }
        if (post != null) {
            addRoute("POST", classPath + post.value(), controller, method);
        }
        if (put != null) {
            addRoute("PUT", classPath + put.value(), controller, method);
        }
        if (delete != null) {
            addRoute("DELETE", classPath + delete.value(), controller, method);
        }
    }

    private void addRoute(String httpMethod, String path, Object controller, Method method) {
        method.setAccessible(true);
        RouteHandler handler = new RouteHandler(controller, method, buildParamResolvers(method));

        if (path.contains("{")) {
            PatternRoute patternRoute = compilePattern(httpMethod, path, handler);
            patternRoutes.add(patternRoute);
            LOGGER.debug("  Pattern route: {} {} -> {}.{}", httpMethod, path,
                    controller.getClass().getSimpleName(), method.getName());
        } else {
            String key = httpMethod + ":" + path;
            exactRoutes.put(key, handler);
            LOGGER.debug("  Exact route: {} {} -> {}.{}", httpMethod, path,
                    controller.getClass().getSimpleName(), method.getName());
        }
    }

    private PatternRoute compilePattern(String httpMethod, String pathPattern, RouteHandler handler) {
        StringBuilder regex = new StringBuilder("^");
        List<String> varNames = new ArrayList<>();
        for (String segment : pathPattern.split("/")) {
            if (segment.isEmpty()) continue;
            regex.append("/");
            if (segment.startsWith("{") && segment.endsWith("}")) {
                String varName = segment.substring(1, segment.length() - 1);
                varNames.add(varName);
                regex.append("([^/]+)");
            } else {
                regex.append(Pattern.quote(segment));
            }
        }
        regex.append("$");
        return new PatternRoute(httpMethod, Pattern.compile(regex.toString()), varNames, handler);
    }

    private ParamResolver[] buildParamResolvers(Method method) {
        Parameter[] params = method.getParameters();
        ParamResolver[] resolvers = new ParamResolver[params.length];

        for (int i = 0; i < params.length; i++) {
            Parameter param = params[i];
            Class<?> type = param.getType();

            RequestParam requestParam = param.getAnnotation(RequestParam.class);
            RequestBody requestBody = param.getAnnotation(RequestBody.class);
            PathVariable pathVariable = param.getAnnotation(PathVariable.class);

            if (requestParam != null) {
                resolvers[i] = new RequestParamResolver(requestParam.value(), requestParam.required(), type);
            } else if (requestBody != null) {
                resolvers[i] = new RequestBodyResolver(type);
            } else if (pathVariable != null) {
                resolvers[i] = new PathVariableResolver(pathVariable.value(), type);
            } else if (HttpServletRequest.class.isAssignableFrom(type)) {
                resolvers[i] = new ServletRequestResolver();
            } else if (HttpServletResponse.class.isAssignableFrom(type)) {
                resolvers[i] = new ServletResponseResolver();
            } else {
                resolvers[i] = new NullResolver();
            }
        }
        return resolvers;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String httpMethod = req.getMethod();
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) pathInfo = "/";

        RouteMatch match = findRoute(httpMethod, pathInfo);
        if (match == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write(JSON.toJSONString(ApiResult.fail("接口不存在", 404)));
            return;
        }

        try {
            Object result = match.handler.invoke(req, resp, match.pathVariables);
            writeResponse(resp, result);
        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(JSON.toJSONString(ApiResult.fail(e.getMessage(), 400)));
        } catch (Exception e) {
            LOGGER.error("Handler invocation failed: {} {}", httpMethod, pathInfo, e);
            Throwable cause = e.getCause();
            if (cause instanceof IllegalArgumentException) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(JSON.toJSONString(ApiResult.fail(cause.getMessage(), 400)));
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write(JSON.toJSONString(
                        ApiResult.fail("服务器内部错误: " + (cause != null ? cause.getMessage() : e.getMessage()), 500)));
            }
        }
    }

    private RouteMatch findRoute(String httpMethod, String pathInfo) {
        String key = httpMethod + ":" + pathInfo;
        RouteHandler exact = exactRoutes.get(key);
        if (exact != null) {
            return new RouteMatch(exact, new HashMap<>());
        }

        for (PatternRoute pr : patternRoutes) {
            if (!pr.httpMethod.equals(httpMethod)) continue;
            Matcher matcher = pr.pattern.matcher(pathInfo);
            if (matcher.matches()) {
                Map<String, String> vars = new LinkedHashMap<>();
                for (int i = 0; i < pr.varNames.size(); i++) {
                    vars.put(pr.varNames.get(i), matcher.group(i + 1));
                }
                return new RouteMatch(pr.handler, vars);
            }
        }
        return null;
    }

    private void writeResponse(HttpServletResponse resp, Object result) throws IOException {
        if (result == null) return;

        if (result instanceof ApiResult) {
            ApiResult apiResult = (ApiResult) result;
            if (apiResult.getStatus() != 200) {
                resp.setStatus(apiResult.getStatus());
            }
            resp.getWriter().write(JSON.toJSONString(apiResult));
        } else if (result instanceof String) {
            resp.getWriter().write((String) result);
        } else {
            resp.getWriter().write(JSON.toJSONString(result));
        }
    }

    static String readRequestBody(HttpServletRequest req) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        java.io.InputStream is = req.getInputStream();
        while ((len = is.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        byte[] bytes = baos.toByteArray();
        
        // 调试：打印前 20 个字节的 Hex 码，确认原始编码
        if (bytes.length > 0) {
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < Math.min(bytes.length, 20); i++) {
                hex.append(String.format("%02X ", bytes[i]));
            }
            LOGGER.debug("Request body (first 20 bytes hex): {}", hex.toString());
        }

        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static class RouteHandler {
        final Object controller;
        final Method method;
        final ParamResolver[] resolvers;

        RouteHandler(Object controller, Method method, ParamResolver[] resolvers) {
            this.controller = controller;
            this.method = method;
            this.resolvers = resolvers;
        }

        Object invoke(HttpServletRequest req, HttpServletResponse resp,
                       Map<String, String> pathVars) throws Exception {
            Object[] args = new Object[resolvers.length];
            for (int i = 0; i < resolvers.length; i++) {
                args[i] = resolvers[i].resolve(req, resp, pathVars);
            }
            return method.invoke(controller, args);
        }
    }

    private static class RouteMatch {
        final RouteHandler handler;
        final Map<String, String> pathVariables;

        RouteMatch(RouteHandler handler, Map<String, String> pathVariables) {
            this.handler = handler;
            this.pathVariables = pathVariables;
        }
    }

    private static class PatternRoute {
        final String httpMethod;
        final Pattern pattern;
        final List<String> varNames;
        final RouteHandler handler;

        PatternRoute(String httpMethod, Pattern pattern, List<String> varNames, RouteHandler handler) {
            this.httpMethod = httpMethod;
            this.pattern = pattern;
            this.varNames = varNames;
            this.handler = handler;
        }
    }

    interface ParamResolver {
        Object resolve(HttpServletRequest req, HttpServletResponse resp,
                        Map<String, String> pathVars) throws IOException;
    }

    static class RequestParamResolver implements ParamResolver {
        private final String name;
        private final boolean required;
        private final Class<?> type;

        RequestParamResolver(String name, boolean required, Class<?> type) {
            this.name = name;
            this.required = required;
            this.type = type;
        }

        @Override
        public Object resolve(HttpServletRequest req, HttpServletResponse resp,
                               Map<String, String> pathVars) {
            String value = req.getParameter(name);
            if (value == null || value.isEmpty()) {
                if (required) {
                    throw new IllegalArgumentException("缺少必要参数: " + name);
                }
                return null;
            }
            return convert(value, type);
        }
    }

    static class RequestBodyResolver implements ParamResolver {
        private final Class<?> type;

        RequestBodyResolver(Class<?> type) {
            this.type = type;
        }

        @Override
        public Object resolve(HttpServletRequest req, HttpServletResponse resp,
                               Map<String, String> pathVars) throws IOException {
            String body = readRequestBody(req);
            if (body == null || body.isEmpty()) return null;
            return JSON.parseObject(body, type);
        }
    }

    static class PathVariableResolver implements ParamResolver {
        private final String name;
        private final Class<?> type;

        PathVariableResolver(String name, Class<?> type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public Object resolve(HttpServletRequest req, HttpServletResponse resp,
                               Map<String, String> pathVars) {
            String value = pathVars.get(name);
            if (value == null) return null;
            return convert(value, type);
        }
    }

    static class ServletRequestResolver implements ParamResolver {
        @Override
        public Object resolve(HttpServletRequest req, HttpServletResponse resp,
                               Map<String, String> pathVars) {
            return req;
        }
    }

    static class ServletResponseResolver implements ParamResolver {
        @Override
        public Object resolve(HttpServletRequest req, HttpServletResponse resp,
                               Map<String, String> pathVars) {
            return resp;
        }
    }

    static class NullResolver implements ParamResolver {
        @Override
        public Object resolve(HttpServletRequest req, HttpServletResponse resp,
                               Map<String, String> pathVars) {
            return null;
        }
    }

    private static Object convert(String value, Class<?> type) {
        if (type == String.class) return value;
        if (type == int.class || type == Integer.class) return Integer.parseInt(value);
        if (type == long.class || type == Long.class) return Long.parseLong(value);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);
        if (type == double.class || type == Double.class) return Double.parseDouble(value);
        return value;
    }
}
