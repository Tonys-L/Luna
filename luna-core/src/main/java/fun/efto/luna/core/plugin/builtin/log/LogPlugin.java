package fun.efto.luna.core.plugin.builtin.log;

import fun.efto.luna.core.bytecode.asm.assembler.ExpressionBytecodeAssembler;
import fun.efto.luna.core.injection.code.CodeType;
import fun.efto.luna.core.plugin.*;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/16 10:00
 */
public class LogPlugin implements LunaPlugin {
    @Override public String getId() { return "log"; }
    @Override public String getDisplayName() { return "日志注入"; }
    @Override public String getVersion() { return "1.0.0"; }
    @Override public String getAuthor() { return "Luna Core Team"; }
    @Override public String getCategory() { return "injection"; }

    @Override
    public void initialize(PluginContext ctx) {
        ctx.registerExpressionHandler(new LogExpressionHandler());
        ctx.registerProbeHandler(new LogProbeHandler());
        ctx.registerAssembler(CodeType.EXPRESSION, new ExpressionBytecodeAssembler());
        ctx.registerCodeCompilerStrategy(new LogCodeCompilerStrategy());
        ctx.registerTemplate(LogTemplates.methodAccessLog());
    }
}
