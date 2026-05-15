package fun.efto.luna.core.plugin;

import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.rule.InjectionRule;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
public interface InjectionRuleConverter {

    InjectionPoint convert(InjectionRule rule);
}
