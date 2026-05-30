package fun.efto.luna.core.plugin.builtin;

import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.PersistentInjection;
import fun.efto.luna.core.injection.code.CodeType;
import fun.efto.luna.core.plugin.InjectionRuleConverter;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/24 10:00
 */
public abstract class AbstractRuleConverter implements InjectionRuleConverter {

    protected String buildExpression(String condition, String content) {
        if (condition != null && !condition.isEmpty()) {
            return "${" + condition + "}::" + content;
        }
        return content;
    }

    protected String buildExpression(String condition, String content, CodeType codeType) {
        if (condition != null && !condition.isEmpty()) {
            return "${" + condition + "}::" + content;
        }
        return content;
    }
}
