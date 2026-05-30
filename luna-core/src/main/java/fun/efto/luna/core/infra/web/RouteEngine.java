package fun.efto.luna.core.infra.web;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTP 容器无关的路由引擎。
 * 负责路由注册、匹配、参数解析和处理器调用，
 * 不依赖任何具体 HTTP 服务器实现。
 *
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/27 12:00
 */
public class RouteEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(RouteEngine.class);

    private final Map<String, RouteHandler> exactRoutes = new ConcurrentHashMap<>();
    private final List<PatternRoute> patternRoutes = new ArrayList<>();

    /**
     * 注册控制器，扫描其注解并建立路由映射
     */
    public void registerController(Object controller) {
        Class<?> clazz = controller.getClass();
        String classPath = resolveClassPath(clazz);

        for (Method method : clazz.getDeclaredMethods()) {
            registerMethod(controller, method, classPath);
        }

        LOGGER.info("Registered controller: {} (prefix={})", clazz.getSimpleName(), classPath);
    }

    /**
     * 注销控制器，移除其所有路由映射
     */
    public void unregisterController(Object controller) {
        String classPath = resolveClassPath(controller.getClass());

        List<String> keysToRemove = new ArrayList<>();
        for (Method method : controller.getClass().getDeclaredMethods()) {
            String key = buildRouteKey(method, classPath);
            if (key != null) {
                keysToRemove.add(key);
            }
        }

        exactRoutes.keySet().removeAll(keysToRemove);
        patternRoutes.removeIf(pr -> keysToRemove.contains(pr.routeKey));
        LOGGER.info("Unregistered controller: {}", controller.getClass().getSimpleName());
    }

    /**
     * 处理请求：匹配路由 → 解析参数 → 调用处理器 → 写入响应
     *
     * @return true 如果找到匹配的路由，false 否者
     */
    public boolean handle(RequestContext ctx) {
        String httpMethod = ctx.getMethod();
        String path = ctx.getPath();

        RouteMatch match = findRoute(httpMethod, path);
        if (match == null) {
            return false;
        }

        try {
            Object result = match.handler.invoke(ctx, match.pathVariables);
            writeResponse(ctx, result);
        } catch (IllegalArgumentException e) {
            writeError(ctx, 400, e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Handler invocation failed: {} {}", httpMethod, path, e);
            Throwable cause = e.getCause();
            LOGGER.error("Exception cause: type={}, message={}", cause != null ? cause.getClass().getName() : "null", cause != null ? cause.getMessage() : "null");
            if (cause instanceof IllegalArgumentException) {
                writeError(ctx, 400, cause.getMessage());
            } else {
                String errorMsg = cause != null ? cause.getMessage() : e.getMessage();
                String errorDetail = cause != null ? cause.getClass().getSimpleName() : e.getClass().getSimpleName();
                writeError(ctx, 500, "服务器内部错误: [" + errorDetail + "] " + errorMsg);
            }
        }
        return true;
    }

    // ==================== 路由注册 ====================

    private String resolveClassPath(Class<?> clazz) {
        String classPath = "";
        if (clazz.isAnnotationPresent(RequestMapping.class)) {
            classPath = clazz.getAnnotation(RequestMapping.class).value();
        }
        if (classPath.isEmpty() && clazz.isAnnotationPresent(Controller.class)) {
            String ctrlValue = clazz.getAnnotation(Controller.class).value();
            if (!ctrlValue.isEmpty()) {
                classPath = ctrlValue;
            }
        }
        return classPath;
    }

    private void registerMethod(Object controller, Method method, String classPath) {
        GetMapping get = method.getAnnotation(GetMapping.class);
        PostMapping post = method.getAnnotation(PostMapping.class);
        PutMapping put = method.getAnnotation(PutMapping.class);
        DeleteMapping delete = method.getAnnotation(DeleteMapping.class);

        if (get != null) addRoute("GET", classPath + get.value(), controller, method);
        if (post != null) addRoute("POST", classPath + post.value(), controller, method);
        if (put != null) addRoute("PUT", classPath + put.value(), controller, method);
        if (delete != null) addRoute("DELETE", classPath + delete.value(), controller, method);
    }

    private String buildRouteKey(Method method, String classPath) {
        GetMapping get = method.getAnnotation(GetMapping.class);
        PostMapping post = method.getAnnotation(PostMapping.class);
        PutMapping put = method.getAnnotation(PutMapping.class);
        DeleteMapping delete = method.getAnnotation(DeleteMapping.class);

        if (get != null) return "GET:" + classPath + get.value();
        if (post != null) return "POST:" + classPath + post.value();
        if (put != null) return "PUT:" + classPath + put.value();
        if (delete != null) return "DELETE:" + classPath + delete.value();
        return null;
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
        String routeKey = httpMethod + ":" + pathPattern;
        return new PatternRoute(httpMethod, Pattern.compile(regex.toString()), varNames, handler, routeKey);
    }

    // ==================== 路由匹配 ====================

    private RouteMatch findRoute(String httpMethod, String pathInfo) {
        String key = httpMethod + ":" + pathInfo;
        RouteHandler exact = exactRoutes.get(key);
        if (exact != null) {
            return new RouteMatch(exact, Collections.emptyMap());
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

    // ==================== 参数解析 ====================

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
                resolvers[i] = new QueryParamResolver(requestParam.value(), requestParam.required(), type);
            } else if (requestBody != null) {
                resolvers[i] = new BodyParamResolver(type);
            } else if (pathVariable != null) {
                resolvers[i] = new PathVarResolver(pathVariable.value(), type);
            } else if (RequestContext.class.isAssignableFrom(type)) {
                resolvers[i] = new ContextResolver();
            } else {
                resolvers[i] = new NullResolver();
            }
        }
        return resolvers;
    }

    private static Object convert(String value, Class<?> type) {
        if (type == String.class) return value;
        if (type == int.class || type == Integer.class) return Integer.parseInt(value);
        if (type == long.class || type == Long.class) return Long.parseLong(value);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);
        if (type == double.class || type == Double.class) return Double.parseDouble(value);
        return value;
    }

    // ==================== 响应写入 ====================

    private void writeResponse(RequestContext ctx, Object result) {
        if (result == null) {
            writeResponse(ctx, ApiResult.ok());
            return;
        }
        if (result instanceof ApiResult) {
            ApiResult apiResult = (ApiResult) result;
            if (apiResult.getStatus() != 200) {
                ctx.setStatus(apiResult.getStatus());
            }
            ctx.writeResponse(JSON.toJSONString(apiResult));
        } else if (result instanceof String) {
            ctx.writeResponse((String) result);
        } else {
            ctx.writeResponse(JSON.toJSONString(result));
        }
    }

    private void writeError(RequestContext ctx, int status, String message) {
        ctx.setStatus(status);
        ctx.writeResponse(JSON.toJSONString(ApiResult.fail(message, status)));
    }

    // ==================== 内部类 ====================

    private static class RouteHandler {
        final Object controller;
        final Method method;
        final ParamResolver[] resolvers;

        RouteHandler(Object controller, Method method, ParamResolver[] resolvers) {
            this.controller = controller;
            this.method = method;
            this.resolvers = resolvers;
        }

        Object invoke(RequestContext ctx, Map<String, String> pathVars) throws Exception {
            Object[] args = new Object[resolvers.length];
            for (int i = 0; i < resolvers.length; i++) {
                args[i] = resolvers[i].resolve(ctx, pathVars);
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
        final String routeKey;

        PatternRoute(String httpMethod, Pattern pattern, List<String> varNames,
                      RouteHandler handler, String routeKey) {
            this.httpMethod = httpMethod;
            this.pattern = pattern;
            this.varNames = varNames;
            this.handler = handler;
            this.routeKey = routeKey;
        }
    }

    interface ParamResolver {
        Object resolve(RequestContext ctx, Map<String, String> pathVars);
    }

    static class QueryParamResolver implements ParamResolver {
        private final String name;
        private final boolean required;
        private final Class<?> type;

        QueryParamResolver(String name, boolean required, Class<?> type) {
            this.name = name;
            this.required = required;
            this.type = type;
        }

        @Override
        public Object resolve(RequestContext ctx, Map<String, String> pathVars) {
            String value = ctx.getQueryParam(name);
            if (value == null || value.isEmpty()) {
                if (required) {
                    throw new IllegalArgumentException("缺少必要参数: " + name);
                }
                return null;
            }
            return convert(value, type);
        }
    }

    static class BodyParamResolver implements ParamResolver {
        private final Class<?> type;

        BodyParamResolver(Class<?> type) {
            this.type = type;
        }

        @Override
        public Object resolve(RequestContext ctx, Map<String, String> pathVars) {
            String body = ctx.getRequestBody();
            if (body == null || body.isEmpty()) return null;
            return JSON.parseObject(body, type);
        }
    }

    static class PathVarResolver implements ParamResolver {
        private final String name;
        private final Class<?> type;

        PathVarResolver(String name, Class<?> type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public Object resolve(RequestContext ctx, Map<String, String> pathVars) {
            String value = pathVars.get(name);
            if (value == null) return null;
            return convert(value, type);
        }
    }

    static class ContextResolver implements ParamResolver {
        @Override
        public Object resolve(RequestContext ctx, Map<String, String> pathVars) {
            return ctx;
        }
    }

    static class NullResolver implements ParamResolver {
        @Override
        public Object resolve(RequestContext ctx, Map<String, String> pathVars) {
            return null;
        }
    }
}
