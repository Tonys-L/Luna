package fun.efto.luna.core.injection.port;

/**
 * LocalVarValidator（出站端口）。
 * 领域层需要"校验局部变量引用合法性"的能力，不直接依赖 ASM LocalVariableScanner。
 *
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/27 18:00
 */
public interface LocalVarValidator {

    /**
     * 校验代码中的局部变量引用在目标位置是否合法
     *
     * @param code        注入代码
     * @param className   类名
     * @param methodName  方法名
     * @param methodDesc  方法描述符
     * @param lineNumber  行号
     * @param classBytes  类字节码
     * @return 校验错误信息，null 表示通过
     */
    String validateLocalVarReferences(String code, String className, String methodName,
                                       String methodDesc, Integer lineNumber, byte[] classBytes);
}
