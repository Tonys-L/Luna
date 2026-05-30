package fun.efto.luna.agent.web.controller;

import fun.efto.luna.agent.clazz.ClassScanner;
import fun.efto.luna.agent.clazz.LoadedClass;
import fun.efto.luna.agent.clazz.ClassResourceHelper;
import fun.efto.luna.agent.web.vo.DecompileResultVO;
import fun.efto.luna.agent.web.vo.LocalVariablesVO;
import fun.efto.luna.core.web.ApiResult;
import fun.efto.luna.core.web.Controller;
import fun.efto.luna.core.web.GetMapping;
import fun.efto.luna.core.web.RequestParam;
import fun.efto.luna.core.analyzer.AnalyzerRegistry;
import fun.efto.luna.core.analyzer.AnalyzerType;
import fun.efto.luna.core.analyzer.ClassAnalysisResult;
import fun.efto.luna.core.analyzer.ClassAnalyzer;
import fun.efto.luna.core.decompile.DecompilerFactory;
import fun.efto.luna.core.injection.InjectionQuery;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/09 10:00
 */
@Controller
public class ClassController {
    private final ClassScanner classScanner;
    private final ClassResourceHelper classResourceHelper;
    private final InjectionQuery injectionQuery;

    public ClassController(ClassScanner classScanner, ClassResourceHelper classResourceHelper,
                           InjectionQuery injectionQuery) {
        this.classScanner = classScanner;
        this.classResourceHelper = classResourceHelper;
        this.injectionQuery = injectionQuery;
    }

    @GetMapping("/classes")
    public ApiResult list(@RequestParam(value = "refresh", required = false) String refresh) {
        Map<String, Set<LoadedClass>> result;
        if ("true".equals(refresh)) {
            result = classScanner.scan();
        } else {
            result = classScanner.getLoadedClasses();
        }

        result.values().forEach(classes -> {
            classes.forEach(lc -> {
                lc.setInjectionCount(injectionQuery.getInjectionCount(lc.getClassName()));
            });
        });

        return ApiResult.ok(result);
    }

    @GetMapping("/decompile")
    public ApiResult decompile(@RequestParam("class") String className) {
        if (className == null || className.isEmpty()) {
            return ApiResult.fail("Missing class parameter");
        }
        try {
            String decompiledCode = DecompilerFactory.getDecompiler().decompile(className);
            return ApiResult.ok(new DecompileResultVO(decompiledCode));
        } catch (Exception e) {
            return ApiResult.fail("Decompile failed: " + e.getMessage(),
                    500);
        }
    }

    @GetMapping("/analysis")
    public ApiResult analysis(@RequestParam("class") String className) {
        Optional<ClassAnalyzer> classAnalyzer = AnalyzerRegistry.getInstance()
                .get(AnalyzerType.valueOf("ASM"));
        if (classAnalyzer.isPresent()) {
            try {
                byte[] bytes = classResourceHelper.loadClassBytes(className);
                ClassAnalysisResult analyze = classAnalyzer.get().analyze(bytes);
                return ApiResult.ok(analyze);
            } catch (IOException e) {
                return ApiResult.fail("类加载失败", 400);
            }
        }
        return ApiResult.fail("No analyzer available", 400);
    }

    @GetMapping("/line-numbers")
    public ApiResult lineNumbers(@RequestParam("class") String className) {
        if (className == null || className.isEmpty()) {
            return ApiResult.fail("缺少class参数");
        }

        try {
            Class<?> targetClass = classResourceHelper.findLoadedClass(className);
            if (targetClass == null) {
                return ApiResult.fail("类未加载: " + className, 404);
            }

            byte[] bytecode = classResourceHelper.loadClassBytes(className);
            ClassAnalyzer analyzer = getAnalyzer();
            Map<String, List<Integer>> result = analyzer.getLineNumbers(bytecode);
            return ApiResult.ok(result);
        } catch (Exception e) {
            return ApiResult.fail("读取行号表失败: " + e.getMessage(),
                    500);
        }
    }

    @GetMapping("/local-variables")
    public ApiResult localVariables(@RequestParam("class") String className,
                                    @RequestParam("method") String methodName,
                                    @RequestParam(value = "desc", required = false) String methodDesc,
                                    @RequestParam("line") String lineStr) {
        if (className == null || className.isEmpty()) {
            return ApiResult.fail("缺少class参数");
        }
        if (methodName == null || methodName.isEmpty()) {
            return ApiResult.fail("缺少method参数");
        }
        if (lineStr == null || lineStr.isEmpty()) {
            return ApiResult.fail("缺少line参数");
        }

        int lineNumber;
        try {
            lineNumber = Integer.parseInt(lineStr);
        } catch (NumberFormatException e) {
            return ApiResult.fail("line参数必须是整数");
        }

        try {
            Class<?> targetClass = classResourceHelper.findLoadedClass(className);
            if (targetClass == null) {
                return ApiResult.fail("类未加载: " + className, 404);
            }

            byte[] bytecode = classResourceHelper.loadClassBytes(className);
            ClassAnalyzer analyzer = getAnalyzer();
            List<ClassAnalysisResult.LocalVariableInfo> visibleVars =
                    analyzer.getVisibleLocalVariables(bytecode, methodName, methodDesc, lineNumber);

            List<LocalVariablesVO.LocalVarInfo> variables = visibleVars.stream()
                    .map(v -> new LocalVariablesVO.LocalVarInfo(v.getName(), v.getDescriptor(), v.getSlot()))
                    .collect(Collectors.toList());

            return ApiResult.ok(new LocalVariablesVO(variables));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("method not found")) {
                return ApiResult.fail(e.getMessage(), 404);
            }
            return ApiResult.fail("failed to read local variable table: " + e.getMessage(),
                    500);
        } catch (Exception e) {
            return ApiResult.fail("failed to read local variable table: " + e.getMessage(),
                    500);
        }
    }

    private ClassAnalyzer getAnalyzer() {
        return AnalyzerRegistry.getInstance()
                .get(AnalyzerType.valueOf("ASM"))
                .orElseThrow(() -> new IllegalStateException("No ASM analyzer available"));
    }
}
