package fun.efto.luna.agent.web.controller;

import fun.efto.luna.agent.web.mvc.ApiResult;
import fun.efto.luna.agent.web.mvc.Controller;
import fun.efto.luna.agent.web.mvc.DeleteMapping;
import fun.efto.luna.agent.web.mvc.GetMapping;
import fun.efto.luna.agent.web.mvc.PathVariable;
import fun.efto.luna.agent.web.mvc.PostMapping;
import fun.efto.luna.agent.web.mvc.PutMapping;
import fun.efto.luna.agent.web.mvc.RequestBody;
import fun.efto.luna.agent.web.mvc.RequestMapping;
import fun.efto.luna.core.rule.InjectionRule;
import fun.efto.luna.core.rule.RuleManager;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/09 10:00
 */
@Controller
@RequestMapping("/rules")
public class RuleController {

    @GetMapping
    public ApiResult list() {
        return ApiResult.ok(RuleManager.getInstance().getRules());
    }

    @GetMapping("/{id}")
    public ApiResult get(@PathVariable("id") long id) {
        InjectionRule rule = RuleManager.getInstance().getRule(id);
        if (rule != null) {
            return ApiResult.ok(rule);
        }
        return ApiResult.fail("规则不存在", HttpServletResponse.SC_NOT_FOUND);
    }

    @PostMapping
    public ApiResult create(@RequestBody InjectionRule rule) {
        long id = RuleManager.getInstance().addRule(rule);
        Map<String, Object> data = new HashMap<>();
        data.put("success", true);
        data.put("id", id);
        return ApiResult.ok(data);
    }

    @PutMapping("/{id}")
    public ApiResult update(@PathVariable("id") long id, @RequestBody InjectionRule rule) {
        RuleManager.getInstance().updateRule(id, rule);
        Map<String, Object> data = new HashMap<>();
        data.put("success", true);
        return ApiResult.ok(data);
    }

    @DeleteMapping("/{id}")
    public ApiResult delete(@PathVariable("id") long id) {
        RuleManager.getInstance().deleteRule(id);
        Map<String, Object> data = new HashMap<>();
        data.put("success", true);
        return ApiResult.ok(data);
    }
}
