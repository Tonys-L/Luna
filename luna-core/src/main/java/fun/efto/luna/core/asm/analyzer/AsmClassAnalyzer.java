package fun.efto.luna.core.asm.analyzer;

import fun.efto.luna.core.asm.Constants;
import fun.efto.luna.core.analyzer.ClassAnalyzer;
import fun.efto.luna.core.analyzer.ClassAnalysisResult;
import fun.efto.luna.core.analyzer.ClassAnalysisResult.FieldInfo;
import fun.efto.luna.core.analyzer.ClassAnalysisResult.MethodInfo;
import fun.efto.luna.core.analyzer.ClassAnalysisResult.ParameterInfo;
import fun.efto.luna.core.analyzer.ClassAnalysisResult.LocalVariableInfo;
import fun.efto.luna.core.analyzer.AnnotationInfo;
import fun.efto.luna.core.analyzer.ExceptionTableEntry;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Label;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ASM 类分析器
 * 使用 ASM ClassReader/ClassVisitor 解析字节码，提取类结构信息
 *
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/03/29 02:30
 */
public class AsmClassAnalyzer implements ClassAnalyzer {

    @Override
    public ClassAnalysisResult analyze(byte[] classBytes) {
        if (classBytes == null || classBytes.length == 0) {
            return new ClassAnalysisResult(
                    "", Collections.emptyList(), Collections.emptyList(),
                    "", Collections.emptyList(), 0
            );
        }

        try {
            ClassReader reader = new ClassReader(classBytes);
            AnalysisVisitor visitor = new AnalysisVisitor();
            // 不使用 SKIP_DEBUG，以保留行号表和局部变量表
            reader.accept(visitor, ClassReader.SKIP_FRAMES);
            return visitor.buildResult();
        } catch (Exception e) {
            return new ClassAnalysisResult(
                    "", Collections.emptyList(), Collections.emptyList(),
                    "", Collections.emptyList(), 0
            );
        }
    }

    @Override
    public Map<String, List<Integer>> getLineNumbers(byte[] classBytes) {
        if (classBytes == null || classBytes.length == 0) {
            return Collections.emptyMap();
        }

        try {
            ClassReader reader = new ClassReader(classBytes);
            Map<String, List<Integer>> result = new LinkedHashMap<>();

            reader.accept(new ClassVisitor(Opcodes.ASM9) {
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
            }, ClassReader.SKIP_FRAMES);

            return result;
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    @Override
    public List<LocalVariableInfo> getVisibleLocalVariables(byte[] classBytes, String methodName,
                                                             String methodDescriptor, int lineNumber) {
        if (classBytes == null || classBytes.length == 0) {
            return Collections.emptyList();
        }

        try {
            ClassReader reader = new ClassReader(classBytes);
            List<LocalVariableInfo> variables = new ArrayList<>();

            reader.accept(new ClassVisitor(Opcodes.ASM9) {
                boolean methodFound = false;

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                 String signature, String[] exceptions) {
                    if (!name.equals(methodName)) return null;
                    if (methodDescriptor != null && !methodDescriptor.isEmpty()
                            && !descriptor.equals(methodDescriptor)) return null;
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
                                    Integer startLine = labelLines.get(startLabel);
                                    Integer endLine = labelLines.get(endLabel);
                                    variables.add(new LocalVariableInfo(vName, vDesc, slot,
                                            startLine != null ? startLine : -1,
                                            endLine != null ? endLine : -1));
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
            }, ClassReader.SKIP_FRAMES);

            return variables;
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("method not found")) {
                throw e;
            }
            return Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private static class AnalysisVisitor extends ClassVisitor {
        private String className;
        private String superClass;
        private int accessFlags;
        private final List<String> interfaces = new ArrayList<>();
        private final List<FieldInfo> fields = new ArrayList<>();
        private final List<MethodInfo> methods = new ArrayList<>();
        private final List<AnnotationInfo> classAnnotations = new ArrayList<>();

        AnalysisVisitor() {
            super(Constants.AMS_API_VERSION);
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            this.className = name != null ? name.replace('/', '.') : "";
            this.superClass = superName != null ? superName.replace('/', '.') : "";
            this.accessFlags = access;
            if (interfaces != null) {
                for (String iface : interfaces) {
                    this.interfaces.add(iface.replace('/', '.'));
                }
            }
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            classAnnotations.add(new AnnotationInfo(descriptor, visible, Collections.emptyMap()));
            return null;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor,
                                       String signature, Object value) {
            fields.add(new FieldInfo(name, descriptor, access));
            return null;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            List<ParameterInfo> parameters = new ArrayList<>();
            List<LocalVariableInfo> localVariables = new ArrayList<>();
            List<AnnotationInfo> methodAnnotations = new ArrayList<>();
            List<ExceptionTableEntry> exceptionTable = new ArrayList<>();
            return new MethodAnalysisVisitor(api, parameters, localVariables,
                    methodAnnotations, exceptionTable,
                    () -> {
                        MethodInfo methodInfo = new MethodInfo(name, descriptor, access, parameters, localVariables);
                        methodInfo.setAnnotations(methodAnnotations);
                        methodInfo.setExceptionTable(exceptionTable);
                        methods.add(methodInfo);
                    });
        }

        ClassAnalysisResult buildResult() {
            ClassAnalysisResult result = new ClassAnalysisResult(className, fields, methods, superClass, interfaces, accessFlags);
            result.setClassAnnotations(classAnnotations);
            return result;
        }
    }

    @FunctionalInterface
    private interface MethodCompleteCallback {
        void onComplete();
    }

    private static class MethodAnalysisVisitor extends MethodVisitor {
        private final List<ParameterInfo> parameters;
        private final List<LocalVariableInfo> localVariables;
        private final List<AnnotationInfo> methodAnnotations;
        private final List<ExceptionTableEntry> exceptionTable;
        private final MethodCompleteCallback callback;

        MethodAnalysisVisitor(int api, List<ParameterInfo> parameters,
                              List<LocalVariableInfo> localVariables,
                              List<AnnotationInfo> methodAnnotations,
                              List<ExceptionTableEntry> exceptionTable,
                              MethodCompleteCallback callback) {
            super(api);
            this.parameters = parameters;
            this.localVariables = localVariables;
            this.methodAnnotations = methodAnnotations;
            this.exceptionTable = exceptionTable;
            this.callback = callback;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            methodAnnotations.add(new AnnotationInfo(descriptor, visible, Collections.emptyMap()));
            return null;
        }

        public void visitTryCatchBlock(int start, int end, int handler, String type) {
            String catchType = type != null ? type.replace('/', '.') : null;
            exceptionTable.add(new ExceptionTableEntry(start, end, handler, catchType));
        }

        @Override
        public void visitLocalVariable(String name, String descriptor, String signature,
                                        Label start, Label end, int index) {
            localVariables.add(new LocalVariableInfo(name, descriptor, index, -1, -1));
        }

        @Override
        public void visitEnd() {
            callback.onComplete();
        }
    }
}
