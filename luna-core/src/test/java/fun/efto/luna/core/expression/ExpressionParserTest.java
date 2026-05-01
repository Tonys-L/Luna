package fun.efto.luna.core.expression;

import fun.efto.luna.core.expression.ast.ExpressionNode;
import fun.efto.luna.core.expression.parser.ExpressionParser;
import fun.efto.luna.core.expression.Token;
import fun.efto.luna.core.expression.Tokenizer;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 表达式解析器测试
 * @author ：Tony.L(<286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
public class ExpressionParserTest {

    @Test
    public void testConstantExpression() {
        String expression = "42";
        ExpressionNode node = parseExpression(expression);
        Object result = node.evaluate(null);
        assertEquals(42, result);
    }

    @Test
    public void testStringExpression() {
        String expression = "\"Hello, World!\"";
        ExpressionNode node = parseExpression(expression);
        Object result = node.evaluate(null);
        assertEquals("Hello, World!", result);
    }

    @Test
    public void testBooleanExpression() {
        String expression = "true";
        ExpressionNode node = parseExpression(expression);
        Object result = node.evaluate(null);
        assertEquals(true, result);

        expression = "false";
        node = parseExpression(expression);
        result = node.evaluate(null);
        assertEquals(false, result);
    }

    @Test
    public void testArithmeticExpression() {
        String expression = "10 + 5";
        ExpressionNode node = parseExpression(expression);
        Object result = node.evaluate(null);
        assertEquals(15.0, result);

        expression = "10 - 5";
        node = parseExpression(expression);
        result = node.evaluate(null);
        assertEquals(5.0, result);

        expression = "10 * 5";
        node = parseExpression(expression);
        result = node.evaluate(null);
        assertEquals(50.0, result);

        expression = "10 / 5";
        node = parseExpression(expression);
        result = node.evaluate(null);
        assertEquals(2.0, result);
    }

    @Test
    public void testComparisonExpression() {
        String expression = "10 > 5";
        ExpressionNode node = parseExpression(expression);
        Object result = node.evaluate(null);
        assertEquals(true, result);

        expression = "10 < 5";
        node = parseExpression(expression);
        result = node.evaluate(null);
        assertEquals(false, result);

        expression = "10 == 10";
        node = parseExpression(expression);
        result = node.evaluate(null);
        assertEquals(true, result);

        expression = "10 != 5";
        node = parseExpression(expression);
        result = node.evaluate(null);
        assertEquals(true, result);
    }

    @Test
    public void testLogicalExpression() {
        String expression = "true && false";
        ExpressionNode node = parseExpression(expression);
        Object result = node.evaluate(null);
        assertEquals(false, result);

        expression = "true || false";
        node = parseExpression(expression);
        result = node.evaluate(null);
        assertEquals(true, result);

        expression = "!true";
        node = parseExpression(expression);
        result = node.evaluate(null);
        assertEquals(false, result);
    }

    @Test
    public void testVariableExpression() {
        Map<String, Object> context = new HashMap<>();
        context.put("x", 10);
        context.put("y", 5);

        String expression = "x + y";
        ExpressionNode node = parseExpression(expression);
        Object result = node.evaluate(context);
        assertEquals(15.0, result);
    }

    @Test
    public void testFunctionCallExpression() {
        String expression = "length(\"Hello\")";
        ExpressionNode node = parseExpression(expression);
        Object result = node.evaluate(null);
        assertEquals(5, result);

        expression = "toLowerCase(\"HELLO\")";
        node = parseExpression(expression);
        result = node.evaluate(null);
        assertEquals("hello", result);
    }

    @Test
    public void testComplexExpression() {
        Map<String, Object> context = new HashMap<>();
        context.put("x", 10);
        context.put("y", 5);

        String expression = "(x + y) * 2 > 20 && length(\"test\") == 4";
        ExpressionNode node = parseExpression(expression);
        Object result = node.evaluate(context);
        assertEquals(true, result);
    }

    private ExpressionNode parseExpression(String expression) {
        Tokenizer tokenizer = new Tokenizer(expression);
        tokenizer.tokenize();
        List<Token> tokens = tokenizer.getTokens();
        ExpressionParser parser = new ExpressionParser(tokens);
        return parser.parse();
    }
}
