package fun.efto.luna.core.plugin.web;

import fun.efto.luna.core.plugin.FormFieldSchema;

import java.util.List;
import java.util.Set;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since : 2026/05/27 17:00
 */
public class UiManifestVO {

    private final List<InjectionLocationEntry> injectionLocations;
    private final List<ProbeTypeEntry> probeTypes;

    public UiManifestVO(List<InjectionLocationEntry> injectionLocations,
                        List<ProbeTypeEntry> probeTypes) {
        this.injectionLocations = injectionLocations;
        this.probeTypes = probeTypes;
    }

    public List<InjectionLocationEntry> getInjectionLocations() { return injectionLocations; }
    public List<ProbeTypeEntry> getProbeTypes() { return probeTypes; }

    public static class InjectionLocationEntry {
        private final String name;
        private final String displayName;
        private final String category;
        private final String categoryLabel;
        private final String color;

        public InjectionLocationEntry(String name, String displayName, String category, String categoryLabel, String color) {
            this.name = name;
            this.displayName = displayName;
            this.category = category;
            this.categoryLabel = categoryLabel;
            this.color = color;
        }

        public String getName() { return name; }
        public String getDisplayName() { return displayName; }
        public String getCategory() { return category; }
        public String getCategoryLabel() { return categoryLabel; }
        public String getColor() { return color; }
    }

    public static class ProbeTypeEntry {
        private final String probeType;
        private final String displayName;
        private final String syntax;
        private final String icon;
        private final String category;
        private final boolean usesCode;
        private final Set<String> supportedInjectionLocations;
        private final String quickActionBehavior;
        private final String glyphColor;
        private final List<FormFieldSchema> configSchema;

        public ProbeTypeEntry(String probeType, String displayName, String syntax,
                              String icon, String category, boolean usesCode,
                              Set<String> supportedInjectionLocations,
                              String quickActionBehavior,
                              String glyphColor,
                              List<FormFieldSchema> configSchema) {
            this.probeType = probeType;
            this.displayName = displayName;
            this.syntax = syntax;
            this.icon = icon;
            this.category = category;
            this.usesCode = usesCode;
            this.supportedInjectionLocations = supportedInjectionLocations;
            this.quickActionBehavior = quickActionBehavior;
            this.glyphColor = glyphColor;
            this.configSchema = configSchema;
        }

        public String getProbeType() { return probeType; }
        public String getDisplayName() { return displayName; }
        public String getSyntax() { return syntax; }
        public String getIcon() { return icon; }
        public String getCategory() { return category; }
        public boolean isUsesCode() { return usesCode; }
        public Set<String> getSupportedInjectionLocations() { return supportedInjectionLocations; }
        public String getQuickActionBehavior() { return quickActionBehavior; }
        public String getGlyphColor() { return glyphColor; }
        public List<FormFieldSchema> getConfigSchema() { return configSchema; }
    }
}
