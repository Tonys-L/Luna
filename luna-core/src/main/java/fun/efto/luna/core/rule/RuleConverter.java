package fun.efto.luna.core.rule;

import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.code.ExpressBaseInjectableCode;
import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.injection.code.type.CodeType;
import fun.efto.luna.core.injection.target.InjectionTarget;
import fun.efto.luna.core.injection.target.LineNumberTarget;
import fun.efto.luna.core.injection.target.MethodTarget;
import fun.efto.luna.core.injection.target.type.InjectionType;
import fun.efto.luna.core.injection.target.type.LineNumberInjectionType;
import fun.efto.luna.core.injection.target.type.MethodInjectionType;

/**
 * 规则转换器：将持久化规则转换为注入点
 */
public class RuleConverter {

    public static InjectionPoint convert(InjectionRule rule) {
        // 1. 解析注入类型
        InjectionType injectionType = resolveInjectionType(rule.getInjectionType());
        
        // 2. 构建目标对象
        InjectionTarget target;
        if (injectionType instanceof LineNumberInjectionType) {
            int lineNumber = rule.getLineNumber();
            LineNumberInjectionType typeWithLine = ((LineNumberInjectionType) injectionType).withLineNumber(lineNumber);
            target = new LineNumberTarget(typeWithLine, rule.getTargetClass(), lineNumber, 0, rule.getTargetMethod(), "");
        } else {
            target = new MethodTarget((MethodInjectionType) injectionType, rule.getTargetClass(), rule.getTargetMethod(), "");
        }

        // 3. 构建可注入代码（符合 ExpressionBytecodeAssembler 的格式要求）
        final CodeType codeType = rule.getCodeType() != null ? CodeType.valueOf(rule.getCodeType()) : CodeType.EXPRESSION;
        final String condition = rule.getExpression();
        final String logContent = rule.getLogContent();
        
        InjectableCode code = new ExpressBaseInjectableCode() {
            @Override
            public CodeType getCodeType() {
                return codeType;
            }
            @Override
            public String getCode() {
                StringBuilder sb = new StringBuilder();
                // 添加条件前缀：${condition}::
                if (condition != null && !condition.trim().isEmpty()) {
                    sb.append("${").append(condition.trim()).append("}::");
                }
                
                // 添加协议头和内容
                if (codeType == CodeType.SNAPSHOT) {
                    sb.append("snapshot:true");
                } else {
                    sb.append("log:").append(logContent != null ? logContent : "");
                }
                return sb.toString();
            }
        };

        return new InjectionPoint(target, code);
    }

    private static InjectionType resolveInjectionType(String typeStr) {
        if (typeStr == null || typeStr.isEmpty()) {
            return MethodInjectionType.ENTER;
        }
        String upper = typeStr.toUpperCase();
        if (upper.contains("LINE")) {
            return upper.contains("AFTER") ? LineNumberInjectionType.AFTER : LineNumberInjectionType.BEFORE;
        }
        if (upper.contains("EXIT")) return MethodInjectionType.EXIT;
        if (upper.contains("AROUND")) return MethodInjectionType.AROUND;
        return MethodInjectionType.ENTER;
    }
}
