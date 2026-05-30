package fun.efto.luna.core;

import fun.efto.luna.core.asm.assembler.BytecodeAssemblerRegistry;
import fun.efto.luna.core.asm.assembler.ExpressionBytecodeAssembler;
import fun.efto.luna.core.asm.injector.BytecodeInjectorRegistry;
import fun.efto.luna.core.injection.code.type.CodeType;
import fun.efto.luna.core.plugin.builtin.line.AfterLineInjector;
import fun.efto.luna.core.plugin.builtin.line.BeforeLineInjector;
import fun.efto.luna.core.plugin.builtin.line.LineNumberInjectionType;
import fun.efto.luna.core.plugin.builtin.log.LogExpressionHandler;
import fun.efto.luna.core.plugin.builtin.method.AroundMethodInjector;
import fun.efto.luna.core.plugin.builtin.method.EnterMethodInjector;
import fun.efto.luna.core.plugin.builtin.method.ExitMethodInjector;
import fun.efto.luna.core.plugin.builtin.method.MethodInjectionType;
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
        // 1. 注册所有的 ExpressionHandler（总是注册，防止被其他测试的 clear() 清理后丢失环境）
        ExpressionHandlerRegistry.getInstance().register(new LogExpressionHandler());
        ExpressionHandlerRegistry.getInstance().register(new SnapshotExpressionHandler());
        ExpressionHandlerRegistry.getInstance().register(new TraceExpressionHandler());

        // 2. 注册所有的 BytecodeAssembler
        BytecodeAssemblerRegistry.getInstance().register(CodeType.EXPRESSION, new ExpressionBytecodeAssembler());

        // 3. 注册所有的 BytecodeInjector
        BytecodeInjectorRegistry.getInstance().register(MethodInjectionType.ENTER, new EnterMethodInjector());
        BytecodeInjectorRegistry.getInstance().register(MethodInjectionType.EXIT, new ExitMethodInjector());
        BytecodeInjectorRegistry.getInstance().register(MethodInjectionType.AROUND, new AroundMethodInjector());
        BytecodeInjectorRegistry.getInstance().register(LineNumberInjectionType.BEFORE, new BeforeLineInjector());
        BytecodeInjectorRegistry.getInstance().register(LineNumberInjectionType.AFTER, new AfterLineInjector());
    }
}
