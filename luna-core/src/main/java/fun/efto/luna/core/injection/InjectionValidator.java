package fun.efto.luna.core.injection;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/26 23:00
 */
public class InjectionValidator {

    public static String validateParamReferences(String code, String methodDescriptor) {
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
            int paramCount = countMethodParameters(methodDescriptor);
            if (maxRef > paramCount) {
                return "参数引用 $" + maxRef + " 超出范围，方法只有 " + paramCount + " 个参数";
            }
        }
        return null;
    }

    public static String validateLocalVarReferences(String code, String className,
                                                     String methodName, String methodDesc,
                                                     Integer lineNumber,
                                                     List<LocalVarInfo> visibleVars) {
        if (code == null || !code.contains("$")) return null;
        if (lineNumber == null || lineNumber < 1) return null;

        Matcher varMatcher = Pattern.compile("\\$([a-zA-Z_]\\w*)").matcher(code);
        Set<String> varNames = new HashSet<>();
        while (varMatcher.find()) {
            varNames.add(varMatcher.group(1));
        }
        if (varNames.isEmpty()) return null;

        if (visibleVars == null) return null;

        Set<String> availableVars = new HashSet<>();
        for (LocalVarInfo v : visibleVars) {
            availableVars.add(v.getName());
        }

        for (String varName : varNames) {
            if (!availableVars.contains(varName)) {
                return "local variable $" + varName + " is not visible at line " + lineNumber;
            }
        }
        return null;
    }

    /**
     * 纯 Java 解析方法描述符中的参数数量，不依赖 ASM。
     * 方法描述符格式：(参数类型...)返回类型
     * 例如：(Ljava/lang/String;I)V = 2个参数
     *       ([I[Ljava/lang/Object;)V = 2个参数
     */
    static int countMethodParameters(String descriptor) {
        if (descriptor == null || descriptor.length() < 2 || descriptor.charAt(0) != '(') {
            return 0;
        }

        int count = 0;
        int i = 1; // skip '('
        while (i < descriptor.length() && descriptor.charAt(i) != ')') {
            count++;
            char c = descriptor.charAt(i);
            if (c == 'L') {
                // 对象类型：L全限定名;
                int semi = descriptor.indexOf(';', i);
                if (semi < 0) return count;
                i = semi + 1;
            } else if (c == '[') {
                // 数组类型：跳过所有 [ 前缀
                i++;
                while (i < descriptor.length() && descriptor.charAt(i) == '[') {
                    i++;
                }
                // 数组元素类型
                if (i < descriptor.length() && descriptor.charAt(i) == 'L') {
                    int semi = descriptor.indexOf(';', i);
                    if (semi < 0) return count;
                    i = semi + 1;
                } else {
                    i++; // 基本类型元素
                }
            } else {
                // 基本类型：B C D F I J S Z
                i++;
            }
        }
        return count;
    }

    /**
     * 局部变量信息（领域对象，替代 ASM 的 AsmInjectionContext.LocalVarInfo）
     */
    public static class LocalVarInfo {
        private final String name;
        private final String descriptor;
        private final int slot;

        public LocalVarInfo(String name, String descriptor, int slot) {
            this.name = name;
            this.descriptor = descriptor;
            this.slot = slot;
        }

        public String getName() { return name; }
        public String getDescriptor() { return descriptor; }
        public int getSlot() { return slot; }
    }
}
