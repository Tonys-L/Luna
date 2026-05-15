package fun.efto.luna.agent.web.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import fun.efto.luna.agent.web.ClassResourceHelper;
import fun.efto.luna.agent.web.mvc.ApiResult;
import fun.efto.luna.agent.web.mvc.Controller;
import fun.efto.luna.agent.web.mvc.GetMapping;
import fun.efto.luna.agent.web.mvc.PostMapping;
import fun.efto.luna.agent.web.mvc.RequestBody;
import fun.efto.luna.agent.web.mvc.RequestMapping;
import fun.efto.luna.agent.web.mvc.RequestParam;
import fun.efto.luna.agent.web.vo.InjectionCommand;
import fun.efto.luna.core.InjectionExecutor;
import fun.efto.luna.core.InstrumentationHolder;
import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.InjectionPointRegistry;
import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.injection.code.type.CodeType;
import fun.efto.luna.core.injection.target.InjectionTarget;
import fun.efto.luna.core.injection.target.LineNumberTarget;
import fun.efto.luna.core.injection.target.MethodTarget;
import fun.efto.luna.core.injection.target.type.InjectionType;
import fun.efto.luna.core.injection.target.type.LineNumberInjectionType;
import fun.efto.luna.core.injection.target.type.MethodInjectionType;
import fun.efto.luna.core.plugin.InjectionTypeRegistry;
import fun.efto.luna.core.testing.InjectionTestHarness;
import fun.efto.luna.core.testing.TestResult;
import fun.efto.luna.core.transformer.ClassTransformer;
import fun.efto.luna.core.transformer.DefaultClassTransformer;
import fun.efto.luna.core.transformer.InjectionResult;
import fun.efto.luna.core.transformer.TransformerResult;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/09 10:00
 */
@Controller
@RequestMapping("/inject")
public class InjectionController {
    private static final Logger LOGGER = LoggerFactory.getLogger(InjectionController.class);

    private final InjectionExecutor injectionExecutor;
    private final ClassResourceHelper classResourceHelper;
    private final InjectionTestHarness testHarness;

    public InjectionController(InjectionExecutor injectionExecutor,
                                ClassResourceHelper classResourceHelper) {
        this.injectionExecutor = injectionExecutor;
        this.classResourceHelper = classResourceHelper;
        this.testHarness = new InjectionTestHarness(
                classResourceHelper.getInstrumentation());
    }

