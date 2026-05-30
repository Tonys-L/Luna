/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/31 22:30
 */
package fun.efto.luna.core.bytekit.adapter;

import com.alibaba.bytekit.asm.MethodProcessor;
import com.alibaba.bytekit.asm.interceptor.InterceptorProcessor;
import com.alibaba.bytekit.asm.interceptor.parser.DefaultInterceptorClassParser;
import fun.efto.luna.core.asm.AsmInjectionContext;
import fun.efto.luna.core.bytekit.interceptor.ExitInterceptor;

import java.util.List;

public class ByteKitExitInjector extends ByteKitInjectorBase {

    @Override
    protected AsmMethodExpressionInjector.Phase getExpressionPhase() {
        return AsmMethodExpressionInjector.Phase.EXIT;
    }

    @Override
    protected List<InterceptorProcessor> createInterceptorProcessors(MethodProcessor methodProcessor, AsmInjectionContext context) {
        DefaultInterceptorClassParser parser = new DefaultInterceptorClassParser();
        return parser.parse(ExitInterceptor.class);
    }
}
