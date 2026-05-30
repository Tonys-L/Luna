package fun.efto.luna.core.plugin.builtin.line;

import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.code.CodeType;
import fun.efto.luna.core.injection.code.ExpressBaseInjectableCode;
import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.injection.target.LineNumberTarget;
import fun.efto.luna.core.plugin.builtin.AbstractRuleConverter;
import fun.efto.luna.core.plugin.registry.InjectionTypeRegistry;
import fun.efto.luna.core.injection.PersistentInjection;
import fun.efto.luna.core.injection.rule.InjectionRule;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/16 10:00
 */
public class LineRuleConverter extends AbstractRuleConverter {
    @Override
    public InjectionPoint convert(InjectionRule rule) {
        PersistentInjection injection = new PersistentInjection();
        injection.setClazz(rule.getTargetClass());
        injection.setMethodName(rule.getTargetMethod());
        injection.setMethodDescriptor(rule.getMethodDescriptor());
        injection.setInjectionType(rule.getInjectionType());
        injection.setLineNumber(rule.getLineNumber());
        injection.setExpression(rule.getExpression());
        injection.setCode(rule.getLogContent());
        injection.setCodeType(rule.getCodeType());
        injection.setGroupId(rule.getGroupId());
        return convertFromPersistent(injection);
    }

    @Override
    public InjectionPoint convert(PersistentInjection injection) {
        return convertFromPersistent(injection);
    }

    private InjectionPoint convertFromPersistent(PersistentInjection injection) {
        LineNumberInjectionType type = (LineNumberInjectionType) InjectionTypeRegistry.getInstance().resolve(injection.getInjectionType());
        int lineNumber = injection.getLineNumber();
        LineNumberInjectionType typeWithLine = type.withLineNumber(lineNumber);
        LineNumberTarget target = new LineNumberTarget(typeWithLine, injection.getClazz(), lineNumber, 0, injection.getMethodName(), injection.getMethodDescriptor() != null ? injection.getMethodDescriptor() : "");
        final CodeType codeType = injection.getCodeType() != null ? CodeType.fromName(injection.getCodeType()) : CodeType.EXPRESSION;
        final String condition = injection.getExpression();
        final String logContent = injection.getCode();
        InjectableCode code = new ExpressBaseInjectableCode() {
            @Override
            public CodeType getCodeType() {
                return codeType;
            }
            @Override
            public String getCode() {
                return buildExpression(condition, logContent, codeType);
            }
        };
        return new InjectionPoint(injection.getId(), target, code);
    }
}
