package fun.efto.luna.core.analyzer;

import java.util.ArrayList;
import java.util.List;

/**
 * 类分析结果
 * 包含类的结构信息
 * 
 * @author Tony.L(286269159@qq.com)
 * @since 2025/10/4
 */
public class ClassAnalysisResult {
    private final String className;
    private final List<FieldInfo> fields;
    private final List<MethodInfo> methods;
    private final String superClass;
    private final List<String> interfaces;
    private final int accessFlags;

    public ClassAnalysisResult(String className, List<FieldInfo> fields, List<MethodInfo> methods,
                               String superClass, List<String> interfaces, int accessFlags) {
        this.className = className;
        this.fields = new ArrayList<>(fields);
        this.methods = new ArrayList<>(methods);
        this.superClass = superClass;
        this.interfaces = new ArrayList<>(interfaces);
        this.accessFlags = accessFlags;
    }

    public String getClassName() {
        return className;
    }

    public List<FieldInfo> getFields() {
        return new ArrayList<>(fields);
    }

    public List<MethodInfo> getMethods() {
        return new ArrayList<>(methods);
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

    public static class FieldInfo {
        private final String name;
        private final String descriptor;
        private final int accessFlags;

        public FieldInfo(String name, String descriptor, int accessFlags) {
            this.name = name;
            this.descriptor = descriptor;
            this.accessFlags = accessFlags;
        }

        public String getName() {
            return name;
        }

        public String getDescriptor() {
            return descriptor;
        }

        public int getAccessFlags() {
            return accessFlags;
        }
    }

    public static class MethodInfo {
        private final String name;
        private final String descriptor;
        private final int accessFlags;
        private final List<ParameterInfo> parameters;

        public MethodInfo(String name, String descriptor, int accessFlags, List<ParameterInfo> parameters) {
            this.name = name;
            this.descriptor = descriptor;
            this.accessFlags = accessFlags;
            this.parameters = new ArrayList<>(parameters);
        }

        public String getName() {
            return name;
        }

        public String getDescriptor() {
            return descriptor;
        }

        public int getAccessFlags() {
            return accessFlags;
        }

        public List<ParameterInfo> getParameters() {
            return new ArrayList<>(parameters);
        }
    }

    public static class ParameterInfo {
        private final String name;
        private final String descriptor;

        public ParameterInfo(String name, String descriptor) {
            this.name = name;
            this.descriptor = descriptor;
        }

        public String getName() {
            return name;
        }

        public String getDescriptor() {
            return descriptor;
        }
    }
}