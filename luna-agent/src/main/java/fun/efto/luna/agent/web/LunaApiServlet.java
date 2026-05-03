package fun.efto.luna.agent.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import fun.efto.luna.agent.clazz.ClassScanner;
import fun.efto.luna.agent.clazz.LoadedClass;
import fun.efto.luna.agent.web.vo.InjectionCommand;
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
import fun.efto.luna.core.injection.target.LineNumberTarget;
import fun.efto.luna.core.injection.target.MethodTarget;
import fun.efto.luna.core.injection.target.type.InjectionType;
import fun.efto.luna.core.injection.target.type.LineNumberInjectionType;
import fun.efto.luna.core.transformer.InjectionResult;
import fun.efto.luna.core.injection.target.type.MethodInjectionType;
import fun.efto.luna.core.rule.InjectionRule;
import fun.efto.luna.core.rule.RuleManager;
import fun.efto.luna.core.testing.InjectionTestHarness;
import fun.efto.luna.core.testing.TestResult;
import fun.efto.luna.core.transformer.ClassTransformer;
import fun.efto.luna.core.transformer.DefaultClassTransformer;
import fun.efto.luna.core.transformer.TransformerResult;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Luna API Servlet - 处理REST API请求
 */
public class LunaApiServlet extends HttpServlet {
    private final InjectionExecutor injectionExecutor;
    private final ClassScanner classScanner;
    private final Instrumentation instrumentation;
    private final InjectionTestHarness testHarness;

    public LunaApiServlet(InjectionExecutor injectionExecutor, ClassScanner classScanner, Instrumentation instrumentation) {
        this.injectionExecutor = injectionExecutor;
        this.classScanner = classScanner;
        this.instrumentation = instrumentation;
        this.testHarness = new InjectionTestHarness(instrumentation);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");

        String pathInfo = request.getPathInfo();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if ("/status".equals(pathInfo)) {
            handleStatus(response);
        } else if (pathInfo != null && pathInfo.startsWith("/classes")) {
            handleClasses(request, response);
        } else if (pathInfo != null && pathInfo.startsWith("/decompile")) {
            handleDecompile(request, response);
        } else if (pathInfo != null && pathInfo.startsWith("/analysis")) {
            handleAnalysis(request, response);
        } else if (pathInfo != null && pathInfo.startsWith("/line-numbers")) {
            handleLineNumbers(request, response);
        } else if (pathInfo != null && pathInfo.startsWith("/rules")) {
            handleRules(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{\"error\":\"接口不存在\"}");
        }
    }

    private void handleAnalysis(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String className = request.getParameter("class");
        Optional<ClassAnalyzer> classAnalyzer = AnalyzerRegistry.getInstance()
                .get(AnalyzerType.valueOf("ASM"));
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
        request.setCharacterEncoding("UTF-8");

        String pathInfo = request.getPathInfo();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if ("/logs".equals(pathInfo)) {
            handleCreateLog(request, response);
        } else if ("/inject/test".equals(pathInfo)) {
            handleInjectTest(request, response, injectionExecutor);
        } else if ("/inject/dry-run".equals(pathInfo)) {
            handleInjectDryRun(request, response, injectionExecutor);
        } else if ("/inject/verify".equals(pathInfo)) {
            handleInjectVerify(request, response, injectionExecutor);
        } else if ("/inject".equals(pathInfo)) {
            handleInject(request, response, injectionExecutor);
        } else if (pathInfo != null && pathInfo.startsWith("/rules")) {
            handleRules(request, response);
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

    private void handleInjectTest(HttpServletRequest request, HttpServletResponse response,
                                   InjectionExecutor injectionExecutor) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        String jsonString = sb.toString();
        InjectionCommand cmd = JSON.parseObject(jsonString, InjectionCommand.class);

        if (cmd == null || !cmd.isValid()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"error\":\"缺少必要参数\"}");
            return;
        }

        String paramError = validateParamReferences(cmd.getCode(), cmd.getDesc());
        if (paramError != null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"error\":\"" + paramError + "\"}");
            return;
        }

        if (cmd.getInjectionType() != null
                && (cmd.getInjectionType().equals("LINE_BEFORE") || cmd.getInjectionType().equals("LINE_AFTER"))) {
            if (cmd.getMethod() == null || cmd.getMethod().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\":false,\"error\":\"行号注入需要指定方法名(method)\"}");
                return;
            }
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
            dryRunStep.put("bytecodeSize", dryRunResult.getGeneratedBytecode() != null ? dryRunResult.getGeneratedBytecode().length : 0);
            dryRunStep.put("originalBytecodeSize", dryRunResult.getOriginalBytecodeSize());
            dryRunStep.put("message", dryRunResult.getOutput());
        } else {
            dryRunStep.put("error", dryRunResult.getError());
            allSuccess = false;
        }
        steps.put("dryRun", dryRunStep);

