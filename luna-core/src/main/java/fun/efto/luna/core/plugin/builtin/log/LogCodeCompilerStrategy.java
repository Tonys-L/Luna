/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/17 18:30
 */
package fun.efto.luna.core.plugin.builtin.log;

import fun.efto.luna.core.injection.CodeCompilerStrategy;
import fun.efto.luna.core.injection.PersistentInjection;
import fun.efto.luna.core.injection.code.ExpressBaseInjectableCode;
import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.plugin.registry.ExpressionHandlerRegistry;

public class LogCodeCompilerStrategy implements CodeCompilerStrategy {

    @Override
    public boolean supports(String codeType, String code) {
        return "EXPRESSION".equalsIgnoreCase(codeType) || (code != null && code.startsWith("log:"));
    }

    @Override
    public InjectableCode compile(PersistentInjection persistent) {
        String condition = persistent.getExpression();
        String content = persistent.getCode();
        
        StringBuilder sb = new StringBuilder();
        if (condition != null && !condition.trim().isEmpty()) {
            sb.append("${").append(condition.trim()).append("}::");
        }
        if (content != null && ExpressionHandlerRegistry.getInstance().hasProtocol(content)) {
            sb.append(content);
        } else {
            sb.append("log:").append(content != null ? content : "");
        }
        
        return new ExpressBaseInjectableCode(sb.toString());
    }
}
