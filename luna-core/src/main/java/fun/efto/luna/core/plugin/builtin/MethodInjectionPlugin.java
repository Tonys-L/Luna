package fun.efto.luna.core.plugin.builtin;

import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.code.ExpressBaseInjectableCode;
import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.injection.code.type.CodeType;
import fun.efto.luna.core.injection.target.MethodTarget;
import fun.efto.luna.core.injection.target.type.MethodInjectionType;
import fun.efto.luna.core.asm.injector.EnterMethodInjector;
import fun.efto.luna.core.asm.injector.ExitMethodInjector;
import fun.efto.luna.core.asm.injector.AroundMethodInjector;
import fun.efto.luna.core.plugin.*;
import fun.efto.luna.core.rule.InjectionRule;
import fun.efto.luna.core.rule.template.RuleTemplate;

import java.util.Collections;
import java.util.List;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
public class MethodInjectionPlugin implements LunaPlugin {
    @Override public String getId() { return "method-injection"; }
    @Override public String getDisplayName() { return "方法注入"; }
    @Override public String getVersion() { return "1.0.0"; }
    @Override public String getAuthor() { return "Luna Core Team"; }
    @Override public String getCategory() { return "injection"; }
    @Override
    public boolean isBuiltin() { return true; }

    @Override
    public void initialize(PluginContext ctx) {
        ctx.registerInjectionType(MethodInjectionType.ENTER);
        ctx.registerInjectionType(MethodInjectionType.EXIT);
        ctx.registerInjectionType(MethodInjectionType.AROUND);

        ctx.registerInjector(MethodInjectionType.ENTER, new EnterMethodInjector());
        ctx.registerInjector(MethodInjectionType.EXIT, new ExitMethodInjector());
        ctx.registerInjector(MethodInjectionType.AROUND, new AroundMethodInjector());

        InjectionRuleConverter methodConverter = rule -> {
            MethodInjectionType type = (MethodInjectionType) InjectionTypeRegistry.resolve(rule.getInjectionType());
            MethodTarget target = new MethodTarget(type, rule.getTargetClass(), rule.getTargetMethod(), rule.getMethodDescriptor() != null ? rule.getMethodDescriptor() : "");
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
                    if (condition != null && !condition.trim().isEmpty()) {
                        sb.append("${").append(condition.trim()).append("}::");
                    }
                    if (logContent != null && ExpressionHandlerRegistry.hasProtocol(logContent)) {
                        sb.append(logContent);
                    } else {
                        sb.append("log:").append(logContent != null ? logContent : "");
                    }
                    return sb.toString();
                }
            };
            return new InjectionPoint(target, code);
        };
        ctx.registerRuleConverter(MethodInjectionType.ENTER, methodConverter);
        ctx.registerRuleConverter(MethodInjectionType.EXIT, methodConverter);
        ctx.registerRuleConverter(MethodInjectionType.AROUND, methodConverter);
    }
}
