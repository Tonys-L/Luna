package fun.efto.luna.core.plugin.builtin.invocation.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since : 2026/07/05 22:00
 */
public class StaticCallGraphAnalyzerTest {

    // ===== CallGraphFilter Tests =====

    @Test
    void testCallGraphFilterJdkClasses() {
        CallGraphFilter filter = new CallGraphFilter();

        assertFalse(filter.shouldTrace("java.lang.String"), "java.lang should be filtered");
        assertFalse(filter.shouldTrace("java.util.ArrayList"), "java.util should be filtered");
        assertFalse(filter.shouldTrace("javax.servlet.http.HttpServlet"), "javax should be filtered");
        assertFalse(filter.shouldTrace("sun.misc.Unsafe"), "sun should be filtered");
        assertFalse(filter.shouldTrace("com.sun.tools.javac.Main"), "com.sun should be filtered");
    }

    @Test
    void testCallGraphFilterLunaClasses() {
        CallGraphFilter filter = new CallGraphFilter();

        assertFalse(filter.shouldTrace("fun.efto.luna.core.probe.ProbeOutput"), "Luna core classes should be filtered");
        assertFalse(filter.shouldTrace("fun.efto.luna.core.plugin.PluginContext"), "Luna plugin classes should be filtered");
        assertFalse(filter.shouldTrace("fun.efto.luna.core.bytecode.asm.injector.BytecodeInjector"), "Luna bytecode classes should be filtered");
    }

    @Test
    void testCallGraphFilterBusinessClasses() {
        CallGraphFilter filter = new CallGraphFilter();

        assertTrue(filter.shouldTrace("com.example.Service"), "Business class com.example should pass");
        assertTrue(filter.shouldTrace("org.example.dao.UserDao"), "Business class org.example should pass");
        assertTrue(filter.shouldTrace("io.quarkus.runtime.StartupEvent"), "Non-JDK non-Luna class should pass");
        assertTrue(filter.shouldTrace("org.springframework.context.ApplicationContext"), "Spring class should pass");
    }

    @Test
    void testCallGraphFilterEdgeCases() {
        CallGraphFilter filter = new CallGraphFilter();

        // "java" without dot should NOT be filtered (starts with "java", not "java.")
        assertTrue(filter.shouldTrace("javafx.application.Application"), "javafx should not be filtered (no dot after java)");
        assertFalse(filter.shouldTrace("java."), "java. prefix should be filtered");
    }

    // ===== CallGraph Tests =====

    @Test
    void testCallGraphBasicStructure() {
        CallGraph root = new CallGraph("com.example.Service", "doWork", "()V");
        assertEquals("com.example.Service", root.getClassName());
        assertEquals("doWork", root.getMethodName());
        assertEquals("()V", root.getMethodDesc());
        assertTrue(root.getCallees().isEmpty(), "New CallGraph should have no callees");
    }

    @Test
    void testCallGraphAddCallee() {
        CallGraph root = new CallGraph("com.example.Service", "doWork", "()V");
        CallGraph callee = new CallGraph("com.example.Dao", "query", "()Ljava/util/List;");
        root.addCallee(callee);

        assertEquals(1, root.getCallees().size());
        assertSame(callee, root.getCallees().get(0));
    }

    @Test
    void testCallGraphNestedStructure() {
        CallGraph root = new CallGraph("com.example.Service", "doWork", "()V");
        CallGraph callee1 = new CallGraph("com.example.Dao", "query", "()Ljava/util/List;");
        CallGraph callee2 = new CallGraph("com.example.Util", "process", "(Ljava/util/List;)V");
        root.addCallee(callee1);
        callee1.addCallee(callee2);

        assertEquals(1, root.getCallees().size());
        assertEquals(1, root.getCallees().get(0).getCallees().size());
        assertEquals("com.example.Util", root.getCallees().get(0).getCallees().get(0).getClassName());
    }

    // ===== StaticCallGraphAnalyzer Tests with Null PluginContext =====

    @Test
    void testAnalyzeWithNullClassBytes() {
        // PluginContext that returns null for all class bytes
        StaticCallGraphAnalyzer analyzer = new StaticCallGraphAnalyzer(
            new NullPluginContext(), 3
        );

        CallGraph result = analyzer.analyze("com.example.Service", "doWork", "()V");
        assertNotNull(result, "Analyze should return a CallGraph even with null bytes");
        assertEquals("com.example.Service", result.getClassName());
        assertEquals("doWork", result.getMethodName());
        assertTrue(result.getCallees().isEmpty(), "Should have no callees when class bytes are null");
    }

    @Test
    void testAnalyzeDefaultMaxDepth() {
        // Verify default maxDepth is 5
        StaticCallGraphAnalyzer analyzer = new StaticCallGraphAnalyzer(new NullPluginContext());
        CallGraph result = analyzer.analyze("com.example.Service", "doWork", "()V");
        assertNotNull(result);
    }

    /**
     * Minimal PluginContext that returns null for all class bytes and empty set for loaded classes.
     */
    private static class NullPluginContext implements fun.efto.luna.core.plugin.PluginContext {

        @Override
        public void registerInjectionLocation(fun.efto.luna.core.injection.target.InjectionLocation location) {}

        @Override
        public void registerInjectionLocation(fun.efto.luna.core.injection.target.InjectionLocation location,
                                               fun.efto.luna.core.plugin.InjectionLocationUIDescriptor uiDescriptor) {}

        @Override
        public void registerInjector(fun.efto.luna.core.injection.target.InjectionLocation location,
                                      fun.efto.luna.core.bytecode.asm.injector.BytecodeInjector injector) {}

        @Override
        public void registerProbeHandler(fun.efto.luna.core.plugin.ProbeHandler handler) {}

        @Override
        public void registerCodeEngine(fun.efto.luna.core.injection.CodeEngine engine) {}

        @Override
        public void registerBootstrapClass(String internalName) {}

        @Override
        public fun.efto.luna.core.analysis.analyzer.ClassAnalyzer getClassAnalyzer() { return null; }

        @Override
        public fun.efto.luna.core.analysis.decompile.Decompiler getDecompiler() { return null; }

        @Override
        public fun.efto.luna.core.plugin.LogEmitter getLogEmitter() { return null; }

        @Override
        public fun.efto.luna.core.probe.RingBuffer<fun.efto.luna.core.probe.ProbeMessage> getLogBuffer() { return null; }

        @Override
        public fun.efto.luna.core.injection.port.Retransformer getRetransformer() { return className -> {}; }

        @Override
        public java.util.Map<String, String> getPluginConfig() { return new java.util.HashMap<>(); }

        @Override
        public void savePluginConfig(java.util.Map<String, String> config) {}

        @Override
        public byte[] getClassBytes(String className) { return null; }

        @Override
        public java.util.Set<String> getLoadedClassNames() { return java.util.Collections.emptySet(); }

        @Override
        public String inject(fun.efto.luna.core.injection.InjectRequest request) { return null; }
    }
}
