/**
 * @author : Tony.L(286269159@qq.com)
 * @since : 2026/06/07 23:30
 */
package fun.efto.luna.core.bytecode.bytekit.adapter;

import fun.efto.luna.core.injection.*;
import fun.efto.luna.core.injection.target.InjectionLocation;
import fun.efto.luna.core.plugin.ProbeHandler;
import fun.efto.luna.core.plugin.builtin.CoreModuleInitializer;
import fun.efto.luna.core.plugin.builtin.method.MethodInjectionLocation;
import fun.efto.luna.core.plugin.builtin.trace.TraceProbeHandler;
import fun.efto.luna.core.plugin.registry.InjectionTypeRegistry;
import fun.efto.luna.core.plugin.registry.ProbeHandlerRegistry;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjectorRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class TraceBytecodeInjectionTest {

    public static class TargetService {
        public String doWork(String input) {
            return "result:" + input;
        }
    }

    static class TestClassLoader extends ClassLoader {
        public Class<?> defineClass(String name, byte[] b) {
            return defineClass(name, b, 0, b.length);
        }
    }

    @BeforeAll
    static void initRegistries() {
        CoreModuleInitializer.initialize();
        // Initialize LogPlugin to register ExpressionCodeEngine
        try {
            java.util.ServiceLoader<fun.efto.luna.core.plugin.LunaPlugin> loader =
                    java.util.ServiceLoader.load(fun.efto.luna.core.plugin.LunaPlugin.class);
            fun.efto.luna.core.plugin.LunaPlugin logPlugin = null;
            for (fun.efto.luna.core.plugin.LunaPlugin plugin : loader) {
                if ("log".equals(plugin.getId())) {
                    logPlugin = plugin;
                    break;
                }
            }
            if (logPlugin != null) {
                fun.efto.luna.core.plugin.lifecycle.PluginManagerImpl pm = new fun.efto.luna.core.plugin.lifecycle.PluginManagerImpl(
                        new fun.efto.luna.core.plugin.lifecycle.ReadyGate(),
                        new fun.efto.luna.core.plugin.DefaultLogEmitter(),
                        fun.efto.luna.core.probe.ProbeOutput.BUFFER,
                        null, null, null
                );
                java.util.List<fun.efto.luna.core.plugin.LunaPlugin> plugins = new java.util.ArrayList<>();
                plugins.add(logPlugin);
                pm.initializeAll(plugins);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize LogPlugin", e);
        }
        ProbeHandlerRegistry.getInstance().register(new TraceProbeHandler());
    }

    private static byte[] getClassBytecode(Class<?> clazz) throws Exception {
        String className = clazz.getName().replace('.', '/') + ".class";
        try (InputStream is = clazz.getClassLoader().getResourceAsStream(className)) {
            assertNotNull(is, "Cannot read bytecode for: " + className);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = is.read(buffer)) != -1) {
                bos.write(buffer, 0, n);
            }
            return bos.toByteArray();
        }
    }

    @Test
    void testTraceAroundInjectionProducesValidBytecode() throws Exception {
        // TRACE 使用 method_around，一次注入同时生成 onTraceStart + onTraceEnd
        PersistentInjection pi = new PersistentInjection();
        pi.setClazz(TargetService.class.getName());
        pi.setMethodName("doWork");
        pi.setMethodDescriptor("(Ljava/lang/String;)Ljava/lang/String;");
        pi.setInjectionLocation("method_around");
        pi.setProbeType("TRACE");
        pi.setCodeType("EXPRESSION");
        pi.setCode("0"); // threshold = 0, 全部输出

        InjectionPoint point = InjectionPointFactory.create(pi);
        assertNotNull(point.getCode(), "CompiledCode should not be null for TRACE around");

        ByteKitAroundInjector injector = (ByteKitAroundInjector) BytecodeInjectorRegistry.getInstance()
                .get(MethodInjectionLocation.AROUND).orElse(null);
        assertNotNull(injector);

        ProbeHandler probeHandler = ProbeHandlerRegistry.getInstance().get("TRACE").orElse(null);
        assertNotNull(probeHandler);

        byte[] original = getClassBytecode(TargetService.class);
        byte[] transformed = injector.inject(point.getCode(), probeHandler, new InjectionContext(point), original);

        assertNotNull(transformed);
        assertTrue(transformed.length > 0);

        assertDoesNotThrow(() -> {
            new ClassReader(transformed).accept(new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9) {}, 0);
        });

        TestClassLoader cl = new TestClassLoader();
        Class<?> enhancedClass = cl.defineClass(TargetService.class.getName(), transformed);
        Object instance = enhancedClass.getDeclaredConstructor().newInstance();
        Method method = enhancedClass.getMethod("doWork", String.class);

        String result = (String) method.invoke(instance, "test");
        assertEquals("result:test", result, "Method should still return original value after TRACE around injection");
    }
}
