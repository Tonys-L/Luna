/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/17 18:32
 */
package fun.efto.luna.core.plugin.builtin.snapshot;

import fun.efto.luna.core.injection.CodeCompilerStrategy;
import fun.efto.luna.core.injection.PersistentInjection;
import fun.efto.luna.core.injection.code.CodeType;
import fun.efto.luna.core.injection.code.ExpressBaseInjectableCode;
import fun.efto.luna.core.injection.code.InjectableCode;

public class SnapshotCodeCompilerStrategy implements CodeCompilerStrategy {

    @Override
    public boolean supports(String codeType, String code) {
        return "SNAPSHOT".equalsIgnoreCase(codeType) || (code != null && code.startsWith("snapshot:"));
    }

    @Override
    public InjectableCode compile(PersistentInjection persistent) {
        String condition = persistent.getExpression();
        
        StringBuilder sb = new StringBuilder();
        if (condition != null && !condition.trim().isEmpty()) {
            sb.append("${").append(condition.trim()).append("}::");
        }
        sb.append("snapshot:true");
        
        return new ExpressBaseInjectableCode(sb.toString()) {
            @Override
            public CodeType getCodeType() {
                return CodeType.SNAPSHOT;
            }
        };
    }
}
