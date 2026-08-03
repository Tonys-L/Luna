package fun.efto.luna.core.plugin.builtin.log;

import fun.efto.luna.core.injection.InjectRequest;
import fun.efto.luna.core.injection.code.CompiledCode;
import fun.efto.luna.core.plugin.AbstractProbeHandler;
import fun.efto.luna.core.plugin.GenerateContext;
import fun.efto.luna.core.plugin.ValidationResult;

import fun.efto.luna.core.plugin.FormFieldSchema;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/01 01:30
 */
public class LogProbeHandler extends AbstractProbeHandler {

    private static final Set<String> SUPPORTED_LOCATIONS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("method_enter", "method_exit", "method_around", "line_before", "line_after", "invoke", "exception_exit"))
    );

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
    public String getCodeType() {
        return "EXPRESSION";
    }

    @Override
    public Set<String> supportedInjectionLocations() {
        return SUPPORTED_LOCATIONS;
    }

    @Override
    protected ValidationResult doValidate(InjectRequest request) {
        if (request.getCode() == null || request.getCode().isEmpty()) {
            return ValidationResult.fail("code is required for LOG probe");
        }
        return ValidationResult.ok();
    }

    @Override
    public String getDisplayName() { return "日志表达式"; }

    @Override
    public String getSyntax() { return "使用 {} 占位符，如: User ID is {}"; }

    @Override
    public String getIcon() { return "fas fa-print"; }

    @Override
    public String getGlyphColor() { return "#6366f1"; }

    @Override
    public List<FormFieldSchema> getConfigSchema() {
        return java.util.Arrays.asList(
            new FormFieldSchema("code", "表达式", "textarea", null, null, true, "输入日志表达式"),
            new FormFieldSchema("condition", "条件", "text", null, null, false, "可选条件表达式")
        );
    }

    @Override
    public void handle(CompiledCode code, GenerateContext ctx) {
        delegate.generateBytecode(ctx);
    }
}
