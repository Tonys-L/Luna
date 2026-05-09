package fun.efto.luna.core.expression;

import java.util.ArrayList;
import java.util.List;

/**
 * 词法分析器，将表达式字符串转换为 Token 序列
 * @author ：Tony.L(<286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
public class Tokenizer {

    private final String expression;
    private final List<Token> tokens;
    private int position;

    public Tokenizer(String expression) {
        this.expression = expression;
        this.tokens = new ArrayList<>();
        this.position = 0;
    }

    /**
     * 执行词法分析
     */
    public void tokenize() {
        while (position < expression.length()) {
            char current = expression.charAt(position);

            if (Character.isWhitespace(current)) {
                // 跳过空白字符
                position++;
            } else if (current == '$' && position + 1 < expression.length() && expression.charAt(position + 1) == '{') {
                // ${ 开始
                tokens.add(new Token(Token.Type.DOLLAR_BRACE_OPEN, "${"));
                position += 2;
            } else if (current == '}') {
                // } 结束
                tokens.add(new Token(Token.Type.BRACE_CLOSE, "}"));
                position++;
            } else if (Character.isLetter(current) || current == '_') {
                // 标识符
                int start = position;
                while (position < expression.length() && (Character.isLetterOrDigit(expression.charAt(position)) || expression.charAt(position) == '_')) {
                    position++;
                }
                String identifier = expression.substring(start, position);
                // 检查是否是布尔值
                if ("true".equals(identifier) || "false".equals(identifier)) {
                    tokens.add(new Token(Token.Type.BOOLEAN, identifier));
                } else {
                    tokens.add(new Token(Token.Type.IDENTIFIER, identifier));
                }
            } else if (Character.isDigit(current)) {
                // 数字（支持整数和浮点数）
                int start = position;
                while (position < expression.length() && Character.isDigit(expression.charAt(position))) {
                    position++;
                }
                // 检查小数点
                if (position < expression.length() && expression.charAt(position) == '.'
                        && position + 1 < expression.length() && Character.isDigit(expression.charAt(position + 1))) {
                    position++; // 跳过小数点
                    while (position < expression.length() && Character.isDigit(expression.charAt(position))) {
                        position++;
                    }
                }
                String number = expression.substring(start, position);
                tokens.add(new Token(Token.Type.NUMBER, number));
            } else if (current == '"') {
                // 双引号字符串
                position++;
                int start = position;
                while (position < expression.length() && expression.charAt(position) != '"') {
                    position++;
                }
                String string = expression.substring(start, position);
                tokens.add(new Token(Token.Type.STRING, string));
                position++;
            } else if (current == '\'') {
                // 单引号字符串
                position++;
                int start = position;
                while (position < expression.length() && expression.charAt(position) != '\'') {
                    position++;
                }
                String string = expression.substring(start, position);
                tokens.add(new Token(Token.Type.STRING, string));
                position++;
            } else if (current == '=') {
                // == 运算符
                if (position + 1 < expression.length() && expression.charAt(position + 1) == '=') {
                    tokens.add(new Token(Token.Type.EQUAL, "=="));
                    position += 2;
                }
            } else if (current == '!') {
                // != 运算符
                if (position + 1 < expression.length() && expression.charAt(position + 1) == '=') {
                    tokens.add(new Token(Token.Type.NOT_EQUAL, "!="));
                    position += 2;
                } else {
                    tokens.add(new Token(Token.Type.NOT, "!"));
                    position++;
                }
            } else if (current == '>') {
                // > 或 >= 运算符
                if (position + 1 < expression.length() && expression.charAt(position + 1) == '=') {
                    tokens.add(new Token(Token.Type.GREATER_EQUAL, ">="));
                    position += 2;
                } else {
                    tokens.add(new Token(Token.Type.GREATER, ">"));
                    position++;
                }
            } else if (current == '<') {
                // < 或 <= 运算符
                if (position + 1 < expression.length() && expression.charAt(position + 1) == '=') {
                    tokens.add(new Token(Token.Type.LESS_EQUAL, "<="));
                    position += 2;
                } else {
                    tokens.add(new Token(Token.Type.LESS, "<"));
                    position++;
                }
            } else if (current == '&') {
                // && 运算符
                if (position + 1 < expression.length() && expression.charAt(position + 1) == '&') {
                    tokens.add(new Token(Token.Type.AND, "&&"));
                    position += 2;
                }
            } else if (current == '|') {
                // || 运算符
                if (position + 1 < expression.length() && expression.charAt(position + 1) == '|') {
                    tokens.add(new Token(Token.Type.OR, "||"));
                    position += 2;
                }
            } else if (current == '[') {
                // [ 左括号
                tokens.add(new Token(Token.Type.LEFT_BRACKET, "["));
                position++;
            } else if (current == ']') {
                // ] 右括号
                tokens.add(new Token(Token.Type.RIGHT_BRACKET, "]"));
                position++;
            } else if (current == '(') {
                // ( 左括号
                tokens.add(new Token(Token.Type.LEFT_PAREN, "("));
                position++;
            } else if (current == ')') {
                // ) 右括号
                tokens.add(new Token(Token.Type.RIGHT_PAREN, ")"));
                position++;
            } else if (current == ',') {
                // , 逗号
                tokens.add(new Token(Token.Type.COMMA, ","));
                position++;
            } else if (current == '+') {
                // + 加号
                tokens.add(new Token(Token.Type.PLUS, "+"));
                position++;
            } else if (current == '-') {
                // - 减号
                tokens.add(new Token(Token.Type.MINUS, "-"));
                position++;
            } else if (current == '*') {
                // * 乘号
                tokens.add(new Token(Token.Type.MULTIPLY, "*"));
                position++;
            } else if (current == '/') {
                // / 除号
                tokens.add(new Token(Token.Type.DIVIDE, "/"));
                position++;
            } else if (current == '.') {
                // . 点号
                tokens.add(new Token(Token.Type.DOT, "."));
                position++;
            } else {
                // 跳过未知字符
                position++;
            }
        }
    }

    /**
     * 获取解析后的 Token 列表
     */
    public List<Token> getTokens() {
        return tokens;
    }
}
