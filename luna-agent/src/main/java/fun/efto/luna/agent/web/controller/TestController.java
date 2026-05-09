package fun.efto.luna.agent.web.controller;

import com.alibaba.fastjson.JSONObject;
import fun.efto.luna.agent.clazz.ClassScanner;
import fun.efto.luna.agent.clazz.LoadedClass;
import fun.efto.luna.agent.web.ClassResourceHelper;
import fun.efto.luna.agent.web.mvc.ApiResult;
import fun.efto.luna.agent.web.mvc.Controller;
import fun.efto.luna.agent.web.mvc.GetMapping;
import fun.efto.luna.agent.web.mvc.PostMapping;
import fun.efto.luna.agent.web.mvc.RequestBody;
import fun.efto.luna.agent.web.mvc.RequestMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/09 10:00
 */
@Controller
@RequestMapping("/test")
public class TestController {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestController.class);
    private final Object outputLock = new Object();
    private final ClassScanner classScanner;
    private final ClassResourceHelper classResourceHelper;

    public TestController(ClassScanner classScanner, ClassResourceHelper classResourceHelper) {
        this.classScanner = classScanner;
        this.classResourceHelper = classResourceHelper;
    }

    @GetMapping("/health")
    public ApiResult health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "ok");
        data.put("timestamp", System.currentTimeMillis());
        data.put("loadedClasses", classResourceHelper.getInstrumentation().getAllLoadedClasses().length);
        return ApiResult.ok(data);
    }

    @GetMapping("/classes")
    public ApiResult classes() {
        Map<String, Set<LoadedClass>> loadedClasses = classScanner.getLoadedClasses();
        List<Map<String, String>> classList = new ArrayList<>();
        for (Map.Entry<String, Set<LoadedClass>> entry : loadedClasses.entrySet()) {
            for (LoadedClass loadedClass : entry.getValue()) {
                Map<String, String> item = new HashMap<>();
                item.put("className", loadedClass.getClassName());
                item.put("classLoader", entry.getKey());
                classList.add(item);
            }
        }
        return ApiResult.ok(classList);
    }

    @GetMapping("/debug")
    public Object debug() {
        Class<?>[] allLoadedClasses = classResourceHelper.getInstrumentation().getAllLoadedClasses();
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
            if (clazz.getName().contains("User") && !clazz.getName().startsWith("java")
                    && !clazz.getName().startsWith("sun") && !clazz.getName().startsWith("com.sun")
                    && !clazz.getName().startsWith("javax")) {
                sb.append(clazz.getName()).append(" loader=").append(clazz.getClassLoader()).append("\n");
                userCount++;
            }
        }
        sb.append("\nUser class count: ").append(userCount).append("\n");
        return sb.toString();
    }

    @PostMapping("/invoke")
    public ApiResult invoke(@RequestBody JSONObject cmd) {
        String className = cmd.getString("className");
        String methodName = cmd.getString("methodName");

        if (className == null || methodName == null) {
            return ApiResult.fail("缺少 className 或 methodName");
        }

        synchronized (outputLock) {
            PrintStream originalOut = System.out;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream captureStream = new PrintStream(baos, true);

            try {
                Class<?> targetClass = classResourceHelper.findLoadedClass(className);
                if (targetClass == null) {
                    return ApiResult.fail("类未加载: " + className);
                }

                Method targetMethod = null;
                for (Method m : targetClass.getDeclaredMethods()) {
                    if (m.getName().equals(methodName)) {
                        targetMethod = m;
                        break;
                    }
                }

                if (targetMethod == null) {
                    return ApiResult.fail("方法不存在: " + methodName);
                }

                targetMethod.setAccessible(true);

                Object instance = null;
                if (!Modifier.isStatic(targetMethod.getModifiers())) {
                    try {
                        instance = targetClass.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        instance = targetClass.getDeclaredConstructor(String.class, int.class, String.class)
                                .newInstance("test", 1, "test@test.com");
                    }
                }

                System.setOut(captureStream);
                Object result = null;
                try {
                    result = targetMethod.invoke(instance);
                } finally {
                    System.setOut(originalOut);
                }

                Map<String, Object> data = new HashMap<>();
                data.put("result", result != null ? result.toString() : "null");
                data.put("output", baos.toString());
                return ApiResult.ok(data);
            } catch (Exception e) {
                LOGGER.error("Invoke failed", e);
                Map<String, Object> data = new HashMap<>();
                data.put("error", e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
                data.put("output", baos.toString());
                return failWith(e.getCause() != null ? e.getCause().getMessage() : e.getMessage(), data);
            }
        }
    }

    private ApiResult failWith(String error, Object data) {
        ApiResult result = ApiResult.fail(error);
        result.setData(data);
        return result;
    }
}
