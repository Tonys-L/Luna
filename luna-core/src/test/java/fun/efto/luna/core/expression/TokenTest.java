/**
 * @author ：Tony.L(<286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
package fun.efto.luna.core.expression;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TokenTest {

    @Test
    public void testTokenCreation() {
        // 测试 Token 创建
        Token token = new Token(Token.Type.IDENTIFIER, "userId");
        assertEquals(Token.Type.IDENTIFIER, token.getType(), "Token 类型应该正确");
        assertEquals("userId", token.getValue(), "Token 值应该正确");
    }

    @Test
    public void testTokenToString() {
        // 测试 Token 的 toString 方法
        Token token = new Token(Token.Type.NUMBER, "100");
        String toString = token.toString();
        assertTrue(toString.contains("NUMBER"), "toString 应该包含类型信息");
        assertTrue(toString.contains("100"), "toString 应该包含值信息");
    }
}
