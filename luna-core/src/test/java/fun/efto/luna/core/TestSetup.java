package fun.efto.luna.core;

import fun.efto.luna.core.asm.assembler.BytecodeAssemblerRegistry;
import fun.efto.luna.core.asm.assembler.ExpressionBytecodeAssembler;
import fun.efto.luna.core.injection.code.type.CodeType;
import fun.efto.luna.core.plugin.builtin.CoreModuleInitializer;
import fun.efto.luna.core.plugin.builtin.log.LogExpressionHandler;
import fun.efto.luna.core.plugin.builtin.snapshot.SnapshotExpressionHandler;
import fun.efto.luna.core.plugin.builtin.trace.TraceExpressionHandler;
import fun.efto.luna.core.plugin.registry.ExpressionHandlerRegistry;

/**
 * 单元测试专用的组件统一初始化和注册工具类，为隔离测试提供完整的插桩运行环境
 *
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/17 19:00
 */
public final class TestSetup {

    public static synchronized void init() {
        CoreModuleInitializer.initialize();

        ExpressionHandlerRegistry.getInstance().register(new LogExpressionHandler());
        ExpressionHandlerRegistry.getInstance().register(new SnapshotExpressionHandler());
        ExpressionHandlerRegistry.getInstance().register(new TraceExpressionHandler());

        BytecodeAssemblerRegistry.getInstance().register(CodeType.EXPRESSION, new ExpressionBytecodeAssembler());
    }
}
