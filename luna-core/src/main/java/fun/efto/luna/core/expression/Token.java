package fun.efto.luna.core.expression;

/**
 * Token 类，用于表示表达式中的各种标记
 * @author ：Tony.L(<286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
public class Token {

    /**
     * Token 类型
     */
    public enum Type {
        // 特殊标记
        DOLLAR_BRACE_OPEN,  // ${ 开始
        BRACE_CLOSE,        // } 结束
        
        // 标识符
        IDENTIFIER,         // 变量名
        
        // 运算符
        PLUS,               // +
        MINUS,              // -
        MULTIPLY,           // *
        DIVIDE,             // /
        EQUAL,              // ==
        NOT_EQUAL,          // !=
        GREATER,            // >
        GREATER_EQUAL,      // >=
        LESS,               // <
        LESS_EQUAL,         // <=
        AND,                // &&
        OR,                 // ||
        NOT,                // !
        
        // 字面量
        NUMBER,             // 数字
        STRING,             // 字符串
        BOOLEAN,            // 布尔值
        
        // 其他
        LEFT_BRACKET,       // [
        RIGHT_BRACKET,      // ]
        LEFT_PAREN,         // (
        RIGHT_PAREN,        // )
        COMMA,              // ,
        DOT                 // .
    }

    private final Type type;
    private final String value;

    public Token(Type type, String value) {
        this.type = type;
        this.value = value;
    }

    public Type getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Token{type=" + type + ", value='" + value + "'}";
    }
}
