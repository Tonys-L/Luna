package fun.efto.luna.core.expression.ast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 属性访问节点，用于表示链式字段访问（如 user.name, param[0].age）
 *
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/10 00:00
 */
public class PropertyAccessNode implements ExpressionNode {

    private final ExpressionNode target;
    private final String propertyName;

    public PropertyAccessNode(ExpressionNode target, String propertyName) {
        this.target = target;
        this.propertyName = propertyName;
    }

    @Override
    public Object evaluate(Object context) {
        Object targetValue = target.evaluate(context);
        if (targetValue == null) {
            return null;
        }
        return resolveProperty(targetValue, propertyName);
    }

    @Override
    public Class<?> getType() {
        return Object.class;
    }

    /**
     * 解析属性值：Map → getter → 字段 三级查找
     */
    private Object resolveProperty(Object target, String property) {
        // 1. Map 查找
        if (target instanceof Map) {
            return ((Map<?, ?>) target).get(property);
        }

        // 2. getter 方法查找（getXxx / isXxx）
        try {
            String capitalized = Character.toUpperCase(property.charAt(0)) + property.substring(1);
            try {
                Method getter = target.getClass().getMethod("get" + capitalized);
                getter.setAccessible(true);
                return getter.invoke(target);
            } catch (NoSuchMethodException e) {
                // 尝试 isXxx (boolean)
                try {
                    Method isGetter = target.getClass().getMethod("is" + capitalized);
                    isGetter.setAccessible(true);
                    return isGetter.invoke(target);
                } catch (NoSuchMethodException ignored) {
                    // fall through to field access
                }
            }
        } catch (Exception e) {
            // fall through to field access
        }

        // 3. 字段直接访问
        try {
            Field field = findField(target.getClass(), property);
            if (field != null) {
                field.setAccessible(true);
                return field.get(target);
            }
        } catch (Exception e) {
            // ignore
        }

        return null;
    }

    /**
     * 递归查找字段（包括父类）
     */
    private Field findField(Class<?> clazz, String name) {
        if (clazz == null || clazz == Object.class) return null;
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            return findField(clazz.getSuperclass(), name);
        }
    }

    public ExpressionNode getTarget() {
        return target;
    }

    public String getPropertyName() {
        return propertyName;
    }

    @Override
    public String toString() {
        return target + "." + propertyName;
    }
}
