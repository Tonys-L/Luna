package fun.efto.luna.agent.web.vo;

/**
 * 测试步骤 VO
 *
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/27 12:00
 */
public class TestStepVO {
    private final boolean success;
    private final boolean skipped;
    private final String reason;
    private final String error;
    private final String message;
    private final Integer bytecodeSize;
    private final Integer originalBytecodeSize;
    private final String output;
    private final String expected;
    private final String actual;

    private TestStepVO(boolean success, boolean skipped, String reason, String error,
                       String message, Integer bytecodeSize, Integer originalBytecodeSize,
                       String output, String expected, String actual) {
        this.success = success;
        this.skipped = skipped;
        this.reason = reason;
        this.error = error;
        this.message = message;
        this.bytecodeSize = bytecodeSize;
        this.originalBytecodeSize = originalBytecodeSize;
        this.output = output;
        this.expected = expected;
        this.actual = actual;
    }

    public static TestStepVO success(String message) {
        return new TestStepVO(true, false, null, null, message, null, null, null, null, null);
    }

    public static TestStepVO successWithBytecode(int bytecodeSize, int originalBytecodeSize, String message) {
        return new TestStepVO(true, false, null, null, message, bytecodeSize, originalBytecodeSize, null, null, null);
    }

    public static TestStepVO failure(String error) {
        return new TestStepVO(false, false, null, error, null, null, null, null, null, null);
    }

    public static TestStepVO skipped(String reason) {
        return new TestStepVO(false, true, reason, null, null, null, null, null, null, null);
    }

    public static TestStepVO verifySuccess(String output) {
        return new TestStepVO(true, false, null, null, null, null, null, output, null, null);
    }

    public static TestStepVO verifyFailure(String error) {
        return new TestStepVO(false, false, null, error, null, null, null, null, null, null);
    }

    public static TestStepVO validateResult(boolean found, String expected, String actual) {
        return new TestStepVO(found, false, null, null, null, null, null, null, expected, actual);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public String getReason() {
        return reason;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public Integer getBytecodeSize() {
        return bytecodeSize;
    }

    public Integer getOriginalBytecodeSize() {
        return originalBytecodeSize;
    }

    public String getOutput() {
        return output;
    }

    public String getExpected() {
        return expected;
    }

    public String getActual() {
        return actual;
    }
}
