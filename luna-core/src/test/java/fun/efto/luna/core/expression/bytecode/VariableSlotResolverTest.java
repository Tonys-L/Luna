package fun.efto.luna.core.expression.bytecode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ：Tony.L(<286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
public class VariableSlotResolverTest {

    private VariableSlotResolver resolver;

    @BeforeEach
    public void setUp() {
        resolver = new VariableSlotResolver();
    }

    @Test
    public void testRegisterAndGetSlot() {
        // 测试注册和获取变量槽位
        resolver.registerVariable("userId", 0);
        resolver.registerVariable("age", 1);

        assertEquals(0, resolver.getSlot("userId"), "userId 的槽位应该是 0");
        assertEquals(1, resolver.getSlot("age"), "age 的槽位应该是 1");
        assertEquals(-1, resolver.getSlot("unknown"), "未知变量的槽位应该是 -1");
    }

    @Test
    public void testContainsVariable() {
        // 测试检查变量是否存在
        resolver.registerVariable("userId", 0);

        assertTrue(resolver.containsVariable("userId"), "应该存在 userId 变量");
        assertFalse(resolver.containsVariable("unknown"), "不应该存在 unknown 变量");
    }
}
