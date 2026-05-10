package fun.efto.luna.core.expression.ast;

/**
 * 一元表达式节点，用于表示一元运算符
 * @author ：Tony.L(<286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
public class UnaryExpressionNode implements ExpressionNode {

    private final String operator;
    private final ExpressionNode expression;

    public UnaryExpressionNode(String operator, ExpressionNode expression) {
        this.operator = operator;
        this.expression = expression;
    }

    @Override
    public Object evaluate(Object context) {
        Object value = expression.evaluate(context);

        switch (operator) {
            case "!":
                return !Boolean.parseBoolean(String.valueOf(value));
            case "-":
                if (value instanceof Number) {
                    return -((Number) value).doubleValue();
                }
                throw new IllegalArgumentException("Cannot negate " + value);
            default:
                throw new UnsupportedOperationException("Unsupported unary operator: " + operator);
        }
    }

    @Override
    public Class<?> getType() {
        switch (operator) {
            case "!":
                return Boolean.class;
            case "-":
                return Number.class;
            default:
                return Object.class;
        }
    }

    public String getOperator() {
        return operator;
    }

    public ExpressionNode getExpression() {
        return expression;
    }

    @Override
    public String toString() {
        return operator + "(" + expression + ")";
    }
}
