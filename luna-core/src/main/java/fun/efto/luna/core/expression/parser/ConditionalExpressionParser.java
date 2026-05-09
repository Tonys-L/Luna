package fun.efto.luna.core.expression.parser;

import fun.efto.luna.core.expression.Token;
import fun.efto.luna.core.expression.Tokenizer;
import fun.efto.luna.core.expression.ast.ExpressionNode;
import fun.efto.luna.core.expression.context.EvaluationContext;

import java.util.List;
import java.util.Map;

/**
 * 条件表达式解析器（门面模式）
 * <p>
 * 处理 Luna 特有的 ${...} 语法糖，委托给 {@link ExpressionParser} 完成核心解析。
 * 提供便捷的一站式 API：解析、求值、条件判断。
 * <p>
 * 使用示例：
 * <pre>
 *   ConditionalExpressionParser parser = new ConditionalExpressionParser();
 *   // 解析并求值
 *   Object result = parser.evaluate("${age > 18}", context);
 *   // 条件判断
 *   boolean match = parser.test("${age > 18 && status == \"active\"}", context);
 * </pre>
 *
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/10 00:00
 */
public class ConditionalExpressionParser {

    /**
     * 解析表达式字符串为 AST
     *
     * @param expression 表达式字符串，支持 ${...} 包裹和裸表达式
     * @return AST 根节点
     */
    public ExpressionNode parse(String expression) {
        String unwrapped = unwrap(expression);
        Tokenizer tokenizer = new Tokenizer(unwrapped);
        tokenizer.tokenize();
        List<Token> tokens = tokenizer.getTokens();
        ExpressionParser parser = new ExpressionParser(tokens);
        return parser.parse();
    }

    /**
     * 解析并求值表达式
     *
     * @param expression 表达式字符串
     * @param context    求值上下文（Map 或 EvaluationContext）
     * @return 求值结果
     */
    public Object evaluate(String expression, Object context) {
        ExpressionNode node = parse(expression);
        return evaluateNode(node, context);
    }

    /**
     * 直接对已解析的 AST 节点求值（高性能路径）
     */
    public Object evaluateNode(ExpressionNode node, Object context) {
        Object evalContext = resolveContext(context);
        return node.evaluate(evalContext);
    }

    /**
     * 条件测试：表达式求值结果是否为 true
     *
     * @param expression 条件表达式字符串
     * @param context    求值上下文
     * @return 条件是否满足
     */
    public boolean test(String expression, Object context) {
        Object result = evaluate(expression, context);
        return toBoolean(result);
    }

    /**
     * 高性能条件测试：直接对 AST 节点求值
     */
    public boolean testNode(ExpressionNode node, Object context) {
        Object result = evaluateNode(node, context);
        return toBoolean(result);
    }

    /**
     * 去除 ${...} 包裹
     */
    private String unwrap(String expression) {
        if (expression == null) {
            throw new IllegalArgumentException("Expression cannot be null");
        }
        String trimmed = expression.trim();
        if (trimmed.startsWith("${") && trimmed.endsWith("}")) {
            return trimmed.substring(2, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    /**
     * 统一上下文类型
     */
    private Object resolveContext(Object context) {
        if (context instanceof EvaluationContext) {
            return ((EvaluationContext) context).asMap();
        }
        return context;
    }

    /**
     * 将求值结果转换为 boolean
     */
    private boolean toBoolean(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).doubleValue() != 0;
        if (value instanceof String) return !((String) value).isEmpty();
        return true;
    }
}
