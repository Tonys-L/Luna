package fun.efto.luna.core.plugin;

import java.util.List;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:00
 */
public class PluginInfo {

    private final String id;
    private final String displayName;
    private final String version;
    private final String author;
    private final String category;
    private volatile PluginState state;
    private final List<String> dependencies;

    public PluginInfo(String id, String displayName, String version, String author,
                      String category, PluginState state, List<String> dependencies) {
        this.id = id;
        this.displayName = displayName;
        this.version = version;
        this.author = author;
        this.category = category;
        this.state = state;
        this.dependencies = dependencies;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getVersion() {
        return version;
    }

    public String getAuthor() {
        return author;
    }

    public String getCategory() {
        return category;
    }

    public PluginState getState() {
        return state;
    }

    public void setState(PluginState state) {
        this.state = state;
    }

    public List<String> getDependencies() {
        return dependencies;
    }
}
