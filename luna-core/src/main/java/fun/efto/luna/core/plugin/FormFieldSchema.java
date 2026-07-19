package fun.efto.luna.core.plugin;

import java.util.List;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since : 2026/06/20 00:00
 */
public class FormFieldSchema {
    private String key;
    private String label;
    private String type;          // text / number / select / textarea
    private Object defaultValue;
    private List<Option> options; // select 类型的选项
    private boolean required;
    private String placeholder;

    public FormFieldSchema() {}

    public FormFieldSchema(String key, String label, String type, Object defaultValue, List<Option> options, boolean required, String placeholder) {
        this.key = key;
        this.label = label;
        this.type = type;
        this.defaultValue = defaultValue;
        this.options = options;
        this.required = required;
        this.placeholder = placeholder;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Object getDefaultValue() { return defaultValue; }
    public void setDefaultValue(Object defaultValue) { this.defaultValue = defaultValue; }
    public List<Option> getOptions() { return options; }
    public void setOptions(List<Option> options) { this.options = options; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public String getPlaceholder() { return placeholder; }
    public void setPlaceholder(String placeholder) { this.placeholder = placeholder; }

    public static class Option {
        private String value;
        private String label;

        public Option() {}

        public Option(String value, String label) {
            this.value = value;
            this.label = label;
        }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }
}
