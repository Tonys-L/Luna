package fun.efto.luna.core.expression.context;

import fun.efto.luna.core.probe.BootstrapClassRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * 表达式求值上下文，用于在运行时绑定变量
 * <p>
 * 支持的变量来源：
 * - 方法参数：param[0], param[1], arg0, arg1 ...
 * - 局部变量：直接使用变量名
 * - this 引用：this
 * - 返回值：return
 *
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/10 00:00
 */
public class EvaluationContext {

    static {
        BootstrapClassRegistry.register(EvaluationContext.class.getName());
    }

    private static final ThreadLocal<EvaluationContext> THREAD_LOCAL = ThreadLocal.withInitial(EvaluationContext::new);

    /**
     * 获取当前线程的复用上下文，已自动清空
     */
    public static EvaluationContext getThreadLocal() {
        EvaluationContext ctx = THREAD_LOCAL.get();
        ctx.clear();
        return ctx;
    }

    private final Map<String, Object> variables = new HashMap<>();

    /**
     * 绑定一个变量
     */
    public EvaluationContext bind(String name, Object value) {
        variables.put(name, value);
        return this;
    }

    /**
     * 绑定方法参数（支持 param[N] 和 argN 两种风格）
     */
    public EvaluationContext bindParams(Object... params) {
        if (params == null) return this;
        for (int i = 0; i < params.length; i++) {
            variables.put("param[" + i + "]", params[i]);
            variables.put("arg" + i, params[i]);
        }
        return this;
    }

    /**
     * 绑定 this 引用
     */
    public EvaluationContext bindThis(Object thisRef) {
        variables.put("this", thisRef);
        return this;
    }

    /**
     * 绑定返回值
     */
    public EvaluationContext bindReturn(Object returnValue) {
        variables.put("return", returnValue);
        return this;
    }

    /**
     * 获取变量值
     */
    public Object get(String name) {
        return variables.get(name);
    }

    /**
     * 检查变量是否存在
     */
    public boolean contains(String name) {
        return variables.containsKey(name);
    }

    /**
     * 返回底层 Map（用于传递给 ExpressionNode.evaluate）
     */
    public Map<String, Object> asMap() {
        return variables;
    }

    /**
     * 清空上下文（用于对象复用，减少 GC）
     */
    public void clear() {
        variables.clear();
    }

    @Override
    public String toString() {
        return "EvaluationContext{variables=" + variables.keySet() + "}";
    }
}
