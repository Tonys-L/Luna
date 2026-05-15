package fun.efto.luna.core.plugin.builtin;

import fun.efto.luna.core.plugin.*;
import fun.efto.luna.core.plugin.handler.TraceExpressionHandler;
import fun.efto.luna.core.rule.template.BuiltinTemplates;
import fun.efto.luna.core.rule.template.RuleTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
public class TracePlugin implements LunaPlugin {
    @Override public String getId() { return "trace"; }
    @Override public String getDisplayName() { return "方法耗时追踪"; }
    @Override public String getVersion() { return "1.0.0"; }
    @Override public String getAuthor() { return "Luna Core Team"; }
    @Override public String getCategory() { return "performance"; }
    @Override public boolean isBuiltin() { return true; }

    @Override
    public List<String> getDependencies() {
        return Arrays.asList("method-injection");
    }

    @Override
    public void initialize(PluginContext ctx) {
        ctx.registerExpressionHandler(new TraceExpressionHandler());
    }

    @Override
    public void getTemplates(List<RuleTemplate> templates) {
        templates.addAll(BuiltinTemplates.allTraceTemplates());
    }
}
