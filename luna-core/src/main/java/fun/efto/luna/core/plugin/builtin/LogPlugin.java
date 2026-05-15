package fun.efto.luna.core.plugin.builtin;

import fun.efto.luna.core.injection.code.type.CodeType;
import fun.efto.luna.core.asm.assmebler.ExpressionBytecodeAssembler;
import fun.efto.luna.core.plugin.*;
import fun.efto.luna.core.plugin.handler.LogExpressionHandler;
import fun.efto.luna.core.rule.template.BuiltinTemplates;
import fun.efto.luna.core.rule.template.RuleTemplate;

import java.util.Collections;
import java.util.List;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
public class LogPlugin implements LunaPlugin {
    @Override public String getId() { return "log"; }
    @Override public String getDisplayName() { return "日志注入"; }
    @Override public String getVersion() { return "1.0.0"; }
    @Override public String getAuthor() { return "Luna Core Team"; }
    @Override public String getCategory() { return "injection"; }
    @Override public boolean isBuiltin() { return true; }

    @Override
    public void initialize(PluginContext ctx) {
        ctx.registerExpressionHandler(new LogExpressionHandler());
        ctx.registerAssembler(CodeType.EXPRESSION, new ExpressionBytecodeAssembler());
    }

    @Override
    public void getTemplates(List<RuleTemplate> templates) {
        templates.add(BuiltinTemplates.methodAccessLog());
    }
}
