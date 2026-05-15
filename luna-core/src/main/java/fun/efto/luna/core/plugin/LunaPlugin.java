package fun.efto.luna.core.plugin;

import fun.efto.luna.core.rule.template.RuleTemplate;

import java.util.Collections;
import java.util.List;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
public interface LunaPlugin {

    String getId();

    String getDisplayName();

    String getVersion();

    String getAuthor();

    String getCategory();

    default List<String> getDependencies() { return Collections.emptyList(); }

    void initialize(PluginContext context);

    default void destroy() {}

    default void getControllers(List<LunaController> controllers) {}

    default void getTemplates(List<RuleTemplate> templates) {}

    default boolean isBuiltin() { return false; }
}
