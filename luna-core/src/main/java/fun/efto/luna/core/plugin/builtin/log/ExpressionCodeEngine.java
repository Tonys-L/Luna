package fun.efto.luna.core.plugin.builtin.log;

import fun.efto.luna.core.injection.CodeEngine;
import fun.efto.luna.core.injection.PersistentInjection;
import fun.efto.luna.core.injection.code.CompiledCode;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/01 10:00
 */
public class ExpressionCodeEngine implements CodeEngine {

    @Override
    public String getCodeType() {
        return "EXPRESSION";
    }

    @Override
    public CompiledCode compile(PersistentInjection persistent) {
        String condition = persistent.getExpression();
        String content = persistent.getCode() != null ? persistent.getCode() : "";

        return new CompiledCode(
            condition != null && !condition.trim().isEmpty() ? "${" + condition.trim() + "}" : null,
            content
        );
    }
}
