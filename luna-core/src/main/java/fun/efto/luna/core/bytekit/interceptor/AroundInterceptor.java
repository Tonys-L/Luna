/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/31 22:30
 */
package fun.efto.luna.core.bytekit.interceptor;

import com.alibaba.bytekit.asm.interceptor.annotation.AtEnter;
import com.alibaba.bytekit.asm.interceptor.annotation.AtExit;
import com.alibaba.bytekit.asm.binding.Binding;
import fun.efto.luna.core.bytekit.bridge.LunaSpyBridge;

public class AroundInterceptor {

    @AtEnter(inline = true, suppress = Throwable.class, suppressHandler = SuppressHandler.class)
    public static void onEnter(
            @Binding.This Object target,
            @Binding.Args Object[] args,
            @Binding.MethodName String methodName) {
        LunaSpyBridge.onMethodEnter(target, args, methodName);
    }

    @AtExit(inline = true, suppress = Throwable.class, suppressHandler = SuppressHandler.class)
    public static void onExit(
            @Binding.This Object target,
            @Binding.Args Object[] args,
            @Binding.MethodName String methodName,
            @Binding.Return Object returnValue) {
        LunaSpyBridge.onMethodExit(target, args, methodName, returnValue);
    }
}
