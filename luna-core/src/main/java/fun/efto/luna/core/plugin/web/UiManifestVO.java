package fun.efto.luna.core.plugin.web;

import java.util.List;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/27 17:00
 */
public class UiManifestVO {

    private final List<InjectionLocationEntry> injectionLocations;
    private final List<ExpressionProtocolEntry> expressionProtocols;
    private final List<TemplateEntry> templates;

    public UiManifestVO(List<InjectionLocationEntry> injectionLocations,
                        List<ExpressionProtocolEntry> expressionProtocols,
                        List<TemplateEntry> templates) {
        this.injectionLocations = injectionLocations;
        this.expressionProtocols = expressionProtocols;
        this.templates = templates;
    }

    public List<InjectionLocationEntry> getInjectionLocations() { return injectionLocations; }
    public List<ExpressionProtocolEntry> getExpressionProtocols() { return expressionProtocols; }
    public List<TemplateEntry> getTemplates() { return templates; }

    public static class InjectionLocationEntry {
        private final String name;
        private final String displayName;
        private final String category;

        public InjectionLocationEntry(String name, String displayName, String category) {
            this.name = name;
            this.displayName = displayName;
            this.category = category;
        }

        public String getName() { return name; }
        public String getDisplayName() { return displayName; }
        public String getCategory() { return category; }
    }

    public static class ExpressionProtocolEntry {
        private final String protocol;
        private final String displayName;
        private final String syntax;

        public ExpressionProtocolEntry(String protocol, String displayName, String syntax) {
            this.protocol = protocol;
            this.displayName = displayName;
            this.syntax = syntax;
        }

        public String getProtocol() { return protocol; }
        public String getDisplayName() { return displayName; }
        public String getSyntax() { return syntax; }
    }

    public static class TemplateEntry {
        private final String name;
        private final String displayName;
        private final String description;
        private final String category;

        public TemplateEntry(String name, String displayName, String description, String category) {
            this.name = name;
            this.displayName = displayName;
            this.description = description;
            this.category = category;
        }

        public String getName() { return name; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public String getCategory() { return category; }
    }
}
