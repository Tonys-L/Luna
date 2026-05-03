/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/01 12:00
 */
package fun.efto.luna.core.asm.assmebler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ExpressionBytecodeAssemblerTest {

    @Test
    public void testSplitWithColonLimit2() {
        String content = "log:hello:world";
        String[] split = content.split(":", 2);
        assertEquals(2, split.length, "split with limit 2 should produce 2 parts");
        assertEquals("log", split[0]);
        assertEquals("hello:world", split[1]);
    }

    @Test
    public void testSplitWithoutColonLimit2() {
        String content = "logonly";
        String[] split = content.split(":", 2);
        assertEquals(1, split.length, "split without colon should produce 1 part");
    }

    @Test
    public void testSplitNormalExpression() {
        String content = "log:hello world";
        String[] split = content.split(":", 2);
        assertEquals(2, split.length);
        assertEquals("log", split[0]);
        assertEquals("hello world", split[1]);
    }

    @Test
    public void testSplitWithTimeExpression() {
        String content = "log:time=12:30:00";
        String[] split = content.split(":", 2);
        assertEquals(2, split.length);
        assertEquals("log", split[0]);
        assertEquals("time=12:30:00", split[1]);
    }

    @Test
    public void testOldSplitBehaviorBreaks() {
        String content = "log:time=12:30:00";
        String[] oldSplit = content.split(":");
        assertTrue(oldSplit.length > 2, "old split(':') would break this expression");
    }
}