        if (dryRunResult.isSuccess()) {
            try {
                InjectionType injectionType = resolveInjectionType(cmd.getInjectionType());
                InjectionTarget target;
                if (injectionType instanceof LineNumberInjectionType) {
                    int lineNumber = cmd.getLineNumber() != null ? cmd.getLineNumber() : 0;
                    target = new LineNumberTarget((LineNumberInjectionType) injectionType, cmd.getClazz(), lineNumber, 0, cmd.getMethod(), cmd.getDesc());
                } else {
                    target = new MethodTarget((MethodInjectionType) injectionType, cmd.getClazz(), cmd.getMethod(), cmd.getDesc());
                }
                InjectableCode code = new InjectableCode() {
                    @Override
                    public String getCode() { return cmd.getCode(); }
                    @Override
                    public CodeType getCodeType() { return CodeType.valueOf(cmd.getCodeType()); }
                };
                InjectionPoint injectionPoint = new InjectionPoint(target, code);
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
            TestResult verifyResult = testHarness.verifyOnly(
                    cmd.getClazz(), cmd.getMethod()
            );
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
                boolean found = verifyResult.getOutput() != null && verifyResult.getOutput().contains(expectedContent);
                JSONObject validateStep = new JSONObject();
                validateStep.put("success", found);
                validateStep.put("expected", expectedContent);
                validateStep.put("actual", verifyResult.getOutput());
                if (!found) {
                    allSuccess = false;
                }
                steps.put("validate", validateStep);
            }
        } else {
            steps.put("verify", new JSONObject().fluentPut("skipped", true).fluentPut("reason", "inject failed"));
            steps.put("validate", new JSONObject().fluentPut("skipped", true).fluentPut("reason", "inject failed"));
        }

