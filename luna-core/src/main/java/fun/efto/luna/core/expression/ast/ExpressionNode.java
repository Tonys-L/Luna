package fun.efto.luna.core.expression.ast;

/**
 * 表达式节点接口，所有表达式节点的基类
 * @author ：Tony.L(<286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
public interface ExpressionNode {

    /**
     * 执行表达式，返回结果
     * @param context 执行上下文
     * @return 表达式执行结果
     */
    Object evaluate(Object context);

    /**
     * 获取表达式的类型
     * @return 表达式类型
     */
    Class<?> getType();
}
