package fun.efto.luna.core.expression;

/**
 * 表达式工具类，提供表达式执行所需的工具方法
 * @author ：Tony.L(<286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
public class ExpressionUtils {

    /**
     * 从上下文获取变量值
     */
    public static Object getVariableValue(Object context, String name) {
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

    /**
     * 加法操作
     */
    public static Object add(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            double leftVal = ((Number) left).doubleValue();
            double rightVal = ((Number) right).doubleValue();
            return leftVal + rightVal;
        } else if (left instanceof String || right instanceof String) {
            return String.valueOf(left) + String.valueOf(right);
        }
        throw new IllegalArgumentException("Cannot add " + left + " and " + right);
    }

    /**
     * 减法操作
     */
    public static Object subtract(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            double leftVal = ((Number) left).doubleValue();
            double rightVal = ((Number) right).doubleValue();
            return leftVal - rightVal;
        }
        throw new IllegalArgumentException("Cannot subtract " + left + " and " + right);
    }

    /**
     * 乘法操作
     */
    public static Object multiply(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            double leftVal = ((Number) left).doubleValue();
            double rightVal = ((Number) right).doubleValue();
            return leftVal * rightVal;
        }
        throw new IllegalArgumentException("Cannot multiply " + left + " and " + right);
    }

    /**
     * 除法操作
     */
    public static Object divide(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            double leftVal = ((Number) left).doubleValue();
            double rightVal = ((Number) right).doubleValue();
            if (rightVal == 0) {
                throw new ArithmeticException("Division by zero");
            }
            return leftVal / rightVal;
        }
        throw new IllegalArgumentException("Cannot divide " + left + " and " + right);
    }

    /**
     * 相等性比较
     */
    public static Boolean equals(Object left, Object right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    /**
     * 不相等性比较
     */
    public static Boolean notEquals(Object left, Object right) {
        return !equals(left, right);
    }

    /**
     * 大于比较
     */
    public static Boolean greaterThan(Object left, Object right) {
        if (left instanceof Comparable && right instanceof Comparable) {
            @SuppressWarnings("unchecked")
            Comparable<Object> leftComp = (Comparable<Object>) left;
            return leftComp.compareTo(right) > 0;
        }
        throw new IllegalArgumentException("Cannot compare " + left + " and " + right);
    }

    /**
     * 大于等于比较
     */
    public static Boolean greaterThanOrEqual(Object left, Object right) {
        if (left instanceof Comparable && right instanceof Comparable) {
            @SuppressWarnings("unchecked")
            Comparable<Object> leftComp = (Comparable<Object>) left;
            return leftComp.compareTo(right) >= 0;
        }
        throw new IllegalArgumentException("Cannot compare " + left + " and " + right);
    }

    /**
     * 小于比较
     */
    public static Boolean lessThan(Object left, Object right) {
        if (left instanceof Comparable && right instanceof Comparable) {
            @SuppressWarnings("unchecked")
            Comparable<Object> leftComp = (Comparable<Object>) left;
            return leftComp.compareTo(right) < 0;
        }
        throw new IllegalArgumentException("Cannot compare " + left + " and " + right);
    }

    /**
     * 小于等于比较
     */
    public static Boolean lessThanOrEqual(Object left, Object right) {
        if (left instanceof Comparable && right instanceof Comparable) {
            @SuppressWarnings("unchecked")
            Comparable<Object> leftComp = (Comparable<Object>) left;
            return leftComp.compareTo(right) <= 0;
        }
        throw new IllegalArgumentException("Cannot compare " + left + " and " + right);
    }

    /**
     * 逻辑与操作
     */
    public static Boolean logicalAnd(Object left, Object right) {
        boolean leftVal = Boolean.parseBoolean(String.valueOf(left));
        boolean rightVal = Boolean.parseBoolean(String.valueOf(right));
        return leftVal && rightVal;
    }

    /**
     * 逻辑或操作
     */
    public static Boolean logicalOr(Object left, Object right) {
        boolean leftVal = Boolean.parseBoolean(String.valueOf(left));
        boolean rightVal = Boolean.parseBoolean(String.valueOf(right));
        return leftVal || rightVal;
    }

    /**
     * 逻辑非操作
     */
    public static Boolean logicalNot(Object value) {
        boolean val = Boolean.parseBoolean(String.valueOf(value));
        return !val;
    }

    /**
     * 取反操作
     */
    public static Object negate(Object value) {
        if (value instanceof Number) {
            return -((Number) value).doubleValue();
        }
        throw new IllegalArgumentException("Cannot negate " + value);
    }

    /**
     * 调用函数
     */
    public static Object callFunction(String functionName, Object[] arguments, Object context) {
        // 处理内置函数
        switch (functionName) {
            case "println":
                return println(arguments);
            case "length":
                return length(arguments);
            case "toString":
                return toString(arguments);
            case "parseInt":
                return parseInt(arguments);
            case "parseDouble":
                return parseDouble(arguments);
            case "toLowerCase":
                return toLowerCase(arguments);
            case "toUpperCase":
                return toUpperCase(arguments);
            default:
                // 尝试从上下文获取函数
                if (context instanceof java.util.Map) {
                    Object function = ((java.util.Map<?, ?>) context).get(functionName);
                    if (function instanceof java.util.function.Function) {
                        return ((java.util.function.Function<Object, Object>) function).apply(arguments);
                    }
                }
                throw new UnsupportedOperationException("Unsupported function: " + functionName);
        }
    }

    private static Object println(Object[] args) {
        for (Object arg : args) {
            System.out.print(arg);
        }
        System.out.println();
        return null;
    }

    private static Object length(Object[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("length function requires exactly one argument");
        }
        Object arg = args[0];
        if (arg instanceof String) {
            return ((String) arg).length();
        } else if (arg instanceof java.util.Collection) {
            return ((java.util.Collection<?>) arg).size();
        } else if (arg.getClass().isArray()) {
            return java.lang.reflect.Array.getLength(arg);
        }
        throw new IllegalArgumentException("length function only works with strings, collections, and arrays");
    }

    private static Object toString(Object[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("toString function requires exactly one argument");
        }
        Object arg = args[0];
        return String.valueOf(arg);
    }

    private static Object parseInt(Object[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("parseInt function requires exactly one argument");
        }
        Object arg = args[0];
        return Integer.parseInt(String.valueOf(arg));
    }

    private static Object parseDouble(Object[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("parseDouble function requires exactly one argument");
        }
        Object arg = args[0];
        return Double.parseDouble(String.valueOf(arg));
    }

    private static Object toLowerCase(Object[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("toLowerCase function requires exactly one argument");
        }
        Object arg = args[0];
        if (arg instanceof String) {
            return ((String) arg).toLowerCase();
        }
        throw new IllegalArgumentException("toLowerCase function only works with strings");
    }

    private static Object toUpperCase(Object[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("toUpperCase function requires exactly one argument");
        }
        Object arg = args[0];
        if (arg instanceof String) {
            return ((String) arg).toUpperCase();
        }
        throw new IllegalArgumentException("toUpperCase function only works with strings");
    }
}
