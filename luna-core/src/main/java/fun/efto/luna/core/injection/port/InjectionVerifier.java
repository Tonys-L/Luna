package fun.efto.luna.core.injection.port;

/**
 * InjectionVerifier（出站端口）。
 * 领域层需要"验证注入效果"的能力，不关心底层是反射调用还是其他方式。
 *
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/27 20:00
 */
public interface InjectionVerifier {

    /**
     * 临时注入并验证效果（注入 → 反射调用 → 捕获输出 → 还原）
     *
     * @param className     类名
     * @param methodName    方法名
     * @param descriptor    方法描述符
     * @param InjectionLocation 注入类型
     * @param code          注入代码
     * @return 验证结果
     */
    VerifyResult testInjection(String className, String methodName, String descriptor,
                               String injectionLocation, String code);

    /**
     * 仅验证已注入的方法（反射调用 → 捕获输出）
     *
     * @param className  类名
     * @param methodName 方法名
     * @return 验证结果
     */
    VerifyResult verifyOnly(String className, String methodName);

    /**
     * 验证结果（领域对象）
     */
    class VerifyResult {
        private final boolean success;
        private final String output;
        private final String error;

        public VerifyResult(boolean success, String output, String error) {
            this.success = success;
            this.output = output;
            this.error = error;
        }

        public static VerifyResult success(String output) {
            return new VerifyResult(true, output, null);
        }

        public static VerifyResult failure(String error) {
            return new VerifyResult(false, null, error);
        }

        public boolean isSuccess() { return success; }
        public String getOutput() { return output; }
        public String getError() { return error; }
    }
}
