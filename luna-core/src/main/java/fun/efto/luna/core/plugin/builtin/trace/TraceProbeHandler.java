package fun.efto.luna.core.plugin.builtin.trace;

import fun.efto.luna.core.injection.InjectRequest;
import fun.efto.luna.core.injection.code.CompiledCode;
import fun.efto.luna.core.plugin.AbstractProbeHandler;
import fun.efto.luna.core.plugin.GenerateContext;
import fun.efto.luna.core.plugin.FormFieldSchema;
import fun.efto.luna.core.plugin.ValidationResult;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since : 2026/06/01 01:30
 */
public class TraceProbeHandler extends AbstractProbeHandler {

    private static final Set<String> SUPPORTED_LOCATIONS = Collections.unmodifiableSet(
            new HashSet<>(Collections.singletonList("method_around"))
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
        return ValidationResult.ok();
    }

    @Override
    public String getDisplayName() { return "方法耗时"; }

    @Override
    public String getSyntax() { return "自动统计方法耗时，可配置阈值(ms)"; }

    @Override
    public String getIcon() { return "fas fa-stopwatch"; }

    @Override
    public String getGlyphColor() { return "#f59e0b"; }

    @Override
    public String getCategory() { return "performance"; }

    @Override
    public List<FormFieldSchema> getConfigSchema() {
        return Collections.singletonList(
            new FormFieldSchema("code", "耗时阈值(ms)", "number", "0", null, false, "仅输出超过阈值的耗时，0 表示全部输出")
        );
    }

    @Override
    public void handle(CompiledCode code, GenerateContext ctx) {
        delegate.generateBytecode(ctx);
    }
}
