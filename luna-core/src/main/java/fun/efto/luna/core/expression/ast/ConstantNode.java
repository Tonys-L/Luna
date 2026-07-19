package fun.efto.luna.core.expression.ast;

/**
 * 常量表达式节点，用于表示字面量值
 * @author : Tony.L(286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
public class ConstantNode implements ExpressionNode {

    private final Object value;
    private final Class<?> type;

    public ConstantNode(Object value, Class<?> type) {
        this.value = value;
        this.type = type;
    }

    @Override
    public Object evaluate(Object context) {
        return value;
    }

    @Override
    public Class<?> getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
