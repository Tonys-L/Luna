package fun.efto.luna.core.bytecode.asm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/31 21:00
 */
@DisplayName("VariableInfo 测试")
class VariableInfoTest {

    @Nested
    @DisplayName("构造器与 Getter")
    class ConstructorTests {

        @Test
        @DisplayName("构造器正确赋值并通过 getter 返回")
        void testConstructorAndGetters() {
            VariableInfo info = new VariableInfo("name", "Ljava/lang/String;", 1);

            assertEquals("name", info.getName());
            assertEquals("Ljava/lang/String;", info.getDescriptor());
            assertEquals(1, info.getSlot());
        }

        @Test
        @DisplayName("基本类型变量构造正确")
        void testPrimitiveType() {
            VariableInfo info = new VariableInfo("count", "I", 3);

            assertEquals("count", info.getName());
            assertEquals("I", info.getDescriptor());
            assertEquals(3, info.getSlot());
        }
    }

    @Nested
    @DisplayName("equals 与 hashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("相同参数的实例 equals 且 hashCode 一致")
        void testEqualInstances() {
            VariableInfo a = new VariableInfo("x", "I", 2);
            VariableInfo b = new VariableInfo("x", "I", 2);

            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("不同 name 不相等")
        void testDifferentName() {
            VariableInfo a = new VariableInfo("x", "I", 2);
            VariableInfo b = new VariableInfo("y", "I", 2);

            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("不同 descriptor 不相等")
        void testDifferentDescriptor() {
            VariableInfo a = new VariableInfo("x", "I", 2);
            VariableInfo b = new VariableInfo("x", "J", 2);

            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("不同 slot 不相等")
        void testDifferentSlot() {
            VariableInfo a = new VariableInfo("x", "I", 2);
            VariableInfo b = new VariableInfo("x", "I", 3);

            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("与 null 不相等")
        void testNotEqualToNull() {
            VariableInfo info = new VariableInfo("x", "I", 2);

            assertNotEquals(null, info);
        }

        @Test
        @DisplayName("与不同类型对象不相等")
        void testNotEqualToOtherType() {
            VariableInfo info = new VariableInfo("x", "I", 2);

            assertNotEquals("x", info);
        }

        @Test
        @DisplayName("自反性: x.equals(x) == true")
        void testReflexivity() {
            VariableInfo info = new VariableInfo("x", "I", 2);

            assertEquals(info, info);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("toString 包含所有字段信息")
        void testToString() {
            VariableInfo info = new VariableInfo("count", "I", 3);
            String str = info.toString();

            assertTrue(str.contains("count"));
            assertTrue(str.contains("I"));
            assertTrue(str.contains("3"));
        }
    }
}
