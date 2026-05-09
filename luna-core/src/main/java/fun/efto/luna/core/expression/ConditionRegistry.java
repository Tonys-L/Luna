package fun.efto.luna.core.expression;

import fun.efto.luna.core.expression.ast.ExpressionNode;
import fun.efto.luna.core.expression.context.EvaluationContext;
import fun.efto.luna.core.expression.parser.ConditionalExpressionParser;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 条件表达式注册表
 *
 * 用于在注入点被触发时，提供高性能的 AST 求值。
 * 避免每次运行时重新解析表达式字符串。
 *
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/10 00:00
 */
public class ConditionRegistry {

    private static final Map<String, ExpressionNode> AST_CACHE = new ConcurrentHashMap<>();
    private static final ConditionalExpressionParser PARSER = new ConditionalExpressionParser();

    /**
     * 注册并预编译条件表达式
     *
     * @param injectionId 注入点 ID
     * @param expression  条件表达式字符串
     */
    public static void register(String injectionId, String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            AST_CACHE.remove(injectionId);
            return;
        }
        ExpressionNode ast = PARSER.parse(expression);
        AST_CACHE.put(injectionId, ast);
    }

    /**
     * 移除注册的条件
     */
    public static void unregister(String injectionId) {
        AST_CACHE.remove(injectionId);
    }

    /**
     * 测试条件是否成立
     * 
     * @param injectionId 注入点 ID
     * @param context     求值上下文
     * @return 如果没有配置条件，或条件成立则返回 true；否则返回 false
     */
    public static boolean test(String injectionId, EvaluationContext context) {
        ExpressionNode ast = AST_CACHE.get(injectionId);
        if (ast == null) {
            return true; // 没有配置条件，默认通过
        }
        try {
            return PARSER.testNode(ast, context);
        } catch (Exception e) {
            // 求值异常（如 NPE）时，默认不输出日志
            return false;
        }
    }
}
