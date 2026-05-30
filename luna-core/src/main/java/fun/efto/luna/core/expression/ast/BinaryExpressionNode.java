package fun.efto.luna.core.expression.ast;

/**
 * 二元表达式节点，用于表示二元运算符
 * @author : Tony.L(286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
public class BinaryExpressionNode implements ExpressionNode {

    private final ExpressionNode left;
    private final String operator;
    private final ExpressionNode right;

    public BinaryExpressionNode(ExpressionNode left, String operator, ExpressionNode right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    @Override
    public Object evaluate(Object context) {
        Object leftValue = left.evaluate(context);
        Object rightValue = right.evaluate(context);

        switch (operator) {
            case "+":
                return add(leftValue, rightValue);
            case "-":
                return subtract(leftValue, rightValue);
            case "*":
                return multiply(leftValue, rightValue);
            case "/":
                return divide(leftValue, rightValue);
            case "==":
                return equals(leftValue, rightValue);
            case "!=":
                return !equals(leftValue, rightValue);
            case ">":
                return greaterThan(leftValue, rightValue);
            case ">=":
                return greaterThanOrEqual(leftValue, rightValue);
            case "<":
                return lessThan(leftValue, rightValue);
            case "<=":
                return lessThanOrEqual(leftValue, rightValue);
            case "&&":
                return logicalAnd(leftValue, rightValue);
            case "||":
                return logicalOr(leftValue, rightValue);
            default:
                throw new UnsupportedOperationException("Unsupported operator: " + operator);
        }
    }

    @Override
    public Class<?> getType() {
        // 根据运算符返回相应的类型
        switch (operator) {
            case "+":
            case "-":
            case "*":
            case "/":
                return Number.class;
            case "==":
            case "!=":
            case ">":
            case ">=":
            case "<":
            case "<=":
            case "&&":
            case "||":
                return Boolean.class;
            default:
                return Object.class;
        }
    }

    private Object add(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            double leftVal = ((Number) left).doubleValue();
            double rightVal = ((Number) right).doubleValue();
            return leftVal + rightVal;
        } else if (left instanceof String || right instanceof String) {
            return String.valueOf(left) + String.valueOf(right);
        }
        throw new IllegalArgumentException("Cannot add " + left + " and " + right);
    }

    private Object subtract(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            double leftVal = ((Number) left).doubleValue();
            double rightVal = ((Number) right).doubleValue();
            return leftVal - rightVal;
        }
        throw new IllegalArgumentException("Cannot subtract " + left + " and " + right);
    }

    private Object multiply(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            double leftVal = ((Number) left).doubleValue();
            double rightVal = ((Number) right).doubleValue();
            return leftVal * rightVal;
        }
        throw new IllegalArgumentException("Cannot multiply " + left + " and " + right);
    }

    private Object divide(Object left, Object right) {
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

    private boolean equals(Object left, Object right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    private boolean greaterThan(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            double leftVal = ((Number) left).doubleValue();
            double rightVal = ((Number) right).doubleValue();
            return leftVal > rightVal;
        } else if (left instanceof Comparable && right instanceof Comparable) {
            @SuppressWarnings("unchecked")
            Comparable<Object> leftComp = (Comparable<Object>) left;
            return leftComp.compareTo(right) > 0;
        }
        throw new IllegalArgumentException("Cannot compare " + left + " and " + right);
    }

    private boolean greaterThanOrEqual(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            double leftVal = ((Number) left).doubleValue();
            double rightVal = ((Number) right).doubleValue();
            return leftVal >= rightVal;
        } else if (left instanceof Comparable && right instanceof Comparable) {
            @SuppressWarnings("unchecked")
            Comparable<Object> leftComp = (Comparable<Object>) left;
            return leftComp.compareTo(right) >= 0;
        }
        throw new IllegalArgumentException("Cannot compare " + left + " and " + right);
    }

    private boolean lessThan(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            double leftVal = ((Number) left).doubleValue();
            double rightVal = ((Number) right).doubleValue();
            return leftVal < rightVal;
        } else if (left instanceof Comparable && right instanceof Comparable) {
            @SuppressWarnings("unchecked")
            Comparable<Object> leftComp = (Comparable<Object>) left;
            return leftComp.compareTo(right) < 0;
        }
        throw new IllegalArgumentException("Cannot compare " + left + " and " + right);
    }

    private boolean lessThanOrEqual(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            double leftVal = ((Number) left).doubleValue();
            double rightVal = ((Number) right).doubleValue();
            return leftVal <= rightVal;
        } else if (left instanceof Comparable && right instanceof Comparable) {
            @SuppressWarnings("unchecked")
            Comparable<Object> leftComp = (Comparable<Object>) left;
            return leftComp.compareTo(right) <= 0;
        }
        throw new IllegalArgumentException("Cannot compare " + left + " and " + right);
    }

    private boolean logicalAnd(Object left, Object right) {
        boolean leftVal = Boolean.parseBoolean(String.valueOf(left));
        boolean rightVal = Boolean.parseBoolean(String.valueOf(right));
        return leftVal && rightVal;
    }

    private boolean logicalOr(Object left, Object right) {
        boolean leftVal = Boolean.parseBoolean(String.valueOf(left));
        boolean rightVal = Boolean.parseBoolean(String.valueOf(right));
        return leftVal || rightVal;
    }

    public ExpressionNode getLeft() {
        return left;
    }

    public String getOperator() {
        return operator;
    }

    public ExpressionNode getRight() {
        return right;
    }

    @Override
    public String toString() {
        return "(" + left + " " + operator + " " + right + ")";
    }
}
