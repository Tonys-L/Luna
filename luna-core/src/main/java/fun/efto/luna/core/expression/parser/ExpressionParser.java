package fun.efto.luna.core.expression.parser;

import fun.efto.luna.core.expression.Token;
import fun.efto.luna.core.expression.ast.*;
import fun.efto.luna.core.expression.ast.PropertyAccessNode;
import java.util.ArrayList;
import java.util.List;

/**
 * 表达式解析器，将Token序列解析为抽象语法树
 * @author : Tony.L(286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
public class ExpressionParser {

    private final List<Token> tokens;
    private int position;

    public ExpressionParser(List<Token> tokens) {
        this.tokens = tokens;
        this.position = 0;
    }

    /**
     * 解析表达式
     * @return 表达式节点
     */
    public ExpressionNode parse() {
        return parseLogicalOr();
    }

    /**
     * 解析逻辑或表达式
     */
    private ExpressionNode parseLogicalOr() {
        ExpressionNode left = parseLogicalAnd();
        while (match(Token.Type.OR)) {
            String operator = getCurrentToken().getValue();
            consume();
            ExpressionNode right = parseLogicalAnd();
            left = new BinaryExpressionNode(left, operator, right);
        }
        return left;
    }

    /**
     * 解析逻辑与表达式
     */
    private ExpressionNode parseLogicalAnd() {
        ExpressionNode left = parseEquality();
        while (match(Token.Type.AND)) {
            String operator = getCurrentToken().getValue();
            consume();
            ExpressionNode right = parseEquality();
            left = new BinaryExpressionNode(left, operator, right);
        }
        return left;
    }

    /**
     * 解析相等性表达式
     */
    private ExpressionNode parseEquality() {
        ExpressionNode left = parseComparison();
        while (match(Token.Type.EQUAL, Token.Type.NOT_EQUAL)) {
            String operator = getCurrentToken().getValue();
            consume();
            ExpressionNode right = parseComparison();
            left = new BinaryExpressionNode(left, operator, right);
        }
        return left;
    }

    /**
     * 解析比较表达式
     */
    private ExpressionNode parseComparison() {
        ExpressionNode left = parseAdditive();
        while (match(Token.Type.GREATER, Token.Type.GREATER_EQUAL, Token.Type.LESS, Token.Type.LESS_EQUAL)) {
            String operator = getCurrentToken().getValue();
            consume();
            ExpressionNode right = parseAdditive();
            left = new BinaryExpressionNode(left, operator, right);
        }
        return left;
    }

    /**
     * 解析加法表达式
     */
    private ExpressionNode parseAdditive() {
        ExpressionNode left = parseMultiplicative();
        while (match(Token.Type.PLUS, Token.Type.MINUS)) {
            String operator = getCurrentToken().getValue();
            consume();
            ExpressionNode right = parseMultiplicative();
            left = new BinaryExpressionNode(left, operator, right);
        }
        return left;
    }

    /**
     * 解析乘法表达式
     */
    private ExpressionNode parseMultiplicative() {
        ExpressionNode left = parseUnary();
        while (match(Token.Type.MULTIPLY, Token.Type.DIVIDE)) {
            String operator = getCurrentToken().getValue();
            consume();
            ExpressionNode right = parseUnary();
            left = new BinaryExpressionNode(left, operator, right);
        }
        return left;
    }

    /**
     * 解析一元表达式
     */
    private ExpressionNode parseUnary() {
        if (match(Token.Type.NOT, Token.Type.MINUS)) {
            String operator = getCurrentToken().getValue();
            consume();
            ExpressionNode expression = parseUnary();
            return new UnaryExpressionNode(operator, expression);
        }
        return parsePrimary();
    }

    /**
     * 解析 primary 表达式
     */
    private ExpressionNode parsePrimary() {
        ExpressionNode node = null;
        if (match(Token.Type.NUMBER)) {
            Token token = getCurrentToken();
            consume();
            try {
                int value = Integer.parseInt(token.getValue());
                node = new ConstantNode(value, Integer.class);
            } catch (NumberFormatException e) {
                try {
                    double value = Double.parseDouble(token.getValue());
                    node = new ConstantNode(value, Double.class);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Invalid number: " + token.getValue());
                }
            }
        } else if (match(Token.Type.STRING)) {
            Token token = getCurrentToken();
            consume();
            node = new ConstantNode(token.getValue(), String.class);
        } else if (match(Token.Type.BOOLEAN)) {
            Token token = getCurrentToken();
            consume();
            boolean value = Boolean.parseBoolean(token.getValue());
            node = new ConstantNode(value, Boolean.class);
        } else if (match(Token.Type.IDENTIFIER)) {
            Token token = getCurrentToken();
            consume();
            // 检查是否是函数调用
            if (match(Token.Type.LEFT_PAREN)) {
                consume();
                List<ExpressionNode> arguments = new ArrayList<>();
                if (!match(Token.Type.RIGHT_PAREN)) {
                    do {
                        arguments.add(parse());
                    } while (match(Token.Type.COMMA) && consume());
                    if (!match(Token.Type.RIGHT_PAREN)) {
                        throw new IllegalArgumentException("Expected )");
                    }
                }
                consume();
                node = new FunctionCallNode(token.getValue(), arguments);
            } else if (match(Token.Type.LEFT_BRACKET)) {
                // 处理数组下标形式的变量，如 param[0]
                consume();
                Token indexToken = getCurrentToken();
                if (!match(Token.Type.NUMBER)) {
                    throw new IllegalArgumentException("Expected number index after [");
                }
                consume();
                if (!match(Token.Type.RIGHT_BRACKET)) {
                    throw new IllegalArgumentException("Expected ]");
                }
                consume();
                node = new VariableNode(token.getValue() + "[" + indexToken.getValue() + "]");
            } else {
                // 否则是变量引用
                node = new VariableNode(token.getValue());
            }
        } else if (match(Token.Type.LEFT_PAREN)) {
            consume();
            node = parse();
            if (!match(Token.Type.RIGHT_PAREN)) {
                throw new IllegalArgumentException("Expected )");
            }
            consume();
        } else {
            throw new IllegalArgumentException("Unexpected token: " + getCurrentToken());
        }

        // 处理后缀属性访问（如 .name）
        while (match(Token.Type.DOT)) {
            consume();
            Token propToken = getCurrentToken();
            if (!match(Token.Type.IDENTIFIER)) {
                throw new IllegalArgumentException("Expected property name after .");
            }
            consume();
            node = new PropertyAccessNode(node, propToken.getValue());
        }

        return node;
    }

    /**
     * 检查当前Token是否匹配指定类型
     */
    private boolean match(Token.Type... types) {
        if (position < tokens.size()) {
            Token token = tokens.get(position);
            for (Token.Type type : types) {
                if (token.getType() == type) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获取当前Token
     */
    private Token getCurrentToken() {
        if (position < tokens.size()) {
            return tokens.get(position);
        }
        throw new IndexOutOfBoundsException("No more tokens");
    }

    /**
     * 消费当前Token
     */
    private boolean consume() {
        if (position < tokens.size()) {
            position++;
            return true;
        }
        return false;
    }
}
