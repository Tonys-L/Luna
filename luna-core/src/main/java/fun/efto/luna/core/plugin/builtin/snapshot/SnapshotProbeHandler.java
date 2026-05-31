package fun.efto.luna.core.plugin.builtin.snapshot;

import fun.efto.luna.core.injection.InjectionCommand;
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
public class SnapshotProbeHandler extends AbstractProbeHandler {

    private final SnapshotExpressionHandler delegate = new SnapshotExpressionHandler();

    @Override
    public String getProbeType() {
        return "SNAPSHOT";
    }

    @Override
    public boolean usesCode() {
        return false;
    }

    @Override
    public Set<String> supportedInjectionLocations() {
        return new HashSet<>(Arrays.asList("line_before", "line_after", "method_enter", "method_exit"));
    }

    @Override
    protected ValidationResult doValidate(InjectionCommand request) {
        if (request.getCodeType() != null) {
            return ValidationResult.okWithWarnings(Collections.singletonList("SNAPSHOT probe ignores codeType"));
        }
        return ValidationResult.ok();
    }

    @Override
    public void handle(CompiledCode code, GenerateContext ctx) {
        delegate.generateBytecode(ctx);
    }
}
