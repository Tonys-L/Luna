package fun.efto.luna.core.web;

import com.alibaba.fastjson.JSON;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
public class DispatcherServlet implements HttpHandler {

    private final Map<String, RouteHandler> exactRoutes = new ConcurrentHashMap<>();
    private final List<PatternRoute> patternRoutes = new ArrayList<>();
    private final Map<Object, Set<String>> controllerRouteKeys = new ConcurrentHashMap<>();

    public void register(Object controller) {
        Class<?> clazz = controller.getClass();
        String classPath = resolveClassPath(clazz);

        Set<String> keys = new java.util.HashSet<>();
        for (Method method : clazz.getDeclaredMethods()) {
            String key = registerMethod(controller, method, classPath);
            if (key != null) {
                keys.add(key);
            }
        }
        controllerRouteKeys.put(controller, keys);
    }

    public void unregister(Object controller) {
        Set<String> keys = controllerRouteKeys.remove(controller);
        if (keys != null) {
            exactRoutes.keySet().removeAll(keys);
            patternRoutes.removeIf(pr -> keys.contains(pr.routeKey));
        }
    }

    private String resolveClassPath(Class<?> clazz) {
        String classPath = "";
        if (clazz.isAnnotationPresent(RequestMapping.class)) {
            classPath = clazz.getAnnotation(RequestMapping.class).value();
        }
        if (clazz.isAnnotationPresent(Controller.class) && classPath.isEmpty()) {
            String ctrlValue = clazz.getAnnotation(Controller.class).value();
            if (!ctrlValue.isEmpty()) {
                classPath = ctrlValue;
            }
        }
        return classPath;
    }

    private String registerMethod(Object controller, Method method, String classPath) {
        GetMapping get = method.getAnnotation(GetMapping.class);
        PostMapping post = method.getAnnotation(PostMapping.class);
        PutMapping put = method.getAnnotation(PutMapping.class);
        DeleteMapping delete = method.getAnnotation(DeleteMapping.class);

        if (get != null) return addRoute("GET", classPath + get.value(), controller, method);
        if (post != null) return addRoute("POST", classPath + post.value(), controller, method);
        if (put != null) return addRoute("PUT", classPath + put.value(), controller, method);
        if (delete != null) return addRoute("DELETE", classPath + delete.value(), controller, method);
        return null;
    }

    private String addRoute(String httpMethod, String path, Object controller, Method method) {
        method.setAccessible(true);
        RouteHandler handler = new RouteHandler(controller, method, buildParamResolvers(method));

        if (path.contains("{")) {
            PatternRoute patternRoute = compilePattern(httpMethod, path, handler);
            patternRoutes.add(patternRoute);
            return patternRoute.routeKey;
        } else {
            String key = httpMethod + ":" + path;
            exactRoutes.put(key, handler);
            return key;
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
            } else if (HttpExchange.class.isAssignableFrom(type)) {
                resolvers[i] = new ExchangeResolver();
            } else {
                resolvers[i] = new NullResolver();
            }
        }
        return resolvers;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String httpMethod = exchange.getRequestMethod();
            String contextPath = exchange.getHttpContext().getPath();
            String fullPath = exchange.getRequestURI().getPath();
            String path = fullPath.substring(contextPath.length());
            if (path.isEmpty()) path = "/";

            RouteMatch match = findRoute(httpMethod, path);
            if (match == null) {
                sendJsonResponse(exchange, 404, ApiResult.fail("Not Found: " + httpMethod + " " + path, 404));
                return;
            }

            Map<String, String> queryParams = parseQuery(exchange.getRequestURI().getRawQuery());
            Object result = match.handler.invoke(exchange, match.pathVariables, queryParams);
            writeResponse(exchange, result);
        } catch (IllegalArgumentException e) {
            sendJsonResponse(exchange, 400, ApiResult.fail(e.getMessage(), 400));
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof IllegalArgumentException) {
                sendJsonResponse(exchange, 400, ApiResult.fail(cause.getMessage(), 400));
            } else {
                sendJsonResponse(exchange, 500, ApiResult.fail("Internal Server Error: " + (cause != null ? cause.getMessage() : e.getMessage()), 500));
            }
        }
    }

    private RouteMatch findRoute(String httpMethod, String path) {
        String key = httpMethod + ":" + path;
        RouteHandler exact = exactRoutes.get(key);
        if (exact != null) {
            return new RouteMatch(exact, new HashMap<>());
        }

        for (PatternRoute pr : patternRoutes) {
            if (!pr.httpMethod.equals(httpMethod)) continue;
            Matcher matcher = pr.pattern.matcher(path);
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

    private Map<String, String> parseQuery(String queryString) {
        Map<String, String> params = new HashMap<>();
        if (queryString == null || queryString.isEmpty()) return params;
        for (String pair : queryString.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                try {
                    String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                    String value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
                    params.put(key, value);
                } catch (java.io.UnsupportedEncodingException ignored) {
                }
            }
        }
        return params;
    }

    private void writeResponse(HttpExchange exchange, Object result) throws IOException {
        if (result == null) {
            sendJsonResponse(exchange, 200, ApiResult.ok(null));
            return;
        }
        if (result instanceof ApiResult) {
            ApiResult apiResult = (ApiResult) result;
            int status = apiResult.getStatus();
            sendJsonResponse(exchange, status, apiResult);
        } else {
            sendJsonResponse(exchange, 200, result);
        }
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = JSON.toJSONString(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return new String(baos.toByteArray(), StandardCharsets.UTF_8);
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

    private static class RouteHandler {
        final Object controller;
        final Method method;
        final ParamResolver[] resolvers;

        RouteHandler(Object controller, Method method, ParamResolver[] resolvers) {
            this.controller = controller;
            this.method = method;
            this.resolvers = resolvers;
        }

        Object invoke(HttpExchange exchange, Map<String, String> pathVars,
                       Map<String, String> queryParams) throws Exception {
            Object[] args = new Object[resolvers.length];
            for (int i = 0; i < resolvers.length; i++) {
                args[i] = resolvers[i].resolve(exchange, pathVars, queryParams);
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
        Object resolve(HttpExchange exchange, Map<String, String> pathVars,
                        Map<String, String> queryParams) throws IOException;
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
        public Object resolve(HttpExchange exchange, Map<String, String> pathVars,
                               Map<String, String> queryParams) {
            String value = queryParams.get(name);
            if (value == null || value.isEmpty()) {
                if (required) {
                    throw new IllegalArgumentException("Missing required parameter: " + name);
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
        public Object resolve(HttpExchange exchange, Map<String, String> pathVars,
                               Map<String, String> queryParams) throws IOException {
            String body = readRequestBody(exchange);
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
        public Object resolve(HttpExchange exchange, Map<String, String> pathVars,
                               Map<String, String> queryParams) {
            String value = pathVars.get(name);
            if (value == null) return null;
            return convert(value, type);
        }
    }

    static class ExchangeResolver implements ParamResolver {
        @Override
        public Object resolve(HttpExchange exchange, Map<String, String> pathVars,
                               Map<String, String> queryParams) {
            return exchange;
        }
    }

    static class NullResolver implements ParamResolver {
        @Override
        public Object resolve(HttpExchange exchange, Map<String, String> pathVars,
                               Map<String, String> queryParams) {
            return null;
        }
    }
}
