package fun.efto.luna.core.plugin.builtin.snapshot;

import fun.efto.luna.core.plugin.*;
import fun.efto.luna.core.plugin.builtin.log.ExpressionCodeEngine;

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
        ctx.registerProbeHandler(new SnapshotProbeHandler());
        ctx.registerCodeEngine(new ExpressionCodeEngine());
        ctx.registerTemplate(SnapshotTemplates.lineSnapshot());
    }
}
