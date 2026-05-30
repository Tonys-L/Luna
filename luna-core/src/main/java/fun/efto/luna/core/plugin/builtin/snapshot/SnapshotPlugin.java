package fun.efto.luna.core.plugin.builtin.snapshot;

import fun.efto.luna.core.bytecode.asm.assembler.ExpressionBytecodeAssembler;
import fun.efto.luna.core.injection.code.CodeType;
import fun.efto.luna.core.plugin.*;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/16 10:00
 */
public class SnapshotPlugin implements LunaPlugin {
    @Override public String getId() { return "snapshot"; }
    @Override public String getDisplayName() { return "快照注入"; }
    @Override public String getVersion() { return "1.0.0"; }
    @Override public String getAuthor() { return "Luna Core Team"; }
    @Override public String getCategory() { return "injection"; }

    @Override
    public void initialize(PluginContext ctx) {
        ctx.registerExpressionHandler(new SnapshotExpressionHandler());
        ctx.registerAssembler(CodeType.SNAPSHOT, new ExpressionBytecodeAssembler());
        ctx.registerCodeCompilerStrategy(new SnapshotCodeCompilerStrategy());
        ctx.registerTemplate(SnapshotTemplates.lineSnapshot());
    }
}
