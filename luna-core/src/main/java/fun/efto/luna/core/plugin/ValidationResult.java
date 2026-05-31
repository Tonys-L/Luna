package fun.efto.luna.core.plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/01 01:30
 */
public class ValidationResult {

    private final boolean valid;
    private final String errorMessage;
    private final List<String> warnings;

    private ValidationResult(boolean valid, String errorMessage, List<String> warnings) {
        this.valid = valid;
        this.errorMessage = errorMessage;
        this.warnings = warnings != null ? warnings : new ArrayList<>();
    }

    public static ValidationResult ok() {
        return new ValidationResult(true, null, null);
    }

    public static ValidationResult fail(String message) {
        return new ValidationResult(false, message, null);
    }

    public static ValidationResult okWithWarnings(List<String> warnings) {
        return new ValidationResult(true, null, warnings);
    }

    public boolean isValid() { return valid; }
    public String getErrorMessage() { return errorMessage; }
    public List<String> getWarnings() { return warnings; }
}
