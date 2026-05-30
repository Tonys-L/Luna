package fun.efto.luna.core.rule.template;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模板注册中心 - 管理内置模板和社区模板
 *
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 10:00
 */
public class TemplateRegistry {

    private static final TemplateRegistry INSTANCE = new TemplateRegistry();
    private final Map<String, RuleTemplate> templates = new ConcurrentHashMap<>();

    private TemplateRegistry() {
        discoverCommunityTemplates();
    }

    public static TemplateRegistry getInstance() {
        return INSTANCE;
    }

    public void register(RuleTemplate template) {
        if (template != null && template.getName() != null) {
            templates.put(template.getName(), template);
        }
    }

    public void unregister(String name) {
        templates.remove(name);
    }

    public RuleTemplate getTemplate(String name) {
        return templates.get(name);
    }

    public List<RuleTemplate> getAllTemplates() {
        return new ArrayList<>(templates.values());
    }

    public List<RuleTemplate> getTemplatesByCategory(String category) {
        List<RuleTemplate> result = new ArrayList<>();
        for (RuleTemplate t : templates.values()) {
            if (category.equals(t.getCategory())) {
                result.add(t);
            }
        }
        return result;
    }

    private void registerBuiltinTemplates() {
    }

    private void discoverCommunityTemplates() {
        try {
            ServiceLoader<RuleTemplate> loader = ServiceLoader.load(RuleTemplate.class);
            for (RuleTemplate template : loader) {
                register(template);
            }
        } catch (Throwable t) {
        }
    }
}
