package fun.efto.luna.core.plugin.web;

import fun.efto.luna.core.injection.target.InjectionType;
import fun.efto.luna.core.plugin.ExpressionHandler;
import fun.efto.luna.core.plugin.registry.ExpressionHandlerRegistry;
import fun.efto.luna.core.plugin.registry.InjectionTypeRegistry;
import fun.efto.luna.core.rule.template.RuleTemplate;
import fun.efto.luna.core.rule.template.TemplateRegistry;

import java.util.*;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/25 20:00
 */
public class UiManifest {

    private List<Map<String, Object>> injectionTypes;
    private List<Map<String, Object>> expressionProtocols;
    private List<Map<String, Object>> templates;

    public static UiManifest collect() {
        UiManifest manifest = new UiManifest();

        List<Map<String, Object>> types = new ArrayList<>();
        for (InjectionType type : InjectionTypeRegistry.getInstance().getAll()) {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("name", type.getName());
            info.put("description", type.getDescription());
            info.put("aliases", type.getAliases());
            types.add(info);
        }
        manifest.injectionTypes = types;

        List<Map<String, Object>> protocols = new ArrayList<>();
        for (ExpressionHandler handler : ExpressionHandlerRegistry.getInstance().getAll()) {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("protocol", handler.getProtocol());
            protocols.add(info);
        }
        manifest.expressionProtocols = protocols;

        List<Map<String, Object>> tmpls = new ArrayList<>();
        for (RuleTemplate tmpl : TemplateRegistry.getInstance().getAllTemplates()) {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("name", tmpl.getName());
            info.put("displayName", tmpl.getDisplayName());
            info.put("description", tmpl.getDescription());
            info.put("version", tmpl.getVersion());
            info.put("author", tmpl.getAuthor());
            info.put("category", tmpl.getCategory());
            tmpls.add(info);
        }
        manifest.templates = tmpls;

        return manifest;
    }

    public List<Map<String, Object>> getInjectionTypes() { return injectionTypes; }
    public List<Map<String, Object>> getExpressionProtocols() { return expressionProtocols; }
    public List<Map<String, Object>> getTemplates() { return templates; }
}
