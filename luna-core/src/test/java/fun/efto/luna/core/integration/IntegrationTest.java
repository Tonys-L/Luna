package fun.efto.luna.core.integration;

import fun.efto.luna.core.expression.ast.ExpressionNode;
import fun.efto.luna.core.expression.parser.ExpressionParser;
import fun.efto.luna.core.expression.Token;
import fun.efto.luna.core.expression.Tokenizer;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 集成测试类，测试完整的功能流程
 * @author ：Tony.L(<286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
public class IntegrationTest {

    @Test
    public void testExpressionParser() {
        // 测试表达式解析和求值
        String expression = "(10 + 5) * 2 > 20 && length(\"test\") == 4";
        
        // 词法分析
        Tokenizer tokenizer = new Tokenizer(expression);
        tokenizer.tokenize();
        List<Token> tokens = tokenizer.getTokens();
        
        // 语法分析
        ExpressionParser parser = new ExpressionParser(tokens);
        ExpressionNode rootNode = parser.parse();
        
        // 求值
        Object result = rootNode.evaluate(null);
        
        // 验证结果
        assertEquals(true, result, "表达式求值结果应该为 true");
    }

    @Test
    public void testFunctionCallExpression() {
        // 测试函数调用表达式
        String expression = "length(\"Hello\")";
        
        // 词法分析
        Tokenizer tokenizer = new Tokenizer(expression);
        tokenizer.tokenize();
        List<Token> tokens = tokenizer.getTokens();
        
        // 语法分析
        ExpressionParser parser = new ExpressionParser(tokens);
        ExpressionNode rootNode = parser.parse();
        
        // 求值
        Object result = rootNode.evaluate(null);
        
        // 验证结果
        assertEquals(5, result, "函数调用结果应该为 5");
    }
}
