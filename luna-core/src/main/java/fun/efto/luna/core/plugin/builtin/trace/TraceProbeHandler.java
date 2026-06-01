package fun.efto.luna.core.plugin.builtin.trace;

import fun.efto.luna.core.injection.InjectRequest;
import fun.efto.luna.core.injection.code.CompiledCode;
import fun.efto.luna.core.plugin.AbstractProbeHandler;
import fun.efto.luna.core.plugin.GenerateContext;
import fun.efto.luna.core.plugin.ValidationResult;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/01 01:30
 */
public class TraceProbeHandler extends AbstractProbeHandler {

    private static final Set<String> SUPPORTED_LOCATIONS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("method_enter", "method_exit"))
    );

    private final TraceExpressionHandler delegate = new TraceExpressionHandler();

    @Override
    public String getProbeType() {
        return "TRACE";
    }

    @Override
    public boolean usesCode() {
        return false;
    }

    @Override
    public Set<String> supportedInjectionLocations() {
        return SUPPORTED_LOCATIONS;
    }

    @Override
    protected ValidationResult doValidate(InjectRequest request) {
        if ("method_around".equals(request.getInjectionLocation())) {
            return ValidationResult.fail("TRACE probe does not support method_around location");
        }
        return ValidationResult.ok();
    }

    @Override
    public void handle(CompiledCode code, GenerateContext ctx) {
        delegate.generateBytecode(ctx);
    }
}
