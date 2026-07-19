package fun.efto.luna.core.bytecode.bytekit.interceptor;

import com.alibaba.bytekit.asm.interceptor.annotation.AtExceptionExit;
import com.alibaba.bytekit.asm.binding.Binding;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/01 00:00
 */
public class ExceptionExitInterceptor {

    @AtExceptionExit(inline = true, suppress = Throwable.class, suppressHandler = SuppressHandler.class)
    public static void onExceptionExit(
            @Binding.This Object target,
            @Binding.MethodName String methodName,
            @Binding.Throwable Throwable t) {
    }
}
