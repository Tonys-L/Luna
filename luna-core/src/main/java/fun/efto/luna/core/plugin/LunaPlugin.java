package fun.efto.luna.core.plugin;

import java.util.Collections;
import java.util.List;

/**
 * @author ：Tony.L(286269159@qq.com)
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

}
