package fun.efto.luna.core.rule.template;

import fun.efto.luna.core.rule.InjectionRule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模板引擎 - 解析模板、替换参数、生成规则列表
 *
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 10:00
 */
public class TemplateEngine {

    private static final Pattern PARAM_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    public static List<InjectionRule> apply(RuleTemplate template, String targetClass,
                                            String targetMethod, String methodDesc,
                                            Map<String, String> paramValues) {
        List<InjectionRule> result = new ArrayList<>();
        Map<String, String> resolvedParams = resolveDefaults(template, paramValues);
        String groupId = UUID.randomUUID().toString();

        for (RuleTemplate.TemplateRule templateRule : template.getRules()) {
            InjectionRule rule = new InjectionRule();
            rule.setTargetClass(targetClass);
            rule.setTargetMethod(targetMethod);
            rule.setInjectionType(templateRule.getInjectionType());
            rule.setCodeType(templateRule.getCodeType() != null ? templateRule.getCodeType() : "EXPRESSION");
            rule.setLineNumber(templateRule.getLineNumber());
            rule.setEnabled(true);
            rule.setGroupId(groupId);

            String code = templateRule.getCode();
            code = substituteParams(code, resolvedParams);
            rule.setLogContent(code);

            String condition = templateRule.getCondition();
            if (condition != null && !condition.trim().isEmpty()) {
                condition = substituteParams(condition, resolvedParams);
                rule.setExpression(condition.trim());
            }

            result.add(rule);
        }

        return result;
    }

    private static Map<String, String> resolveDefaults(RuleTemplate template, Map<String, String> paramValues) {
        Map<String, String> resolved = new HashMap<>();
        if (template.getParameters() != null) {
            for (RuleTemplate.TemplateParameter param : template.getParameters()) {
                String value = paramValues != null ? paramValues.get(param.getName()) : null;
                if (value == null || value.isEmpty()) {
                    value = param.getDefaultValue();
                }
                if (value != null) {
                    resolved.put(param.getName(), value);
                }
            }
        }
        if (paramValues != null) {
            for (Map.Entry<String, String> entry : paramValues.entrySet()) {
                if (!resolved.containsKey(entry.getKey())) {
                    resolved.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return resolved;
    }

    private static String substituteParams(String template, Map<String, String> params) {
        if (template == null || params == null || params.isEmpty()) {
            return template;
        }
        Matcher matcher = PARAM_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String paramName = matcher.group(1);
            String value = params.get(paramName);
            matcher.appendReplacement(sb, value != null ? Matcher.quoteReplacement(value) : matcher.group(0));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
