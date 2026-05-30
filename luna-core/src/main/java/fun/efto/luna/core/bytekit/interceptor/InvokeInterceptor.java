/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/06/01 00:00
 */
package fun.efto.luna.core.bytekit.interceptor;

import com.alibaba.bytekit.asm.interceptor.annotation.AtInvoke;
import com.alibaba.bytekit.asm.binding.Binding;

public class InvokeInterceptor {

    @AtInvoke(inline = true, suppress = Throwable.class, suppressHandler = SuppressHandler.class, owner = String.class, name = "toUpperCase")
    public static void onInvoke(
            @Binding.This Object target,
            @Binding.MethodName String methodName) {
    }
}
