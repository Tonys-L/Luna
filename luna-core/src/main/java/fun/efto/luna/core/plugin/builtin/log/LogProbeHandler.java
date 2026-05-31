package fun.efto.luna.core.plugin.builtin.log;

import fun.efto.luna.core.injection.InjectRequest;
import fun.efto.luna.core.injection.code.CompiledCode;
import fun.efto.luna.core.plugin.AbstractProbeHandler;
import fun.efto.luna.core.plugin.GenerateContext;
import fun.efto.luna.core.plugin.ValidationResult;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/01 01:30
 */
public class LogProbeHandler extends AbstractProbeHandler {

    private final LogExpressionHandler delegate = new LogExpressionHandler();

    @Override
    public String getProbeType() {
        return "LOG";
    }

    @Override
    public boolean usesCode() {
        return true;
    }

    @Override
    public Set<String> supportedInjectionLocations() {
        return new HashSet<>(Arrays.asList("method_enter", "method_exit", "method_around", "line_before", "line_after", "invoke", "exception_exit"));
    }

    @Override
    protected ValidationResult doValidate(InjectRequest request) {
        if (request.getCode() == null || request.getCode().isEmpty()) {
            return ValidationResult.fail("code is required for LOG probe");
        }
        return ValidationResult.ok();
    }

    @Override
    public void handle(CompiledCode code, GenerateContext ctx) {
        delegate.generateBytecode(ctx);
    }
}
