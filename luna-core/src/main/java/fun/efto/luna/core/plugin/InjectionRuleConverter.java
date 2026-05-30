package fun.efto.luna.core.plugin;

import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.PersistentInjection;
import fun.efto.luna.core.rule.InjectionRule;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
public interface InjectionRuleConverter {

    InjectionPoint convert(InjectionRule rule);

    default InjectionPoint convert(PersistentInjection injection) {
        InjectionRule rule = new InjectionRule();
        rule.setTargetClass(injection.getClazz());
        rule.setTargetMethod(injection.getMethodName());
        rule.setMethodDescriptor(injection.getMethodDescriptor());
        rule.setInjectionType(injection.getInjectionType());
        rule.setLineNumber(injection.getLineNumber() != null ? injection.getLineNumber() : 0);
        rule.setExpression(injection.getExpression());
        rule.setLogContent(injection.getCode());
        rule.setCodeType(injection.getCodeType());
        rule.setEnabled(injection.isEnabled());
        return convert(rule);
    }
}
