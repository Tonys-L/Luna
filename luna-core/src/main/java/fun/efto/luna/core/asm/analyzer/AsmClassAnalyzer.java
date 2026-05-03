package fun.efto.luna.core.asm.analyzer;

import fun.efto.luna.core.asm.Constants;
import fun.efto.luna.core.analyzer.ClassAnalyzer;
import fun.efto.luna.core.analyzer.ClassAnalysisResult;
import fun.efto.luna.core.analyzer.ClassAnalysisResult.FieldInfo;
import fun.efto.luna.core.analyzer.ClassAnalysisResult.MethodInfo;
import fun.efto.luna.core.analyzer.ClassAnalysisResult.ParameterInfo;
import fun.efto.luna.core.analyzer.ClassAnalysisResult.LocalVariableInfo;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Label;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ASM 类分析器
 * 使用 ASM ClassReader/ClassVisitor 解析字节码，提取类结构信息
 *
 * @author : Tony.L(<286269159@qq.com>)
 * @since : 2026/03/29 02:30
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
            reader.accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return visitor.buildResult();
        } catch (Exception e) {
            return new ClassAnalysisResult(
                    "", Collections.emptyList(), Collections.emptyList(),
                    "", Collections.emptyList(), 0
            );
        }
    }

    private static class AnalysisVisitor extends ClassVisitor {
        private String className;
        private String superClass;
        private int accessFlags;
        private final List<String> interfaces = new ArrayList<>();
        private final List<FieldInfo> fields = new ArrayList<>();
        private final List<MethodInfo> methods = new ArrayList<>();

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
            return new MethodAnalysisVisitor(api, parameters, localVariables,
                    () -> methods.add(new MethodInfo(name, descriptor, access, parameters, localVariables)));
        }

        ClassAnalysisResult buildResult() {
            return new ClassAnalysisResult(className, fields, methods, superClass, interfaces, accessFlags);
        }
    }

    @FunctionalInterface
    private interface MethodCompleteCallback {
        void onComplete();
    }

    private static class MethodAnalysisVisitor extends MethodVisitor {
        private final List<ParameterInfo> parameters;
        private final List<LocalVariableInfo> localVariables;
        private final MethodCompleteCallback callback;

        MethodAnalysisVisitor(int api, List<ParameterInfo> parameters,
                              List<LocalVariableInfo> localVariables,
                              MethodCompleteCallback callback) {
            super(api);
            this.parameters = parameters;
            this.localVariables = localVariables;
            this.callback = callback;
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
