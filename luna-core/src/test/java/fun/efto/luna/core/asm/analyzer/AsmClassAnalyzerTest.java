/**
 * @author ：Tony.L(<286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
package fun.efto.luna.core.asm.analyzer;

import fun.efto.luna.core.analyzer.ClassAnalysisResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AsmClassAnalyzerTest {

    private AsmClassAnalyzer analyzer;

    @BeforeEach
    public void setUp() {
        analyzer = new AsmClassAnalyzer();
    }

    @Test
    public void testAnalyze() {
        // 测试分析方法
        byte[] classBytes = new byte[0];
        ClassAnalysisResult result = analyzer.analyze(classBytes);

        // 验证分析结果不为 null
        assertNotNull(result, "分析结果应该不为 null");
        // 验证分析结果的基本属性
        assertEquals("", result.getClassName(), "类名应该为空字符串");
        assertTrue(result.getFields().isEmpty(), "字段列表应该为空");
        assertTrue(result.getMethods().isEmpty(), "方法列表应该为空");
        assertEquals("", result.getSuperClass(), "父类名应该为空字符串");
        assertTrue(result.getInterfaces().isEmpty(), "接口列表应该为空");
        assertEquals(0, result.getAccessFlags(), "访问标志应该为 0");
    }
}
