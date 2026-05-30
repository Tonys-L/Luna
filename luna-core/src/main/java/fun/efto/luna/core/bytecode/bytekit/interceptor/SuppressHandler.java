package fun.efto.luna.core.bytecode.bytekit.interceptor;

import com.alibaba.bytekit.asm.interceptor.annotation.ExceptionHandler;
import com.alibaba.bytekit.asm.binding.Binding;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/31 23:30
 */
public class SuppressHandler {

    @ExceptionHandler(inline = true)
    public static void onSuppress(@Binding.Throwable Throwable t) {
    }
}
