package fun.efto.luna.core.bytecode.asm.analyzer;

import fun.efto.luna.core.analysis.analyzer.ClassAnalysisResult;
import fun.efto.luna.core.analysis.analyzer.ClassAnalysisResult.FieldInfo;
import fun.efto.luna.core.analysis.analyzer.ClassAnalysisResult.MethodInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since : 2026/03/29 02:30
 */
public class AsmClassAnalyzerTest {

    private AsmClassAnalyzer analyzer;

    @BeforeEach
    public void setUp() {
        analyzer = new AsmClassAnalyzer();
    }

    @Test
    public void testAnalyzeEmptyBytes() {
        byte[] classBytes = new byte[0];
        ClassAnalysisResult result = analyzer.analyze(classBytes);

        assertNotNull(result, "分析结果应该不为 null");
        assertEquals("", result.getClassName(), "类名应该为空字符串");
        assertTrue(result.getFields().isEmpty(), "字段列表应该为空");
        assertTrue(result.getMethods().isEmpty(), "方法列表应该为空");
    }

    @Test
    public void testAnalyzeNullBytes() {
        ClassAnalysisResult result = analyzer.analyze(null);

        assertNotNull(result, "分析结果应该不为 null");
        assertEquals("", result.getClassName(), "类名应该为空字符串");
    }

    @Test
    public void testAnalyzeInvalidBytes() {
        byte[] invalidBytes = new byte[]{1, 2, 3, 4, 5};
        ClassAnalysisResult result = analyzer.analyze(invalidBytes);

        assertNotNull(result, "分析结果应该不为 null");
        assertEquals("", result.getClassName(), "无效字节码应返回空结果");
    }

    @Test
    public void testAnalyzeRealClass() throws Exception {
        byte[] classBytes = getClassBytes(TestSubject.class);
        ClassAnalysisResult result = analyzer.analyze(classBytes);

        assertNotNull(result);
        assertEquals(TestSubject.class.getName(), result.getClassName());
        assertEquals(Object.class.getName(), result.getSuperClass());
        assertFalse(result.getFields().isEmpty(), "应该有字段");
        assertFalse(result.getMethods().isEmpty(), "应该有方法");
    }

    @Test
    public void testAnalyzeFields() throws Exception {
        byte[] classBytes = getClassBytes(TestSubject.class);
        ClassAnalysisResult result = analyzer.analyze(classBytes);

        boolean hasNameField = result.getFields().stream()
                .anyMatch(f -> f.getName().equals("name"));
        boolean hasAgeField = result.getFields().stream()
                .anyMatch(f -> f.getName().equals("age"));
        assertTrue(hasNameField, "应该有 name 字段");
        assertTrue(hasAgeField, "应该有 age 字段");
    }

    @Test
    public void testAnalyzeMethods() throws Exception {
        byte[] classBytes = getClassBytes(TestSubject.class);
        ClassAnalysisResult result = analyzer.analyze(classBytes);

        boolean hasGetName = result.getMethods().stream()
                .anyMatch(m -> m.getName().equals("getName"));
        boolean hasSetName = result.getMethods().stream()
                .anyMatch(m -> m.getName().equals("setName"));
        boolean hasConstructor = result.getMethods().stream()
                .anyMatch(m -> m.getName().equals("<init>"));
        assertTrue(hasGetName, "应该有 getName 方法");
        assertTrue(hasSetName, "应该有 setName 方法");
        assertTrue(hasConstructor, "应该有构造函数");
    }

    private byte[] getClassBytes(Class<?> clazz) throws Exception {
        String className = clazz.getName();
        String resourcePath = className.replace('.', '/') + ".class";
        java.io.InputStream inputStream = clazz.getClassLoader().getResourceAsStream(resourcePath);
        assertNotNull(inputStream, "无法读取类字节码: " + className);
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
        } finally {
            inputStream.close();
        }
    }

    @SuppressWarnings("unused")
    public static class TestSubject {
        private String name;
        private int age;

        public TestSubject() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }
}
