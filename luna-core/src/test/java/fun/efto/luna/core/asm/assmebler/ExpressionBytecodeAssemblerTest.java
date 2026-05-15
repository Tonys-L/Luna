package fun.efto.luna.core.asm.assmebler;

import fun.efto.luna.core.asm.AsmInjectionContext.LocalVarInfo;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/01 12:00
 */
public class ExpressionBytecodeAssemblerTest {

    private static final Pattern COMBINED_REF_PATTERN = Pattern.compile("\\$(\\d+|[a-zA-Z_]\\w*)");

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

    @Test
    public void testCombinedRefPatternMatchesDigits() {
        Matcher matcher = COMBINED_REF_PATTERN.matcher("$1 $2 $3");
        int count = 0;
        while (matcher.find()) {
            count++;
            assertTrue(matcher.group(1).matches("\\d+"));
        }
        assertEquals(3, count);
    }

    @Test
    public void testCombinedRefPatternMatchesIdentifiers() {
        Matcher matcher = COMBINED_REF_PATTERN.matcher("$result $count $user_name");
        int count = 0;
        List<String> names = new ArrayList<>();
        while (matcher.find()) {
            count++;
            names.add(matcher.group(1));
        }
        assertEquals(3, count);
        assertEquals(Arrays.asList("result", "count", "user_name"), names);
    }

    @Test
    public void testCombinedRefPatternMixed() {
        Matcher matcher = COMBINED_REF_PATTERN.matcher("name=$1, result=$result");
        List<String> refs = new ArrayList<>();
        while (matcher.find()) {
            refs.add(matcher.group(1));
        }
        assertEquals(2, refs.size());
        assertEquals("1", refs.get(0));
        assertEquals("result", refs.get(1));
    }

    @Test
    public void testLocalVarInfoCreation() {
        LocalVarInfo info = new LocalVarInfo("user", "Ljava/lang/String;", 2);
        assertEquals("user", info.getName());
        assertEquals("Ljava/lang/String;", info.getDescriptor());
        assertEquals(2, info.getSlot());
    }

    @Test
    public void testLocalVarInfoFields() {
        LocalVarInfo info1 = new LocalVarInfo("count", "I", 3);
        LocalVarInfo info2 = new LocalVarInfo("count", "I", 3);
        assertEquals(info1.getName(), info2.getName());
        assertEquals(info1.getDescriptor(), info2.getDescriptor());
        assertEquals(info1.getSlot(), info2.getSlot());
    }

    @Test
    public void testParseExpressionLocalVarOnly() {
        String expression = "result=$result";
        List<String> refs = extractRefs(expression);
        assertEquals(1, refs.size());
        assertEquals("result", refs.get(0));
        assertFalse(refs.get(0).matches("\\d+"));
    }

    @Test
    public void testParseExpressionParamOnly() {
        String expression = "name=$1, age=$2";
        List<String> refs = extractRefs(expression);
        assertEquals(2, refs.size());
        assertEquals("1", refs.get(0));
        assertEquals("2", refs.get(1));
    }

    @Test
    public void testParseExpressionMixed() {
        String expression = "name=$1, result=$result, count=$count";
        List<String> refs = extractRefs(expression);
        assertEquals(3, refs.size());
        assertEquals("1", refs.get(0));
        assertEquals("result", refs.get(1));
        assertEquals("count", refs.get(2));
    }

    @Test
    public void testParseExpressionUnderscoreVar() {
        String expression = "val=$_temp";
        List<String> refs = extractRefs(expression);
        assertEquals(1, refs.size());
        assertEquals("_temp", refs.get(0));
    }

    @Test
    public void testParseExpressionNoRefs() {
        String expression = "hello world";
        List<String> refs = extractRefs(expression);
        assertTrue(refs.isEmpty());
    }

    @Test
    public void testLocalVarInfoFindByName() {
        List<LocalVarInfo> vars = Arrays.asList(
                new LocalVarInfo("user", "Ljava/lang/String;", 2),
                new LocalVarInfo("count", "I", 3),
                new LocalVarInfo("result", "Ljava/lang/Object;", 4)
        );

        LocalVarInfo found = null;
        for (LocalVarInfo lv : vars) {
            if (lv.getName().equals("result")) {
                found = lv;
                break;
            }
        }
        assertNotNull(found);
        assertEquals("Ljava/lang/Object;", found.getDescriptor());
        assertEquals(4, found.getSlot());
    }

    @Test
    public void testLocalVarInfoNotFound() {
        List<LocalVarInfo> vars = Arrays.asList(
                new LocalVarInfo("user", "Ljava/lang/String;", 2),
                new LocalVarInfo("count", "I", 3)
        );

        LocalVarInfo found = null;
        for (LocalVarInfo lv : vars) {
            if (lv.getName().equals("nonexistent")) {
                found = lv;
                break;
            }
        }
        assertNull(found);
    }

    @Test
    public void testRefDigitVsIdentifierDisambiguation() {
        String expression = "$1 $result $2 $count";
        List<String[]> results = new ArrayList<>();
        Matcher matcher = COMBINED_REF_PATTERN.matcher(expression);
        while (matcher.find()) {
            String ref = matcher.group(1);
            boolean isDigit = ref.matches("\\d+");
            results.add(new String[]{ref, isDigit ? "param" : "localvar"});
        }
        assertEquals(4, results.size());
        assertEquals("param", results.get(0)[1]);
        assertEquals("localvar", results.get(1)[1]);
        assertEquals("param", results.get(2)[1]);
        assertEquals("localvar", results.get(3)[1]);
    }

    private List<String> extractRefs(String expression) {
        List<String> refs = new ArrayList<>();
        Matcher matcher = COMBINED_REF_PATTERN.matcher(expression);
        while (matcher.find()) {
            refs.add(matcher.group(1));
        }
        return refs;
    }
}
