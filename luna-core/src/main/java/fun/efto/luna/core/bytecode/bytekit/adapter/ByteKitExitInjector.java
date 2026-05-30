package fun.efto.luna.core.bytecode.bytekit.adapter;

import com.alibaba.bytekit.asm.MethodProcessor;
import com.alibaba.bytekit.asm.interceptor.InterceptorProcessor;
import com.alibaba.bytekit.asm.interceptor.parser.DefaultInterceptorClassParser;
import fun.efto.luna.core.bytecode.asm.AsmInjectionContext;
import fun.efto.luna.core.bytecode.asm.AsmMethodExpressionInjector;
import fun.efto.luna.core.bytecode.bytekit.interceptor.ExitInterceptor;

import java.util.List;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/31 22:30
 */
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