        JSONObject responseJson = new JSONObject();
        responseJson.put("success", allSuccess);
        responseJson.put("steps", steps);
        if (!allSuccess) {
            responseJson.put("error", "One or more steps failed");
        }
        response.getWriter().write(responseJson.toJSONString());
    }

    private void handleInject(HttpServletRequest request, HttpServletResponse response,
                              InjectionExecutor injectionExecutor) throws IOException {
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
        InjectionCommand cmd = JSON.parseObject(jsonString, InjectionCommand.class);

        if (cmd == null || !cmd.isValid()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"缺少必要参数\"}");
            return;
        }

        String paramError = validateParamReferences(cmd.getCode(), cmd.getDesc());
        if (paramError != null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"" + paramError + "\"}");
            return;
        }

        if (cmd.getInjectionType() != null
                && (cmd.getInjectionType().equals("LINE_BEFORE") || cmd.getInjectionType().equals("LINE_AFTER"))) {
            if (cmd.getMethod() == null || cmd.getMethod().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\":\"行号注入需要指定方法名(method)\"}");
                return;
            }
        }

        try {
            InjectionType injectionType = resolveInjectionType(cmd.getInjectionType());
            InjectionTarget target;

            if (injectionType instanceof LineNumberInjectionType) {
                int lineNumber = cmd.getLineNumber() != null ? cmd.getLineNumber() : 0;
                target = new LineNumberTarget((LineNumberInjectionType) injectionType, cmd.getClazz(), lineNumber, 0, cmd.getMethod(), cmd.getDesc());
            } else {
                target = new MethodTarget((MethodInjectionType) injectionType, cmd.getClazz(), cmd.getMethod(), cmd.getDesc());
            }

            InjectableCode code = new InjectableCode() {
                @Override
                public String getCode() {
                    return cmd.getCode();
                }

                @Override
                public CodeType getCodeType() {
                    return CodeType.valueOf(cmd.getCodeType());
                }
            };

            InjectionPoint injectionPoint = new InjectionPoint(target, code);
            List<InjectionResult> results = injectionExecutor.execute(injectionPoint);

            StringBuilder jsonBuilder = new StringBuilder();
            boolean allSuccess = results.stream().allMatch(InjectionResult::isSuccess);
            jsonBuilder.append("{\"success\":").append(allSuccess);
            jsonBuilder.append(",\"results\":[");
            for (int i = 0; i < results.size(); i++) {
                InjectionResult r = results.get(i);
                if (i > 0) jsonBuilder.append(",");
                jsonBuilder.append("{");
                jsonBuilder.append("\"success\":").append(r.isSuccess());
                jsonBuilder.append(",\"type\":\"").append(r.getInjectionType()).append("\"");
                jsonBuilder.append(",\"method\":\"").append(r.getMethodName() != null ? r.getMethodName() : "").append("\"");
                jsonBuilder.append(",\"message\":\"").append(r.getMessage() != null ? r.getMessage().replace("\"", "\\\"") : "").append("\"");
                jsonBuilder.append("}");
            }
            jsonBuilder.append("]}");
            response.getWriter().write(jsonBuilder.toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"注入失败: " + e.getMessage() + "\"}");
        }
    }

    private void handleInjectDryRun(HttpServletRequest request, HttpServletResponse response,
                                     InjectionExecutor injectionExecutor) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        String jsonString = sb.toString();
        InjectionCommand cmd = JSON.parseObject(jsonString, InjectionCommand.class);

        if (cmd == null || !cmd.isValid()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"error\":\"缺少必要参数\"}");
            return;
        }

        String paramError = validateParamReferences(cmd.getCode(), cmd.getDesc());
        if (paramError != null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"error\":\"" + paramError + "\"}");
            return;
        }

        try {
            InjectionType injectionType = resolveInjectionType(cmd.getInjectionType());
            InjectionTarget target;
            if (injectionType instanceof LineNumberInjectionType) {
                int lineNumber = cmd.getLineNumber() != null ? cmd.getLineNumber() : 0;
                target = new LineNumberTarget((LineNumberInjectionType) injectionType, cmd.getClazz(), lineNumber, 0, cmd.getMethod(), cmd.getDesc());
            } else {
                target = new MethodTarget((MethodInjectionType) injectionType, cmd.getClazz(), cmd.getMethod(), cmd.getDesc());
            }

            InjectableCode code = new InjectableCode() {
                @Override
                public String getCode() {
                    return cmd.getCode();
                }

                @Override
                public CodeType getCodeType() {
                    return CodeType.valueOf(cmd.getCodeType());
                }
            };

            InjectionPoint injectionPoint = new InjectionPoint(target, code);

            Class<?> targetClass = findLoadedClass(cmd.getClazz());
            if (targetClass == null) {
                response.getWriter().write("{\"success\":false,\"error\":\"类未加载: " + cmd.getClazz() + "\"}");
                return;
            }

            String classInternalName = cmd.getClazz().replace('.', '/');
            InputStream is = targetClass.getClassLoader().getResourceAsStream(classInternalName + ".class");
            if (is == null) {
                response.getWriter().write("{\"success\":false,\"error\":\"无法读取类字节码: " + cmd.getClazz() + "\"}");
                return;
            }

            ByteArrayOutputStream classBytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = is.read(buffer)) != -1) {
                classBytes.write(buffer, 0, n);
            }
            is.close();
            byte[] originalBytes = classBytes.toByteArray();

            ClassTransformer classTransformer = new DefaultClassTransformer();
            TransformerResult result = classTransformer.transform(injectionPoint, cmd.getClazz(), originalBytes);

            JSONObject responseJson = new JSONObject();
            if (result.isTransformed()) {
                responseJson.put("success", true);
                responseJson.put("bytecodeSize", result.getBytecode().length);
                responseJson.put("originalSize", originalBytes.length);
                responseJson.put("injectedMethod", cmd.getMethod());
                responseJson.put("message", result.getMessage());
            } else {
                responseJson.put("success", false);
                responseJson.put("error", result.getMessage());
            }
            response.getWriter().write(responseJson.toJSONString());

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"success\":false,\"error\":\"预览失败: " + e.getMessage() + "\"}");
        }
    }

    private void handleInjectVerify(HttpServletRequest request, HttpServletResponse response,
                                     InjectionExecutor injectionExecutor) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        String jsonString = sb.toString();
        InjectionCommand cmd = JSON.parseObject(jsonString, InjectionCommand.class);

        if (cmd == null || !cmd.isValid()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"error\":\"缺少必要参数\"}");
            return;
        }

        try {
            TestResult testResult = testHarness.testInjection(
                    cmd.getClazz(), cmd.getMethod(), cmd.getDesc(),
                    cmd.getInjectionType(), cmd.getCode()
            );

            JSONObject responseJson = new JSONObject();
            responseJson.put("success", testResult.isSuccess());
            if (testResult.isSuccess()) {
                responseJson.put("output", testResult.getOutput());
            } else {
                responseJson.put("error", testResult.getError());
            }
            response.getWriter().write(responseJson.toJSONString());

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"success\":false,\"error\":\"注入验证失败: " + e.getMessage() + "\"}");
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

    private void handleCreateLog(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // 简化实现
        response.getWriter().write("{\"success\":true,\"id\":\"test\"}");
    }

    private void handleRules(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String method = request.getMethod();
        String pathInfo = request.getPathInfo();
        
        switch (method) {
            case "GET":
                if (pathInfo.equals("/rules")) {
                    response.getWriter().write(JSON.toJSONString(RuleManager.getInstance().getRules()));
                } else if (pathInfo.matches("/rules/\\d+")) {
                    long id = Long.parseLong(pathInfo.substring(7));
                    InjectionRule rule = RuleManager.getInstance().getRule(id);
                    if (rule != null) {
                        response.getWriter().write(JSON.toJSONString(rule));
                    } else {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        response.getWriter().write("{\"error\":\"规则不存在\"}");
                    }
                }
                break;
            case "POST":
                StringBuilder sb = new StringBuilder();
                String line;
                try (BufferedReader reader = request.getReader()) {
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                }
                InjectionRule newRule = JSON.parseObject(sb.toString(), InjectionRule.class);
                long id = RuleManager.getInstance().addRule(newRule);
                response.getWriter().write("{\"success\":true,\"id\":\"" + id + "\"}");
                break;
            case "PUT":
                if (pathInfo.matches("/rules/\\d+")) {
                    id = Long.parseLong(pathInfo.substring(7));
                    sb = new StringBuilder();
                    try (BufferedReader reader = request.getReader()) {
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                    }
                    InjectionRule updatedRule = JSON.parseObject(sb.toString(), InjectionRule.class);
                    RuleManager.getInstance().updateRule(id, updatedRule);
                    response.getWriter().write("{\"success\":true}");
                } else {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("{\"error\":\"缺少规则ID\"}");
                }
                break;
            case "DELETE":
                if (pathInfo.matches("/rules/\\d+")) {
                    id = Long.parseLong(pathInfo.substring(7));
                    RuleManager.getInstance().deleteRule(id);
                    response.getWriter().write("{\"success\":true}");
                } else {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("{\"error\":\"缺少规则ID\"}");
                }
                break;
            default:
                response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                response.getWriter().write("{\"error\":\"不支持的请求方法\"}");
        }
    }

    private void handleLineNumbers(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String className = request.getParameter("class");
        if (className == null || className.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"缺少class参数\"}");
            return;
        }

        try {
            Class<?> targetClass = findLoadedClass(className);
            if (targetClass == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"error\":\"类未加载: " + className + "\"}");
                return;
            }

            String classPath = className.replace('.', '/') + ".class";
            InputStream is = targetClass.getClassLoader().getResourceAsStream(classPath);
            if (is == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"error\":\"无法读取类字节码\"}");
                return;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = is.read(buffer)) != -1) {
                baos.write(buffer, 0, n);
            }
            is.close();
            byte[] bytecode = baos.toByteArray();

            org.objectweb.asm.ClassReader cr = new org.objectweb.asm.ClassReader(bytecode);
            StringBuilder jsonBuilder = new StringBuilder("{");

            cr.accept(new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9) {
                boolean firstMethod = true;

                @Override
                public org.objectweb.asm.MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    return new org.objectweb.asm.MethodVisitor(org.objectweb.asm.Opcodes.ASM9) {
                        final java.util.List<int[]> lines = new java.util.ArrayList<>();

                        @Override
                        public void visitLineNumber(int line, org.objectweb.asm.Label start) {
                            lines.add(new int[]{line});
                        }

                        @Override
                        public void visitEnd() {
                            if (!lines.isEmpty()) {
                                if (!firstMethod) jsonBuilder.append(",");
                                firstMethod = false;
                                jsonBuilder.append("\"").append(name).append(descriptor.replace('/', '.').replace('$', '.')).append("\":[");
                                for (int i = 0; i < lines.size(); i++) {
                                    if (i > 0) jsonBuilder.append(",");
                                    jsonBuilder.append(lines.get(i)[0]);
                                }
                                jsonBuilder.append("]");
                            }
                        }
                    };
                }
            }, 0);

            jsonBuilder.append("}");
            response.getWriter().write(jsonBuilder.toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"读取行号表失败: " + e.getMessage().replace("\"", "\\\"") + "\"}");
        }
    }

    private InjectionType resolveInjectionType(String injectionType) {
        if (injectionType == null || injectionType.isEmpty()) {
            return MethodInjectionType.ENTER;
        }
        switch (injectionType.toUpperCase()) {
            case "METHOD_ENTER":
            case "ENTER":
            case "ENTER_METHOD":
                return MethodInjectionType.ENTER;
            case "METHOD_EXIT":
            case "EXIT":
            case "EXIT_METHOD":
                return MethodInjectionType.EXIT;
            case "METHOD_AROUND":
            case "AROUND":
            case "AROUND_METHOD":
                return MethodInjectionType.AROUND;
            case "LINE_BEFORE":
            case "BEFORE_LINE":
                return LineNumberInjectionType.BEFORE;
            case "LINE_AFTER":
            case "AFTER_LINE":
                return LineNumberInjectionType.AFTER;
            default:
                return MethodInjectionType.ENTER;
        }
    }

    private String validateParamReferences(String code, String methodDescriptor) {
        if (code == null || !code.contains("$")) {
            return null;
        }

        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\$(\\d+)").matcher(code);
        int maxRef = 0;
        while (matcher.find()) {
            int ref = Integer.parseInt(matcher.group(1));
            if (ref > maxRef) maxRef = ref;
        }

        if (maxRef == 0) return null;

        if (methodDescriptor == null || methodDescriptor.isEmpty()) {
            return "使用参数引用 $" + maxRef + " 需要提供方法描述符(desc)";
        }

        org.objectweb.asm.Type[] argTypes = org.objectweb.asm.Type.getArgumentTypes(methodDescriptor);
        int paramCount = argTypes.length;

        if (maxRef > paramCount) {
            return "参数引用 $" + maxRef + " 超出范围，方法只有 " + paramCount + " 个参数";
        }

        return null;
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