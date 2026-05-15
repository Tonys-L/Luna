package fun.efto.luna.core.injection.code.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
class CodeTypeTest {

    @Test
    void testStaticConstants() {
        assertNotNull(CodeType.JAVA);
        assertNotNull(CodeType.EXPRESSION);
        assertNotNull(CodeType.SNAPSHOT);

        assertEquals("java", CodeType.JAVA.getName());
        assertEquals("expression", CodeType.EXPRESSION.getName());
        assertEquals("snapshot", CodeType.SNAPSHOT.getName());

        assertEquals("Java 代码", CodeType.JAVA.getDescription());
        assertEquals("表达式代码", CodeType.EXPRESSION.getDescription());
        assertEquals("快照代码", CodeType.SNAPSHOT.getDescription());
    }

    @Test
    void testValueOf() {
        assertEquals(CodeType.EXPRESSION, CodeType.valueOf("EXPRESSION"));
        assertEquals(CodeType.SNAPSHOT, CodeType.valueOf("SNAPSHOT"));
        assertEquals(CodeType.JAVA, CodeType.valueOf("JAVA"));
    }

    @Test
    void testValueOfUnknown() {
        assertThrows(IllegalArgumentException.class, () -> CodeType.valueOf("unknown"));
        assertThrows(NullPointerException.class, () -> CodeType.valueOf(null));
    }

    @Test
    void testToString() {
        assertEquals("java", CodeType.JAVA.toString());
        assertEquals("expression", CodeType.EXPRESSION.toString());
        assertEquals("snapshot", CodeType.SNAPSHOT.toString());
    }

    @Test
    void testJsonCompatibility() {
        assertEquals("expression", CodeType.EXPRESSION.getName());
        assertEquals("snapshot", CodeType.SNAPSHOT.getName());
        assertEquals("java", CodeType.JAVA.getName());
    }

    @Test
    void testEqualsAndHashCode() {
        CodeType expr1 = CodeType.EXPRESSION;
        CodeType expr2 = CodeType.valueOf("EXPRESSION");
        assertEquals(expr1, expr2);
        assertEquals(expr1.hashCode(), expr2.hashCode());

        assertNotEquals(CodeType.EXPRESSION, CodeType.SNAPSHOT);
        assertNotEquals(CodeType.EXPRESSION, CodeType.JAVA);
    }
}
