package fun.efto.luna.asm.analyzer;

import fun.efto.luna.core.analyzer.AbstractAnalyzer;
import fun.efto.luna.core.analyzer.ClassAnalysisResult;
import fun.efto.luna.core.util.ClassNameUtils;
import org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 17:45
 */
public class AsmClassAnalyzer extends AbstractAnalyzer {

    @Override
    protected ClassAnalysisResult doAnalyze(byte[] bytecode) {
        if (bytecode == null || bytecode.length == 0) {
            throw new IllegalArgumentException("Bytecode cannot be null or empty");
        }

        ClassReader classReader = new ClassReader(bytecode);
        ClassAnalysisVisitor visitor = new ClassAnalysisVisitor();
        classReader.accept(visitor, 0);

        return new ClassAnalysisResult(
                visitor.getClassName(),
                visitor.getFields(),
                visitor.getMethods(),
                visitor.getSuperClass(),
                visitor.getInterfaces(),
                visitor.getAccessFlags()
        );
    }

    /**
     * 类分析访问器
     * 用于访问和收集类的结构信息
     */
    private static class ClassAnalysisVisitor extends ClassVisitor {
        private String className;
        private String superClass;
        private List<String> interfaces = new ArrayList<>();
        private int accessFlags;
        private List<ClassAnalysisResult.FieldInfo> fields = new ArrayList<>();
        private List<ClassAnalysisResult.MethodInfo> methods = new ArrayList<>();

        public ClassAnalysisVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.className = ClassNameUtils.toFqn(name);
            this.superClass = superName != null ? ClassNameUtils.toFqn(superName) : null;
            this.accessFlags = access;

            if (interfaces != null) {
                for (String iface : interfaces) {
                    this.interfaces.add(ClassNameUtils.toFqn(iface));
                }
            }

            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            fields.add(new ClassAnalysisResult.FieldInfo(name, descriptor, access));
            return super.visitField(access, name, descriptor, signature, value);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodAnalysisVisitor methodVisitor = new MethodAnalysisVisitor(access, name, descriptor);
            methods.add(methodVisitor.getMethodInfo());
            return methodVisitor;
        }

        // Getters
        public String getClassName() {
            return className;
        }

        public String getSuperClass() {
            return superClass;
        }

        public List<String> getInterfaces() {
            return new ArrayList<>(interfaces);
        }

        public int getAccessFlags() {
            return accessFlags;
        }

        public List<ClassAnalysisResult.FieldInfo> getFields() {
            return new ArrayList<>(fields);
        }

        public List<ClassAnalysisResult.MethodInfo> getMethods() {
            return new ArrayList<>(methods);
        }
    }

    /**
     * 方法分析访问器
     * 用于访问和收集方法的参数信息
     */
    private static class MethodAnalysisVisitor extends MethodVisitor {
        private final int access;
        private final String name;
        private final String descriptor;
        private final List<ClassAnalysisResult.ParameterInfo> parameters = new ArrayList<>();

        public MethodAnalysisVisitor(int access, String name, String descriptor) {
            super(Opcodes.ASM9);
            this.access = access;
            this.name = name;
            this.descriptor = descriptor;

            // 解析方法参数
            parseParameters(descriptor);
        }

        private void parseParameters(String descriptor) {
            // 方法描述符格式: (参数类型)返回值类型
            int start = descriptor.indexOf('(');
            int end = descriptor.indexOf(')');

            if (start != -1 && end != -1 && end > start) {
                String paramsPart = descriptor.substring(start + 1, end);
                parseParameterDescriptors(paramsPart);
            }
        }

        private void parseParameterDescriptors(String paramsPart) {
            int paramIndex = 0;
            int i = 0;

            while (i < paramsPart.length()) {
                char c = paramsPart.charAt(i);
                String paramType;
                int endPos;

                switch (c) {
                    case 'B': // byte
                    case 'C': // char
                    case 'D': // double
                    case 'F': // float
                    case 'I': // int
                    case 'J': // long
                    case 'S': // short
                    case 'Z': // boolean
                        paramType = String.valueOf(c);
                        i++;
                        break;
                    case 'L': // 对象类型
                        endPos = paramsPart.indexOf(';', i);
                        if (endPos != -1) {
                            paramType = paramsPart.substring(i, endPos + 1);
                            i = endPos + 1;
                        } else {
                            paramType = paramsPart.substring(i);
                            i = paramsPart.length();
                        }
                        break;
                    case '[': // 数组类型
                        int arrayStart = i;
                        while (i < paramsPart.length() && paramsPart.charAt(i) == '[') {
                            i++;
                        }
                        if (i < paramsPart.length()) {
                            char arrayType = paramsPart.charAt(i);
                            if (arrayType == 'L') {
                                endPos = paramsPart.indexOf(';', i);
                                if (endPos != -1) {
                                    paramType = paramsPart.substring(arrayStart, endPos + 1);
                                    i = endPos + 1;
                                } else {
                                    paramType = paramsPart.substring(arrayStart);
                                    i = paramsPart.length();
                                }
                            } else {
                                paramType = paramsPart.substring(arrayStart, i + 1);
                                i++;
                            }
                        } else {
                            paramType = paramsPart.substring(arrayStart);
                            i = paramsPart.length();
                        }
                        break;
                    default:
                        // 未知类型，跳过
                        i++;
                        continue;
                }

                // 生成参数名（ASM无法获取真实参数名，使用arg0, arg1等）
                String paramName = "arg" + paramIndex;
                parameters.add(new ClassAnalysisResult.ParameterInfo(paramName, paramType));
                paramIndex++;
            }
        }

        public ClassAnalysisResult.MethodInfo getMethodInfo() {
            return new ClassAnalysisResult.MethodInfo(name, descriptor, access, parameters);
        }
    }
}