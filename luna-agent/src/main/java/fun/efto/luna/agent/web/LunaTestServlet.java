package fun.efto.luna.agent.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import fun.efto.luna.agent.clazz.ClassScanner;
import fun.efto.luna.agent.clazz.LoadedClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/01 12:00
 */
public class LunaTestServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(LunaTestServlet.class);
    private final Object outputLock = new Object();
    private final ClassScanner classScanner;
    private final Instrumentation instrumentation;

    public LunaTestServlet(ClassScanner classScanner, Instrumentation instrumentation) {
        this.classScanner = classScanner;
        this.instrumentation = instrumentation;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        String pathInfo = request.getPathInfo();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if ("/health".equals(pathInfo)) {
            handleHealth(response);
        } else if ("/classes".equals(pathInfo)) {
            handleClasses(response);
        } else if ("/debug".equals(pathInfo)) {
            handleDebug(response);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{\"error\":\"接口不存在\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        String pathInfo = request.getPathInfo();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if ("/invoke".equals(pathInfo)) {
            handleInvoke(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{\"error\":\"接口不存在\"}");
        }
    }

    private void handleHealth(HttpServletResponse response) throws IOException {
        JSONObject result = new JSONObject();
        result.put("status", "ok");
        result.put("timestamp", System.currentTimeMillis());
        result.put("loadedClasses", instrumentation.getAllLoadedClasses().length);
        response.getWriter().write(result.toJSONString());
    }

    private void handleClasses(HttpServletResponse response) throws IOException {
        Map<String, Set<LoadedClass>> loadedClasses = classScanner.getLoadedClasses();
        List<JSONObject> classList = new ArrayList<>();
        for (Map.Entry<String, Set<LoadedClass>> entry : loadedClasses.entrySet()) {
            for (LoadedClass loadedClass : entry.getValue()) {
                JSONObject item = new JSONObject();
                item.put("className", loadedClass.getClassName());
                item.put("classLoader", entry.getKey());
                classList.add(item);
            }
        }
        response.getWriter().write(JSON.toJSONString(classList));
    }

    private void handleInvoke(HttpServletRequest request, HttpServletResponse response) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        JSONObject cmd = JSON.parseObject(sb.toString());
        String className = cmd.getString("className");
        String methodName = cmd.getString("methodName");

        if (className == null || methodName == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"error\":\"缺少 className 或 methodName\"}");
            return;
        }

        synchronized (outputLock) {
            PrintStream originalOut = System.out;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream captureStream = new PrintStream(baos, true);

            try {
                Class<?> targetClass = findLoadedClass(className);
                if (targetClass == null) {
                    response.getWriter().write("{\"success\":false,\"error\":\"类未加载: " + className + "\"}");
                    return;
                }

                Object instance = null;
                Method targetMethod = null;

                for (Method m : targetClass.getDeclaredMethods()) {
                    if (m.getName().equals(methodName)) {
                        targetMethod = m;
                        break;
                    }
                }

                if (targetMethod == null) {
                    response.getWriter().write("{\"success\":false,\"error\":\"方法不存在: " + methodName + "\"}");
                    return;
                }

                targetMethod.setAccessible(true);

                if (!java.lang.reflect.Modifier.isStatic(targetMethod.getModifiers())) {
                    try {
                        instance = targetClass.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        instance = targetClass.getDeclaredConstructor(String.class, int.class, String.class).newInstance("test", 1, "test@test.com");
                    }
                }

                System.setOut(captureStream);
                Object result = null;
                try {
                    result = targetMethod.invoke(instance);
                } finally {
                    System.setOut(originalOut);
                }

                JSONObject responseJson = new JSONObject();
                responseJson.put("success", true);
                responseJson.put("result", result != null ? result.toString() : "null");
                responseJson.put("output", baos.toString());
                response.getWriter().write(responseJson.toJSONString());

            } catch (Exception e) {
                LOGGER.error("Invoke failed", e);
                JSONObject responseJson = new JSONObject();
                responseJson.put("success", false);
                responseJson.put("error", e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
                responseJson.put("output", baos.toString());
                response.getWriter().write(responseJson.toJSONString());
            }
        }
    }

    private Class<?> findLoadedClass(String className) {
        Class<?>[] allLoadedClasses = instrumentation.getAllLoadedClasses();
        for (Class<?> clazz : allLoadedClasses) {
            if (clazz.getName().equals(className)) {
                return clazz;
            }
        }
        return null;
    }

    private void handleDebug(HttpServletResponse response) throws IOException {
        Class<?>[] allLoadedClasses = instrumentation.getAllLoadedClasses();
        StringBuilder sb = new StringBuilder();
        sb.append("Total loaded classes: ").append(allLoadedClasses.length).append("\n\n");
        sb.append("Classes containing 'fun.efto':\n");
        for (Class<?> clazz : allLoadedClasses) {
            if (clazz.getName().contains("fun.efto")) {
                sb.append(clazz.getName()).append(" loader=").append(clazz.getClassLoader()).append("\n");
            }
        }
        sb.append("\nClasses containing 'User':\n");
        int userCount = 0;
        for (Class<?> clazz : allLoadedClasses) {
            if (clazz.getName().contains("User") && !clazz.getName().startsWith("java") && !clazz.getName().startsWith("sun") && !clazz.getName().startsWith("com.sun") && !clazz.getName().startsWith("javax")) {
                sb.append(clazz.getName()).append(" loader=").append(clazz.getClassLoader()).append("\n");
                userCount++;
            }
        }
        sb.append("\nUser class count: ").append(userCount).append("\n");
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(sb.toString());
    }
}
