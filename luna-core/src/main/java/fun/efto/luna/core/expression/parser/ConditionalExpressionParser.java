package fun.efto.luna.core.expression.parser;

import fun.efto.luna.core.expression.Token;
import fun.efto.luna.core.expression.Tokenizer;
import fun.efto.luna.core.expression.ast.ExpressionNode;
import fun.efto.luna.core.expression.ast.ConstantNode;
import fun.efto.luna.core.expression.ast.VariableNode;

import java.util.List;

/**
 * 条件表达式解析器
 * @author ：Tony.L(<286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
public class ConditionalExpressionParser {

    private List<Token> tokens;
    private int position;

    /**
     * 解析表达式
     * @param expression 表达式字符串
     * @return 解析后的 AST 节点
     */
    public ExpressionNode parse(String expression) {
        Tokenizer tokenizer = new Tokenizer(expression);
        tokenizer.tokenize();
        this.tokens = tokenizer.getTokens();
        this.position = 0;

        // 跳过 ${ 标记
        if (position < tokens.size() && tokens.get(position).getType() == Token.Type.DOLLAR_BRACE_OPEN) {
            position++;
        }

        // 解析表达式
        ExpressionNode node = parseExpression();

        // 跳过 } 标记
        if (position < tokens.size() && tokens.get(position).getType() == Token.Type.BRACE_CLOSE) {
            position++;
        }

        return node;
    }

    /**
     * 解析表达式
     */
    private ExpressionNode parseExpression() {
        // 这里实现表达式解析逻辑
        // 暂时返回一个简单的节点
        if (position < tokens.size()) {
            Token token = tokens.get(position);
            position++;

            if (token.getType() == Token.Type.IDENTIFIER) {
                return new VariableNode(token.getValue());
            } else if (token.getType() == Token.Type.NUMBER) {
                return new ConstantNode(Integer.parseInt(token.getValue()), Integer.class);
            } else if (token.getType() == Token.Type.STRING) {
                return new ConstantNode(token.getValue(), String.class);
            } else if (token.getType() == Token.Type.BOOLEAN) {
                return new ConstantNode(Boolean.parseBoolean(token.getValue()), Boolean.class);
            }
        }
        return null;
    }
}
