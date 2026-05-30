package fun.efto.luna.core.injection.rule.template;

import java.util.List;
import java.util.Map;

/**
 * 规则模板 - 可分享、可组合、社区可贡献的注入模式
 *
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 10:00
 */
public class RuleTemplate {

    private String name;
    private String displayName;
    private String description;
    private String version;
    private String author;
    private String category;
    private List<TemplateParameter> parameters;
    private List<TemplateRule> rules;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<TemplateParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<TemplateParameter> parameters) {
        this.parameters = parameters;
    }

    public List<TemplateRule> getRules() {
        return rules;
    }

    public void setRules(List<TemplateRule> rules) {
        this.rules = rules;
    }

    /**
     * 模板参数定义
     */
    public static class TemplateParameter {
        private String name;
        private String displayName;
        private String type;
        private String defaultValue;
        private String description;
        private boolean required;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getDefaultValue() { return defaultValue; }
        public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
    }

    /**
     * 模板中的单条规则定义
     */
    public static class TemplateRule {
        private String injectionType;
        private String codeType;
        private String code;
        private String condition;
        private int lineNumber;

        public String getInjectionType() { return injectionType; }
        public void setInjectionType(String injectionType) { this.injectionType = injectionType; }
        public String getCodeType() { return codeType; }
        public void setCodeType(String codeType) { this.codeType = codeType; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }
        public int getLineNumber() { return lineNumber; }
        public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
    }
}
