package fun.efto.luna.core.expression;

import fun.efto.luna.core.expression.ast.ExpressionNode;
import fun.efto.luna.core.expression.context.EvaluationContext;
import fun.efto.luna.core.expression.parser.ConditionalExpressionParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConditionalExpressionParser + EvaluationContext 集成测试
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/10 00:00
 */
public class ConditionalExpressionParserTest {

    private ConditionalExpressionParser parser;

    @BeforeEach
    void setUp() {
        parser = new ConditionalExpressionParser();
    }

    // ========== ${} 语法糖解析 ==========

    @Test
    @DisplayName("解析 ${} 包裹的表达式")
    void testUnwrapDollarBrace() {
        ExpressionNode node = parser.parse("${42}");
        assertEquals(42, node.evaluate(null));
    }

    @Test
    @DisplayName("解析裸表达式（无 ${} 包裹）")
    void testBareExpression() {
        ExpressionNode node = parser.parse("42");
        assertEquals(42, node.evaluate(null));
    }

    @Test
    @DisplayName("解析 ${} 内含空格")
    void testUnwrapWithSpaces() {
        ExpressionNode node = parser.parse("${ 10 + 5 }");
        assertEquals(15.0, node.evaluate(null));
    }

    @Test
    @DisplayName("null 表达式抛出异常")
    void testNullExpressionThrows() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(null));
    }

    // ========== 条件求值 ==========

    @Test
    @DisplayName("evaluate 方法：使用 Map 上下文")
    void testEvaluateWithMap() {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("age", 25);
        Object result = parser.evaluate("${age > 18}", ctx);
        assertEquals(true, result);
    }

    @Test
    @DisplayName("evaluate 方法：使用 EvaluationContext")
    void testEvaluateWithContext() {
        EvaluationContext ctx = new EvaluationContext();
        ctx.bind("age", 25);
        ctx.bind("status", "active");
        Object result = parser.evaluate("${age > 18}", ctx);
        assertEquals(true, result);
    }

    @Test
    @DisplayName("test 方法：条件为 true")
    void testConditionTrue() {
        EvaluationContext ctx = new EvaluationContext().bind("x", 10).bind("y", 5);
        assertTrue(parser.test("${x > y}", ctx));
    }

    @Test
    @DisplayName("test 方法：条件为 false")
    void testConditionFalse() {
        EvaluationContext ctx = new EvaluationContext().bind("x", 3).bind("y", 5);
        assertFalse(parser.test("${x > y}", ctx));
    }

    @Test
    @DisplayName("复杂条件表达式：&& 和 ||")
    void testComplexCondition() {
        EvaluationContext ctx = new EvaluationContext()
                .bind("age", 25)
                .bind("score", 80);

        assertTrue(parser.test("${age > 18 && score > 60}", ctx));
        assertFalse(parser.test("${age > 30 && score > 60}", ctx));
        assertTrue(parser.test("${age > 30 || score > 60}", ctx));
    }

    // ========== EvaluationContext 绑定 ==========

    @Test
    @DisplayName("EvaluationContext 参数绑定")
    void testParamBinding() {
        EvaluationContext ctx = new EvaluationContext().bindParams("Alice", 25);
        assertEquals("Alice", ctx.get("param[0]"));
        assertEquals("Alice", ctx.get("arg0"));
        assertEquals(25, ctx.get("param[1]"));
        assertEquals(25, ctx.get("arg1"));
    }

    @Test
    @DisplayName("EvaluationContext this 和 return 绑定")
    void testThisAndReturnBinding() {
        EvaluationContext ctx = new EvaluationContext()
                .bindThis("thisObj")
                .bindReturn(42);
        assertEquals("thisObj", ctx.get("this"));
        assertEquals(42, ctx.get("return"));
    }

    @Test
    @DisplayName("EvaluationContext clear 复用")
    void testContextClear() {
        EvaluationContext ctx = new EvaluationContext().bind("x", 1);
        assertTrue(ctx.contains("x"));
        ctx.clear();
        assertFalse(ctx.contains("x"));
    }

    // ========== 浮点数解析 ==========

    @Test
    @DisplayName("浮点数词法解析")
    void testFloatTokenization() {
        ExpressionNode node = parser.parse("3.14");
        Object result = node.evaluate(null);
        assertEquals(3.14, ((Number) result).doubleValue(), 0.001);
    }

    @Test
    @DisplayName("浮点数比较")
    void testFloatComparison() {
        assertTrue(parser.test("${3.14 > 3}", null));
        assertFalse(parser.test("${2.5 > 3}", null));
    }

    // ========== 单引号字符串 ==========

    @Test
    @DisplayName("单引号字符串解析")
    void testSingleQuoteString() {
        Object result = parser.evaluate("'hello'", null);
        assertEquals("hello", result);
    }

    @Test
    @DisplayName("单引号字符串比较")
    void testSingleQuoteComparison() {
        EvaluationContext ctx = new EvaluationContext().bind("status", "active");
        assertTrue(parser.test("${status == 'active'}", ctx));
        assertFalse(parser.test("${status == 'inactive'}", ctx));
    }

    // ========== toBoolean 边界 ==========

    @Test
    @DisplayName("链式属性访问")
    void testPropertyAccess() {
        // Mock a user object
        class User {
            private String name = "Alice";
            public int getAge() { return 25; }
        }
        User user = new User();
        EvaluationContext ctx = new EvaluationContext()
                .bind("user", user)
                .bindParams(user); // param[0] is user

        // test field access
        assertEquals("Alice", parser.evaluate("${user.name}", ctx));
        // test getter access
        assertEquals(25, parser.evaluate("${user.age}", ctx));
        // test param[0] access
        assertEquals("Alice", parser.evaluate("${param[0].name}", ctx));
        // test chained comparison
        assertTrue(parser.test("${param[0].age >= 18}", ctx));
    }

    @Test
    @DisplayName("toBoolean: null → false")
    void testToBooleanNull() {
        EvaluationContext ctx = new EvaluationContext().bind("x", null);
        // x 为 null，null 参与比较不应崩溃
        assertFalse(parser.test("${x == 1}", ctx));
    }

    @Test
    @DisplayName("toBoolean: 数字 0 → false, 非0 → true")
    void testToBooleanNumber() {
        // 直接用数字做 test
        assertFalse(parser.test("0", null));
        assertTrue(parser.test("42", null));
    }
}
