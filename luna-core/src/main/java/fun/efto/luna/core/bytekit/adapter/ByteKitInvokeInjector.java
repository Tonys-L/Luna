/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/06/01 00:00
 */
package fun.efto.luna.core.bytekit.adapter;

import com.alibaba.bytekit.asm.MethodProcessor;
import com.alibaba.bytekit.asm.interceptor.InterceptorProcessor;
import com.alibaba.bytekit.asm.interceptor.parser.DefaultInterceptorClassParser;
import fun.efto.luna.core.asm.AsmInjectionContext;
import fun.efto.luna.core.bytekit.interceptor.InvokeInterceptor;

import java.util.List;

public class ByteKitInvokeInjector extends ByteKitInjectorBase {

    @Override
    protected List<InterceptorProcessor> createInterceptorProcessors(MethodProcessor methodProcessor, AsmInjectionContext context) {
        DefaultInterceptorClassParser parser = new DefaultInterceptorClassParser();
        return parser.parse(InvokeInterceptor.class);
    }
}
