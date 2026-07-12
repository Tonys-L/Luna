package fun.efto.luna.core.plugin.builtin.invocation;

import fun.efto.luna.core.plugin.LunaPlugin;
import fun.efto.luna.core.plugin.PluginContext;

import java.util.Arrays;
import java.util.List;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since : 2026/07/05 20:00
 */
public class InvocationPlugin implements LunaPlugin {

    @Override public String getId() { return "invocation"; }

    @Override public String getDisplayName() { return "调用链追踪"; }

    @Override public String getVersion() { return "1.0.0"; }

    @Override public String getAuthor() { return "Luna Core Team"; }

    @Override public String getCategory() { return "observability"; }

    @Override
    public List<String> getDependencies() {
        return Arrays.asList("method-target");
    }

    @Override
    public void initialize(PluginContext ctx) {
        ctx.registerProbeHandler(new InvocationProbeHandler());
    }
}
