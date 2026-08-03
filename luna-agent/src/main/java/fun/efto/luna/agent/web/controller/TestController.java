package fun.efto.luna.agent.web.controller;

import com.alibaba.fastjson.JSONObject;
import fun.efto.luna.agent.clazz.ClassScanner;
import fun.efto.luna.agent.clazz.LoadedClass;
import fun.efto.luna.agent.clazz.ClassResourceHelper;
import fun.efto.luna.agent.web.MethodInvokeService;
import fun.efto.luna.agent.web.vo.HealthVO;
import fun.efto.luna.agent.web.vo.InvokeResultVO;
import fun.efto.luna.core.infra.web.ApiResult;
import fun.efto.luna.core.infra.web.Controller;
import fun.efto.luna.core.infra.web.GetMapping;
import fun.efto.luna.core.infra.web.PostMapping;
import fun.efto.luna.core.infra.web.RequestBody;
import fun.efto.luna.core.infra.web.RequestMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/09 10:00
 */
@Controller
@RequestMapping("/test")
public class TestController {
    private final ClassScanner classScanner;
    private final ClassResourceHelper classResourceHelper;
    private final MethodInvokeService invokeService;

    public TestController(ClassScanner classScanner, ClassResourceHelper classResourceHelper) {
        this.classScanner = classScanner;
        this.classResourceHelper = classResourceHelper;
        this.invokeService = new MethodInvokeService(classResourceHelper);
    }

    @GetMapping("/health")
    public ApiResult health() {
        return ApiResult.ok(new HealthVO("ok", System.currentTimeMillis(),
                classResourceHelper.getLoadedClassCount()));
    }

    @GetMapping("/classes")
    public ApiResult classes() {
        Map<String, Set<LoadedClass>> loadedClasses = classScanner.getLoadedClasses();
        List<ClassInfoVO> classList = new ArrayList<>();
        for (Map.Entry<String, Set<LoadedClass>> entry : loadedClasses.entrySet()) {
            for (LoadedClass loadedClass : entry.getValue()) {
                classList.add(new ClassInfoVO(loadedClass.getClassName(), entry.getKey()));
            }
        }
        return ApiResult.ok(classList);
    }

    @PostMapping("/invoke")
    public ApiResult invoke(@RequestBody JSONObject cmd) {
        String className = cmd.getString("className");
        String methodName = cmd.getString("methodName");

        if (className == null || methodName == null) {
            return ApiResult.fail("缺少 className 或 methodName");
        }

        MethodInvokeService.InvokeResult result = invokeService.invoke(className, methodName);
        if (result.isSuccess()) {
            return ApiResult.ok(InvokeResultVO.success(result.getResult(), result.getOutput()));
        } else {
            return ApiResult.fail(result.getError(),
                    InvokeResultVO.failure(result.getError(), result.getOutput()), 400);
        }
    }

    /**
     * 类信息 VO，替代 Map<String, String>
     */
    private static class ClassInfoVO {
        private final String className;
        private final String classLoader;

        ClassInfoVO(String className, String classLoader) {
            this.className = className;
            this.classLoader = classLoader;
        }

        public String getClassName() { return className; }
        public String getClassLoader() { return classLoader; }
    }
}
