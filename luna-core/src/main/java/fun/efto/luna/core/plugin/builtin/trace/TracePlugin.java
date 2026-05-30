package fun.efto.luna.core.plugin.builtin.trace;

import fun.efto.luna.core.plugin.*;
import fun.efto.luna.core.rule.template.RuleTemplate;

import java.util.Arrays;
import java.util.List;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/16 10:00
 */
public class TracePlugin implements LunaPlugin {
    @Override public String getId() { return "trace"; }
    @Override public String getDisplayName() { return "方法耗时追踪"; }
    @Override public String getVersion() { return "1.0.0"; }
    @Override public String getAuthor() { return "Luna Core Team"; }
    @Override public String getCategory() { return "performance"; }

    @Override
    public List<String> getDependencies() {
        return Arrays.asList("method-target");
    }

    @Override
    public void initialize(PluginContext ctx) {
        ctx.registerExpressionHandler(new TraceExpressionHandler());
        for (RuleTemplate t : TraceTemplates.all()) ctx.registerTemplate(t);
    }
}
