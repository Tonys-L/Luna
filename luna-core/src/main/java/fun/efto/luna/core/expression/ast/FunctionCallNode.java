package fun.efto.luna.core.expression.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * 函数调用表达式节点，用于表示函数调用
 * @author : Tony.L(286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
public class FunctionCallNode implements ExpressionNode {

    private final String functionName;
    private final List<ExpressionNode> arguments;

    public FunctionCallNode(String functionName, List<ExpressionNode> arguments) {
        this.functionName = functionName;
        this.arguments = arguments != null ? arguments : new ArrayList<>();
    }

    @Override
    public Object evaluate(Object context) {
        // 计算所有参数值
        List<Object> argValues = new ArrayList<>();
        for (ExpressionNode arg : arguments) {
            argValues.add(arg.evaluate(context));
        }

        // 处理内置函数
        switch (functionName) {
            case "println":
                return println(argValues);
            case "length":
                return length(argValues);
            case "toString":
                return toString(argValues);
            case "parseInt":
                return parseInt(argValues);
            case "parseDouble":
                return parseDouble(argValues);
            case "toLowerCase":
                return toLowerCase(argValues);
            case "toUpperCase":
                return toUpperCase(argValues);
            default:
                // 尝试从上下文获取函数
                if (context instanceof java.util.Map) {
                    Object function = ((java.util.Map<?, ?>) context).get(functionName);
                    if (function instanceof java.util.function.Function) {
                        return ((java.util.function.Function<Object, Object>) function).apply(argValues);
                    }
                }
                throw new UnsupportedOperationException("Unsupported function: " + functionName);
        }
    }

    @Override
    public Class<?> getType() {
        // 函数返回类型在运行时才能确定，这里返回Object
        return Object.class;
    }

    private Object println(List<Object> args) {
        for (Object arg : args) {
            System.out.print(arg);
        }
        System.out.println();
        return null;
    }

    private Object length(List<Object> args) {
        if (args.size() != 1) {
            throw new IllegalArgumentException("length function requires exactly one argument");
        }
        Object arg = args.get(0);
        if (arg instanceof String) {
            return ((String) arg).length();
        } else if (arg instanceof java.util.Collection) {
            return ((java.util.Collection<?>) arg).size();
        } else if (arg.getClass().isArray()) {
            return java.lang.reflect.Array.getLength(arg);
        }
        throw new IllegalArgumentException("length function only works with strings, collections, and arrays");
    }

    private Object toString(List<Object> args) {
        if (args.size() != 1) {
            throw new IllegalArgumentException("toString function requires exactly one argument");
        }
        Object arg = args.get(0);
        return String.valueOf(arg);
    }

    private Object parseInt(List<Object> args) {
        if (args.size() != 1) {
            throw new IllegalArgumentException("parseInt function requires exactly one argument");
        }
        Object arg = args.get(0);
        return Integer.parseInt(String.valueOf(arg));
    }

    private Object parseDouble(List<Object> args) {
        if (args.size() != 1) {
            throw new IllegalArgumentException("parseDouble function requires exactly one argument");
        }
        Object arg = args.get(0);
        return Double.parseDouble(String.valueOf(arg));
    }

    private Object toLowerCase(List<Object> args) {
        if (args.size() != 1) {
            throw new IllegalArgumentException("toLowerCase function requires exactly one argument");
        }
        Object arg = args.get(0);
        if (arg instanceof String) {
            return ((String) arg).toLowerCase();
        }
        throw new IllegalArgumentException("toLowerCase function only works with strings");
    }

    private Object toUpperCase(List<Object> args) {
        if (args.size() != 1) {
            throw new IllegalArgumentException("toUpperCase function requires exactly one argument");
        }
        Object arg = args.get(0);
        if (arg instanceof String) {
            return ((String) arg).toUpperCase();
        }
        throw new IllegalArgumentException("toUpperCase function only works with strings");
    }

    public String getFunctionName() {
        return functionName;
    }

    public List<ExpressionNode> getArguments() {
        return arguments;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(functionName).append("(");
        for (int i = 0; i < arguments.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(arguments.get(i));
        }
        sb.append(")");
        return sb.toString();
    }
}
