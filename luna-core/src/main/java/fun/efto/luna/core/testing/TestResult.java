package fun.efto.luna.core.testing;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/02 10:00
 */
public class TestResult {

    private final boolean success;
    private final String output;
    private final byte[] generatedBytecode;
    private final String error;
    private final int originalBytecodeSize;

    public TestResult(boolean success, String output, byte[] generatedBytecode, String error) {
        this(success, output, generatedBytecode, error, 0);
    }

    public TestResult(boolean success, String output, byte[] generatedBytecode, String error, int originalBytecodeSize) {
        this.success = success;
        this.output = output;
        this.generatedBytecode = generatedBytecode;
        this.error = error;
        this.originalBytecodeSize = originalBytecodeSize;
    }

    public static TestResult success(String output, byte[] generatedBytecode) {
        return new TestResult(true, output, generatedBytecode, null, 0);
    }

    public static TestResult success(String output, byte[] generatedBytecode, int originalBytecodeSize) {
        return new TestResult(true, output, generatedBytecode, null, originalBytecodeSize);
    }

    public static TestResult fail(String error) {
        return new TestResult(false, null, null, error, 0);
    }

    public static TestResult fail(String error, byte[] generatedBytecode) {
        return new TestResult(false, null, generatedBytecode, error, 0);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getOutput() {
        return output;
    }

    public byte[] getGeneratedBytecode() {
        return generatedBytecode;
    }

    public String getError() {
        return error;
    }

    public int getOriginalBytecodeSize() {
        return originalBytecodeSize;
    }
}
