package fun.efto.luna.core.plugin.builtin.snapshot;

import fun.efto.luna.core.injection.InjectionCommand;
import fun.efto.luna.core.plugin.AbstractProbeHandler;
import fun.efto.luna.core.plugin.GenerateContext;
import fun.efto.luna.core.plugin.ValidationResult;

import java.util.List;
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
        return Set.of("line_before", "line_after", "method_enter", "method_exit");
    }

    @Override
    protected ValidationResult doValidate(InjectionCommand request) {
        if (request.getCodeType() != null) {
            return ValidationResult.okWithWarnings(List.of("SNAPSHOT probe ignores codeType"));
        }
        return ValidationResult.ok();
    }

    @Override
    public void handle(Object code, GenerateContext ctx) {
        delegate.generateBytecode(ctx);
    }
}
