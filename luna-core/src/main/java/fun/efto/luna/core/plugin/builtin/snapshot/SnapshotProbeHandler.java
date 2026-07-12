package fun.efto.luna.core.plugin.builtin.snapshot;

import fun.efto.luna.core.injection.InjectRequest;
import fun.efto.luna.core.injection.code.CompiledCode;
import fun.efto.luna.core.plugin.AbstractProbeHandler;
import fun.efto.luna.core.plugin.GenerateContext;
import fun.efto.luna.core.plugin.QuickActionBehavior;
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

    private static final Set<String> SUPPORTED_LOCATIONS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("line_before", "line_after", "method_enter", "method_exit"))
    );

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
        return SUPPORTED_LOCATIONS;
    }

    @Override
    protected ValidationResult doValidate(InjectRequest request) {
        if (request.getCodeType() != null) {
            return ValidationResult.okWithWarnings(Collections.singletonList("SNAPSHOT probe ignores codeType"));
        }
        return ValidationResult.ok();
    }

    @Override
    public String getDisplayName() { return "内存快照"; }

    @Override
    public String getSyntax() { return "自动捕获当前作用域内所有局部变量"; }

    @Override
    public String getIcon() { return "fas fa-camera"; }

    @Override
    public String getGlyphColor() { return "#06b6d4"; }

    @Override
    public QuickActionBehavior getQuickActionBehavior() { return QuickActionBehavior.FORM; }

    @Override
    public void handle(CompiledCode code, GenerateContext ctx) {
        delegate.generateBytecode(ctx);
    }
}
