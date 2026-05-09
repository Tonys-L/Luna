package fun.efto.luna.agent.web.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import fun.efto.luna.agent.clazz.ClassScanner;
import fun.efto.luna.agent.clazz.LoadedClass;
import fun.efto.luna.agent.web.ClassResourceHelper;
import fun.efto.luna.agent.web.mvc.ApiResult;
import fun.efto.luna.agent.web.mvc.Controller;
import fun.efto.luna.agent.web.mvc.GetMapping;
import fun.efto.luna.agent.web.mvc.RequestParam;
import fun.efto.luna.core.analyzer.AnalyzerRegistry;
import fun.efto.luna.core.analyzer.AnalyzerType;
import fun.efto.luna.core.analyzer.ClassAnalysisResult;
import fun.efto.luna.core.analyzer.ClassAnalyzer;
import fun.efto.luna.core.decompile.DecompilerFactory;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/09 10:00
 */
@Controller
public class ClassController {
    private final ClassScanner classScanner;
    private final ClassResourceHelper classResourceHelper;

    public ClassController(ClassScanner classScanner, ClassResourceHelper classResourceHelper) {
        this.classScanner = classScanner;
        this.classResourceHelper = classResourceHelper;
    }

    @GetMapping("/classes")
    public ApiResult list(@RequestParam(value = "refresh", required = false) String refresh) {
        Map<String, Set<LoadedClass>> result;
        if ("true".equals(refresh)) {
            result = classScanner.scan();
        } else {
            result = classScanner.getLoadedClasses();
        }
        return ApiResult.ok(result);
    }

    @GetMapping("/decompile")
    public ApiResult decompile(@RequestParam("class") String className) {
        if (className == null || className.isEmpty()) {
            return ApiResult.fail("Missing class parameter");
        }
        try {
            String decompiledCode = DecompilerFactory.getDecompiler().decompile(className);
            Map<String, String> data = new HashMap<>();
            data.put("decompiled", decompiledCode);
            return ApiResult.ok(data);
        } catch (Exception e) {
            return ApiResult.fail("Decompile failed: " + e.getMessage(),
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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
                return ApiResult.fail("类加载失败", HttpServletResponse.SC_BAD_REQUEST);
            }
        }
        return ApiResult.fail("No analyzer available", HttpServletResponse.SC_BAD_REQUEST);
    }

    @GetMapping("/line-numbers")
    public ApiResult lineNumbers(@RequestParam("class") String className) {
        if (className == null || className.isEmpty()) {
            return ApiResult.fail("缺少class参数");
        }

        try {
            Class<?> targetClass = classResourceHelper.findLoadedClass(className);
            if (targetClass == null) {
                return ApiResult.fail("类未加载: " + className, HttpServletResponse.SC_NOT_FOUND);
            }

            byte[] bytecode = classResourceHelper.loadClassBytes(className);
            ClassReader cr = new ClassReader(bytecode);
            Map<String, List<Integer>> result = new LinkedHashMap<>();

            cr.accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                  String signature, String[] exceptions) {
                    return new MethodVisitor(Opcodes.ASM9) {
                        final List<Integer> lines = new ArrayList<>();

                        @Override
                        public void visitLineNumber(int line, Label start) {
                            lines.add(line);
                        }

                        @Override
                        public void visitEnd() {
                            if (!lines.isEmpty()) {
                                String key = name + descriptor.replace('/', '.').replace('$', '.');
                                result.put(key, lines);
                            }
                        }
                    };
                }
            }, 0);

            return ApiResult.ok(result);
        } catch (Exception e) {
            return ApiResult.fail("读取行号表失败: " + e.getMessage(),
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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
                return ApiResult.fail("类未加载: " + className, HttpServletResponse.SC_NOT_FOUND);
            }

            byte[] bytecode = classResourceHelper.loadClassBytes(className);
            ClassReader cr = new ClassReader(bytecode);
            List<JSONObject> variables = new ArrayList<>();

            cr.accept(new ClassVisitor(Opcodes.ASM9) {
                boolean methodFound = false;

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                  String signature, String[] exceptions) {
                    if (!name.equals(methodName)) return null;
                    if (methodDesc != null && !methodDesc.isEmpty() && !descriptor.equals(methodDesc)) return null;
                    methodFound = true;

                    return new MethodVisitor(Opcodes.ASM9) {
                        final Map<Label, Integer> labelLines = new LinkedHashMap<>();
                        final List<Object[]> localVarEntries = new ArrayList<>();

                        @Override
                        public void visitLineNumber(int line, Label start) {
                            labelLines.put(start, line);
                        }

                        @Override
                        public void visitLocalVariable(String vName, String vDesc, String vSig,
                                                        Label start, Label end, int index) {
                            localVarEntries.add(new Object[]{vName, vDesc, index, start, end});
                        }

                        @Override
                        public void visitEnd() {
                            for (Object[] entry : localVarEntries) {
                                String vName = (String) entry[0];
                                String vDesc = (String) entry[1];
                                int slot = (Integer) entry[2];
                                Label startLabel = (Label) entry[3];
                                Label endLabel = (Label) entry[4];

                                if (isVariableVisibleAtLine(startLabel, endLabel, lineNumber)) {
                                    JSONObject varObj = new JSONObject();
                                    varObj.put("name", vName);
                                    varObj.put("descriptor", vDesc);
                                    varObj.put("slot", slot);
                                    variables.add(varObj);
                                }
                            }
                        }

                        private boolean isVariableVisibleAtLine(Label start, Label end, int line) {
                            Integer startLine = labelLines.get(start);
                            Integer endLine = labelLines.get(end);
                            if (startLine == null) return true;
                            if (line < startLine) return false;
                            if (endLine == null || endLine <= startLine) return true;
                            return line <= endLine;
                        }
                    };
                }

                @Override
                public void visitEnd() {
                    if (!methodFound) {
                        throw new RuntimeException("method not found: " + methodName);
                    }
                }
            }, 0);

            Map<String, Object> data = new HashMap<>();
            data.put("variables", variables);
            return ApiResult.ok(data);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("method not found")) {
                return ApiResult.fail(e.getMessage(), HttpServletResponse.SC_NOT_FOUND);
            }
            return ApiResult.fail("failed to read local variable table: " + e.getMessage(),
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return ApiResult.fail("failed to read local variable table: " + e.getMessage(),
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
