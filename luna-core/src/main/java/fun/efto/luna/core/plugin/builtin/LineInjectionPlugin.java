package fun.efto.luna.core.plugin.builtin;

import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.code.ExpressBaseInjectableCode;
import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.injection.code.type.CodeType;
import fun.efto.luna.core.injection.target.LineNumberTarget;
import fun.efto.luna.core.injection.target.type.LineNumberInjectionType;
import fun.efto.luna.core.asm.injector.BeforeLineInjector;
import fun.efto.luna.core.asm.injector.AfterLineInjector;
import fun.efto.luna.core.plugin.*;
import fun.efto.luna.core.rule.InjectionRule;
import fun.efto.luna.core.rule.template.RuleTemplate;

import java.util.Collections;
import java.util.List;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
public class LineInjectionPlugin implements LunaPlugin {
    @Override public String getId() { return "line-injection"; }
    @Override public String getDisplayName() { return "行号注入"; }
    @Override public String getVersion() { return "1.0.0"; }
    @Override public String getAuthor() { return "Luna Core Team"; }
    @Override public String getCategory() { return "injection"; }
    @Override
    public boolean isBuiltin() { return true; }

    @Override
    public void initialize(PluginContext ctx) {
        ctx.registerInjectionType(LineNumberInjectionType.BEFORE);
        ctx.registerInjectionType(LineNumberInjectionType.AFTER);

        ctx.registerInjector(LineNumberInjectionType.BEFORE, new BeforeLineInjector());
        ctx.registerInjector(LineNumberInjectionType.AFTER, new AfterLineInjector());

        InjectionRuleConverter lineConverter = rule -> {
            LineNumberInjectionType type = (LineNumberInjectionType) InjectionTypeRegistry.resolve(rule.getInjectionType());
            int lineNumber = rule.getLineNumber();
            LineNumberInjectionType typeWithLine = type.withLineNumber(lineNumber);
            LineNumberTarget target = new LineNumberTarget(typeWithLine, rule.getTargetClass(), lineNumber, 0, rule.getTargetMethod(), rule.getMethodDescriptor() != null ? rule.getMethodDescriptor() : "");
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
                    if (codeType == CodeType.SNAPSHOT) {
                        sb.append("snapshot:true");
                    } else if (logContent != null && ExpressionHandlerRegistry.hasProtocol(logContent)) {
                        sb.append(logContent);
                    } else {
                        sb.append("log:").append(logContent != null ? logContent : "");
                    }
                    return sb.toString();
                }
            };
            return new InjectionPoint(target, code);
        };
        ctx.registerRuleConverter(LineNumberInjectionType.BEFORE, lineConverter);
        ctx.registerRuleConverter(LineNumberInjectionType.AFTER, lineConverter);
    }
}
