package fun.efto.luna.core.expression.ast;

/**
 * 变量表达式节点，用于表示变量引用
 * @author : Tony.L(286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
public class VariableNode implements ExpressionNode {

    private final String name;

    public VariableNode(String name) {
        this.name = name;
    }

    @Override
    public Object evaluate(Object context) {
        if (context == null) {
            return null;
        }

        // 尝试从上下文获取变量值
        if (context instanceof java.util.Map) {
            return ((java.util.Map<?, ?>) context).get(name);
        }

        // 尝试通过反射获取字段值
        try {
            java.lang.reflect.Field field = context.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(context);
        } catch (Exception e) {
            // 忽略异常，返回null
            return null;
        }
    }

    @Override
    public Class<?> getType() {
        // 变量类型在运行时才能确定，这里返回Object
        return Object.class;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