    @GetMapping("/list")
    public ApiResult injectList(@RequestParam("class") String className) {
        if (className == null || className.isEmpty()) {
            return ApiResult.fail("Missing class parameter");
        }

        List<InjectionPoint> points = InjectionPointRegistry.getInstance().getAllInjectionPoints(className);
        JSONArray array = new JSONArray();
        for (InjectionPoint point : points) {
            JSONObject obj = new JSONObject();
            obj.put("id", point.getId());
            obj.put("type", point.getInjectionType().toString());
            obj.put("method", point.getTarget().getMethodName());
            obj.put("code", point.getCode().getCode());
            obj.put("codeType", point.getCodeType().toString());
            if (point.getTarget() instanceof fun.efto.luna.core.injection.target.LineNumberTarget) {
                fun.efto.luna.core.injection.target.LineNumberTarget lnt =
                        (fun.efto.luna.core.injection.target.LineNumberTarget) point.getTarget();
                obj.put("lineNumber", lnt.getLineNumber());
            }
            array.add(obj);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("injections", array);
        return ApiResult.ok(data);
    }

    @PostMapping
    public ApiResult inject(@RequestBody InjectionCommand cmd) {
        if (cmd == null || !cmd.isValid()) {
            return ApiResult.fail("缺少必要参数");
        }

        String paramError = validateParamReferences(cmd.getCode(), cmd.getDesc());
        if (paramError != null) return ApiResult.fail(paramError);

        String localVarError = validateLocalVarReferences(cmd.getCode(), cmd.getClazz(),
                cmd.getMethod(), cmd.getDesc(), cmd.getLineNumber(), cmd.getInjectionType());
        if (localVarError != null) return ApiResult.fail(localVarError);

        if (isLineInjection(cmd) && (cmd.getMethod() == null || cmd.getMethod().isEmpty())) {
            return ApiResult.fail("行号注入需要指定方法名(method)");
        }

        try {
            InjectionPoint injectionPoint = buildInjectionPoint(cmd);
            List<InjectionResult> results = injectionExecutor.execute(injectionPoint);

            boolean allSuccess = results.stream().allMatch(InjectionResult::isSuccess);
            Map<String, Object> data = new HashMap<>();
            data.put("success", allSuccess);
            JSONArray resultsArray = new JSONArray();
            for (InjectionResult r : results) {
                JSONObject rObj = new JSONObject();
                rObj.put("success", r.isSuccess());
                rObj.put("type", r.getInjectionType());
                rObj.put("method", r.getMethodName() != null ? r.getMethodName() : "");
                rObj.put("message", r.getMessage() != null ? r.getMessage() : "");
                resultsArray.add(rObj);
            }
            data.put("results", resultsArray);
            data.put("injectionPointId", injectionPoint.getId());
            return allSuccess ? ApiResult.ok(data) : failWith("注入失败", data, 500);
        } catch (Throwable t) {
            LOGGER.error("注入过程发生严重错误", t);
            return ApiResult.fail("注入失败: " + (t.getMessage() != null ? t.getMessage() : t.getClass().getName()), 500);
        }
    }

    @PostMapping("/test")
    public ApiResult injectTest(@RequestBody InjectionCommand cmd) {
        if (cmd == null || !cmd.isValid()) {
            return ApiResult.fail("缺少必要参数");
        }

        String paramError = validateParamReferences(cmd.getCode(), cmd.getDesc());
        if (paramError != null) return ApiResult.fail(paramError);

        String localVarError = validateLocalVarReferences(cmd.getCode(), cmd.getClazz(),
                cmd.getMethod(), cmd.getDesc(), cmd.getLineNumber(), cmd.getInjectionType());
        if (localVarError != null) return ApiResult.fail(localVarError);

        if (isLineInjection(cmd) && (cmd.getMethod() == null || cmd.getMethod().isEmpty())) {
            return ApiResult.fail("行号注入需要指定方法名(method)");
        }

        JSONObject steps = new JSONObject();
        boolean allSuccess = true;

        TestResult dryRunResult = testHarness.dryRun(
                cmd.getClazz(), cmd.getMethod(), cmd.getDesc(),
                cmd.getInjectionType(), cmd.getCode()
        );
        JSONObject dryRunStep = new JSONObject();
        dryRunStep.put("success", dryRunResult.isSuccess());
        if (dryRunResult.isSuccess()) {
            dryRunStep.put("bytecodeSize", dryRunResult.getGeneratedBytecode() != null
                    ? dryRunResult.getGeneratedBytecode().length : 0);
            dryRunStep.put("originalBytecodeSize", dryRunResult.getOriginalBytecodeSize());
            dryRunStep.put("message", dryRunResult.getOutput());
        } else {
            dryRunStep.put("error", dryRunResult.getError());
            allSuccess = false;
        }
        steps.put("dryRun", dryRunStep);

        if (dryRunResult.isSuccess()) {
            try {
                InjectionPoint injectionPoint = buildInjectionPoint(cmd);
                injectionExecutor.execute(injectionPoint);
                JSONObject injectStep = new JSONObject();
                injectStep.put("success", true);
                injectStep.put("message", "Retransform completed");
                steps.put("inject", injectStep);
            } catch (Exception e) {
                JSONObject injectStep = new JSONObject();
                injectStep.put("success", false);
                injectStep.put("error", e.getMessage());
                steps.put("inject", injectStep);
                allSuccess = false;
            }
        } else {
            steps.put("inject", new JSONObject().fluentPut("skipped", true).fluentPut("reason", "dry-run failed"));
        }

        if (allSuccess) {
            TestResult verifyResult = testHarness.verifyOnly(cmd.getClazz(), cmd.getMethod());
            JSONObject verifyStep = new JSONObject();
            verifyStep.put("success", verifyResult.isSuccess());
            if (verifyResult.isSuccess()) {
                verifyStep.put("output", verifyResult.getOutput());
            } else {
                verifyStep.put("error", verifyResult.getError());
                allSuccess = false;
            }
            steps.put("verify", verifyStep);

            if (verifyResult.isSuccess()) {
                String expectedContent = cmd.getCode();
                if (expectedContent.startsWith("log:")) {
                    expectedContent = expectedContent.substring(4);
                }
                boolean found = verifyResult.getOutput() != null
                        && verifyResult.getOutput().contains(expectedContent);
                JSONObject validateStep = new JSONObject();
                validateStep.put("success", found);
                validateStep.put("expected", expectedContent);
                validateStep.put("actual", verifyResult.getOutput());
                if (!found) allSuccess = false;
                steps.put("validate", validateStep);
            }
        } else {
            steps.put("verify", new JSONObject().fluentPut("skipped", true).fluentPut("reason", "inject failed"));
            steps.put("validate", new JSONObject().fluentPut("skipped", true).fluentPut("reason", "inject failed"));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("success", allSuccess);
        data.put("steps", steps);
        if (!allSuccess) {
            data.put("error", "One or more steps failed");
        }
        return allSuccess ? ApiResult.ok(data) : failWith("One or more steps failed", data, 400);
    }

    @PostMapping("/dry-run")
    public ApiResult dryRun(@RequestBody InjectionCommand cmd) {
        if (cmd == null || !cmd.isValid()) {
            return ApiResult.fail("缺少必要参数");
        }

        String paramError = validateParamReferences(cmd.getCode(), cmd.getDesc());
        if (paramError != null) return ApiResult.fail(paramError);

        String localVarError = validateLocalVarReferences(cmd.getCode(), cmd.getClazz(),
                cmd.getMethod(), cmd.getDesc(), cmd.getLineNumber(), cmd.getInjectionType());
        if (localVarError != null) return ApiResult.fail(localVarError);

        try {
            InjectionPoint injectionPoint = buildInjectionPoint(cmd);

            Class<?> targetClass = classResourceHelper.findLoadedClass(cmd.getClazz());
            if (targetClass == null) {
                return ApiResult.fail("类未加载: " + cmd.getClazz());
            }

            byte[] originalBytes = classResourceHelper.loadClassBytes(cmd.getClazz());

            ClassTransformer classTransformer = new DefaultClassTransformer();
            TransformerResult result = classTransformer.transform(injectionPoint, cmd.getClazz(), originalBytes);

            Map<String, Object> data = new HashMap<>();
            if (result.isTransformed()) {
                data.put("bytecodeSize", result.getBytecode().length);
                data.put("originalSize", originalBytes.length);
                data.put("injectedMethod", cmd.getMethod());
                data.put("message", result.getMessage());
                return ApiResult.ok(data);
            } else {
                return ApiResult.fail(result.getMessage());
            }
        } catch (Exception e) {
            return ApiResult.fail("预览失败: " + e.getMessage(),
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/verify")
    public ApiResult verify(@RequestBody InjectionCommand cmd) {
        if (cmd == null || !cmd.isValid()) {
            return ApiResult.fail("缺少必要参数");
        }

        try {
            TestResult testResult = testHarness.testInjection(
                    cmd.getClazz(), cmd.getMethod(), cmd.getDesc(),
                    cmd.getInjectionType(), cmd.getCode()
            );
            Map<String, Object> data = new HashMap<>();
            if (testResult.isSuccess()) {
                data.put("output", testResult.getOutput());
                return ApiResult.ok(data);
            } else {
                data.put("error", testResult.getError());
                return failWith(testResult.getError(), data, 400);
            }
        } catch (Exception e) {
            return ApiResult.fail("注入验证失败: " + e.getMessage(),
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/remove")
    public ApiResult remove(@RequestBody JSONObject params) {
        String id = params.getString("id");
        if (id == null || id.isEmpty()) {
            return ApiResult.fail("Missing id parameter");
        }

        String className = InjectionPointRegistry.getInstance().removeById(id);
        if (className == null) {
            return ApiResult.fail("Injection point not found: " + id,
                    HttpServletResponse.SC_NOT_FOUND);
        }

        List<InjectionPoint> remaining = InjectionPointRegistry.getInstance().getAllInjectionPoints(className);
        if (!remaining.isEmpty()) {
            try {
                injectionExecutor.execute(remaining.get(0));
            } catch (Exception ignored) {
            }
        } else {
            try {
                Class<?> clazz = classResourceHelper.findLoadedClass(className);
                if (clazz != null) {
                    InstrumentationHolder.retransformClasses(clazz);
                    LOGGER.info("Successfully retransformed class {} after removing all injections", className);
                } else {
                    LOGGER.warn("Could not find class {} for retransformation after injection removal", className);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to retransform class {} after injection removal", className, e);
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("success", true);
        data.put("removedId", id);
        data.put("className", className);
        return ApiResult.ok(data);
    }

    private InjectionPoint buildInjectionPoint(InjectionCommand cmd) {
        InjectionType injectionType = InjectionTypeRegistry.resolve(cmd.getInjectionType());
        InjectionTarget target;
        if (injectionType instanceof LineNumberInjectionType) {
            int lineNumber = cmd.getLineNumber() != null ? cmd.getLineNumber() : 0;
            LineNumberInjectionType typeWithLine = ((LineNumberInjectionType) injectionType).withLineNumber(lineNumber);
            target = new LineNumberTarget(typeWithLine,
                    cmd.getClazz(), lineNumber, 0, cmd.getMethod(), cmd.getDesc());
        } else {
            target = new MethodTarget((MethodInjectionType) injectionType,
                    cmd.getClazz(), cmd.getMethod(), cmd.getDesc());
        }
        InjectableCode code = new InjectableCode() {
            @Override
            public String getCode() { return cmd.getCode(); }
            @Override
            public CodeType getCodeType() {
                try {
                    return CodeType.valueOf(cmd.getCodeType().toUpperCase());
                } catch (Exception e) {
                    return CodeType.EXPRESSION;
                }
            }
        };
        return new InjectionPoint(target, code);
    }

    private boolean isLineInjection(InjectionCommand cmd) {
        return cmd.getInjectionType() != null
                && (cmd.getInjectionType().equals("LINE_BEFORE")
                || cmd.getInjectionType().equals("LINE_AFTER"));
    }

    private String validateParamReferences(String code, String methodDescriptor) {
        if (code == null || !code.contains("$")) return null;

        Matcher numMatcher = Pattern.compile("\\$(\\d+)").matcher(code);
        int maxRef = 0;
        while (numMatcher.find()) {
            int ref = Integer.parseInt(numMatcher.group(1));
            if (ref > maxRef) maxRef = ref;
        }

        if (maxRef > 0) {
            if (methodDescriptor == null || methodDescriptor.isEmpty()) {
                return "使用参数引用 $" + maxRef + " 需要提供方法描述符(desc)";
            }
            Type[] argTypes = Type.getArgumentTypes(methodDescriptor);
            if (maxRef > argTypes.length) {
                return "参数引用 $" + maxRef + " 超出范围，方法只有 " + argTypes.length + " 个参数";
            }
        }
        return null;
    }

    private String validateLocalVarReferences(String code, String className,
                                               String methodName, String methodDesc,
                                               Integer lineNumber, String injectionType) {
        if (code == null || !code.contains("$")) return null;
        if (lineNumber == null || lineNumber < 1) return null;

        Matcher varMatcher = Pattern.compile("\\$([a-zA-Z_]\\w*)").matcher(code);
        Set<String> varNames = new HashSet<>();
        while (varMatcher.find()) {
            varNames.add(varMatcher.group(1));
        }
        if (varNames.isEmpty()) return null;

        try {
            Class<?> targetClass = classResourceHelper.findLoadedClass(className);
            if (targetClass == null) return null;

            byte[] bytecode = classResourceHelper.loadClassBytes(className);
            org.objectweb.asm.ClassReader cr = new org.objectweb.asm.ClassReader(bytecode);
            Set<String> availableVars = new HashSet<>();

            cr.accept(new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9) {
                @Override
                public org.objectweb.asm.MethodVisitor visitMethod(int access, String name,
                                                                     String descriptor, String signature,
                                                                     String[] exceptions) {
                    if (!name.equals(methodName)) return null;
                    if (methodDesc != null && !methodDesc.isEmpty() && !descriptor.equals(methodDesc)) return null;

                    return new org.objectweb.asm.MethodVisitor(org.objectweb.asm.Opcodes.ASM9) {
                        final Map<org.objectweb.asm.Label, Integer> labelLines = new LinkedHashMap<>();
                        final List<Object[]> localVarEntries = new java.util.ArrayList<>();

                        @Override
                        public void visitLineNumber(int line, org.objectweb.asm.Label start) {
                            labelLines.put(start, line);
                        }

                        @Override
                        public void visitLocalVariable(String vName, String vDesc, String vSig,
                                                        org.objectweb.asm.Label start, org.objectweb.asm.Label end,
                                                        int index) {
                            localVarEntries.add(new Object[]{vName, start, end});
                        }

                        @Override
                        public void visitEnd() {
                            for (Object[] entry : localVarEntries) {
                                String vName = (String) entry[0];
                                org.objectweb.asm.Label startLabel = (org.objectweb.asm.Label) entry[1];
                                org.objectweb.asm.Label endLabel = (org.objectweb.asm.Label) entry[2];

                                Integer startLine = labelLines.get(startLabel);
                                Integer endLine = labelLines.get(endLabel);

                                boolean visible;
                                if (startLine == null) {
                                    visible = true;
                                } else if (lineNumber < startLine) {
                                    visible = false;
                                } else if (endLine == null || endLine <= startLine) {
                                    visible = true;
                                } else {
                                    visible = lineNumber <= endLine;
                                }

                                if (visible) {
                                    availableVars.add(vName);
                                }
                            }
                        }
                    };
                }
            }, 0);

            for (String varName : varNames) {
                if (!availableVars.contains(varName)) {
                    return "local variable $" + varName + " is not visible at line " + lineNumber;
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private ApiResult failWith(String error, Object data, int status) {
        ApiResult result = ApiResult.fail(error, status);
        result.setData(data);
        return result;
    }
}
