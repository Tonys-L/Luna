package fun.efto.luna.core.plugin.web;

import java.util.List;

/**
 * 插件 UI 清单 VO，描述前端可用的注入类型、表达式协议和模板。
 *
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/27 17:00
 */
public class UiManifestVO {

    private final List<InjectionTypeEntry> injectionTypes;
    private final List<ExpressionProtocolEntry> expressionProtocols;
    private final List<TemplateEntry> templates;

    public UiManifestVO(List<InjectionTypeEntry> injectionTypes,
                        List<ExpressionProtocolEntry> expressionProtocols,
                        List<TemplateEntry> templates) {
        this.injectionTypes = injectionTypes;
        this.expressionProtocols = expressionProtocols;
        this.templates = templates;
    }

    public List<InjectionTypeEntry> getInjectionTypes() { return injectionTypes; }
    public List<ExpressionProtocolEntry> getExpressionProtocols() { return expressionProtocols; }
    public List<TemplateEntry> getTemplates() { return templates; }

    public static class InjectionTypeEntry {
        private final String name;
        private final String displayName;
        private final String category;

        public InjectionTypeEntry(String name, String displayName, String category) {
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
