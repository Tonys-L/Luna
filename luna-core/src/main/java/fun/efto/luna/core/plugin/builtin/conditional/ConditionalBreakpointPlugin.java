package fun.efto.luna.core.plugin.builtin.conditional;

import fun.efto.luna.core.plugin.LunaPlugin;
import fun.efto.luna.core.plugin.PluginContext;

import java.util.Arrays;
import java.util.List;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/16 10:00
 */
public class ConditionalBreakpointPlugin implements LunaPlugin {

    @Override public String getId() { return "conditional-breakpoint"; }
    @Override public String getDisplayName() { return "条件断点"; }
    @Override public String getVersion() { return "1.0.0"; }
    @Override public String getAuthor() { return "Luna Core Team"; }
    @Override public String getCategory() { return "debug"; }

    @Override
    public List<String> getDependencies() {
        return Arrays.asList("snapshot", "line-target");
    }

    @Override
    public void initialize(PluginContext context) {
    }
}
