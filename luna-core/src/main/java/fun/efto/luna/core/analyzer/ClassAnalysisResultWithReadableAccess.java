package fun.efto.luna.core.analyzer;

import fun.efto.luna.core.util.AccessFlagsConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * 带有格式化访问标识的类分析结果
 * 扩展了ClassAnalysisResult，添加了可读的访问标识字符串
 *
 * @author Tony.L(286269159@qq.com)
 * @since 2025/10/23
 */
public class ClassAnalysisResultWithReadableAccess extends ClassAnalysisResult {
    private final String readableAccessFlags;
    private final List<FieldInfoWithReadableAccess> convertedFields;
    private final List<MethodInfoWithReadableAccess> convertedMethods;

    public ClassAnalysisResultWithReadableAccess(String className, List<FieldInfo> fields, List<MethodInfo> methods,
                                                 String superClass, List<String> interfaces, int accessFlags) {
        super(className, fields, methods, superClass, interfaces, accessFlags);
        
        this.readableAccessFlags = AccessFlagsConverter.convertClassAccessFlags(accessFlags);
        
        // 转换字段信息
        this.convertedFields = new ArrayList<>();
        for (FieldInfo field : fields) {
            this.convertedFields.add(new FieldInfoWithReadableAccess(field.getName(), field.getDescriptor(),
                field.getAccessFlags(), AccessFlagsConverter.convertFieldAccessFlags(field.getAccessFlags())));
        }
        
        // 转换方法信息
        this.convertedMethods = new ArrayList<>();
        for (MethodInfo method : methods) {
            this.convertedMethods.add(new MethodInfoWithReadableAccess(method.getName(), method.getDescriptor(),
                method.getAccessFlags(), method.getParameters(), 
                AccessFlagsConverter.convertMethodAccessFlags(method.getAccessFlags())));
        }
    }

    public String getReadableAccessFlags() {
        return readableAccessFlags;
    }

    public List<FieldInfoWithReadableAccess> getConvertedFields() {
        return new ArrayList<>(convertedFields);
    }

    public List<MethodInfoWithReadableAccess> getConvertedMethods() {
        return new ArrayList<>(convertedMethods);
    }

    /**
     * 带有格式化访问标识的字段信息
     */
    public static class FieldInfoWithReadableAccess extends ClassAnalysisResult.FieldInfo {
        private final String readableAccessFlags;

        public FieldInfoWithReadableAccess(String name, String descriptor, int accessFlags, String readableAccessFlags) {
            super(name, descriptor, accessFlags);
            this.readableAccessFlags = readableAccessFlags;
        }

        public String getReadableAccessFlags() {
            return readableAccessFlags;
        }
    }

    /**
     * 带有格式化访问标识的方法信息
     */
    public static class MethodInfoWithReadableAccess extends ClassAnalysisResult.MethodInfo {
        private final String readableAccessFlags;

        public MethodInfoWithReadableAccess(String name, String descriptor, int accessFlags,
                                            List<ParameterInfo> parameters, String readableAccessFlags) {
            super(name, descriptor, accessFlags, parameters);
            this.readableAccessFlags = readableAccessFlags;
        }

        public String getReadableAccessFlags() {
            return readableAccessFlags;
        }
    }
}