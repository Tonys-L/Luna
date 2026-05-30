package fun.efto.luna.agent.web.controller;

import fun.efto.luna.agent.web.vo.RuleOperationVO;
import fun.efto.luna.core.infra.web.ApiResult;
import fun.efto.luna.core.infra.web.Controller;
import fun.efto.luna.core.infra.web.DeleteMapping;
import fun.efto.luna.core.infra.web.GetMapping;
import fun.efto.luna.core.infra.web.PathVariable;
import fun.efto.luna.core.infra.web.PostMapping;
import fun.efto.luna.core.infra.web.PutMapping;
import fun.efto.luna.core.infra.web.RequestBody;
import fun.efto.luna.core.infra.web.RequestMapping;
import fun.efto.luna.core.injection.rule.InjectionRule;
import fun.efto.luna.core.injection.rule.RuleManager;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/09 10:00
 */
@Controller
@RequestMapping("/rules")
public class RuleController {

    private final RuleManager ruleManager;

    public RuleController(RuleManager ruleManager) {
        this.ruleManager = ruleManager;
    }

    @GetMapping
    public ApiResult list() {
        return ApiResult.ok(ruleManager.getRules());
    }

    @GetMapping("/{id}")
    public ApiResult get(@PathVariable("id") long id) {
        InjectionRule rule = ruleManager.getRule(id);
        if (rule != null) {
            return ApiResult.ok(rule);
        }
        return ApiResult.fail("规则不存在", 404);
    }

    @PostMapping
    public ApiResult create(@RequestBody InjectionRule rule) {
        long id = ruleManager.addRule(rule);
        return ApiResult.ok(RuleOperationVO.successWithId(id));
    }

    @PutMapping("/{id}")
    public ApiResult update(@PathVariable("id") long id, @RequestBody InjectionRule rule) {
        ruleManager.updateRule(id, rule);
        return ApiResult.ok(RuleOperationVO.success());
    }

    @DeleteMapping("/{id}")
    public ApiResult delete(@PathVariable("id") long id) {
        ruleManager.deleteRule(id);
        return ApiResult.ok(RuleOperationVO.success());
    }
}
