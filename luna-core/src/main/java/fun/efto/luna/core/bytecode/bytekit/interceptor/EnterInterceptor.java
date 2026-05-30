package fun.efto.luna.core.bytecode.bytekit.interceptor;

import com.alibaba.bytekit.asm.interceptor.annotation.AtEnter;
import com.alibaba.bytekit.asm.binding.Binding;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/31 22:00
 */
public class EnterInterceptor {

    @AtEnter(inline = true, suppress = Throwable.class, suppressHandler = SuppressHandler.class)
    public static void onEnter(
            @Binding.This Object target,
            @Binding.Args Object[] args,
            @Binding.MethodName String methodName) {
    }
}
